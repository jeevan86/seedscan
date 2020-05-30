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

public class PulseDetectionPeakMetric extends PulseDetectionMetric {

  private static final Logger logger = LoggerFactory.getLogger(PulseDetectionPeakMetric.class);

  private double coefficientThreshold = 0.7;

  public PulseDetectionPeakMetric() {
    super();
    addArgument("channel-restriction");
    addArgument("coefficient-threshold");
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
    } catch (NoSuchFieldException | NumberFormatException ignored) {
    }

    // iterate over channels but ignore triggered and derived channels
    for (Channel channel : stationMeta.getChannelArray(preSplitBands, true, true)) {
      ChannelKey key = new ChannelKey(channel);

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

      ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel),
          getForceUpdate());
      if (digest == null) {
        logger.info("Digest unchanged station:[{}] channel:[{}] day:[{}] --> Skip metric",
            getStation(), channel, getDay());
        continue;
      }
      double maxPeak = 0;
      List<List<PulseDetectionPoint>> allData =
          pulseDetectionResultMap.get(key).correlationsWithAmplitude;
      for (List<PulseDetectionPoint> points : allData) {
        for (PulseDetectionPoint point : points) {
          if (point.correlationValue > coefficientThreshold) {
            maxPeak = Math.max(maxPeak, point.amplitude);
          }
        }
      }
      if (maxPeak > 0) {
        metricResult.addResult(channel, maxPeak, digest);
      }
    }
  }
}
