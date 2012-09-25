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

public class NLNMDeviationMetric
extends PowerBandMetric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.NLNMDeviationMetric");

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getBaseName()
    {
        return "NLNMDeviationMetric";
    }

    public void process()
    {

           System.out.format("\n              [ == Metric %s == ]\n", getName() ); 

   // Grab station metadata for all channels for this day:
           StationMeta stnMeta = data.getMetaData();

   // Create a 3-channel array and check that we have metadata for all 3 channels:

           ChannelArray channelArray = new ChannelArray("00","LHZ", "LH1", "LH2");

           if (stnMeta.hasChannels(channelArray) ){
              //System.out.println("== Found metadata for all 3 channels for this epoch");
           }
           else {
              System.out.println("== Channel Meta not found for this epoch");
           }

           ArrayList<Channel> channels = channelArray.getChannels();

   // We are computing a metric for each channel in channelArray.
   //   Loop over channels, get metadata & data for channel and compute metric.

           result = new MetricResult();

           for (Channel channel : channels){

           // At this point we know what channel(s) are going into this metric and we can
           //   check to see if the channel metadata hash(es) or data hash(es) have changed
           //   since we last computed the metric.  If not, then we don't have to re-compute it.
           //   so break to next metric (=channel) calculation.

             if (!data.hashChanged(channel)) continue;

             ChannelMeta chanMeta = stnMeta.getChanMeta(channel);
             if (chanMeta == null){
               System.out.format("%s Error: stnMeta.getChannel returned null for channel=%s\n", getName(), channel.getChannel());
             }
             else { //Do something with chanMeta
               if (chanMeta.hasDayBreak() ){ // Check to see if the metadata for this channel changes during this day
                  System.out.format("%s Error: channel=%s metadata has a break!\n", getName(), channel.getChannel() );
               }
             } // end chanMeta for this channel

          // Get DataSet(s) for this channel
             ArrayList<DataSet>datasets = data.getChannelData(channel);
             if (datasets == null){
               System.out.format("%s Error: No data for requested channel:%s\n", getName(), channel.getChannel());
             }
             else { // DO something with this channel data
               int numberOfDataSets = datasets.size();
               if (numberOfDataSets != 1) {
                 System.out.format("%s: Warning: number of datasets for channel:%s != 1\n", getName(), channel.getChannel() );
               }

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

          // Calculate something for this channel ...
/** Break 24-hour data into hourly blocks with 50% overlap
 *    Break each hour into 13 segments, each ~15 min long, and overlapping 75%
 *      Demean segment, truncate or pad to pow2, and apply 10% cosine taper
 *      Compute FFT of demeaned, tapered segment
 *      Compute PSD of segment and correct for power loss to cosine taper (1.142857)
 *      Remove I(f) from PSD segment --> acceleration
 *      Convert PSD to dB with respect to acceleration
 *    Average together the 13 segments --> PSD for this hour
 *    Compare to NLNM ??
**/ 
                 int intArray[] = dataset.getSeries();

                 for (int i=0; i<intArray.length; i++){
                   //massPosition += Math.pow( (a0 + intArray[i] * a1), 2);
                 }
                 if (intArray.length == 0){
                   System.out.println("NLNMDeviationMetric: Error: Array Length == 0 --> Divide by Zero");
                 }
                 else {
                   //massPosition = Math.sqrt( massPosition / (double)intArray.length );
                 }
               } // end for each dataset
             }// end else (= we DO have data for this channel)

             String calibrationResult = "Nothing-to-Report";
         
             System.out.format("%s-%s [%s] %s %s-%s ", stnMeta.getStation(), stnMeta.getNetwork(), 
               EpochData.epochToDateString(stnMeta.getTimestamp()), getName(), chanMeta.getLocation(), chanMeta.getName() );
             System.out.format("NLNMDeviationMetric:%s %s\n", calibrationResult, chanMeta.getDigestString() ); 

             String key   = getName() + "+Channel(s)=" + channel.getLocation() + "-" + channel.getChannel();
             String value = String.format("%s",calibrationResult);
             result.addResult(key, value);
           }// end foreach channel
    } // end process()
} // end class
