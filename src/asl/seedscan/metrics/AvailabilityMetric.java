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

import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.seedsplitter.*;

public class AvailabilityMetric
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.AvailabilityMetric");

    public String getName()
    {
        return "AvailabilityMetric";
    }

    public void process()
    {

           System.out.format("\n              [ == Metric %s == ]\n", getName() ); 

   // Grab station metadata for all channels for this day:
           StationMeta stnMeta = data.getMetaData();

   // Create a 3-channel array and check that we have metadata for all 3 channels:
           ChannelArray channelArray = new ChannelArray("00","BHZ", "BH1", "BH2");

           if (stnMeta.hasChannels(channelArray) ){
              //System.out.println("== Found metadata for all 3 channels for this epoch");
           }
           else {
              //System.out.println("== Channel Meta not found for this epoch");
           }

           ArrayList<Channel> channels = channelArray.getChannels();

           result = new MetricResult();
   // Loop over channels, get metadata & data for channel and Do Something ...

           for (Channel channel : channels){

             ChannelMeta chanMeta = stnMeta.getChanMeta(channel);
             if (chanMeta == null){ // Skip channel, we have no metadata for it
               System.out.format("%s Error: metadata not found for requested channel:%s --> Skipping\n", getName(), channel.getChannel());
               continue;
             }
             else {
               if (chanMeta.hasDayBreak() ){ // Check to see if the metadata for this channel changes during this day
                  System.out.format("%s Error: channel=%s metadata has a break!\n", getName(), channel.getChannel() );
               }
             } // end chanMeta for this channel

             if (!data.hashChanged(channel)) continue;

             int totalPoints  = 0;

// Maybe getSampleRate() should return integer ??
             int sampleRate = (int)chanMeta.getSampleRate();

        // Get DataSet(s) for this channel
             ArrayList<DataSet>datasets = data.getChannelData(channel);
             if (datasets == null){
               System.out.format("%s Error: No data for requested channel:%s\n", getName(), channel.getChannel());
             }
             else {
               for (DataSet dataset : datasets) {
                 String knet    = dataset.getNetwork(); String kstn = dataset.getStation();
                 String locn    = dataset.getLocation();String kchn = dataset.getChannel();
                 double srate   = dataset.getSampleRate();
                 long startTime = dataset.getStartTime();  // microsecs since Jan. 1, 1970
                 long endTime   = dataset.getEndTime();
                 long interval  = dataset.getInterval();
                 int length     = dataset.getLength();
                 Calendar startTimestamp = new GregorianCalendar();
                 startTimestamp.setTimeInMillis(startTime/1000);
                 Calendar endTimestamp = new GregorianCalendar();
                 endTimestamp.setTimeInMillis(endTime/1000);

                 totalPoints += dataset.getLength();

               } // end for each dataset
             }// end else (= we DO have data for this channel)

             int expectedPoints = sampleRate * 24 * 60 * 60; 
         
             double availability = 100 * totalPoints/expectedPoints;

             System.out.format("%s-%s [%s] %s %s-%s ", stnMeta.getStation(), stnMeta.getNetwork(),
               EpochData.epochToDateString(stnMeta.getTimestamp()), getName(), chanMeta.getLocation(), chanMeta.getName() );
             System.out.format("totalPoints:%d (%.0f%%) %s\n", totalPoints, availability, chanMeta.getDigestString()); 

             String key   = getName() + "+Channel(s)=" + channel.getLocation() + "-" + channel.getChannel();
             String value = String.format("%.2f",availability);
             result.addResult(key, value);

           }// end foreach channel

     }
}

