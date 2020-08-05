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
 * Gets a count of the pulse detection count by counting the number of returned points that match
 * a user-specified lower bound for coefficient and amplitude values of the cross-correlation of
 * a channel's data with a step function. If at least one point matching those restrictions in a
 * contiguous list of points exists, then that list is counted as a pulse.
 * @see PulseDetectionMetric (The backend for pulse enumeration)
 * @see asl.seedscan.metrics.PulseDetectionMetric.PulseDetectionData (Specification for the
 * list of (list of) contiguous pulse values)
 */
public class PulseDetectionCountMetric extends PulseDetectionMetric {

  @Override
  public String getSimpleDescription() {
    return "Calculates the number of pulses found over a day's data";
  }

  @Override
  public String getLongDescription() {
    return "This metric convolves the (response-corrected) timeseries data with a step function "
        + "in order to identify potential pulse sources. Pulse candidates are screened for"
        + "a matching envelope over a 140s range, and a sharpness constraint that the 1-minute"
        + "amplitude moving average must be 4 times greater than a 15-minute moving average. "
        + "Pulses are then screened according to a user-specified threshold for the correlation "
        + "amplitude and coefficient values. The number of valid pulses found is returned.";
  }

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
          pulseDetectionResultMap.put(key, calculatePulseResults(channel));
        } catch (ChannelMetaException e) {
          logger.error("Could not get metadata for channel [{}-{}]", getStation(), channel, e);
          continue;
        }
      }

      PulseDetectionData result = pulseDetectionResultMap.get(key);
      logger.info("Number of non-contiguous potentially valid points: {}",
          result.correlationsWithAmplitude.size());
      int count = 0;
      // sublist of points is all contiguous points over a region
      for (List<PulseDetectionPoint> points : result.correlationsWithAmplitude) {
        for (PulseDetectionPoint point : points) {
          // all we need is at least one point greater than both thresholds
          // note that we don't bother with getter/setter because these are finalized
          if (point.amplitude > amplitudeThreshold &&
              point.correlationValue > coefficientThreshold) {
            ++count;
            break;
          }
        }
      }
      metricResult.addResult(channel, (double) count, digest);

    }

  }
}
