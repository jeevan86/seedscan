/*
 * Copyright 2012, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */
package asl.seedscan.metrics;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.seedsplitter.DataSet;

public class GapCountMetric extends Metric {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.metrics.GapCountMetric.class);

	@Override
	public long getVersion() {
		return 1;
	}

	@Override
	public String getName() {
		return "GapCountMetric";
	}

	public void process() {
		logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

		String station = getStation();
		String day = getDay();
		String metric = getName();

		// Get a sorted list of continuous channels for this stationMeta and
		// loop over:
		List<Channel> channels = stationMeta.getContinuousChannels();

		for (Channel channel : channels) {
			if (!metricData.hasChannelData(channel)) {
				logger.warn("No data found for station:[{}] channel:[{}] day:[{}] --> Skip metric",
						getStation(), channel, getDay());
				continue;
			}

			ByteBuffer digest = metricData.valueDigestChanged(channel,
					createIdentifier(channel), getForceUpdate());

			if (digest == null) { // means oldDigest == newDigest and we don't
									// need to recompute the metric
				logger.warn(
						"Digest unchanged station:[{}] channel:[{}] day:[{}] --> Skip metric",
						getStation(), channel, day);
				continue;
			}

			double result = computeMetric(channel, station, day, metric);

			if (result == NO_RESULT) {
				// Do nothing --> skip to next channel
				logger.warn("NO_RESULT for station={} channel={} day={}",
						getStation(), channel, day);
			} else {
				metricResult.addResult(channel, result, digest);
			}
		}// end foreach channel
	} // end process()

	private double computeMetric(Channel channel, String station, String day,
			String metric) {

		List<DataSet> datasets = metricData.getChannelData(channel);
		if (datasets == null) { // No data --> Skip this channel
			logger.error(
					"No datasets found for station=[{}] channel=[{}] day=[{}] --> Skip Metric",
					station, channel, day);
			return NO_RESULT;
		}

		// First count any interior gaps (= gaps that aren't at the
		// beginning/end of the day)
		int gapCount = datasets.size() - 1;

		long firstSetStartTime = datasets.get(0).getStartTime(); // time in
																	// microsecs
																	// since
																	// epoch
		long interval = datasets.get(0).getInterval(); // sample dt in microsecs

		// stationMeta.getTimestamp() returns a Calendar object for the expected
		// day
		// convert it from milisecs to microsecs
		long expectedStartTime = stationMeta.getTimestamp().getTimeInMillis() * 1000;
		// double gapThreshold = interval / 2.;
		double gapThreshold = interval / 1.;

		// Check for possible gap at the beginning of the day
		if ((firstSetStartTime - expectedStartTime) > gapThreshold) {
			gapCount++;
		}

		long expectedEndTime = expectedStartTime + 86400000000L; // end of day
																	// in
																	// microsecs
		long lastSetEndTime = datasets.get(datasets.size() - 1).getEndTime();

		// Check for possible gap at the end of the day
		// We expect a full day to be 24:00:00 - one sample = (86400 - dt) secs
		if ((expectedEndTime - lastSetEndTime) > interval) {
			gapCount++;
		}

		return (double) gapCount;
	} // end computeMetric()
}
