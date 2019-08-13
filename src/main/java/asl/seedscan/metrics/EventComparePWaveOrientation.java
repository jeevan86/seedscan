package asl.seedscan.metrics;

import asl.metadata.Channel;
import asl.seedscan.event.EventCMT;
import asl.timeseries.TimeseriesUtils;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sac.SacTimeSeries;
import uk.me.berndporr.iirj.Butterworth;

public class EventComparePWaveOrientation extends Metric {

  private static final Logger logger =
      LoggerFactory.getLogger(asl.seedscan.metrics.EventComparePWaveOrientation.class);

  // length of window to take for p-wave data in seconds, including a lead-in time
  private static final int P_WAVE_WINDOW = 40;

  // range of degrees (arc length) over which data will be valid
  private static final int MIN_DEGREES = 20;
  private static final int MAX_DEGREES = 90;
  /**
   * https://github.com/usgs/seedscan.git Filter corner 0.05 Hz or 20 seconds period.
   */
  private static final double LOW_PASS_FILTER_CORNER = 0.05;
  /**
   * 50 second window for p-wave ringing offset data
   */
  private static final int P_WAVE_RINGING_OFFSET = 50;

  EventComparePWaveOrientation() {
    super();
    addArgument("base-channel");
    addArgument("channel-restriction");
  }

  /**
   * Determine if a channel meets the needed criteria.
   *
   * @param channel Channel to be tested.
   * @param allowedBands List of allowed bands in String format
   * @return True if filtered out, False if not
   */
  static boolean filterChannel(Channel channel, List<String> allowedBands) {
    //Only process ND channels.
    if (!channel.getChannel().endsWith("ND")) {
      return true;
    }
    //Test if in requested bands
    return !allowedBands.contains(channel.getChannel().substring(0, 2));
  }

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public String getName() {
    return "EventComparePWaveOrientation";
  }

  @Override
  public void process() {
    logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

    // a bunch of this is copy-pasted from eventCompareSynthetic since it's the same thing
    Hashtable<String, EventCMT> eventCMTs = getEventTable();
    if (eventCMTs == null) {
      logger.info(
          String.format("No Event CMTs found for Day=[%s] --> Skip EventComparePOrientation Metric",
              getDay()));
      return;
    }

    List<Channel> channels = stationMeta.getRotatableChannels();

    // get pairs of ED, ND data and then do the rotation with those
    String basePreSplit = null;

    try {
      basePreSplit = get("base-channel");
    } catch (NoSuchFieldException ignored) {
    }

    if (basePreSplit == null) {
      basePreSplit = "XX-LX";
      logger.info("No base channel for Event Compare P Orientation using: " + basePreSplit);
    }

    String preSplitBands = null;
    try {
      preSplitBands = get("channel-restriction");
    } catch (NoSuchFieldException ignored) {
    }
    if (preSplitBands == null) {
      preSplitBands = "LH";
      logger.info("No band restriction set for Event Compare P Orientation using: "
          + preSplitBands);
    }
    List<String> allowedBands = Arrays.asList(preSplitBands.split(","));

    // map used for quick access to paired channel data
    // we only do calculations once both N/E channel pairs (or ND/ED)
    // exist in the map
    Map<String, Channel> chNameMap = new HashMap<>();
    for (Channel curChannel : channels) {

      String name = curChannel.toString();
      chNameMap.put(name, curChannel);
      String channelVal = name.split("-")[1];
      if (!allowedBands.contains(channelVal.substring(0, 2))) {
        // current channel not part of valid band options, skip it
        continue;
      }

      int lastCharIdx = channelVal.length() - 1;
      char last = channelVal.charAt(lastCharIdx);
      if (last == 'Z' || last == 'E') {
        // assume vertical sensor component requires no orientation
        // assume east sensor will be referenced when north is read in
        continue;
      } else if (last == 'D') {
        if (channelVal.charAt(lastCharIdx - 1) == 'E') {
          // deal with, say, LHED (east-derived) when we read in LHND (north-derived)
          continue;
        }
      }

      String vertName = getVerticalChannelNameString(name);

      // this is the function that produces rotated data given the current chanel
      ByteBuffer digest = metricData.valueDigestChanged(curChannel, createIdentifier(curChannel),
          getForceUpdate());

      if (digest == null) {
        continue;
      }

      Channel pairChannel = curChannel.getHorizontalOrthogonalChannel();
      Channel vertChannel = chNameMap.get(vertName);

      double angleDifference = computeMetric(eventCMTs, curChannel, pairChannel, vertChannel);

      //Catch any bail out of internal metric.
      if (Double.isNaN(angleDifference)) {
        continue;
      }

      metricResult.addResult(curChannel, angleDifference, digest);
    }
  }

