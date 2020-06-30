package asl.seedscan.metrics;

import static asl.seedscan.event.ArrivalTimeUtils.getPArrivalTime;
import static asl.utils.FilterUtils.bandFilter;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.ChannelKey;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.ChannelMetaException;
import asl.metadata.meta_new.PoleZeroStage;
import asl.metadata.meta_new.ResponseStage;
import asl.seedscan.event.ArrivalTimeUtils.ArrivalTimeException;
import asl.seedscan.event.EventCMT;
import asl.timeseries.CrossPower;
import asl.timeseries.InterpolatedNHNM;
import asl.util.Logging;
import asl.utils.NumericUtils;
import edu.sc.seis.TauP.SphericalCoords;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
  private static final double DAMPING_CONSTANT = 0.707; // may need to give more precision to this
  // anticipated corner frequency of, say, STS-2 instrument
  private static final double CORNER_FREQ_120 = 2. * Math.PI / 120.;
  // anticipated corner frequency of, say, STS-1
  private static final double CORNER_FREQ_360 = 2. * Math.PI / 360.;

  /**
   * Number of milliseconds in 3 hours of data
   */
  private static final long THREE_HRS_MILLIS = 3 * 60 * 60 * 1000;

  public WPhaseQualityMetric() {
    super();
    addArgument("channel-restriction");
    addArgument("base-channel");
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

    Hashtable<String, EventCMT> eventCMTs = getEventTable();
    if (eventCMTs == null) {
      logger.info(
          String.format("No Event CMTs found for Day=[%s] --> Skip W-Phase Quality Metric",
              getDay()));
      return;
    }

    String basePreSplit = null;
    String preSplitBands = null;
    try {
      preSplitBands = get("channel-restriction");
    } catch (NoSuchFieldException ignored) {
    }
    if (preSplitBands == null) {
      preSplitBands = "LH";
      logger.info("No band restriction set, using: {}", preSplitBands);
    }
    List<String> allowedBands = Arrays.asList(preSplitBands.split(","));

    try {
      basePreSplit = get("base-channel");

    } catch (NoSuchFieldException ignored) {
    }
    if (basePreSplit == null) {
      basePreSplit = "XX-LX";
      logger.info("No base channel for W Phase Quality, using: " + basePreSplit);
    }
    String [] basechannel = basePreSplit.split("-");

    List<Channel> channels = stationMeta.getRotatableChannels();
    List<Channel> validChannels = new LinkedList<>();
    for (Channel channel : channels) {
      // This digest is not injected into the DB, but this prevents channels with missing data from being processed.
      // This also rotates channels if possible.
      ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel),
          getForceUpdate());
      if (digest == null) {
        continue;
      }

      String channelVal = channel.toString().split("-")[1];
      if (allowedBands.contains(channelVal.substring(0, 2))) {

        if (metricData.getNextMetricData() != null) {
          logger.info("No result gotten for station:[{}] channel:[{}] day:[{}]",
              getStation(), channel, getDay());
          //Rotate next day's data if possible, not handled by valuedigest. Special case for event metrics.
          metricData.getNextMetricData().checkForRotatedChannels(new ChannelArray(channel.getLocation(), channel.getChannel()));
        }
        validChannels.add(channel);
      }
    }

    if (validChannels.size() == 0){
      // Bail, no valid channels.
      return;
    }

    //Compare only to the first channel, since all channels should have the same digest value if they exist.
    //At this point all channels in allowed list will have data, because of the earlier check.
    ByteBuffer digest = metricData.valueDigestChanged(new ChannelArray(validChannels), createIdentifier(validChannels.get(0)),
        getForceUpdate());
    if (digest == null) {
      logger.info("Digest unchanged station:[{}] channel:[{}] day:[{}] --> Skip metric",
          getStation(), validChannels.get(0).toString(), getDay());
      //Bail, nothing is changed since last calculation in any of the channels, nothing to compute.
      return;
    }

    Map<ChannelKey, ResultIncrementer> results = computeMetric(validChannels, eventCMTs, basechannel);
    for (ChannelKey channelKey : results.keySet()) {
      Channel channel = channelKey.toChannel();

      double result = results.get(channelKey).getResult();
      if (result != NO_RESULT) {
        //Use the shared digest between all dependent channels.
        metricResult.addResult(channel, result, digest);
      }
    }
  }

  private Map<ChannelKey, ResultIncrementer> computeMetric(Collection<Channel> channels,
      Hashtable<String, EventCMT> eventCMTs, String[] basechannel){

    // the logic in this method is a bit weird because we have a series of filtering operations
    // that remove some data a couple steps into the operation -- and then have to do a test
    // over all the traces that aren't yet excluded after those steps. Lots of maps to store
    // intermediate results of functions that we want to keep cached for later.

    // used to store the damping parameter for channels that pass the first screening
    Map<ChannelKey, Double> channelsWithCornerFreq = new HashMap<>();
    // used to identify outliers of peak-to-peak difference compared to median value
    Map<ChannelKey, double[]> tracesPerUnfilteredChannel = new HashMap<>();
    // stores metric results for data, and is what is actually returned
    Map<ChannelKey, ResultIncrementer> channelsWithMetricResults = new HashMap<>();

    // this allows us to immediately filter-out results with inappropriate metadata
    for (Channel channel : channels) {
      // w is used later to choose which nominal corner freq is appropriate for this inst.
      double w;
      Pair<Double, Double> pair = getFreqAndDamping(stationMeta.getChannelMetadata(channel));
      if (pair == null) {
        logger.warn("Metadata for channel=[{}] does not include pole/zero stage, skipping",
            getStation() + "-" + channel.toString());
        continue;
      }
      w = pair.getFirst();
      // corner frequency (w) and damping (h)
      double h = pair.getSecond();
      // prescreening step: get instrument response and compare to a specific dummy response
      if (!passesResponseCheck(w, h)) {
        logger.warn("channel=[{}] metadata differs from nominal parameters by > 3%,"
            + " skipping", getStation() + "-" + channel.toString());
        continue;
      }
      channelsWithCornerFreq.put(new ChannelKey(channel), w);
    }

    double stationLatitude = stationMeta.getLatitude();
    double stationLongitude = stationMeta.getLongitude();
    SortedSet<String> eventKeys = new TreeSet<>(eventCMTs.keySet());
    for (String key : eventKeys) {
      EventCMT eventCMT = eventCMTs.get(key);

      // first prescreen step is to ensure that we're close enough to the event for it to matter
      double eventLatitude = eventCMT.getLatitude();
      double eventLongitude = eventCMT.getLongitude();
      double angleBetween = SphericalCoords
          .distance(eventLatitude, eventLongitude, stationLatitude, stationLongitude);
      if (angleBetween > 90) {
        logger.info("Arc ({}) from event (key=[{}]) to station=[{}] too large for evaluation.",
            angleBetween, key, getStation());
        continue;
      }

      long eventStart = eventCMT.getTimeInMillis();
      long pTravelTime;
      try {
        // TODO: see if any further correction needs to be done to this time
        pTravelTime = getPArrivalTime(eventCMT, stationMeta, getName());
        eventStart += pTravelTime;
      } catch (ArrivalTimeException ignore) {
        // error was already logged in getPArrivalTime
        continue;
      }

      // second prescreen step: take the sum of the PSD of 3 hours of data pre-event
      // and then get the average difference over the 1-10 mHz range with the NHNM curve
      // if the average difference is positive then don't compute the metric here
      // we also extract the traces from data that passes this check to do more comparisons on
      for (ChannelKey channelKey : channelsWithCornerFreq.keySet()) {
        Channel channel = channelKey.toChannel();
        channelsWithMetricResults.put(channelKey, new ResultIncrementer());
        // this block will do the prescreening operation and scope out the 3-hour timeseries data
        {
          long prescreenWindowStart = eventStart - THREE_HRS_MILLIS;
          double[] prescreenCheck =
              metricData.getWindowedData(channel, prescreenWindowStart, eventStart);
          // run the getCrossPower stuff specifically on the above range of data and no more
          CrossPower crossPower = null;
          try {
            crossPower = new CrossPower(channel, channel, metricData,
                prescreenCheck, prescreenCheck);
          } catch (MetricPSDException | ChannelMetaException e) {
            logger.error("Unable to create CrossPower for channel {}-{}", getStation(), channel, e);
            continue;
          }
          double difference =
              passesPSDNoiseScreening(crossPower.getSpectrum(), crossPower.getSpectrumDeltaF());

          if (difference > 0) {
            logger.warn("Difference ({}) between NHNM and PSD was positive; "
                    + "channel=[{}] is too noisy.", difference,
                getStation() + "-" + channel.toString());
            channelsWithMetricResults.get(channelKey).addInvalidCase();
            continue;
          }
        }

        double w = channelsWithCornerFreq.get(channelKey);
        // getting the data. time range is from event start to 15sec * degrees distance
        long endTime = eventStart + (long) (15000 * angleBetween);
        double[] data = metricData.getWindowedData(channel, eventStart, endTime);
        if (data == null) {
          continue;
        }
        double sampleRate = stationMeta.getChannelMetadata(channel).getSampleRate();
        // time-domain deconvolution and bandpass filtering (1-5 mHz band) goes here
        // we require the gain, so we can use the stage 0 as overall gain
        double gain = stationMeta.getChannelMetadata(channel).getStage(0).getStageGain();
        data = getRecursiveFilter(data, 1. / sampleRate, w, gain);

        // perform a band-pass filter on the data from 1-5 milliHertz
        data = bandFilter(data, sampleRate, 0.001, 0.005, 4);
        // recursive filter gives us acceleration so go into velocity
        data = performIntegrationByTrapezoid(data, 1. / sampleRate);
        // and now into displacement by integrating twice
        data = performIntegrationByTrapezoid(data, 1. / sampleRate);

        tracesPerUnfilteredChannel.put(channelKey, data);
      }

      if (tracesPerUnfilteredChannel.size() == 0) {
        logger.error("No data matched to event {} was able to be acquired for peak-to-peak "
            + "analysis at station {}", key, getStation());
        continue;
      }

      // next step to check: does each channel have a peak-to-peak difference that's near the
      // median value of that set? This is over ALL channels associated with the station that
      // haven't yet been filtered out
      {
        Map<ChannelKey, Double> channelToPeakToPeak = new HashMap<>();
        for (ChannelKey channelKey : tracesPerUnfilteredChannel.keySet()) {
          double[] data = tracesPerUnfilteredChannel.get(channelKey);
          double min = data[0];
          double max = data[0];
          for (int i = 1; i < data.length; ++i) {
            min = Math.min(min, data[i]);
            max = Math.max(max, data[i]);
          }
          double peakToPeak = max - min;
          channelToPeakToPeak.put(channelKey, peakToPeak);
        }
        double median;
        {
          List<Double> peakToPeaksForMedian = new ArrayList<>(channelToPeakToPeak.values());
          Collections.sort(peakToPeaksForMedian);
          median = peakToPeaksForMedian.get(peakToPeaksForMedian.size() / 2);
        }
        // now to actually check each channel and find the ones that are in the set
        // and we will filter out the ones that are not so we don't keep processing them here
        for (ChannelKey channelKey : channelToPeakToPeak.keySet()) {
          Channel channel = channelKey.toChannel();
          double peakToPeak = channelToPeakToPeak.get(channelKey);
          if (peakToPeak < median * 0.1 || peakToPeak > median * 3.) {
            logger.info(
                "Amplitude difference ({}) of event trace outside median ({}) screen bounds; "
                    + "channel=[{}] amplitude is too wide for analysis.",
                peakToPeak, median, getStation() + "-" + channel.toString());
            // almost 100% sure that trying to remove from the map while in the loop would
            // mess up the iteration here and make everything wrong
            tracesPerUnfilteredChannel.remove(channelKey);
            channelsWithMetricResults.get(channelKey).addInvalidCase();
          }
        }
      }

      // last screen is misfit against the synthetic data over the same time range
      Hashtable<String, SacTimeSeries> synthetics = getEventSynthetics(key);
      if (synthetics == null) {
        logger.warn("No synthetics found for key=[{}] for this station=[{}]\n", key, getStation());
        continue;
      }

      for (ChannelKey channelKey : tracesPerUnfilteredChannel.keySet()) {
        Channel channel = channelKey.toChannel();
        double[] data = tracesPerUnfilteredChannel.get(channelKey);
        double sampleRate = stationMeta.getChannelMetadata(channel).getSampleRate();
        String syntheticsName =
            getStn() + "." + basechannel[0] + "." + basechannel[1].substring(0, 2)
                + channel.toString().split("-")[1].substring(2, 3) + ".modes.sac.proc";
        if (!synthetics.containsKey(syntheticsName)) {
          logger.error("Did not find sac synthetic=[{}] in Hashtable", syntheticsName);
          // if there's no synthetic to compare to, don't increase the event count (TODO: verify)
          continue; // Try next event
        }

        SacTimeSeries sacSynthetics = synthetics.get(syntheticsName);
        SacHeader header = sacSynthetics.getHeader();
        double delta = header.getDelta();
        double synthSampleRate = 1. / delta;
        assert (synthSampleRate == sampleRate);

        // synthetic data starts at event time, not including arrivals
        int startingIndex = (int) Math.ceil((pTravelTime * synthSampleRate) / 1000);
        float[] synthData =
            Arrays.copyOfRange(sacSynthetics.getY(), startingIndex, startingIndex + data.length);

        // synthetic data is in m but sensor data is in nm, so convert sensor data from nm to m
        for (int i = 0; i < data.length; ++i) {
          data[i] *= 1E-9;
        }
        double[] synthDataDbl = floatToDouble(synthData);

        if (!passesMisfitScreening(synthDataDbl, data)) {
          logger.warn("Trace misfit against synthetic data outside bound of 3.0; "
              + "channel=[{}-{}] is too noisy.", getStation(), channel.toString());
          channelsWithMetricResults.get(channelKey).addInvalidCase();
          continue;
        }

        // now that we've passed all screenings, we've got an event that is good, and can count it
        channelsWithMetricResults.get(channelKey).addValidCase();
      }
    }

    return channelsWithMetricResults;
  }

  /**
   * Ensure that the crosspower for the data meets the noise screening requirements (i.e.,
   * data is overall lower than the NHNM from the range 1mHz, 10mHz range).
   * If the value is positive then the noise screening is considered failed.
   * @param psd PSD data over (positive) frequency range (index 0 = 0Hz)
   * @param df Change in frequency between PSD values
   * @return Total difference between PSD and NHNM over frequency range
   */
  static double passesPSDNoiseScreening(double[] psd, double df) {
    // for array, index * df = frequency at that index
    // so if we want to get the 1-10mHz region of the data, divide freq by df and make integer
    // we'll make the bounds inclusive so that 0.001 or last point less than it is included
    int lowerBound = (int) Math.floor(0.001 / df);
    // make sure that 10 mHz is included in the set or first point above that value
    int upperBound = (int) Math.ceil(0.010 / df) + 1;
    upperBound = Math.min(upperBound, psd.length);
    psd = Arrays.copyOfRange(psd, lowerBound, upperBound);
    double[] freqs = new double[upperBound - lowerBound];
    assert(psd.length == freqs.length);
    for (int i = lowerBound; i < upperBound; ++i) {
      int arrayIndex = i - lowerBound;
      freqs[arrayIndex] = df * i;
    }
    double[] NHNM = getNHNMOverFrequencies(freqs);
    double difference = 0.;
    for (int i = 0; i < freqs.length; ++i) {
      // need to convert psd value into log value
      double psdValue = 10 * Math.log10(psd[i]);
      difference += (psdValue - NHNM[i]);
    }
    return difference;
  }

  static double[] floatToDouble(float[] floats) {
    double[] data = new double[floats.length];
    for (int i = 0; i < floats.length; ++i) {
      data[i] = floats[i];
    }
    return data;
  }

  /**
   * Perform the misfit screening step as described in Duputel, Rivera, et. al (2012).
   * Sum of squared difference between the synth and corrected sensor traces divided by the
   * sum of squared synth data points. Returns false if this value is < 3.
   * Will return true if the synthetic data is 0., though this is not an expected input.
   * @param synthData Synthetic data derived from event specification
   * @param channelData Processed (filtered) channel data over the given range.
   * @return True if the synthetic data is within our expected error range.
   */
  static boolean passesMisfitScreening(float[] synthData, double[] channelData) {
    return passesMisfitScreening(floatToDouble(synthData), channelData);
  }

  /**
   * Perform the misfit screening step as described in Duputel, Rivera, et. al (2012).
   * Sum of squared difference between the synth and corrected sensor traces divided by the
   * sum of squared synth data points. Returns false if this value is < 3.
   * Will return true if the synthetic data is 0., though this is not an expected input.
   * @param synthData Synthetic data derived from event specification
   * @param channelData Processed (filtered) channel data over the given range.
   * @return True if the synthetic data is within our expected error range.
   */
  static boolean passesMisfitScreening(double[] synthData, double[] channelData) {
    double numerator = 0;
    double denominator = 0;

    for (int i = 0; i < channelData.length; ++i) {
      denominator += Math.pow(synthData[i], 2);
      numerator += Math.pow(synthData[i] - channelData[i], 2);
    }
    // if denominator is zero short-circuit to prevent errors
    return denominator == 0. || numerator / denominator < 3;
  }

  /**
   * Perform trapezoid integration on set of data with equal space.
   * Meant to match CUMTRAPZ functions in matlab or numpy
   * @param toIntegrate Y values of some function
   * @param deltaT spacing between each value
   * @return integrated function by trapezoid rule
   */
  static double[] performIntegrationByTrapezoid(double[] toIntegrate, double deltaT) {
    double[] integration = new double[toIntegrate.length];
    integration[0] = 0;
    for (int i = 1; i < integration.length; ++i) {
      // cumulative sum plus the function of linear slope between
      integration[i] = integration[i-1] + deltaT * ((toIntegrate[i] + toIntegrate[i-1]) / (2));
    }
    return integration;
  }

  /**
   * Produces a response filter in the time domain given a channel's timeseries data.
   * Derived from formula given in Kanamori and Rivera (2008).
   * @param y Timeseries data
   * @param deltaT Sample interval of data (seconds)
   * @param cornerFreq Corner frequency of response (for matching to closest nominal of 120 or 360)
   * @param gain Gain value taken from sensitivity of resp (stage 0 gain)
   * @return Timeseries data with response deconvolution done
   */
  static double[] getRecursiveFilter(double[] y, double deltaT, double cornerFreq, double gain) {
    int length = y.length;
    double[] filter = new double[length];
    double h = 0.707;
    // corner frequencies are expected to be either 120 or 360
    double w = Math.abs(CORNER_FREQ_120 - cornerFreq) < Math.abs(CORNER_FREQ_360 - cornerFreq) ?
        CORNER_FREQ_120 : CORNER_FREQ_360;
    double c0 = 1/(gain * deltaT);
    double c1 = -2 * (1 + (h * w * deltaT)) / (gain * deltaT);
    double c2 = (1 + 2 * h * w * deltaT + Math.pow(w * deltaT, 2)) / (gain * deltaT);
    // based on the algorithm described in Kanamori, Rivera 2008 about phase inversion
    // first two values are fixed at 0
    for (int i = 2; i < length; ++i) {
      filter[i] = filter[i-2] + c0 * y[i-2] + c1 * y[i-1] + c2 * y[i];
    }

    return filter;
  }

  /**
   * Returns true if response-derived damping and corner frequency are within the acceptable error
   * bound {@link #PERCENT_CUTOFF} of nominal responses.
   * The values are respectively compared to {@link #DAMPING_CONSTANT} and
   * either {@link #CORNER_FREQ_120} or {@link #CORNER_FREQ_360} as appropriate.
   * @param w Corner frequency derived from response
   * @param h Damping value derived from response
   * @return True if the values are within the acceptable error compared to the nominal values.
   */
  static boolean passesResponseCheck(double w, double h) {
    double hError = 100 * Math.abs(h - DAMPING_CONSTANT) / Math.abs(DAMPING_CONSTANT);
    double wError120 =  100 * Math.abs(w - CORNER_FREQ_120) / Math.abs(CORNER_FREQ_120);
    double wError360 =  100 * Math.abs(w - CORNER_FREQ_360) / Math.abs(CORNER_FREQ_360);
    // make sure both freq and damping are within error threshold
    return hError < PERCENT_CUTOFF && (wError120 < PERCENT_CUTOFF || wError360 < PERCENT_CUTOFF);
  }

  /**
   * Derive the corner frequency and damping from the channel's response's first pole and zero.
   * @param channelMeta metadata for the channel under analysis
   * @return Pair with the corner frequency and damping parameters as first and second value
   */
  static Pair<Double, Double> getFreqAndDamping(ChannelMeta channelMeta) {
    for (Integer stageKey : channelMeta.getStages().keySet()) {
      ResponseStage stage = channelMeta.getStage(stageKey);
      if (!(stage instanceof PoleZeroStage)) {
        // need a PZ stage to get the damping and frequency constant
        continue;
      }
      PoleZeroStage pzStage = (PoleZeroStage) stage;
      List<Complex> poles = pzStage.getPoles();
      if (poles.size() == 0) {
        // prevent index out of bounds exception for unusual metadata where PZ stage has no poles
        continue;
      }
      NumericUtils.complexMagnitudeSorter(poles);
      Complex pole = poles.get(0);
      double w = pole.abs(); // corner frequency (2pi correction accounted for in resp check)
      double h = Math.abs(pole.getReal() / pole.abs()); // damping
      return new Pair<>(w, h);
    }
    return null;
  }

  /**
   * Get the interpolated NHNM over a set of frequencies for PSD comparison
   * @param frequencies Range of PSD frequencies to compare with NHNM
   * @return NHNM values evaluated at each frequency
   */
  static double[] getNHNMOverFrequencies(double[] frequencies) {
    double[] returnValue = new double[frequencies.length];
    for (int i = 0; i < frequencies.length; ++i) {
      double f = frequencies[i];
      // NHNM interpolation function expects period values
      double period = 1. / f;
      double modelValue = InterpolatedNHNM.fnhnm(period);
      returnValue[i] = modelValue;
    }
    return returnValue;
  }

  private class ResultIncrementer {
    private int numerator;
    private int denominator;
    public ResultIncrementer() {
      numerator = 0;
      denominator = 0;
    }

    public void addValidCase() {
      ++numerator;
      ++denominator;
    }
    public void addInvalidCase() {
      ++denominator;
    }
    public double getResult() {
      if (denominator == 0) {
        return NO_RESULT;
      }
      return (double) numerator / denominator;
    }
  }
}
