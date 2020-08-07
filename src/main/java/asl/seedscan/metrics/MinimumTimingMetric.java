package asl.seedscan.metrics;

import asl.metadata.Channel;
import java.nio.ByteBuffer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinimumTimingMetric extends Metric {

  private static final Logger logger = LoggerFactory
      .getLogger(MinimumTimingMetric.class);

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public String getName() {
    return "TimingQualityMetric";
  }

  public void process() {
    logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

    // Get a sorted list of continuous channels for this stationMeta and
    // loop over:
    List<Channel> channels = stationMeta.getContinuousChannels();

    for (Channel channel : channels) {
      if (!metricData.hasChannelData(channel)) {
        // logger.warn("No data found for channel[{}] --> Skip metric",
        // channel);
        continue;
      }

      ByteBuffer digest = metricData.valueDigestChanged(channel,
          createIdentifier(channel), getForceUpdate());

      if (digest == null) { // means oldDigest == newDigest and we don't
        // need to recompute the metric
        logger.info(
            "Digest unchanged station:[{}] channel:[{}] day:[{}] --> Skip metric",
            getStation(), channel, getDay());
        continue;
      }

      double result = computeMetric(channel);

      if (result == NO_RESULT) {
        // Do nothing --> skip to next channel
        logger.warn("NO_RESULT for station={} channel={} day={}",
            getStation(), channel, getDay());
      } else {
        metricResult.addResult(channel, result, digest);
      }
    }// end foreach channel
  } // end process()

  @Override
  public String getSimpleDescription() {
    return "Returns the minimum value of the blockette 1001 timing records for the day";
  }

  @Override
  public String getLongDescription() {
    return "This metric takes the timing records for each blockette 1001 from a day's SEED data "
        + "and returns the lowest value.";
  }

  private double computeMetric(Channel channel) {

    if (!metricData.hasChannelData(channel)) {
      return NO_RESULT;
    }

    List<Integer> qualities = metricData.getChannelTimingQualityData(channel);

    if (qualities == null) {
      return NO_RESULT;
    } else if (qualities.size() == 0) {
      logger.warn(
          "TimingQualityMetric: We have NO timing quality measurements for channel={} day={}",
          channel, getDay());
      return NO_RESULT;
    }

    int minTiming = qualities.get(0);

    for (Integer quality : qualities) {
      minTiming = Math.min(minTiming, quality);
    }

    return minTiming;

  } // end computeMetric()
}
