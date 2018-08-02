package asl.seedscan.metrics;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.seedscan.event.EventCMT;
import asl.timeseries.TimeseriesUtils;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sac.SacHeader;
import sac.SacTimeSeries;
import uk.me.berndporr.iirj.Butterworth;

public class EventComparePWaveOrientation extends Metric {

  private static final Logger logger =
      LoggerFactory.getLogger(asl.seedscan.metrics.EventComparePWaveOrientation.class);

  // length of window to take for p-wave data in seconds
  private static final int P_WAVE_WINDOW = 5;

  // range of degrees (arc length) over which data will be valid
  private static final int MIN_DEGREES = 30;
  private static final int MAX_DEGREES = 90;
  /**
   * Filter corner 0.05 Hz or 20 seconds period.
   */
  private static final double LOW_PASS_FILTER_CORNER = 0.05;
  /**
   * 50 second window for p-wave ringing offset data
   */
  private static final int P_WAVE_RINGING_OFFSET = 50;

  public EventComparePWaveOrientation() {
    super();
    addArgument("base-channel");
    addArgument("channel-restriction");
  }

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public String getName() {
    return "EventComparePWaveOrientation";
  }

  /**
   * Determine if a channel meets the needed criteria.
   * @param channel Channel to be tested.
   * @param allowedBands List of allowed bands in String format
   * @return True if filtered out, False if not
   */
  static boolean filterChannel(Channel channel, List<String> allowedBands ){
    //Only process ND channels.
    if (!channel.getChannel().endsWith("ND")) {
      return true;
    }
    //Test if in requested bands
    return !allowedBands.contains(channel.getChannel().substring(0, 2));
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
    String[] basechannel;
    String basePreSplit = null;

    try {
      basePreSplit = get("base-channel");
    } catch (NoSuchFieldException ignored) {
    }

    if (basePreSplit == null) {
      basePreSplit = "XX-LX";
      logger.info("No base channel for Event Compare P Orientation using: " + basePreSplit);
    }
    basechannel = basePreSplit.split("-");

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



    for (Channel curChannel : channels) {

      if (filterChannel(curChannel, allowedBands) ) {
        continue;
      }

      Channel pairChannel = curChannel.getHorizontalOrthogonalChannel();

      // this is the function that produces rotated data given the current chanel
      ByteBuffer digest = metricData.valueDigestChanged(new ChannelArray(curChannel, pairChannel), createIdentifier(curChannel, pairChannel),
          getForceUpdate());

      //Skip if digest is null - Either the value is already computed or there is no data.
      if (digest == null) {
        continue;
      }

      double angleDifference = computeMetric(eventCMTs, curChannel, pairChannel);

      //Catch any bail out of internal metric.
      if (Double.isNaN(angleDifference))
        continue;

      metricResult.addResult(curChannel, angleDifference, digest);
    }
  }

