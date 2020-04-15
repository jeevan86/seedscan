package asl.seedscan.metrics;

import static asl.seedscan.event.ArrivalTimeUtils.getPArrivalTime;

import asl.metadata.Channel;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.PoleZeroStage;
import asl.metadata.meta_new.ResponseStage;
import asl.seedscan.event.ArrivalTimeUtils.ArrivalTimeException;
import asl.seedscan.event.EventCMT;
import asl.timeseries.CrossPower;
import asl.timeseries.InterpolatedNHNM;
import asl.util.Logging;
import asl.utils.FilterUtils;
import asl.utils.NumericUtils;
import edu.sc.seis.TauP.SphericalCoords;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sac.SacHeader;
import sac.SacTimeSeries;

/**
 * Calculates the quality of data relative to an event if it is a feasible candidate for
 *
 */
public class WPhaseQualityMetric extends Metric {

  private static final Logger logger = LoggerFactory
      .getLogger(WPhaseQualityMetric.class);

  private static final double PERCENT_CUTOFF = 3.;
  private static final double DAMPING_CONSTANT = 0.707; // TODO: give more precision to this?
  // anticipated corner frequency of, say, STS-2 instrument
  private static final double CORNER_FREQ_120 = 2. * Math.PI / 120.;
  // anticipated corner frequency of, say, STS-1
  private static final double CORNER_FREQ_360 = 2. * Math.PI / 360.;

  /**
   * Number of milliseconds in 3 hours of data
   */
  private static final long THREE_HRS_MILLIS = 3 * 60 * 60 * 1000;

  public WPhaseQualityMetric() {
    // TODO: add additional parameters?
    super();
  }

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public String getName() {
    return "WPhaseQualityMetric";
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

    // TODO: find out a good way to implement restrictions for this metric
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

    // data expected to be continuous for these methods to work
    List<Channel> channels = stationMeta.getContinuousChannels();
    for (Channel channel : channels) {
      String channelVal = channel.toString().split("-")[1];
      if (!allowedBands.contains(channelVal.substring(0, 2))) {
        // current channel not part of valid band options, skip it
        continue;
      }
      ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel),
          getForceUpdate());

      if (digest == null) {
        continue;
      }

