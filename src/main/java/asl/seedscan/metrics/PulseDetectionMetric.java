package asl.seedscan.metrics;

import static asl.utils.NumericUtils.demeanInPlace;
import static asl.utils.NumericUtils.detrend;
import static asl.utils.NumericUtils.detrendEnds;
import static asl.utils.NumericUtils.getMean;
import static java.lang.Math.PI;
import static java.util.Collections.sort;

import asl.metadata.Channel;
import asl.metadata.ChannelKey;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.ChannelMeta.ResponseUnits;
import asl.metadata.meta_new.ChannelMetaException;
import asl.utils.FFTResult;
import asl.utils.FilterUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metric to implement the backend for pulse detection operations.
 * The backend is used as a base for count and max peak metrics for these pulses.
 * Peak detection is done by taking timeseries data and getting ground motion
 * cross-correlation with a pulse. Checks are done to make sure the returned pulse aren't actual
 * seismic phenomena, and then collect the retained pulses for reporting by the implementing metric.
 *
 * The full procedure is as follows:
 * Full day data (plus some additional data crossing the day boundary if available) is
 * detrended, tapered, and has its response removed and placed into acceleration.
 * The data is then low-pass filtered and its correlation with a pulse is taken, which is used
 * to identify potential pulses.
 * Points from here are filtered out by exclusion criterion.
 * The first is an envelope constraint,
 * which takes a window over each point of data, gets the mean of each 1/3-percentile of it,
 * and checks that the differences of two points equidistant from that point have the same sign.
 * The second is a sharpness constraint,
 * which checks that the magnitude of a 60-second windowed average at a given point is at least
 * 4 times the magnitude of a 900-second windowed average (magnitude meaning sign is ignored).
 *
 * Points that are able to pass this filtering then get collected as a list of pulses, and those
 * values can then be filtered by constraints on the correlation and amplitude in a class that
 * implements a specific metric on that data such as pulse count or the peak amplitude.
 *
 * In order to prevent redundant calculation between metrics, we cache the results of these metrics
 * in a custom format, so one round of calculation allows us to determine both the largest pulse and
 * the total number of pulses that match the filtering criteria.
 *
 */
public abstract class PulseDetectionMetric extends Metric {

  private static final Logger logger = LoggerFactory.getLogger(PulseDetectionMetric.class);

  /**
   * Map from ChannelKeys to the pulse detection data, a list of lists of paired
   * correlations with their associated amplitudes.
   */
  protected Map<ChannelKey, PulseDetectionData> pulseDetectionResultMap;

  /**
   * Method to pass the pulse detection data from one implementing metric to another
   * @return Pulse Detection Data, a list of lists of amplitude-correlation pairs, see
   * {@link PulseDetectionData}
   */
  public Map<ChannelKey, PulseDetectionData> getPulseDetectionData() {
    return pulseDetectionResultMap;
  }

  /**
   * Method to pass in pulse detection data from a metric that has already calculated results for
   * a station. If the implementing metric runs over a channel not in the key set, then it will
   * create a new result for that channel and add it to the map.
   * @param cached Map of ChannelKeys to PulseDetectionData, see {@link PulseDetectionData}
   */
  public void setPulseDetectionData(Map<ChannelKey, PulseDetectionData> cached) {
    pulseDetectionResultMap = cached;
  }