  double computeMetric(Hashtable<String, EventCMT> eventCMTs, Channel curChannel,
      Channel pairChannel, Channel vertChannel) {

    double sumAngleDifference = 0.;
    int numEvents = 0;

    // get lat and long to make sure data is within a reasonable range
    double stationLatitude = stationMeta.getLatitude();
    double stationLongitude = stationMeta.getLongitude();
    double sampleRateN = stationMeta.getChannelMetadata(curChannel).getSampleRate();
    double sampleRateE = stationMeta.getChannelMetadata(pairChannel).getSampleRate();
    double sampleRateV = stationMeta.getChannelMetadata(vertChannel).getSampleRate();
    // now curChannel, pairChannel the two channels to get the orientation of
    // double azi = stationMeta.getChannelMetadata(curChannel).getAzimuth();

    // now we have the angle for which to rotate data

    // now to get the synthetics data
    SortedSet<String> eventKeys = new TreeSet<>(eventCMTs.keySet());
    outerLoop: for (String key : eventKeys) {

      EventCMT eventMeta = eventCMTs.get(key);
      double eventLatitude = eventMeta.getLatitude();
      double eventLongitude = eventMeta.getLongitude();
      // derived data has azimuth of 0 -- can use this w/ station coords to get azimuth relative to event
      double azimuth = SphericalCoords
          .azimuth(stationLatitude, stationLongitude, eventLatitude, eventLongitude);
      azimuth = (360 + azimuth) % 360;
      double angleBetween = SphericalCoords
          .distance(eventLatitude, eventLongitude, stationLatitude, stationLongitude);
      if (angleBetween < MIN_DEGREES || angleBetween > MAX_DEGREES) {
        logger.info("== {}: Arc length ({}) to key=[{}] out of range for this station=[{}]\n",
            getName(), angleBetween, key, getStation());
        continue;
      }

      Hashtable<String, SacTimeSeries> synthetics = getEventSynthetics(key);
      if (synthetics == null) {
        logger.warn("== {}: No synthetics found for key=[{}] for this station=[{}]\n",
            getName(), key, getStation());
        continue;
      }

      // get start time of p-wave, then take data 100 secs before that
      long stationDataStartTime = eventMeta.getTimeInMillis();
      try {
        // give us a 10 second cushion for start of the p-arrival in case metadata is wrong
        long pTravelTime = getPArrivalTime(eventMeta) - (10 * 1000);
        stationDataStartTime += pTravelTime;
      } catch (ArrivalTimeException ignore) {
        // error was already logged in getPArrivalTime
        continue;
      }

      // ending of p-wave is this length of time afterward
      // add 100 seconds on each end to compensate for potential filter ringing later
      // (after filtering we will trim the ringing from each side)
      long stationEventEndTime =
          stationDataStartTime + (P_WAVE_WINDOW + P_WAVE_RINGING_OFFSET) * 1000;
      // set window start back by 100 seconds (units in ms here) plus 50s ring-compensation offset
      stationDataStartTime -= (100 + P_WAVE_RINGING_OFFSET) * 1000; // (100 + X) sec * 1000 ms/sec

      double[] northData = metricData.getWindowedData(curChannel, stationDataStartTime,
          stationEventEndTime);
      double[] eastData = metricData.getWindowedData(pairChannel, stationDataStartTime,
          stationEventEndTime);
      double[] vertData = metricData.getWindowedData(vertChannel, stationDataStartTime,
          stationEventEndTime);

      boolean dataMissing = false;
      if (null == northData) {
        logger.error("== {}: {} attempt to get north-facing data returned nothing",
            getName(), getStation());
        dataMissing = true;
      }
      // separate conditionals for each to log if one or both pieces of data not gettable
      if (null == eastData) {
        logger.error("== {}: {} attempt to get east-facing data returned nothing",
            getName(), getStation());
        dataMissing = true;
      }
      if (dataMissing) {
        continue;
      }
      if (northData.length != eastData.length) {
        logger.error("== {}: {} datasets of north & east not the same length!!",
            getName(), getStation());
        continue;
      }

      // filter, trim, remove mean
      northData = preprocess(northData, sampleRateN);
      eastData = preprocess(eastData, sampleRateE);
      vertData = preprocess(vertData, sampleRateV);

      normalize(northData, eastData);
      normalize(vertData);

      // evaluate signal-to-noise ratio of data (RMS)
      // get the window of data
      int noiseLength = getSamplesInTimePeriod(P_WAVE_WINDOW, sampleRateN);
      // signal is last length of data (i.e., after p-wave arrival);
      int signalOffset = northData.length - noiseLength;

      double snr = getSignalToNoiseRatioOfRMS(northData, eastData, signalOffset, noiseLength);
      final double SIGNAL_CUTOFF = 5.;
      if (snr < SIGNAL_CUTOFF) {
        logger.warn("== {}: Signal to noise ratio under 5 -- ({}, {}): {}", getName(),
            curChannel, pairChannel, snr);
        continue;
      }

      double linearity = calculateLinearity(northData, eastData, signalOffset);
      if (linearity < 0.95) {
        logger.warn("== {}: Skipping; data linearity less than .95 -- [({} - {}) - {}]",
            getName(), curChannel, pairChannel, linearity);
        continue;
      }

      double backAzimuth = calculateBackAzimuth(northData, eastData, azimuth, signalOffset);
      // starting offset for getting coherence match, increments by 1 on loop start
      int offsetForSignCalculations = getSamplesInTimePeriod(19, sampleRateN);
      int increment = getSamplesInTimePeriod(1, sampleRateN);
      int signumN = 0;
      int signumE = 0;
      int signumZ = 0;
      while (signumN == 0 || signumE == 0 || signumZ == 0) {
        offsetForSignCalculations += increment;
        int lookupIndex = signalOffset + offsetForSignCalculations;
        if (lookupIndex >= northData.length) {
          logger.warn("== {}: Check that traces under consideration do not have data issues; "
                  + "could not find nonzero data for quadrant orientation -- [STA:{}-{},{},{}]",
              getName(), getStation(), curChannel, pairChannel, vertChannel
          );
          continue outerLoop;
        }
        signumN = (int) Math.signum(northData[signalOffset + offsetForSignCalculations]);
        signumE = (int) Math.signum(eastData[signalOffset + offsetForSignCalculations]);
        signumZ = (int) Math.signum(vertData[signalOffset + offsetForSignCalculations]);

      }

      backAzimuth = correctBackAzimuthQuadrant(backAzimuth, signumN, signumE, signumZ);

      double angleDifference = (azimuth - backAzimuth) % 360;
      // add warning before publishing result if it's inconsistent with expected
      if (Math.abs(angleDifference) > 5) {
        logger.warn("== {}: Difference btwn calc. and est. azimuth > 5 degrees -- "
                + "[Station=[{}], Channels=({} - {}), Azimuths: ({} (calc) vs. {} (exp))]",
            getName(), getStation(), curChannel, pairChannel, backAzimuth, azimuth);
      }
      // now, populate the results from this data
      sumAngleDifference += angleDifference;
      numEvents++;
    }

    //Average our event angle differences.
    if (numEvents <= 0) {
      return Double.NaN;
    } else {
      return sumAngleDifference / numEvents;
    }
  }

