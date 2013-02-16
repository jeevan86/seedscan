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

import sac.SacTimeSeries;
import sac.SacHeader;

public class EventCompareSynthetic
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.EventCompareSynthetic");

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getName()
    {
        return "EventCompareSynthetic";
    }


    public void process()
    {
        System.out.format("\n              [ == Metric %s == ]\n", getName() ); 

        System.out.format("== %s: Day=[%s]\n", getName(), getDay() );

        Hashtable<String, EventCMT> eventCMTs = getEventTable();
        if (eventCMTs == null) {
            logger.info(String.format("No Event CMTs found for Day=[%s] --> Skip EventCompareSynthetic Metric", getDay()) );
            return;
        }

        else { // Loop over Events for this day
            SortedSet<String> keys = new TreeSet<String>(eventCMTs.keySet());
            for (String key : keys){
                System.out.format("== %s: Got EventCMT key=[%s] --> [%s]\n", getName(), key, eventCMTs.get(key) );
                long eventStartTime = (eventCMTs.get(key)).getTimeInMillis();

                Hashtable<String, SacTimeSeries> synthetics = getEventSynthetics(key);
                if (synthetics == null) {
                    System.out.format("== %s: No synthetics found for key=[%s] for this station\n", getName(), key);
                }
                else {
                    System.out.format("== %s: Found [n=%d] synthetics found for key=[%s] for this station\n", getName(), 
                        synthetics.size(), key);
                    SortedSet<String> fileKeys = new TreeSet<String>(synthetics.keySet());
                    for (String fileKey : fileKeys){
                        SacTimeSeries sac = synthetics.get(fileKey);
                        SacHeader hdr = sac.getHeader();
                        System.out.format("Found SacFile key=[%s] kstnm=%s kcmpnm=%s delta=%f npts=%d kstart=[%4d-%03d %02d:%02d:%02d.%03d]\n", 
                            fileKey, hdr.getKstnm(), hdr.getKcmpnm(), hdr.getDelta(), hdr.getNpts(),
                            hdr.getNzyear(), hdr.getNzjday(), hdr.getNzhour(), hdr.getNzmin(), hdr.getNzsec(), hdr.getNzmsec());
                            long eventDurationMilliSecs = (long)(hdr.getNpts() * hdr.getDelta() * 1000);
                            long eventEndTime = eventStartTime + eventDurationMilliSecs;
                            Channel channel = new Channel("00", "LHZ");
                        //double[] data = metricData.getWindowedData( channel, eventStartTime, eventEndTime);
                        double[] data = metricData.getFilteredDisplacement(channel, eventStartTime, eventEndTime, 1.0, 10.0);
                        SacTimeSeries sacOut = new SacTimeSeries(hdr, data);
                        try {
                            sacOut.write("sacOut.sac");
                        }
                        catch (Exception e) {
                            System.out.format("== sac write: caught exception:%s\n", e);
                        }
                        
                    }
                }
            }
        }




            //ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel));

        // At this point we KNOW we have metadata so we WILL compute a digest.  If the digest is null 
        //  then nothing has changed (OR we DON'T have data for this channel) and we don't need to recompute the metric
            //if (digest == null) { 
                //System.out.format("%s INFO: Data and metadata have NOT changed for this channel:%s --> Skipping\n"
                                //,getName(), channel);
                //continue;
            //}

/**
            double result = computeMetric(channel);

            if (result == NO_RESULT) {
                // Do nothing --> skip to next channel
            }
            else {
                metricResult.addResult(channel, result, digest);
            }
**/
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
