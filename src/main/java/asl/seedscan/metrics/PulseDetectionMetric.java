package asl.seedscan.metrics;

import static asl.utils.NumericUtils.detrend;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PulseDetectionMetric extends Metric {

  private static final Logger logger = LoggerFactory.getLogger(PulseDetectionMetric.class);

  protected Map<ChannelKey, PulseDetectionData> pulseDetectionResultMap;

  public Map<ChannelKey, PulseDetectionData> getPulseDetectionData() {
    return pulseDetectionResultMap;
  }

  public void setPulseDetectionData(Map<ChannelKey, PulseDetectionData> cached) {
    pulseDetectionResultMap = cached;
  }

  public PulseDetectionData calculatePulseResults(Channel channel) throws ChannelMetaException {
    // instead of having the boundary be right at midnight, seedscan doesn't easily support getting
    // the previous day's data for doing centered differences on windowed estimations
    // instead pulse detections will be off such that the first 140 or so seconds of the next day
    // are included in the current day's metric, unless
    long start = metricData.getChannelData(channel).get(0).getStartTime();
    long end = Instant.ofEpochMilli(start).plus(280, ChronoUnit.SECONDS)
        .plus(1, ChronoUnit.DAYS).toEpochMilli();

    double[] trace = metricData.getWindowedData(channel, start, end);
    if (trace == null || trace.length == 0.) {
      // this happens if we're scanning the most recent full day of data, in which case we'll
      // just run on the data currently available to us to get some estimation of the values
      trace = metricData.getDetrendedPaddedDayData(channel);
      logger.info("Got full day data for channel [{}]", channel.toString());
    }

    FFTResult.cosineTaper(trace, 0.05); // taper done in-place
    double sampleRate = stationMeta.getChannelMetadata(channel).getSampleRate();
    // remove response, convert to acceleration, detrend, and filter (order 4 butterworth)
    // here's the response removal block
    trace = removeResponseOnTimeDomainData(trace, stationMeta.getChannelMetadata(channel),
        0.001);
    // now do the conversion into acceleration and detrend
    // trace = differentiateTimeSeries(trace, sampleRate);
    trace = detrend(trace);
    double filterBand = 1. / 40;
    trace = FilterUtils.lowPassFilter(trace, sampleRate, filterBand, 4);

    // use moving average to remove long-period (~1 hr) trend
    double[] meanOffset = getCenteredMovingAverage(trace, (int) (3600 * sampleRate));
    double[] traceFiltered = new double[meanOffset.length];
    for (int i = 0; i < meanOffset.length; ++i) {
      traceFiltered[i] = traceFiltered[i] - meanOffset[i];
    }

    // correl is the cross-correlation value, called xcC in the matlab code
    // scale is the scale factor thereof, used to get the pulse amplitude at the given point
    double[] correl, scale;
    {
      double[][] crossCorr = computeCrossCorrelationWithStep(trace, sampleRate);
      assert (crossCorr.length == 2);
      correl = crossCorr[0];
      scale = crossCorr[1];
    }

    // this set will keep track of points which pass the following constraints on pulse data
    Set<Integer> pointInclusions = new HashSet<>();

    // with cross-correlation done we now set up exclusion criteria for possible peaks
    // get a 240s window and then get the averages of the 33, 66, and 99 percentile
    int windowLength = (int) (sampleRate * 240);
    // outer index is 0 for lowThird, 1 for midThird, and 2 is for upperThird of curve
    double[][] movingAverages = getCenteredMovingAveragePercentiles(traceFiltered, windowLength);
    assert (3 == movingAverages.length);

    // first constraint for pulses: sign of difference between points on either side of 140s
    // must match the sign of the average; this constraint would not hold in a case of
    // sharp changes, such as during an actual earthquake (in which case the pulse is not an
    // artifact of the sensor but a reflection of ground motion behavior).
    int startingPoint = (int) (140 * sampleRate);
    for (int i = startingPoint; i < correl.length; ++i) {
      // expect that all points are potentially valid here
      pointInclusions.add(i);
    }
    // over the three percentile moving average filter out what doesn't match our criteria
    for (double[] movingAverage : movingAverages) {
      for (int i = startingPoint; i < trace.length - startingPoint; ++i) {
        if (!pointInclusions.contains(i)) {
          continue;
        }

        double changeOverTime = Math.signum(movingAverage[i + startingPoint]
            - movingAverage[i - startingPoint]);
        double present = Math.signum(movingAverage[i]);
        // if the signs don't match, the point is excluded
        if (present != changeOverTime) {
          pointInclusions.remove(i);
        }
      }
    }

    logger.info("Number of points under potential consideration: {}", trace.length);
    logger.info("Number of points that pass moving average criteria: {}", pointInclusions.size());

    // next exclusion criteria: sharpness constraint.
    {
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
      // construct this list to allow for removal from the set without messing up loop operation
      Integer[] iteration = pointInclusions.toArray(new Integer[]{});
      // now for all the points ensure that the 60s value is > 4 times the 900s value
      for (int i : iteration) {
        double sixtyPoint = Math.abs(windowedSixtySec[i]);
        double pointOfComparison = (((windowedFifteenMin[i] * 900) - (sixtyPoint * 60)) / 840)
                / windowedSixtySec[i];
        if (pointInclusions.contains(i) && Math.abs(pointOfComparison) > 0.25) {
          pointInclusions.remove(i);
        }
      }
    }

    logger.info("Number of points that also pass sharpness criteria: {}", pointInclusions.size());

    // sensitivity is used to scale the correlation amplitude values to match the cutoff
    double sensitivity = stationMeta.getChannelMetadata(channel).getStage(0).getStageGain();

    // because we parametrize other exclusion criteria we now publish our current results
    // so any additional filter (i.e., by amplitude) is to be managed by the implementing metric
    return new PulseDetectionData(correl, scale, pointInclusions, sensitivity);
  }

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
   * Produces a value to use to prevent issues with division by zero, by getting 0.1% of the maximum
   * magnitude value of a trace. Add this to the denominator of a calculation involving the trace.
   *
   * @param trace timeseries data to get max value for
   * @return Value to use as water level calculation
   */
  static double getWaterLevel(double[] trace) {
    double waterLevel;
    double traceMaxValue = trace[0];
    for (double point : trace) {
      traceMaxValue = Math.max(Math.abs(point), traceMaxValue);
    }
    waterLevel = 0.001 * traceMaxValue;
    return waterLevel;
  }

  static double[] getPerPointDifference(double[] trace) {
    double[] differences = new double[trace.length];
    differences[0] = 0;
    for (int i = 1; i < trace.length; ++i) {
      differences[i] = trace[i] - trace[i - 1];
    }
    return differences;
  }

  static double[][] computeCrossCorrelationWithStep(double[] trace, double sampleRate) {
    // first, get the 15-minute step function
    double[] stepFunction = getStepFunction(sampleRate);
    return crossCorrelate(trace, stepFunction);
  }

  static double[][] crossCorrelate(double[] trace, double[] stepFunction) {
    double waterLevel = getWaterLevel(trace);

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

    int stepMidpoint = (int) Math.ceil(0.5 * stepFunction.length);
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

  public static double[] differentiateTimeSeries(double[] data, double sampleRate) {
    double[] output = new double[data.length - 1];

    for (int i = 0; i < output.length; ++i) {
      // sample rate is in units of 1/Hz so multiplying it is like dividing by a unit of seconds
      output[i] = (data[i + 1] - data[i]) * sampleRate;
    }
    return output;
  }

  public static double[] getCenteredMovingAverage(double[] data, int windowLength) {
    double[] mean = new double[data.length];
    // a bit weird but helps us make sure each side of the data is centered
    // so if we have a 240 second window the data extends 119 seconds on each side
    int singleSide = (int) Math.floor((Math.ceil(windowLength / 2.) * 2. - 1.) / 2.);

    // note that while this is supposed to be centered, due to array bounds points within the first
    // and last 120 seconds will not be centered in the moving average
    // the solution to this is either to add 120 seconds on either end of the data or
    // to ignore that time range (obviously the former is preferred)
    for (int i = 0; i < data.length; ++i) {
      int minBound = Math.max(0, i - singleSide);
      int maxBound = Math.min(data.length, i + singleSide);
      double[] window = Arrays.copyOfRange(data, minBound, maxBound);
      mean[i] = getMean(window);
    }
    return mean;
  }

  public static double[][] getCenteredMovingAveragePercentiles(double[] data, int windowLength) {
    double[] lowestThird = new double[data.length];
    double[] middleThird = new double[data.length];
    double[] upperThird = new double[data.length];

    // a bit weird but helps us make sure each side of the data is centered
    // so if we have a 240 second window the data extends 119 seconds on each side
    int singleSide = (int) Math.floor((Math.ceil(windowLength / 2.) * 2. - 1.) / 2.);

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
      int thirdDivider = window.length / 3;
      lowestThird[i] = getMean(Arrays.copyOfRange(window, 0, thirdDivider));
      middleThird[i] = getMean(Arrays.copyOfRange(window, thirdDivider, 2 * thirdDivider));
      upperThird[i] = getMean(Arrays.copyOfRange(window, thirdDivider, window.length));
    }
    return new double[][]{lowestThird, middleThird, upperThird};
  }

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
        System.out.println(invertedResponse[i]);
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

  public static class PulseDetectionData {

    public final List<PulseDetectionPoint> correlationsWithAmplitude;
    public final double peakAmplitude;
    public final double corrOfPeak;

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

    public PulseDetectionData(double[] correlations, double[] amplitudes, Set<Integer> inclusions,
        double sensitivity) {
      if (inclusions.size() == 0) {
        correlationsWithAmplitude = Collections.unmodifiableList(Collections.emptyList());
        peakAmplitude = 0;
        corrOfPeak = 0;
        return;
      }
      List<PulseDetectionPoint> tempList = new ArrayList<>(correlations.length);
      double tempMax = Math.abs(amplitudes[0]) * sensitivity;
      double tempCorr = correlations[0];
      tempList.add(new PulseDetectionPoint(tempCorr, tempMax));
      List<Integer> inclusionList = new ArrayList<>(inclusions);
      sort(inclusionList);
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
        // out of the contiguous index range, report the one with the largest pulse value
        double maxPulse = 0;
        // don't forget that the entries in the list are indicies into the corr/amp arrays
        for (int contiguousIndex : contiguousIndices) {
          double candidatePulse = Math.abs(amplitudes[contiguousIndex]) * sensitivity;
          if (candidatePulse > maxPulse) {
            i = contiguousIndex;
          }
        }
        // having found the best pulse in the contiguous range we can add it to our list of reports
        double pulse = Math.abs(amplitudes[i]) * sensitivity;
        tempList.add(new PulseDetectionPoint(Math.abs(correlations[i]), pulse));
        tempMax = Math.max(tempMax, pulse);
        if (tempMax == pulse) {
          tempCorr = Math.abs(correlations[i]);
        }
        ++idx;
      } while (idx < inclusionList.size());
      // now to use our collected points to publish a result
      peakAmplitude = tempMax;
      if (tempList.size() == 0) {
        corrOfPeak = 0;
      } else {
        corrOfPeak = tempCorr;
      }
      correlationsWithAmplitude = Collections.unmodifiableList(tempList);
    }

    public String toString() {
      return correlationsWithAmplitude.toString();
    }
  }

}