  /**
   * Calculate the pulse detection metric for a given channel according to the procedure stated
   * in the prior documentation.
   *
   * @param channel Channel to run the metric calculation over
   * @return List of lists of amplitude-correlation pairs {@link PulseDetectionData}
   * @throws ChannelMetaException
   */
  public PulseDetectionData calculatePulseResults(Channel channel) throws ChannelMetaException {
    // instead of having the boundary be right at midnight, seedscan doesn't easily support getting
    // the previous day's data for doing centered differences on windowed estimations
    // instead pulse detections will be off such that the first 140 or so seconds of the next day
    // are included in the current day's metric, unless the data does not exist

    long start = metricData.getChannelData(channel).get(0).getStartTime();
    long end = Instant.ofEpochMilli(start).plus(280, ChronoUnit.SECONDS)
        .plus(1, ChronoUnit.DAYS).toEpochMilli();

    double[] trace = metricData.getWindowedData(channel, start, end);

    if (trace == null || trace.length == 0.) {
      // this happens if we're scanning the most recent full day of data, in which case we'll
      // just run on the data currently available to us to get some estimation of the values
      // though we will miss pulses that happen within ~5 minutes of the day boundary
      trace = metricData.getDetrendedPaddedDayData(channel);
      logger.info("Got full day data for channel [{}]", channel.toString());
    } else {
      trace = detrend(trace);
    }

    FFTResult.cosineTaper(trace, 0.05); // taper done in-place
    double sampleRate = stationMeta.getChannelMetadata(channel).getSampleRate();
    // remove response, convert to acceleration, detrend, and filter (order 4 butterworth)
    // here's the response removal block, includes using accel to switch units
    trace = detrendEnds(removeResponseOnTimeDomainData(trace,
        stationMeta.getChannelMetadata(channel), 0.001));
    // now detrend, taper again, and do a low-pass filter over the data
    demeanInPlace(trace);
    trace = detrend(trace);
    FFTResult.cosineTaper(trace, 0.05); // taper done in-place
    double filterBand = 1. / 40;
    trace = FilterUtils.lowPassFilter(trace, sampleRate, filterBand, 4, true);

    // correl is the cross-correlation value, called xcC in the matlab code
    // scale is the scale factor thereof, used to get the pulse amplitude at the given point
    double[] correl, scale;
    {
      double[][] crossCorr = computeCrossCorrelationWithStep(trace, sampleRate);
      assert (crossCorr.length == 2);
      correl = crossCorr[0];
      scale = crossCorr[1];
      assert(correl.length == scale.length);
    }

    // use moving average to remove long-period (~1 hr) trend for constraint evaluation
    double[] meanOffset = getCenteredMovingAverage(trace, (int) (3600 * sampleRate));
    double[] traceFiltered = new double[meanOffset.length];
    for (int i = 0; i < meanOffset.length; ++i) {
      traceFiltered[i] = trace[i] - meanOffset[i];
    }

    // first constraint for pulses: sign of difference between points on either side of 140s
    // must match the sign of the average; this constraint would not hold in a case of
    // sharp changes, such as during an actual earthquake (in which case the pulse is not an
    // artifact of the sensor but a reflection of ground motion behavior).

    // this set will keep track of points which pass the following constraints on pulse data
    // and will initially contain all points within the bounds of the correlation data
    Set<Integer> pointInclusions = envelopeConstraint(traceFiltered, sampleRate);

    logger.info("Number of points to check: {}", trace.length);
    logger.info("Number of points that pass envelope test: {}", pointInclusions.size());

    // next exclusion criteria: sharpness constraint.
    pointInclusions.removeAll(
        sharpnessConstraint(traceFiltered, sampleRate, pointInclusions.toArray(new Integer[]{})));

    logger.info("Number of points that also pass sharpness test: {}", pointInclusions.size());

    // sensitivity is used to scale the correlation amplitude values to match the cutoff
    double sensitivity = stationMeta.getChannelMetadata(channel).getStage(0).getStageGain();

    // because we parametrize other exclusion criteria we now publish our current results
    // so any additional filter (i.e., by amplitude) is to be managed by the implementing metric
    return new PulseDetectionData(correl, scale, pointInclusions, sensitivity,
        getCorrelationOffset(sampleRate));
  }

  /**
   * Constructs a 15-minute step function; the station data is convolved with this to find
   * potential pulse data. Pulse ranges from 0 to 1 with a 40-second slope between the two points
   * @param sampleRate Sample rate of the data.
   * @return Array of points representing the step.
   */
  static double[] getStepFunction(double sampleRate) {
    // construct 15 minute step function
    int pointCount = (int) (15 * 60 * sampleRate); // 900 seconds
    double[] stepFunction = new double[pointCount];
    // here is the expected structure of this step function:
    // 430 seconds of zero (array set to this by default)
    // 40 seconds of a ramp between 0 and 1
    // 430 seconds at 1 (starts at 470 seconds)
    // now for the slope
    int slopeStart = (int) (430 * sampleRate);
    int slopeLength = (int) (40 * sampleRate);
    Arrays.fill(stepFunction, slopeStart + slopeLength, pointCount, 1);
    for (int i = slopeStart; i < slopeStart + slopeLength; ++i) {
      int numer = i - slopeStart + 1;
      // double denom = pulsePeakPoint - slopeStart + 1;
      stepFunction[i] = numer / (double) slopeLength;
    }
    return stepFunction;
  }

