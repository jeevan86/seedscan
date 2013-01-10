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

   // Create a 3-channel array to use for loop
        ChannelArray primaryChannelArray   = new ChannelArray("00","BHZ", "BH1", "BH2");
        ChannelArray secondaryChannelArray = new ChannelArray("00","BHZ", "BHN", "BHE"); // Use these if we can't find primaries

        ArrayList<Channel> primaryChannels   = primaryChannelArray.getChannels();
        ArrayList<Channel> secondaryChannels = secondaryChannelArray.getChannels();

   // Loop over channels, get metadata & data for channel and Calculate Metric

      //for (Channel channel : channels){
        for (int i=0; i<primaryChannels.size(); i++){

            Channel channel = primaryChannels.get(i);

            if (!stationMeta.hasChannel(channel)) { // If we can't located the primary channel --> try the secondary channel
                channel = secondaryChannels.get(i);
            }

         // Check to see that we have data + metadata & see if the digest has changed wrt the database:

            double availability = 0;
            ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel));
            //logger.fine(String.format("%s: digest=%s\n", getName(), (digest == null) ? "null" : Hex.byteArrayToHexString(digest.array())));

        // digest could be null because it hasn't changed OR because we don't have metadata for this channel
            if (digest == null) { 
                System.out.format("%s INFO: Data and metadata have NOT changed for this channel:%s --> Skipping\n"
                                ,getName(), channel);
                continue;
            }

        // If we're here then we DO have metadata but still we might not have data for the channel
        //                                             in which case we'll report availability = 0
            if (!metricData.hasChannelData(channel)) {
                System.out.format("== AvailabilityMetric: We DONT! have data for channel=%s "
                + " return Availability=0\n", channel);
            }
            else {     // If we're here, it means we need to (re)compute the metric for this channel:

                ChannelMeta chanMeta = stationMeta.getChanMeta(channel);
                ArrayList<DataSet>datasets = metricData.getChannelData(channel);

                int ndata    = 0;

                for (DataSet dataset : datasets) {
                    ndata   += dataset.getLength();
                } // end for each dataset

                int expectedPoints  = (int)chanMeta.getSampleRate() * 24 * 60 * 60; 
                availability = 100 * ndata/expectedPoints;
    
            } // else compute the metric

            metricResult.addResult(channel, availability, digest);

        }// end foreach channel
    } // end process()
}