  static String getVerticalChannelNameString(String name) {
    char[] chNameArray = name.toCharArray();
    int lastCharIdx = chNameArray.length - 1;
    char lastChar = chNameArray[lastCharIdx];
    if (lastChar == 'D') {
      --lastCharIdx;
    }
    char pairChar = 'Z';
    chNameArray[lastCharIdx] = pairChar;
    // don't need vertical-derived channel
    return new String(Arrays.copyOfRange(chNameArray, 0, lastCharIdx + 1));
  }

  private static double[] preprocess(double[] data, double sampleRate) {
    // first, we low-pass filter the data
    // filter corner at 0.05Hz (20 s interval)
    // and use a 4 poles in the filter
    data = lowPassFilter(data, sampleRate, LOW_PASS_FILTER_CORNER);

    // assume there are filter artifacts in first 50 seconds' worth of data
    int afterRinging = getSamplesInTimePeriod(P_WAVE_RINGING_OFFSET, sampleRate);
    data = Arrays.copyOfRange(data, afterRinging, data.length - afterRinging);

    // detrend operations are done in-place
    TimeseriesUtils.demean(data);

    return data;

  }

  static void normalize(double[] northData, double[] eastData) {
    if (northData.length != eastData.length) {
      String error = "Cannot scale when array sizes differ.";
      logger.error(error);
      throw new IllegalArgumentException(error);
    }
    // now normalize -- both sets of data by the largest abs value in either
    double maxNorth = northData[0];
    double maxEast = eastData[0];
    // get min and max values for scaling to (-1, 1)
    for (int i = 0; i < northData.length; ++i) {
      double testNorth = Math.abs(northData[i]);
      double testEast = Math.abs(eastData[i]);
      maxNorth = Math.max(maxNorth, testNorth);
      maxEast = Math.max(maxEast, testEast);
    }
    // now scale data by maximum value of all components
    double scaleFactor = Math.max(maxNorth, maxEast);
    if (scaleFactor == 0) {
      return; // avoid div by 0 error - true only if all inputs are 0
    }
    for (int i = 0; i < northData.length; ++i) {
      northData[i] /= scaleFactor;
      eastData[i] /= scaleFactor;
    }
  }