  /**
   * Gets the basic per-point difference for a dataset, akin to the numpy diff function.
   * @param trace Dataset to get difference over
   * @return Trace of same length as input where first point is 0
   * and all other points are trace[i+1]-trace[i]
   */
  static double[] getPerPointDifference(double[] trace) {
    double[] differences = new double[trace.length];
    differences[0] = 0;
    for (int i = 1; i < trace.length; ++i) {
      differences[i] = trace[i] - trace[i - 1];
    }
    return differences;
  }

  /**
   * Computes the cross-correlation of a trace with a 15-minute step function -- see
   * {@link #crossCorrelate(double[], double[])}.
   * The data returned will have values that would otherwise be guaranteed to be 0 at the ends
   * of the data trimmed off; any operation that requires the index of these values to match with
   * the points of the inputted trace will need to compare them with the offset value from
   * {@link #getCorrelationOffset(double)}.
   * @param trace Data to get cross correlation with step from
   * @param sampleRate Sample rate of the data (used to generate the step function)
   * @return 2D array of correlation and associated amplitude values from the
   */
  static double[][] computeCrossCorrelationWithStep(double[] trace, double sampleRate) {
    // first, get the 15-minute step function
    double[] stepFunction = getStepFunction(sampleRate);
    // now correlate the data with it (step function detrended in that method)
    return crossCorrelate(trace, stepFunction);
  }

  /**
   * Gets the length of zeros on either side of the step function, for use in associating
   * values in the correlation arrays with the raw data going into them. Adding this value to an
   * index in those arrays will match it to the index of a point on the raw data.
   * Because this is derived from the length of the step function -- the number of zeros on each
   * end of the correlation array is equal to half the length of the step -- the only necessary
   * parameter is the sample rate of the data, which is used to get the length of the step.
   * @param sampleRate Sample rate of the data being correlated
   * @return Half the length of the step function generated, which is the number of zeros on either
   * end of the correlation array.
   */
  private int getCorrelationOffset(double sampleRate) {
    int stepPointCount = (int) (15 * 60 * sampleRate); // 900 seconds
    return (stepPointCount / 2);
  }

  /**
   * Produce the cross-correlation between a given channel trace and a 15-minute step function.
   * The step function is detrended here before the correlation is calculated.
   * The result is a 2D array of length 2, where the first array is the correlation values
   * per-point and the second array is the associated amplitude values. The lengths of these arrays
   * are trimmed down from the length of the trace by the length of the step function, as each
   * side of it are flanked by zeros half the length of the step function; matching the indices
   * of these arrays to points in the trace can be done by adding the value gotten from the method
   * {@link #getCorrelationOffset(double)}.
   *
   * @param trace Sensor data to correlate with step
   * @param stepFunction 15-minute step function, see {@link #getStepFunction(double)} and
   * {@link #computeCrossCorrelationWithStep(double[], double)}.
   * @return 2D array containing sub-arrays of the correlation values and amplitudes respectively
   */
  static double[][] crossCorrelate(double[] trace, double[] stepFunction) {
    // detrend the step function and normalize
    double[] stepFunctionProcessed = detrend(stepFunction);
    double summedSquares = 0.;
    for (double point : stepFunctionProcessed) {
      summedSquares += Math.pow(point, 2);
    }
    summedSquares = Math.sqrt(summedSquares);
    for (int i = 0; i < stepFunctionProcessed.length; ++i) {
      stepFunctionProcessed[i] /= summedSquares;
    }
    // pre-trim the outputted arrays; anything past this boundary will be zero
    int corrLen = trace.length - stepFunction.length;
    double[] correl = new double[corrLen];
    double[] scal = new double[corrLen];
    // double[] xerr = new double[trace.length]; // this value was taken from old matlab code
    for (int i = 0; i < corrLen; ++i) {
      double[] tr2 = Arrays.copyOfRange(trace, i, i + stepFunction.length);
      tr2 = detrend(tr2);
      // now some inner loops to do the calculations of cross-correlation over this trimmed range
      double sumSqd = 0;
      for (double v : tr2) {
        sumSqd += Math.pow(v, 2);
      }
      sumSqd = Math.sqrt(sumSqd);
      double scalNumer = 0;
      double scalDenom = 0;
      for (int j = 0; j < tr2.length; ++j) {
        double correlPoint = tr2[j] / sumSqd;
        correl[i] += correlPoint * stepFunctionProcessed[j];
        scalNumer += tr2[j] * stepFunctionProcessed[j];
        scalDenom += stepFunction[j] * stepFunctionProcessed[j];
      }
      scal[i] = scalNumer / scalDenom;
      // xerr[i] = std(tr2 - scal(n). * q);
    }
    return new double[][]{correl, scal};
  }