  double computeMetric(Hashtable<String, EventCMT> eventCMTs, Channel curChannel,
      Channel pairChannel){
    double sumAngleDifference = 0.0;
    int numEvents = 0;

    double sampleRateN = stationMeta.getChannelMetadata(curChannel).getSampleRate();
    double sampleRateE = stationMeta.getChannelMetadata(pairChannel).getSampleRate();

    // get lat and long to make sure data is within a reasonable range
    double stationLatitude = stationMeta.getLatitude();
    double stationLongitude = stationMeta.getLongitude();
    // now curChannel, pairChannel the two channels to get the orientation of
    // double azi = stationMeta.getChannelMetadata(curChannel).getAzimuth();

    // now we have the angle for which to rotate data

    // now to get the synthetics data
    SortedSet<String> eventKeys = new TreeSet<>(eventCMTs.keySet());
    for (String key : eventKeys) {

      EventCMT eventMeta = eventCMTs.get(key);
      double eventLatitude = eventMeta.getLatitude();
      double eventLongitude = eventMeta.getLongitude();
      // derived data has azimuth of 0
      double azimuth = SphericalCoords.azimuth(stationLatitude, stationLongitude, eventLatitude, eventLongitude);

      double angleBetween = SphericalCoords.distance(eventLatitude, eventLongitude, stationLatitude, stationLongitude);
      if (angleBetween < MIN_DEGREES || angleBetween > MAX_DEGREES) {
        logger.info("== {}: Arc length to key=[{}] out of range for this station\n",
            getName(), key);
        continue;
      }

      Hashtable<String, SacTimeSeries> synthetics = getEventSynthetics(key);
      if (synthetics == null) {
        logger.warn("== {}: No synthetics found for key=[{}] for this station\n", getName(), key);
        continue;
      }

      // get start time of p-wave, then take data 100 secs before that
      long stationDataStartTime = eventMeta.getTimeInMillis();
      try {
        long pTravelTime = getPArrivalTime(eventMeta);
        stationDataStartTime += pTravelTime;
      } catch (ArrivalTimeException ignore) {
        // error was already logged in getPArrivalTime
        continue;
      }

      // ending of p-wave is this length of time afterward
      // add 100 seconds on each end to compensate for potential filter ringing later
      // (after filtering we will trim the ringing from each side)
      long stationEventEndTime = stationDataStartTime + (P_WAVE_WINDOW + P_WAVE_RINGING_OFFSET) * 1000;
      // set window start back by 100 seconds (units in ms here) plus 50s ring-compensation offset
      stationDataStartTime -= (100 + P_WAVE_RINGING_OFFSET) * 1000; // (100 + X) sec * 1000 ms/sec

      double[] northData = metricData.getWindowedData(curChannel, stationDataStartTime,
          stationEventEndTime);
      double[] eastData = metricData.getWindowedData(pairChannel, stationDataStartTime,
          stationEventEndTime);

      if (northData == null || eastData == null || northData.length != eastData.length) {
        // Missing data within the window; skip to next event in day
        continue;
      }

      // first, we low-pass filter the data
      // filter corner at 0.05Hz (20 s interval)
      // and use a 4 poles in the filter
      northData = lowPassFilter(northData, sampleRateN, LOW_PASS_FILTER_CORNER);
      eastData = lowPassFilter(eastData, sampleRateE, LOW_PASS_FILTER_CORNER);

      // assume there are filter artifacts in first 50 seconds' worth of data
      int afterRinging = getSamplesInTimePeriod(P_WAVE_RINGING_OFFSET, sampleRateN);
      northData = Arrays.copyOfRange(northData, afterRinging, northData.length - afterRinging);
      eastData = Arrays.copyOfRange(eastData, afterRinging, eastData.length - afterRinging);

      // detrend operations are done in-place
      TimeseriesUtils.demean(northData);
      TimeseriesUtils.demean(eastData);

      scaleByMax(northData, eastData);


      // evaluate signal-to-noise ratio of data (RMS)
      // noise is first 15 seconds of data (i.e., before p-wave arrival)
      int noiseLength = getSamplesInTimePeriod(P_WAVE_WINDOW, sampleRateN);
      // signal is last 15 seconds of data (i.e., after p-wave arrival);
      int signalOffset = northData.length - noiseLength;

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

      double signalNoiseRatioNorth = signalNorth / noiseNorth;
      double signalNoiseRatioEast = signalEast / noiseEast;
      double signalNoiseRatioTotal = (signalNorth + signalEast) / (noiseNorth + noiseEast);
      final double SIGNAL_CUTOFF = 5.;
      if (signalNoiseRatioTotal < SIGNAL_CUTOFF) {
        logger.info("== {}: Signal to noise ratio under 5 -- [{} - {}], [{} - {}]", getName(),
            curChannel, signalNoiseRatioNorth, pairChannel, signalNoiseRatioEast);
        continue;
      }

      double sumNN = 0., sumEN = 0., sumEE = 0.;
      for (int i = signalOffset; i < northData.length; ++i) {
        sumNN += northData[i] * northData[i];
        sumEE += eastData[i] * eastData[i];
        sumEN += eastData[i] * northData[i];
      }

      RealMatrix mat = new BlockRealMatrix(new double[][]{{sumNN, sumEN}, {sumEN, sumEE}});
      EigenDecomposition eigenDecomposition = new EigenDecomposition(mat);
      RealMatrix eigenValues = eigenDecomposition.getD();
      double linearity = (eigenValues.getEntry(0, 0) / eigenValues.getTrace()) -
          (eigenValues.getEntry(1, 1) / eigenValues.getTrace());
      if (linearity < 0.95) {
        logger.info("== {}: Skipping; data linearity less than .95 -- [({} - {}) - {}]",
            getName(), curChannel, pairChannel, linearity);
        continue;
      }

      // Make effectively final for lambda
      double[] east = eastData;
      double[] north = northData;
      // now do least squares on the data to get the linearity
      // and then do arctan solving on that to get the angle of orientation
      MultivariateJacobianFunction slopeCalculation = point -> {
        double slope = point.getEntry(0);
        RealVector value = new ArrayRealVector(east.length);
        RealMatrix jacobian = new BlockRealMatrix(east.length, 1);
        for (int i = 0; i < east.length; ++i) {
          value.setEntry(i, (east[i] * slope));
          jacobian.setEntry(i, 0, east[i]);
        }
        return new Pair<>(value, jacobian);
      };

      LeastSquaresProblem lsp = new LeastSquaresBuilder().
          start(new double[]{0.}).
          target(north).
          model(slopeCalculation).
          lazyEvaluation(false).
          maxEvaluations(Integer.MAX_VALUE).
          maxIterations(Integer.MAX_VALUE).
          build();

      LeastSquaresOptimizer.Optimum opt = new LevenbergMarquardtOptimizer().optimize(lsp);
      LeastSquaresProblem.Evaluation initEval = lsp.evaluate(opt.getPoint());
      double backAzimuth = Math.atan(initEval.getPoint().getEntry(0));
      backAzimuth = Math.toDegrees(backAzimuth);

      azimuth = ((azimuth % 360) + 360) % 360;

      if (Math.abs(azimuth - (backAzimuth + 180)) < Math.abs(azimuth - backAzimuth)) {
        // recall that Java modulo permits negative numbers up to -(modulus)
        backAzimuth = ((backAzimuth + 180) % 360 + 360) % 360;
      }

      double angleDifference = azimuth - backAzimuth;

      // now, populate the results from this data
      sumAngleDifference += angleDifference;
      numEvents++;
    }

    //Average our event angle differences.
    if (numEvents <= 0) return Double.NaN;
    else return sumAngleDifference / numEvents;
  }

