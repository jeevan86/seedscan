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

import asl.metadata.EpochData;
import asl.metadata.meta_new.*;
import asl.seedsplitter.*;
import asl.seedscan.Channel;

public class CalibrationMetric
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.CalibrationMetric");

    public CalibrationMetric(MetricData data)
    {
        super(data);
    }

    public void process()
    {
             double avge = 0;

           StationMeta stnMeta = data.getMetaData();
           Calendar stnMetaTimestamp = stnMeta.getTimestamp();
           System.out.format("== %s-%s [Meta Date:%s] Latitude: %6.2f Longitude: %6.2f\n",
            stnMeta.getStation(), stnMeta.getNetwork(), EpochData.epochToDateString(stnMetaTimestamp),stnMeta.getLatitude(), stnMeta.getLongitude() );
         //stnMeta.print();
         //Channel channel = new Channel("00","BHZ");

           ChannelArray channelArray = new ChannelArray("00","BHZ","BH1","BH2");
           Boolean hasChannels = stnMeta.hasChannels(channelArray);
           if (hasChannels){
              System.out.println("== Found metadata for all 3 channels for this epoch");
           }
           else {
              System.out.println("== Channel Meta not found for this epoch");
           }

           ArrayList<Channel> channels = channelArray.getChannels();

      // Loop over channels, get metadata & data for channel and Do Something ...
           for (Channel channel : channels){
        // Get Metadata for this channel
             ChannelMeta chanMeta = stnMeta.getChanMeta(channel);
             if (chanMeta == null){
               System.out.println("Scanner Error: stnMeta.getChannel returned null!");
             }
             else {
               chanMeta.print();
               System.out.format(" Channel: %s Location: %s Azimuth: %6.2f SampleRate: %6.2f\n",
                  chanMeta.getName(), chanMeta.getLocation(), chanMeta.getAzimuth(), chanMeta.getSampleRate() );
             }

        // Get DataSet(s) for this channel
             ArrayList<DataSet>datasets = data.getChannelData(channel);
             int numberOfDataSets = datasets.size();

             if (datasets != null) {
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

                 String out = String.format("%s_%s: [%s-%s] srate=%.2f [=%dL] length=%d startTime=%d endTime=%d [%s-%s]",knet,kstn,locn,kchn,srate,
                        interval,length,startTime,endTime,EpochData.epochToDateString(startTimestamp),EpochData.epochToDateString(endTimestamp) );
                 System.out.println(out);

// Adam would probably rather have the data points as floats right from the start (?)
                 int intArray[] = dataset.getSeries();
                 avge = 0;
                 for (int i=0; i<intArray.length; i++){
                   avge += intArray[i];
                 }
                 if (intArray.length == 0){
                   System.out.println("CalibrationMetric: Error: Array Length == 0 --> Divide by Zero");
                 }
                 else {
                   avge /= (double)intArray.length;
                 }
               } // end for each dataset
             }
             else {
               System.out.format("Error: Did not get requested ArrayList<DataSet> for %s-%s\n", channel.getLocation(), channel.getChannel());
             }


           }// end foreach channel
           
           String value = String.format("%.2f",avge);
           result = new MetricResult();
           result.addResult("Calibration", value);

/**  Example of how to grab the complex frequency response for a given channel:
           Complex[] Response = chnMeta.getResponse(double freq[]); // Return complex response at freq[]
**/

     }
}