  /**
   * Performs an averaging of the values centered at a given point over a range specified by
   * windowLength. Used to perform some basic high-pass filtering and to get the 15-min and
   * 60-second averages for the sharpness constraint.
   * @param data Trace to get the windowed averages for
   * @param windowLength How many points to include in a windowed average
   * @return Result of applying moving average to the data
   */
  public static double[] getCenteredMovingAverage(double[] data, int windowLength) {
    double[] mean = new double[data.length];
    // a bit weird but helps us make sure each side of the data is centered
    // so if we have a 240 second window the data extends 119 seconds on each side
    int singleSide = (int) Math.floor((Math.ceil(windowLength / 2.) * 2. - 1.) / 2.);

    // note that while this is supposed to be centered, due to array bounds points within the first
    // and last 120 seconds will not be centered in the moving average
    // the solution to this is either to add 120 seconds on either end of the data or
    // to ignore that time range (obviously the former is preferred, which is why we get data
    // that extends past the day boundary)
    for (int i = 0; i < data.length; ++i) {
      int minBound = Math.max(0, i - singleSide);
      int maxBound = Math.min(data.length, i + singleSide);
      double[] window = Arrays.copyOfRange(data, minBound, maxBound);
      mean[i] = getMean(window);
    }
    return mean;
  }

  /**
   * Similar to the method {@link #getCenteredMovingAverage(double[], int)} but is done in such a
   * way that the averages of the lowest, middle, and upper 1/3 percentiles are averaged separately.
   * That is, we sort the array and get the average of the first 1/3 of the data,
   * the second 1/3 of the data, and the last 1/3 of the data.
   * This produces a 2D array of length 3 corresponding to the windowed averages of those
   * percentiles in the order {low, mid, high}.
   * @param data Trace to get the averages over
   * @param windowLength Length of the window to get the moving averages over
   * @return 2D array representing the low, middle, and high averages for each point;
   * the index [0][27] returns the low-third percentile result for point 27 in the input array.
   */
  public static double[][] getCenteredMovingAveragePercentiles(double[] data, int windowLength) {
    double[] lowestThird = new double[data.length];
    double[] middleThird = new double[data.length];
    double[] upperThird = new double[data.length];

    // a bit weird but helps us make sure each side of the data is centered
    // so if we have a 240 second window the data extends 119 seconds on each side
    int singleSide = (int) ((Math.ceil(windowLength / 2.) * 2. - 1.) / 2.);

    // note that while this is supposed to be centered, due to array bounds points within the first
    // and last 120 seconds will not be centered in the moving average
    // the solution to this is either to add 120 seconds on either end of the data or
    // to ignore that time range (obviously the former is preferred)
    for (int i = 0; i < data.length; ++i) {
      int minBound = Math.max(0, i - singleSide);
      int maxBound = Math.min(data.length, i + singleSide);
      double[] window = Arrays.copyOfRange(data, minBound, maxBound);
      // need to sort here in order to get the percentile windows for each point
      Arrays.sort(window);
      int firstDivider = window.length / 3;
      int secondDivider = (int) (window.length / 1.5);
      lowestThird[i] = getMean(Arrays.copyOfRange(window, 0, firstDivider));
      middleThird[i] = getMean(Arrays.copyOfRange(window, firstDivider, secondDivider));
      upperThird[i] = getMean(Arrays.copyOfRange(window, secondDivider, window.length));
    }
    return new double[][]{lowestThird, middleThird, upperThird};
  }