  /**
   * Scales 2 arrays in place by the max abs value.
   * @param data1 array of doubles
   * @param data2 array of doubles
   */
  static void scaleByMax(double[] data1, double[] data2) {
    if (data1.length != data2.length) {
      String error = "Cannot scale when array sizes differ.";
      logger.error(error);
      throw new IllegalArgumentException(error);
    }
    double max = data1[0];
    // Assumes that data1 and data2 have same length.
    for (int i = 0; i < data1.length; ++i) {
      max = Math.max(max, Math.max(Math.abs(data1[i]), Math.abs(data2[i])));
    }
    // now scale data by maximum value of all components
    if (max == 0) return; //Avoid divide by 0 error. Only occurs if all values are 0.
    for (int i = 0; i < data1.length; ++i) {
      data1[i] /= max;
      data2[i] /= max;
    }
  }

  /**
   *
   * @param secs seconds
   * @param sampleRate sample rate in hertz
   * @return number samples in a given time period.
   */
  private static int getSamplesInTimePeriod(int secs, double sampleRate) {
    // input is the sample rate in hertz
    // samples = 15 (s) * sampleRate (samp/s)
    return (int) Math.ceil(secs * sampleRate);
  }

  private long getPArrivalTime(EventCMT eventCMT) throws ArrivalTimeException {
    double eventLatitude = eventCMT.getLatitude();
    double eventLongitude = eventCMT.getLongitude();
    double eventDepth = eventCMT.getDepth();
    double stationLatitude = stationMeta.getLatitude();
    double stationLongitude = stationMeta.getLongitude();
    double greatCircleArc = SphericalCoords.distance(eventLatitude, eventLongitude, stationLatitude, stationLongitude);
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
        eventCMT.getEventID(), eventLatitude, eventLongitude, getStation(), stationLatitude, stationLongitude, greatCircleArc, arrivalTimeP);

    return ((long) arrivalTimeP) * 1000; // get the arrival time in ms
  }

  /**
   * Gets the sac start time in millis.
   *
   * @param hdr the sac header
   * @return the sac start time in millis
   */
  private static long getSacStartTimeInMillis(SacHeader hdr) {
    GregorianCalendar gcal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    gcal.set(Calendar.YEAR, hdr.getNzyear());
    gcal.set(Calendar.DAY_OF_YEAR, hdr.getNzjday());
    gcal.set(Calendar.HOUR_OF_DAY, hdr.getNzhour());
    gcal.set(Calendar.MINUTE, hdr.getNzmin());
    gcal.set(Calendar.SECOND, hdr.getNzsec());
    gcal.set(Calendar.MILLISECOND, hdr.getNzmsec());

    return gcal.getTimeInMillis();
  }

  private class ArrivalTimeException extends Exception {
    private static final long serialVersionUID = 6851116640460104395L;

    ArrivalTimeException(String message) {
      super(message);
    }

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

}
