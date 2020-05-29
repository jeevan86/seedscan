package asl.seedscan.metrics;

import asl.metadata.Channel;
import asl.metadata.ChannelKey;
import asl.metadata.meta_new.ChannelMetaException;
import asl.seedscan.metrics.PulseDetectionMetric.PulseDetectionData.PulseDetectionPoint;
import java.nio.ByteBuffer;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PulseDetectionCountMetric extends PulseDetectionMetric {

  private static final Logger logger = LoggerFactory.getLogger(PulseDetectionCountMetric.class);

  private double coefficientThreshold = 0.7;
  private double amplitudeThreshold = 10;

  public PulseDetectionCountMetric() {
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
    return "PulseDetectionMetric";
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

    try {
      String field = get("coefficient-threshold");
      if (field != null) {
        coefficientThreshold = Double.parseDouble(field);
      }
    } catch (NoSuchFieldException | NumberFormatException ignored) {
    }

    try {
      String field = get("amplitude-threshold");
      if (field != null) {
        amplitudeThreshold = Double.parseDouble(field);
      }
    } catch (NoSuchFieldException | NumberFormatException ignored) {
    }

    if (pulseDetectionResultMap == null) {
      pulseDetectionResultMap = new HashMap<>();
    }

    // iterate over channels but ignore triggered and derived channels
    for (Channel channel : stationMeta.getChannelArray(preSplitBands, true, true)) {
      ChannelKey key = new ChannelKey(channel);

      // only calculate a new result if the map is currently unpopulated
      if (!pulseDetectionResultMap.containsKey(key)) {
        try {
          pulseDetectionResultMap.put(key, calculatePulseResults(channel));
        } catch (ChannelMetaException e) {
          logger.error("Could not get metadata for channel [{}-{}]", getStation(), channel, e);
          continue;
        }
      }

      ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel),
          getForceUpdate());
      if (digest == null) {
        logger.info("Digest unchanged station:[{}] channel:[{}] day:[{}] --> Skip metric",
            getStation(), channel, getDay());
        continue;
      }

      PulseDetectionData result = pulseDetectionResultMap.get(key);
      logger.info("Number of non-contiguous valid points: {}",
          result.correlationsWithAmplitude.size());
      int count = 0;
      for (PulseDetectionPoint point : result.correlationsWithAmplitude) {
        if (point.amplitude > amplitudeThreshold &&
            point.correlationValue > coefficientThreshold) {
          ++count;
        }
      }
      metricResult.addResult(channel, (double) count, digest);

    }

  }
}