  /**
   * Evaluate an envelope constraint on the data.
   * This checks that the percentile windowed averages (see
   * {@link #getCenteredMovingAveragePercentiles(double[], int)}) for each data point all change
   * in the same way; specifically, we check that the values 140 seconds before and after that point
   * have a difference that shares the same sign for each of the three percentile groups we get
   * the average over. If they do not have the same sign, the point is excluded as a possible value.
   * @param data Trace to get the envelope constraint over
   * @param sampleRate Sample rate of the data (for determining how many points are in 140 seconds)
   * @return Set of indices matching points that are valid by this criterion.
   */
  public static Set<Integer> envelopeConstraint(double[] data, double sampleRate) {

    int startingPoint = (int) (140 * sampleRate);
    int endingPoint = data.length - startingPoint;

    int[] iteration = new int[endingPoint - startingPoint];
    Arrays.setAll(iteration, i -> i + startingPoint);
    Set<Integer> pointInclusions = new HashSet<>();
    for (int value : iteration) {
      pointInclusions.add(value);
    }

    // with cross-correlation done we now set up exclusion criteria for possible peaks
    // get a 240s window and then get the averages of the 33, 66, and 99 percentile
    int windowLength = (int) (sampleRate * 240);
    // outer index is 0 for lowThird, 1 for midThird, and 2 is for upperThird of curve
    double[][] movingAverages = getCenteredMovingAveragePercentiles(data, windowLength);

    // over the three percentile moving average filter out what doesn't match our criteria
    for (int i : iteration) {
      double[] changeOverTime = new double[movingAverages.length];
      for (int j = 0; j < movingAverages.length; ++j) {
        if (!pointInclusions.contains(i)) {
          continue;
        }

        changeOverTime[j] = Math.signum(movingAverages[j][i + startingPoint]
            - movingAverages[j][i - startingPoint]);
        // double present = Math.signum(movingAverage[i]);
        // if the signs don't match, the point is excluded
      }
      if (changeOverTime[0] != changeOverTime[1] || changeOverTime[1] != changeOverTime[2]) {
        pointInclusions.remove(i);
      }
    }
    return pointInclusions;
  }

  /**
   * Evaluate a sharpness constraint on the data. This method iterates over the points
   * included for consideration by envelope evaluation and then produces a set of points that are
   * NOT valid. The evaluation is to take a 60-second moving average of the data at each point, and
   * compare it to the 900-second (15 min.) moving average of the magnitude of the data at each
   * point. If the magnitude of the 60-second window value is at least 4 times that of the
   * 900-second value, then the point is included (and thus not part of the returned set).
   * Because this method returns the EXCLUSION of points from the input, the set of valid points
   * that match both results can be done by removing all values in the returned set of this method
   * from the returned set of the envelope constraint check.
   * @see #envelopeConstraint(double[], double)
   * @see java.util.Set#removeAll(Collection)
   * @param traceFiltered Trace of data to get sharpness constraint over
   * @param sampleRate Sample rate of data (for determining points in 60, 900 second windows)
   * @param validPoints Subset of indices to check against; a null value is acceptable here, in
   * which case all points in the trace will be checked.
   * @return A set of indices to EXCLUDE from the published pulse detection result.
   */
  public static Set<Integer> sharpnessConstraint(double[] traceFiltered, double sampleRate,
      Integer[] validPoints) {
    if (validPoints == null || validPoints.length == 0.) {
      validPoints = new Integer[traceFiltered.length];
      Arrays.setAll(validPoints, i -> i);
    }

    // for 60-second moving average. per-point difference, then get windowed average
    double[] windowedSixtySec = getPerPointDifference(traceFiltered);
    // for 900-second moving average we ALSO need to get the abs val of that diff first
    double[] windowedFifteenMin = new double[traceFiltered.length];
    for (int i = 0; i < traceFiltered.length; ++i) {
      windowedFifteenMin[i] = Math.abs(windowedSixtySec[i]);
    }
    // compare the 60-second moving average with the 900-second one over the absolute val
    int samplesInSixty = (int) (60 * sampleRate);
    int samplesInNineHund = (int) (900 * sampleRate);
    windowedSixtySec = getCenteredMovingAverage(windowedSixtySec, samplesInSixty);
    windowedFifteenMin = getCenteredMovingAverage(windowedFifteenMin, samplesInNineHund);

    Set<Integer> excludedPoints = new HashSet<>();
    // now for all the points ensure that the 60s value is > 4 times the 900s value
    for (int i : validPoints) {
      double sixtyPoint = Math.abs(windowedSixtySec[i]);
      double pointOfComparison = (((windowedFifteenMin[i] * 900) - (sixtyPoint * 60)) / 840)
          / windowedSixtySec[i];
      if (Math.abs(pointOfComparison) > 0.25) {
        excludedPoints.add(i);
      }
    }
    return excludedPoints;
  }

