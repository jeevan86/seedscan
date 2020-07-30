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
 * @see PulseDetectionMetric (The backend for pulse enumeration)
 * @see asl.seedscan.metrics.PulseDetectionMetric.PulseDetectionData (Specification for the
 * list of list of contiguous pulse values)
 */
public class PulseDetectionPeakMetric extends PulseDetectionMetric {

  private static final Logger logger = LoggerFactory.getLogger(PulseDetectionPeakMetric.class);

  private double coefficientThreshold = 0.7;
  private double amplitudeThreshold = 0.1;

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
      List<List<PulseDetectionPoint>> allData =
          pulseDetectionResultMap.get(key).correlationsWithAmplitude;
      for (List<PulseDetectionPoint> points : allData) {
        for (PulseDetectionPoint point : points) {
          if (point.correlationValue > coefficientThreshold &&
              point.amplitude > amplitudeThreshold) {
            maxPeak = Math.max(maxPeak, point.rawData);
          }
        }
      }
      if (maxPeak > 0) {
        metricResult.addResult(channel, maxPeak, digest);
      }
    }
  }
}