      try { // computeMetric() handle
        double result = computeMetric(channel, eventCMTs);
        if (result != NO_RESULT) {
          metricResult.addResult(channel, result, digest);
        }
      } catch (MetricException e) {
        logger.error(Logging.prettyExceptionWithCause(e));
      }
    }
  }

  private double computeMetric(Channel channel, Hashtable<String, EventCMT> eventCMTs)
      throws MetricException {

    // w is used later to choose which nominal corner freq is appropriate for this inst.
    double w;
    {
      Pair<Double, Double> pair = getFreqAndDamping(stationMeta.getChannelMetadata(channel));
      if (pair == null) {
        logger.warn("== {}: Metadata for channel=[{}] does not include pole/zero stage, skipping",
            getName(), getStation() + "-" + channel.toString());
        return NO_RESULT;
      }
      w = pair.getFirst();
      // corner frequency (w) and damping (h)
      double h = pair.getSecond();
      // prescreening step: get instrument response and compare to a specific dummy response
      if (passesResponseCheck(w, h)) {
        // TODO: add more explicit information here about channel, etc.
        logger.warn("== {}: channel=[{}] metadata differs from nominal parameters by > 3%,"
                + " skipping", getName(), getStation() + "-" + channel.toString());
        return NO_RESULT;
      }
    }

    // get lat and long to make sure data is within a reasonable range
    double stationLatitude = stationMeta.getLatitude();
    double stationLongitude = stationMeta.getLongitude();

    // numbers used to get the count of passable events per total events analyzed
    int eventsPassed = 0;
    int numEvents = 0;


    SortedSet<String> eventKeys = new TreeSet<>(eventCMTs.keySet());
    for (String key : eventKeys) {

      EventCMT eventCMT = eventCMTs.get(key);

      // first prescreen step: make sure that the station is < 90 deg. from the event
      double eventLatitude = eventCMT.getLatitude();
      double eventLongitude = eventCMT.getLongitude();
      double angleBetween = SphericalCoords
          .distance(eventLatitude, eventLongitude, stationLatitude, stationLongitude);
      if (angleBetween > 90) {
        // TODO: make this info statement more detailed
        logger.info("== {}: Arc from event (key=[{}]) to station=[{}] too large for evaluation.",
            getName(), key, getStation());
        // presumably we don't have this as a matter of the
        continue;
      }

      // second prescreen step: take the sum of the PSD of 3 hours of data pre-event
      // and then get the average difference over the 1-10 mHz range with the NHNM curve
      // if the average difference is positive then don't compute the metric here
      long eventStart = eventCMT.getTimeInMillis();
      long pTravelTime = 0;
      // this block will do the prescreening operation and scope out the 3-hour timeseries data
      {
        try {
          // TODO: see if any correction needs to be done to this time
          pTravelTime = getPArrivalTime(eventCMT, stationMeta, logger);
          eventStart += pTravelTime;
        } catch (ArrivalTimeException ignore) {
          // error was already logged in getPArrivalTime
          continue;
        }
        long prescreenWindowStart = eventStart - THREE_HRS_MILLIS;
        double[] prescreenCheck =
            metricData.getWindowedData(channel, prescreenWindowStart, eventStart);

        CrossPower crossPower = getCrossPower(channel, channel);
        double[] psd = crossPower.getSpectrum();
        double df = crossPower.getSpectrumDeltaF();
        // for array, index * df = frequency at that index
        // so if we want to get the 1-10Hz region of the data, divide freq by df and make integer
        int lowerBound = (int) Math.floor(0.001/df);
        int upperBound = (int) Math.ceil(0.010/df);
        psd = Arrays.copyOfRange(psd, lowerBound, upperBound);
        double[] freqs = new double[upperBound - lowerBound];
        assert(psd.length == freqs.length);
        for (int i = lowerBound; i < upperBound; ++i) {
          int arrayIndex = i - lowerBound;
          freqs[arrayIndex] = df * i;
        }
        double[] NHNM = getNHNMOverFrequencies(freqs, df);
        double difference = 0.;
        for (int i = 0; i < freqs.length; ++i) {
          difference += (NHNM[i] - psd[i]);
        }
        if (difference > 0) {
          logger.warn("== {}: Difference between NHNM and PSD was positive; "
                  + "channel=[{}] is too noisy.",
              getName(), getStation() + "-" + channel.toString());
          ++numEvents;
          continue;
        }
      }

      // getting the data. time range is from event start to 15sec * degrees distance
      long endTime = eventStart + (long) (15000 * angleBetween);
      double[] data = metricData.getWindowedData(channel, eventStart, endTime);
      double sampleRate = stationMeta.getChannelMetadata(channel).getSampleRate();
      // time-domain deconvolution and bandpass filtering (1-5 mHz band) goes here
      // we require the gain, so we can use the stage 0 as overall gain
      double gain = stationMeta.getChannelMetadata(channel).getStage(0).getStageGain();
      data = getRecursiveFilter(data, 1./sampleRate, w, gain);
      // recursive filter gives us acceleration so go into velocity
      data = performIntegrationByTrapezoid(data, 1./sampleRate);
      // and now into displacement by integrating twice
      data = performIntegrationByTrapezoid(data, 1./sampleRate);
      // and lastly perform a band-pass filter on the data from 1-5 milliHertz
      FilterUtils.bandFilter(data, sampleRate, 0.001, 0.005, 4);

      // after processing: how does the peak-to-peak value compare to the median value?
      // if the min or max value is too far from the median, we reject this
      double[] sortedData = Arrays.copyOf(data, data.length);
      Arrays.sort(sortedData);
      double min = data[0];
      double median = sortedData[sortedData.length / 2];
      double max = data[data.length - 1];
      if (min < median * 0.1 || max > median * 3.) {
        logger.info("== {}: Min or max value of event trace outside median screen bounds; "
                + "channel=[{}] amplitude is unstable for analysis.",
            getName(), getStation() + "-" + channel.toString());
        ++numEvents;
        continue;
      }

      // last screen is misfit against the synthetic data over the same time range
      Hashtable<String, SacTimeSeries> synthetics = getEventSynthetics(key);
      if (synthetics == null) {
        logger.warn("== {}: No synthetics found for key=[{}] for this station=[{}]\n",
            getName(), key, getStation());
        continue;
      }
      String syntheticsName = stationMeta.getStation() +
          channel.toString().replace('-', '.') + ".modes.sac.proc";
      if (!synthetics.containsKey(syntheticsName)) {
        logger.error("Did not find sac synthetic=[{}] in Hashtable", syntheticsName);
        // if there's no synthetic to compare to, don't increase the event count (TODO: verify)
        continue; // Try next event
      }

      SacTimeSeries sacSynthetics = synthetics.get(syntheticsName);
      SacHeader header = sacSynthetics.getHeader();
      double delta = (double) header.getDelta();
      double synthSampleRate = 1. / delta;
      assert(synthSampleRate == sampleRate);

      // TODO: see if we can presume the synthetic start matches the one given by the CMT
      assert(eventStart == EventCompareSynthetic.getSacStartTimeInMillis(header));

      int startingIndex = (int) Math.ceil((pTravelTime * synthSampleRate) / 1000);
      float[] synthData =
          Arrays.copyOfRange(sacSynthetics.getY(), startingIndex, startingIndex + data.length);
      double numerator = 0;
      double denominator = 0;
      for (int i = 0; i < data.length; ++i) {
        denominator += Math.pow(synthData[i], 2);
        numerator += Math.pow(synthData[i] - data[i], 2);
      }
      if (numerator / denominator > 3) {
        logger.warn("== {}: Trace misfit against synthetic data outside bound of 3.0; "
                + "channel=[{}] is too noisy.",
            getName(), getStation() + "-" + channel.toString());
      }

      // now that we've passed all screenings, we've got an event that is good, and can count it
      ++eventsPassed;
      ++numEvents;
    }
    if (numEvents == 0) {
      logger.info("== {}: No valid events found for channel=[{}]", getName(),
          getStation() + "-" + channel.toString());
      return NO_RESULT;
    }
    return ((double) eventsPassed) / numEvents;
  }

  double[] performIntegrationByTrapezoid(double[] toIntegrate, double deltaT) {
    double[] integration = new double[toIntegrate.length];
    integration[0] = 0.;
    for (int i = 1; i < integration.length; ++i) {
      // cumulative sum plus the function of linear slope between
      integration[i] = deltaT * (integration[i-1] + ((toIntegrate[i] - toIntegrate[i-1]) / 2.));
    }
    return integration;
  }

  double[] getRecursiveFilter(double[] y, double deltaT, double cornerFreq, double gain) {
    int length = y.length;
    double[] filter = new double[length];
    double h = 0.707;
    // corner frequencies are expected to be either 120 or 360
    double w = Math.abs(120 - cornerFreq) > Math.abs(360 - cornerFreq) ? 360 : 120;
    double c0 = 1/(gain * deltaT);
    double c1 = -2 * (1 + (h * w * deltaT)) / (gain * deltaT);
    double c2 = (1 + 2 * h * w * deltaT + Math.pow(w * deltaT, 2)) / (gain * deltaT);
    // based on the algorithm described in Kanamori, Rivera 2008 about phase inversion
    // first two values are fixed at 0
    for (int i = 2; i < length; ++i) {
      filter[i] = filter[i-1] + c0 * y[i] + c1 * y[i-1] + c2 * y[i-2];
    }

    return filter;
  }

  static boolean passesResponseCheck(double w, double h) {

      double fError = 100 * Math.abs(h - DAMPING_CONSTANT) / Math.abs(DAMPING_CONSTANT);
      double hError120 =  100 * Math.abs(w - CORNER_FREQ_120) / Math.abs(CORNER_FREQ_120);
      double hError360 =  100 * Math.abs(w - CORNER_FREQ_360) / Math.abs(CORNER_FREQ_360);
      // make sure both freq and damping are within error threshold
      if (fError < PERCENT_CUTOFF && (hError120 < PERCENT_CUTOFF || hError360 < PERCENT_CUTOFF)) {
        return true;
      }

    return false;
  }

  static Pair<Double, Double> getFreqAndDamping(ChannelMeta channelMeta) {
    for (Integer stageKey : channelMeta.getStages().keySet()) {
      ResponseStage stage = channelMeta.getStage(stageKey);
      if (!(stage instanceof PoleZeroStage)) {
        // need a PZ stage to get the damping and frequency constant
        continue;
      }
      PoleZeroStage pzStage = (PoleZeroStage) stage;
      List<Complex> poles = pzStage.getPoles();
      NumericUtils.complexMagnitudeSorter(poles);
      Complex pole = poles.get(0);
      double w = pole.abs(); // corner frequency
      double h = Math.abs(pole.getReal() / pole.abs()); // damping
      return new Pair<>(w, h);
    }
    return null;
  }

  static double[] getNHNMOverFrequencies(double[] frequencies, double df) {
    double[] returnValue = new double[frequencies.length];
    double minX = frequencies[0];
    for (int i = 0; i < frequencies.length; ++i) {
      double f = minX + df * i;
      double period = 1. / f;
      double modelValue = InterpolatedNHNM.fnhnm(period);
      returnValue[i] = modelValue;
    }
    return returnValue;
  }

  /**
   * Determine if a channel meets the needed criteria.
   *
   * @param channel Channel to be tested.
   * @param allowedBands List of allowed bands in String format
   * @return True if filtered out, False if not
   */
  static boolean filterChannel(Channel channel, List<String> allowedBands) {
    //Only process derived channels.
    if (!channel.getChannel().endsWith("D")) {
      return true;
    }
    //Test if in requested bands
    return !allowedBands.contains(channel.getChannel().substring(0, 2));
  }
}
