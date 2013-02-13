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
import asl.seedscan.event.*;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.SortedSet;

import java.nio.ByteBuffer;
import asl.util.Hex;

import asl.metadata.Channel;
import asl.metadata.meta_new.ChannelMeta;
import asl.seedsplitter.DataSet;

public class EventCompareStrongMotion
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.EventCompareStrongMotion");

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getName()
    {
        return "EventCompareStrongMotion";
    }


    public void process()
    {
        System.out.format("\n              [ == Metric %s == ]\n", getName() ); 

            Hashtable<String, EventCMT> eventCMTs = getEventTable();
            if (eventCMTs != null) {
                SortedSet<String> keys = new TreeSet<String>(eventCMTs.keySet());
                for (String key : keys){
                    System.out.format("== %s: Got EventCMT key=[%s] --> [%s]\n", getName(), key, eventCMTs.get(key) );
                }
            }
            else {
                System.out.format("== Scanner: NO CMTs FOUND for this day\n");
            }


    // Get a sorted list of continuous channels for this stationMeta and loop over:
        ArrayList<Channel> channels = stationMeta.getChannelArray("00","LH");

        for (Channel channel : channels){

            ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel));

            //logger.fine(String.format("%s: digest=%s\n", getName(), (digest == null) ? "null" : Hex.byteArrayToHexString(digest.array())));

        // At this point we KNOW we have metadata so we WILL compute a digest.  If the digest is null 
        //  then nothing has changed (OR we DON'T have data for this channel) and we don't need to recompute the metric
            if (digest == null) { 
                System.out.format("%s INFO: Data and metadata have NOT changed for this channel:%s --> Skipping\n"
                                ,getName(), channel);
                continue;
            }

            double result = computeMetric(channel);

            if (result == NO_RESULT) {
                // Do nothing --> skip to next channel
            }
            else {
                metricResult.addResult(channel, result, digest);
            }

        }// end foreach channel
    } // end process()


    private double computeMetric(Channel channel) {

System.out.format("== computeMetric: channel=%s\n", channel);

/**
        ArrayList<DataSet>datasets = metricData.getChannelData(channel);
        if (datasets == null) {  // No data --> Skip this channel
            System.out.format("== Error: Metric=%s --> No datasets found for channel=[%s]\n",
                               getName(), channel);
            return NO_RESULT;
        }

     // First count any interior gaps (= gaps that aren't at the beginning/end of the day)
        int gapCount = datasets.size()-1;

        long firstSetStartTime = datasets.get(0).getStartTime();  // time in microsecs since epoch
        long interval          = datasets.get(0).getInterval();   // sample dt in microsecs

     // stationMeta.getTimestamp() returns a Calendar object for the expected day
     //   convert it from milisecs to microsecs
        long expectedStartTime = stationMeta.getTimestamp().getTimeInMillis() * 1000;
        double gapThreshold = interval / 2.;

        return (double)gapCount;
**/
        return (double)0.;

    } // end computeMetric()

}
