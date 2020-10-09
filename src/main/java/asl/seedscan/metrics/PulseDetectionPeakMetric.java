package asl.seedscan.metrics;

import asl.metadata.Channel;
import asl.metadata.ChannelKey;
import asl.metadata.meta_new.ChannelMetaException;
import asl.seedscan.metrics.PulseDetectionMetric.PulseDetectionData.PulseDetectionPoint;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets the maximum-amplitude pulse over a channel's data based on a user-specified lower bound
 * for coefficient of cross-correlation with a step function. The highest amplitude that passes
 * the backend filtering criteria and is above that coefficient lower-bound is published.
 * Amplitude is specifically defined as correlation amplitude with a step-function,
 * rather than the value from the sensor at a given moment.
 * @see PulseDetectionMetric (The backend for pulse enumeration)
 * @see asl.seedscan.metrics.PulseDetectionMetric.PulseDetectionData (Specification for the
 * list of list of contiguous pulse values)
 */
public class PulseDetectionPeakMetric extends PulseDetectionMetric {

  private static final Logger logger = LoggerFactory.getLogger(PulseDetectionPeakMetric.class);

  /**
   * Conversion factor to return values of amplitude
   */
  private static final double METERS_TO_NANOMETERS = 1E9;

  private double coefficientThreshold = 0.7;
  private double amplitudeThreshold = 0.0; // can be used to restrict results

  public PulseDetectionPeakMetric() {
    super();
    addArgument("channel-restriction");
    addArgument("coefficient-threshold");
    addArgument("amplitude-threshold");
  }

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public String getName() {
    return "PulseDetectionPeakMetric";
  }

  @Override
  public void process() {
    logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());
    // first iterate over the channels
    String preSplitBands = null;
    try {
      preSplitBands = get("channel-restriction");
    } catch (NoSuchFieldException ignored) {
    }
    if (preSplitBands == null) {
      preSplitBands = "LH,LN";
      logger.info("No band restriction set, using: {}", preSplitBands);
    }

    if (pulseDetectionResultMap == null) {
      pulseDetectionResultMap = new HashMap<>();
    }

    try {
      coefficientThreshold = Double.parseDouble(get("coefficient-threshold"));
    } catch (NoSuchFieldException | NumberFormatException | NullPointerException ignored) {
    }

    try {
      amplitudeThreshold = Double.parseDouble(get("amplitude-threshold"));
    } catch (NoSuchFieldException | NumberFormatException | NullPointerException ignored) {
    }

    // iterate over channels but ignore triggered and derived channels
    for (Channel channel : stationMeta.getChannelArray(preSplitBands,
        true, true)) {
      ChannelKey key = new ChannelKey(channel);

      ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel),
          getForceUpdate());
      if (digest == null) {
        logger.info("Digest unchanged station:[{}] channel:[{}] day:[{}] --> Skip metric",
            getStation(), channel, getDay());
        continue;
      }

      // only calculate a new result if the map is currently unpopulated
      if (!pulseDetectionResultMap.containsKey(key)) {
        try {
          PulseDetectionData result = calculatePulseResults(channel);
          pulseDetectionResultMap.put(key, result);
        } catch (ChannelMetaException e) {
          logger.error("Could not get metadata for channel [{}-{}]", getStation(), channel, e);
          continue;
        }
      }

      double maxPeak = 0;
      PulseDetectionData result = pulseDetectionResultMap.get(key);
      List<List<PulseDetectionPoint>> allData = result.correlationsWithAmplitude;
      for (List<PulseDetectionPoint> points : allData) {
        for (PulseDetectionPoint point : points) {
          if (point.correlationValue > coefficientThreshold &&
              point.amplitude  > amplitudeThreshold / result.sensitivity) {
            maxPeak = Math.max(maxPeak, point.amplitude * METERS_TO_NANOMETERS);
          }
        }
      }
      if (maxPeak > 0) {
        metricResult.addResult(channel, maxPeak, digest);
      }
    }
  }

  @Override
  public String getSimpleDescription() {
    return "Calculates the largest pulse found over a day's data";
  }

  @Override
  public String getLongDescription() {
    return "This metric convolves the (response-corrected) timeseries data with a step function "
        + "in order to identify potential pulse sources. Pulse candidates are screened for"
        + "a matching envelope over a 140s range, and a sharpness constraint that the 1-minute"
        + "amplitude moving average must be 4 times greater than a 15-minute moving average. "
        + "Pulses are then screened according to a user-specified threshold for the correlation "
        + "amplitude and coefficient values. The peak of the largest valid pulse is returned, "
        + "represented as the correlation amplitude value.";
  }
}