  /**
   * Removes the response on data and returns it back in the time domain.
   * Response gain is accounted for at the final step of this method; because this method is used
   * to produce time-domain data rather than stay in frequency space,
   * it is distinct from the response methods used elsewhere.
   * During the response removal process the data is converted to units of acceleration.
   * @param trace Data to remove response from.
   * @param metadata Metadata from which response is extracted
   * @param waterLevel Small decimal factor to prevent division by zero (i.e., 0.001)
   * @see #applyWaterLevelToResponse(Complex[], double)
   * @return Trace with response removed, in acceleration
   * @throws ChannelMetaException If there is an issue with getting the channel metadata
   */
  public static double[] removeResponseOnTimeDomainData(double[] trace, ChannelMeta metadata,
      double waterLevel) throws ChannelMetaException {
    double sampleRate = metadata.getSampleRate();
    // while this should be nearly identical to the response removal operation in ChannelMeta,
    // we handle it this way to use a water level parameter similar to one available in obspy
    FFTResult frequencySpace = FFTResult.simpleFFT(trace, sampleRate);
    Complex[] complexData = frequencySpace.getFFT();
    Complex[] response = metadata.getResponseUnscaled(frequencySpace.getFreqs(),
        ResponseUnits.VELOCITY);
    for (int i = 0; i < response.length; ++i) {
      if (response[i].equals(Complex.ZERO)) {
        continue;
      }
      Complex c = new Complex(0, 2 * PI * frequencySpace.getFreq(i));
      response[i] = response[i].divide(c);
    }
    response = applyWaterLevelToResponse(response, waterLevel);
    // that step also inverts the response, so our next step will be to multiply, not divide
    for (int i = 0; i < complexData.length; ++i) {
      complexData[i] = complexData[i].multiply(response[i]);
    }
    double[] returnValue = FFTResult.simpleInverseFFT(complexData, trace.length);
    for (int i = 0; i < returnValue.length; ++i) {
      returnValue[i] /= metadata.getStage(0).getStageGain();
    }
    return returnValue;
  }

  /**
   * Inverts response and applies a minimum water level to prevent division by zero.
   * Due to inverting the response in this process, response removal is done by multiplying
   * the returned value with the FFT of some trace data.
   * @param response Response curve (acceleration) for given metadata
   * @param waterLevel Minimum value to give to nonzero response values
   * @return Response inverted with water level applied.
   */
  static Complex[] applyWaterLevelToResponse(Complex[] response, double waterLevel) {
    Complex[] invertedResponse = Arrays.copyOf(response, response.length);
    double max = Double.NEGATIVE_INFINITY;
    for (Complex complex : response) {
      max = Math.max(max, complex.abs());
    }

    double waterLevelScaled = max * waterLevel;
    // initial water-level scale for points less than water level scale
    for (int i = 0; i < response.length; ++i) {
      double respAbs = response[i].abs();

      if (respAbs > 0 && respAbs < waterLevelScaled) {
        invertedResponse[i] = response[i].multiply(waterLevelScaled / respAbs);
        respAbs = invertedResponse[i].abs();
      }
      // if it's still greater than zero amplitude, then invert the response
      if (respAbs > 0) {
        invertedResponse[i] = Complex.ONE.divide(invertedResponse[i]);
      } else {
        // if it's zero just keep it at zero because otherwise we'd get NaNs from inversion
        invertedResponse[i] = Complex.ZERO; // this explicit assignment is probably not necessary
      }
    }
    return invertedResponse;
  }