  static void normalize(double[] data) {
    // now normalize -- both sets of data by the largest abs value in either
    double max = data[0];
    // get min and max values for scaling to (-1, 1)
    for (double point : data) {
      max = Math.max(max, Math.abs(point));
    }
    if (max == 0) {
      return; // don't change data if everything is 0-valued (should not happen)
    }
    for (int i = 0; i < data.length; ++i) {
      data[i] /= max;
    }
  }

  private double calculateLinearity(double[] northData, double[] eastData, int signalOffset) {
    double sumNN = 0., sumEN = 0., sumEE = 0.;
    for (int i = signalOffset; i < northData.length; ++i) {
      sumNN += northData[i] * northData[i];
      sumEE += eastData[i] * eastData[i];
      sumEN += eastData[i] * northData[i];
    }
    RealMatrix mat = new BlockRealMatrix(new double[][]{{sumNN, sumEN}, {sumEN, sumEE}});
    EigenDecomposition eigenDecomposition = new EigenDecomposition(mat);
    RealMatrix eigenValues = eigenDecomposition.getD();
    return (eigenValues.getEntry(0, 0) / eigenValues.getTrace()) -
        (eigenValues.getEntry(1, 1) / eigenValues.getTrace());
  }

  private double getSignalToNoiseRatioOfRMS(double[] northData, double[] eastData, int signalOffset,
      int noiseLength) {
    double signalNorth = 0., noiseNorth = 0., signalEast = 0., noiseEast = 0.;
    for (int i = 0; i < noiseLength; ++i) {
      signalNorth += Math.pow(northData[signalOffset + i], 2);
      noiseNorth += Math.pow(northData[i], 2);
      signalEast += Math.pow(eastData[signalOffset + i], 2);
      noiseEast += Math.pow(eastData[i], 2);
    }
    signalNorth /= noiseLength;
    signalEast /= noiseLength;
    noiseNorth /= noiseLength;
    noiseEast /= noiseLength;

    signalNorth = Math.sqrt(signalNorth);
    signalEast = Math.sqrt(signalEast);
    noiseNorth = Math.sqrt(noiseNorth);
    noiseEast = Math.sqrt(noiseEast);

    return (signalNorth + signalEast) / (noiseNorth + noiseEast);
  }

