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

import java.util.logging.Logger;
import java.util.List;

import java.nio.ByteBuffer;
import asl.util.Hex;

import asl.metadata.Channel;
import asl.metadata.meta_new.ChannelMeta;
import asl.seedsplitter.DataSet;

public class AvailabilityMetric
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.AvailabilityMetric");

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
        System.out.format("\n              [ == Metric %s == ]    [== Station %s ==]    [== Day %s ==]\n", 
                          getName(), getStation(), getDay() ); 

    // Get a sorted list of continuous channels for this stationMeta and loop over:

        List<Channel> channels = stationMeta.getContinuousChannels();

        for (Channel channel : channels){

            ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel), getForceUpdate());

        // At this point we KNOW we have metadata so we WILL compute a digest.  If the digest is null
        //  then nothing has changed and we don't need to recompute the metric
            if (digest == null) { 
                System.out.format("%s INFO: Data and metadata have NOT changed for this channel:%s --> Skipping\n"
                                ,getName(), channel);
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
            System.out.format("== AvailabilityMetric: WARNING: Expected points for channel=[%s] = 0!!\n", channel);
            return NO_RESULT;
        }
        if (availability >= 101.00) {
            System.out.format("== AvailabilityMetric: WARNING: Availability=%f.2 > 100% for channel=[%s] sRate=[%f]\n", 
                               availability, channel, chanMeta.getSampleRate());
        }

        return availability;

    } // end computeMetric()

}
