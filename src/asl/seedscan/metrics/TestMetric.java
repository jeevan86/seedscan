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
import java.util.ArrayList;

import java.nio.ByteBuffer;
import asl.util.Hex;

import asl.metadata.Channel;
import asl.metadata.meta_new.ChannelMeta;
import asl.seedsplitter.DataSet;

public class TestMetric
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.TestMetric");

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getName()
    {
        return "TestMetric";
    }


    public void process()
    {
        System.out.format("\n              [ == Metric %s == ]\n", getName() ); 

    // Get a sorted list of continuous channels for this stationMeta and loop over:

        ArrayList<Channel> channels = stationMeta.getContinuousChannels();

        for (Channel channel : channels){

          //ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel));

            computeMetric(channel);

          //metricResult.addResult(channel, result, digest);

        }// end foreach channel

    } // end process()

    private void computeMetric(Channel channel) {

        if (!metricData.hasChannelData(channel)) {
        }

        ChannelMeta chanMeta = stationMeta.getChanMeta(channel);
        chanMeta.plotPoleZeroResp();
System.exit(0);

     // The actual (=from data) number of samples:
        ArrayList<DataSet>datasets = metricData.getChannelData(channel);

        int ndata    = 0;

        for (DataSet dataset : datasets) {
            ndata   += dataset.getLength();
        } // end for each dataset

    } // end computeMetric()

}
