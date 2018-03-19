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
    List<String> bands;
    bands = Arrays.asList(preSplitBands.split(","));

    // get lat and long to make sure data is within a reasonable range
    double stationLatitude = stationMeta.getLatitude();
    double stationLongitude = stationMeta.getLongitude();

    // map used for quick access to paired channel data
    // we only do calculations once both N/E channel pairs (or ND/ED)
    // exist in the map
    Map<String, Channel> chNameMap = new HashMap<>();
    for (Channel curChannel : channels) {

      String name = curChannel.toString();
      chNameMap.put(name, curChannel);
      String channelVal = name.split("-")[1];
      if (!bands.contains(channelVal.substring(0, 2))) {
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

      String pairName;
      try {
        pairName = getPairedChannelNameString(name);
      } catch (MetricException e) {
        logger.error("Error in format of channel name (expected E, N, or Z channel code.): " + name);
        continue;
      }

      if (!chNameMap.keySet().contains(pairName)) {
        logger.warn("Could not find data for station with name " + pairName);
        continue;
      }

      // this is the function that produces rotated data given the current chanel
      ByteBuffer digest = metricData.valueDigestChanged(curChannel, createIdentifier(curChannel),
          getForceUpdate());

      Channel pairChannel = chNameMap.get(pairName);
      double sampleRateN = stationMeta.getChannelMetadata(curChannel).getSampleRate();
      double sampleRateE = stationMeta.getChannelMetadata(pairChannel).getSampleRate();
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

        SacTimeSeries sacSynthetics;
        String fileKey = getStn() + "." + basechannel[0] + "." + basechannel[1].substring(0, 2)
            + channelVal.substring(2, 3) + ".modes.sac.proc";
        // e.g. "ANMO.XX.LXZ.modes.sac.proc"
        if (synthetics.containsKey(fileKey)) {
          sacSynthetics = synthetics.get(fileKey);
        } else {
          logger.warn("Did not find sac synthetic=[{}] in Hashtable", fileKey);
          continue; // Try next event
        }

        // get start time of p-wave, then take data 100 secs before that
        SacHeader header = sacSynthetics.getHeader();
        long stationDataStartTime = getSacStartTimeInMillis(header);
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
        // now normalize
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
        for (int i = 0; i < northData.length; ++i) {
          northData[i] /= scaleFactor;
          eastData[i] /= scaleFactor;
        }

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
          logger.warn("== {}: Signal to noise ratio under 5 -- [{} - {}], [{} - {}]", getName(),
              name, signalNoiseRatioNorth, pairName, signalNoiseRatioEast);
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
          logger.warn("== {}: Skipping; data linearity less than .95 -- [({} - {}) - {}]",
              getName(), name, pairName, linearity);
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

        // correct backAzimuth value (i.e., make an actual azimuth)
        if (backAzimuth < 0) {
          backAzimuth = Math.abs(backAzimuth) + 90;
        }
        if (Math.abs(azimuth - (backAzimuth + 180)) < Math.abs(azimuth - backAzimuth)) {
          // recall that Java modulo permits negative numbers up to -(modulus)
          backAzimuth = ((backAzimuth + 180) % 360 + 360) % 360;
        }

        double angleDifference = azimuth - backAzimuth;

        // add warning before publishing result if it's inconsistent with expected
        if (Math.abs(angleDifference) > 5) {
          logger.warn("== {}: Difference btwn calc. and est. azimuth > 5 degrees -- "
              + "[({} - {}) - ({} (calc) vs. {} (exp))]", getName(), name, pairName, backAzimuth, azimuth);
        }

        // now, populate the results from this data
        metricResult.addResult(curChannel, angleDifference, digest);
      }
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

  static String getPairedChannelNameString(String channelName) throws MetricException {
    // get the string for the

    char[] chNameArray = channelName.toCharArray();
    int lastCharIdx = chNameArray.length - 1;
    char lastChar = chNameArray[lastCharIdx];
    if (lastChar == 'D') {
      --lastCharIdx;
      lastChar = chNameArray[lastCharIdx];
    }
    char pairChar = getPairChar(lastChar);
    chNameArray[lastCharIdx] = pairChar;
    return new String(chNameArray);
  }

  private static char getPairChar(char orient) throws MetricException {
    // get N,E pairs (horizontal data) for orientation calculation
    switch (orient) {
      case 'N':
        return 'E';
      case 'E':
        return 'N';
      default:
        throw new MetricException("Error with channel orientation label format");
    }
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
