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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import java.nio.ByteBuffer;
import asl.util.Hex;

import asl.metadata.Channel;
import asl.metadata.meta_new.ChannelMeta;
import asl.seedsplitter.DataSet;

public class AvailabilityMetric
extends Metric
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.AvailabilityMetric.class);

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getName()
    {
        return "AvailabilityMetric";
    }


    public void process()
    {
        logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

    // Get a sorted list of continuous channels for this stationMeta and loop over:

        List<Channel> channels = stationMeta.getContinuousChannels();

        for (Channel channel : channels){

            if (!metricData.hasChannelData(channel)){
                //logger.warn("No data found for channel[{}] --> Skip metric", channel);
                continue;
            }

            ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel), getForceUpdate());

            if (digest == null) { // means oldDigest == newDigest and we don't need to recompute the metric 
                logger.warn("Digest unchanged station:[{}] channel:[{}] --> Skip metric", getStation(), channel);
                continue;
            }

            double result = computeMetric(channel);

            metricResult.addResult(channel, result, digest);

        }// end foreach channel

    } // end process()

    private double computeMetric(Channel channel) {

     // AvailabilityMetric still returns a result (availability=0) even when there is NO data for this channel
        if (!metricData.hasChannelData(channel)) {
            return 0.;
        }

        double availability = 0;

     // The expected (=from metadata) number of samples:
        ChannelMeta chanMeta = stationMeta.getChanMeta(channel);
        final int SECONDS_PER_DAY = 86400;
        int expectedPoints  = (int) (chanMeta.getSampleRate() * SECONDS_PER_DAY + 1); 
        //int expectedPoints  = (int) (chanMeta.getSampleRate() * 24. * 60. * 60.); 

     // The actual (=from data) number of samples:
        List<DataSet>datasets = metricData.getChannelData(channel);

        int ndata    = 0;

        for (DataSet dataset : datasets) {
            ndata   += dataset.getLength();
        } // end for each dataset

        if (expectedPoints > 0) {
            availability = 100. * (double)ndata/(double)expectedPoints;
        }
        else {
            logger.warn("Expected points for channel={} = 0!", channel);
            return NO_RESULT;
        }
        if (availability >= 101.00) {
            logger.warn("Availability={} > 100%% for channel={} sRate={}", 
                         availability, channel, chanMeta.getSampleRate());
        }

        return availability;

    } // end computeMetric()

}