  /**
   * Inner class for storing results of metrics using this backend.
   * Each pulse is stored as a list of PulseDetectionPoint objects, which are a pair of finalized
   * doubles representing a point's correlation and amplitude. That is, these lists are all of a
   * contiguous set of data, from which the max pulse peak can be gotten by finding the point
   * of all of these lists with the largest amplitude value that fits a prescribed correlation
   * cutoff. The number of pulses detected is the size of the outer list.
   *
   * This class's elements are all final and the lists are unmodifiable, so points are iterated
   * through and the correlation and amplitude fields are accessed directly; no getters or setters
   * are implemented for this class.
   * @see PulseDetectionPoint
   */
  public static class PulseDetectionData {

    public final List<List<PulseDetectionPoint>> correlationsWithAmplitude;
    public final double sensitivity;

    /**
     * Pair of doubles, the correlation value and the amplitude at a given point.
     */
    protected static class PulseDetectionPoint {

      public final double correlationValue;
      public final double amplitude;

      public PulseDetectionPoint(double correlationValue, double amplitude) {
        this.correlationValue = correlationValue;
        this.amplitude = amplitude;
      }

      public String toString() {
        return "Correlation: " + correlationValue + " | Amplitude: " + amplitude;
      }
    }

    /**
     * Constructs the result of the backend calculations for use in an implementing metric.
     * Given the correlation and amplitude values of some data's cross-correlation with a step,
     * the set of included (i.e., valid) indices, and an offset to deal with the fact that the
     * points are offset by half the length of the step function they were correlated over (because
     * those points would otherwise be zero).
     * In the process of this construction, all contiguous accepted points are collected into
     * the sublists of PulseDetectionPoints.
     * @see PulseDetectionPoint
     * @param correlations Correlated values between trace and step
     * @param amplitudes Amplitudes of the above correlation
     * @param inclusions Indices in the original trace that passed screening criteria
     * @param sensitivity Sensitivity value of response (for scaling amplitude values)
     * @param startingOffset Index of first nonzero point in the untrimmed correlation arrays
     */
    public PulseDetectionData(double[] correlations, double[] amplitudes,
        Collection<Integer> inclusions, double sensitivity, int startingOffset) {
      this.sensitivity = sensitivity;
      if (inclusions.size() == 0) {
        correlationsWithAmplitude = Collections.unmodifiableList(Collections.emptyList());
        return;
      }
      // get a sorted list of inclusions here so that contiguous indices are grouped
      List<Integer> inclusionList = new ArrayList<>(inclusions);
      sort(inclusionList);

      // each entry in this list are a set of contiguous points (i.e., one pulse)
      List<List<PulseDetectionPoint>> tempList = new ArrayList<>(correlations.length);
      int idx = 0;
      // this is a little weird because we're iterating over a filtered set of indices
      do {
        int i = inclusionList.get(idx);
        List<Integer> contiguousIndices = new ArrayList<>();
        contiguousIndices.add(i);
        // get all indices that are in a contiguous range (i.e., 3, 4, 5,)
        while (idx + 1 < inclusionList.size() &&
            contiguousIndices.get(contiguousIndices.size() - 1) + 1 ==
                inclusionList.get(idx + 1)) {
          idx += 1;
          contiguousIndices.add(inclusionList.get(idx));
        }
        List<PulseDetectionPoint> subList = new ArrayList<>(contiguousIndices.size());
        for (int contiguousIndex : contiguousIndices) {
          int indexWithOffset = contiguousIndex - startingOffset;
          if (indexWithOffset < 0 || indexWithOffset >= amplitudes.length) {
            // this check may not be necessary but prevents us running into issues where
            // data was accepted close to the boundary of the day and wound up not being filtered
            // by the exclusion criteria but had a zero-valued correlation/amplitude
            continue;
          }
          double pulse = Math.abs(amplitudes[indexWithOffset]);
          double correlation = Math.abs(correlations[indexWithOffset]);
          subList.add(new PulseDetectionPoint(correlation, pulse));
        }
        subList = Collections.unmodifiableList(subList);
        if (subList.size() > 0) {
          // again, reasoning for this matches the check for a positive index above
          // where a possible pulse is too close to the day boundary to list here
          tempList.add(subList);
        }
        ++idx;
      } while (idx < inclusionList.size());
      // now to use our collected points to publish a result
      correlationsWithAmplitude = Collections.unmodifiableList(tempList);
    }

    public String toString() {
      return correlationsWithAmplitude.toString();
    }
  }

}