  private double calculateBackAzimuth(double[] north, double[] east, double evtAzimuth,
      int signalOffset) {

    // we don'fintt care a but the intercept, only the slope
    SimpleRegression slopeCalculation = new SimpleRegression(false);
    for (int i = signalOffset; i < north.length; ++i) {
      slopeCalculation.addData(east[i], north[i]);
    }
    double backAzimuth = Math.atan(1. / slopeCalculation.getSlope());
    backAzimuth = 360 + Math.toDegrees(backAzimuth);

    return backAzimuth;

  }

  static double correctBackAzimuthQuadrant(double azimuth, int signumN, int signumE, int signumZ) {
    double correctedAzimuth = ((azimuth % 360) + 360) % 360;
    double minValue = 0;
    double maxValue = 360;
    if (signumN == signumE) {
      if (signumN == signumZ) {
        // everything in phase
        minValue = 180;
        maxValue = 270;
      } else {
        // Z out of phase
        maxValue = 90;
      }
    } else {
      if (signumN == signumZ) {
        // E out of phase
        minValue = 90;
        maxValue = 180;
      } else {
        // N out of phase
        minValue = 270;
      }
    }
    while (correctedAzimuth < minValue) {
      correctedAzimuth += 90;
    }
    while (correctedAzimuth > maxValue) {
      correctedAzimuth -= 90;
    }
    return correctedAzimuth % 360;
  }

  /**
   * @param secs seconds
   * @param sampleRate sample rate in hertz
   * @return number samples in a given time period.
   */
  static int getSamplesInTimePeriod(int secs, double sampleRate) {
    // samples = time (s) * sampleRate (samp/s)
    return (int) Math.ceil(secs * sampleRate);
  }

  private long getPArrivalTime(EventCMT eventCMT) throws ArrivalTimeException {

    double eventLatitude = eventCMT.getLatitude();
    double eventLongitude = eventCMT.getLongitude();
    double eventDepth = eventCMT.getDepth();
    double stationLatitude = stationMeta.getLatitude();
    double stationLongitude = stationMeta.getLongitude();
    double greatCircleArc = SphericalCoords
        .distance(eventLatitude, eventLongitude, stationLatitude, stationLongitude);
    TauP_Time timeTool;
    try {
      timeTool = new TauP_Time("prem");
      timeTool.parsePhaseList("P");
      timeTool.setSourceDepth(eventDepth);
      timeTool.calculate(greatCircleArc);
    } catch (TauModelException e) {
      //Arrival times are not determinable.
      logger.error(e.getMessage());
      throw new ArrivalTimeException(e.getMessage());
    }

    List<Arrival> arrivals = timeTool.getArrivals();

    double arrivalTimeP;
    if (arrivals.get(0).getName().equals("P")) {
      arrivalTimeP = arrivals.get(0).getTime();
    } else {
      logger.info("Got an arrival, but it was not a P-wave");
      throw new ArrivalTimeException("Arrival time found was not a P-wave");
    }

    logger.info(
        "Event:{} <eventLatitude,eventLongitude> = <{}, {}> Station:{} <{}, {}> greatCircleArc={} tP={}",
        eventCMT.getEventID(), eventLatitude, eventLongitude, getStation(), stationLatitude,
        stationLongitude, greatCircleArc, arrivalTimeP);

    return ((long) arrivalTimeP) * 1000; // get the arrival time in ms
  }

  private static double[] lowPassFilter(double[] toFilt, double sps, double corner) {
    Butterworth cascadeFilter = new Butterworth();
    cascadeFilter.lowPass(4, sps, corner);

    double[] filtered = new double[toFilt.length];
    for (int i = 0; i < toFilt.length; ++i) {
      filtered[i] = cascadeFilter.filter(toFilt[i]);
    }

    return filtered;
  }

  private class ArrivalTimeException extends Exception {

    private static final long serialVersionUID = 6851116640460104395L;

    ArrivalTimeException(String message) {
      super(message);
    }

  }

}