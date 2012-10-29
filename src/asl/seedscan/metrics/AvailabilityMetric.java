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
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Calendar;

import java.nio.ByteBuffer;
import asl.util.Hex;

import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.seedsplitter.*;

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
        System.out.format("\n              [ == Metric %s == ]\n", getName() ); 
   // Grab station metadata for all channels for this day:
        StationMeta stnMeta = metricData.getMetaData();

   // Create a 3-channel array to use for loop
        ChannelArray channelArray = new ChannelArray("00","BHZ", "BH1", "BH2");
        ArrayList<Channel> channels = channelArray.getChannels();

        metricResult = new MetricResult(stnMeta);

   // Loop over channels, get metadata & data for channel and Calculate Metric

        for (Channel channel : channels){

            byte[] byteArray    = new byte[16];
            Boolean hashChanged = metricData.hashChanged(channel, byteArray);
            ByteBuffer digest   = ByteBuffer.wrap(byteArray);
            System.out.format("== Multi DataDigest string=%s\n", Hex.byteArrayToHexString(digest.array()) );

         // If we've already computed this metric and the data+metadata hasn't changed --> Skip channel
            if (!hashChanged) { 
                System.out.format("%s INFO: Data and metadata have NOT changed for this channel:%s --> Skipping\n"
                                  ,getName(), channel);
                continue;
            }

         // If we're here, it means we need to (re)compute the metric for this channel:

            ChannelMeta chanMeta = stnMeta.getChanMeta(channel);
            ArrayList<DataSet>datasets = metricData.getChannelData(channel);

            int ndata    = 0;
            String dataHashString = null;

            for (DataSet dataset : datasets) {
                ndata   += dataset.getLength();
                dataHashString = dataset.getDigestString();
            } // end for each dataset

            int expectedPoints  = (int)chanMeta.getSampleRate() * 24 * 60 * 60; 
            double availability = 100 * ndata/expectedPoints;

            metricResult.addResult(channel, availability, digest);

/**
            System.out.format("%s-%s [%s] %s %s-%s ", stnMeta.getStation(), stnMeta.getNetwork(),
              EpochData.epochToDateString(stnMeta.getTimestamp()), getName(), chanMeta.getLocation(), chanMeta.getName() );
            System.out.format("ndata:%d (%.0f%%) %s %s\n", ndata, availability, chanMeta.getDigestString(), dataHashString); 
**/

        }// end foreach channel
    } // end process()
}

