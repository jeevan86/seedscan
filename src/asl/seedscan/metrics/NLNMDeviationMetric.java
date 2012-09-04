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

import freq.Cmplx;

public class NLNMDeviationMetric
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.NLNMDeviationMetric");

    public String getName()
    {
        return "NLNMDeviationMetric";
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

        // Get DataSet(s) for this channel
             int ndata      = 0;
             double srate   = 0;
             int[] intArray = null;

             ArrayList<DataSet>datasets = data.getChannelData(channel);
             if (datasets == null){
               System.out.format("%s Error: No data for requested channel:%s\n", getName(), channel.getChannel());
             }
             else {
               for (DataSet dataset : datasets) {
                 String knet    = dataset.getNetwork(); String kstn = dataset.getStation();
                 String locn    = dataset.getLocation();String kchn = dataset.getChannel();
                 long startTime = dataset.getStartTime();  // microsecs since Jan. 1, 1970
                 long endTime   = dataset.getEndTime();
                 long interval  = dataset.getInterval();
                 int length     = dataset.getLength();
                 Calendar startTimestamp = new GregorianCalendar();
                 startTimestamp.setTimeInMillis(startTime/1000);
                 Calendar endTimestamp = new GregorianCalendar();
                 endTimestamp.setTimeInMillis(endTime/1000);

                 intArray   = dataset.getSeries();
                 srate      = dataset.getSampleRate();
                 ndata     += dataset.getLength();

               } // end for each dataset
             }// end else (= we DO have data for this channel)

      // Compute PSD for this channel using the following algorithm:
      //   Break up the data (one day) into 13 overlapping segments of 75% 
      //   Remove the trend and mean 
      //   Apply a taper (cosine) 
      //   Zero pad to a power of 2 
      //   Compute FFT 
      //   Average all 13 FFTs 
      //   Remove response 

      // For 13 windows with 75% overlap, each window will contain ndata/4 points
      // ** Still need to handle the case of multiple datasets with gaps!
        int nseg_pnts = ndata / 4;  
        int noff      = nseg_pnts / 4;  
      // Find smallest power of 2 >= nseg_pnts:
        int nfft=1;
        while (nfft < nseg_pnts) nfft *= 2;
        int nf=nfft/2;
        double dt = srate;
        double df = 1./(nfft*dt);

        float[] xseg     = new float[nfft];
        Cmplx[]  xfft    = new Cmplx[nfft];
        double[] psd     = new double[nfft];

        int iwin=0;
        int ilst=nseg_pnts-1;
        int offset = noff;

        while (ilst < ndata) // ndata needs to come from largest dataset
        {
          for(int k=0; k<nseg_pnts; k++)
          {
            xseg[k]=intArray[k+offset]; // Load current window
          }
          //debias(xseg); detrend(xseg); taper(xseg); pad(xseg, nfft);

          xfft = Cmplx.fft(xseg);
          for(int k = 0; k < nfft; k++){
              psd[k]= psd[k] +  Math.pow(xfft[k].mag(),2);
          }
          iwin ++;
          offset += noff;
          ilst   += noff;
        }

/**
        for(int curind = 0; curind < 512; curind++){
            psd[curind] = psd[curind]/(double)numOfWins;
            freq[curind] = ((double)SPS)*(double)curind/((double)512);
**/


/**
             System.out.format("%s-%s [%s] %s %s-%s ", stnMeta.getStation(), stnMeta.getNetwork(),
               EpochData.epochToDateString(stnMeta.getTimestamp()), getName(), chanMeta.getLocation(), chanMeta.getName() );
             System.out.format("ndata:%d (%.0f%%) %s\n", ndata, availability, chanMeta.getDigestString()); 

             String key   = getName() + "+Channel(s)=" + channel.getLocation() + "-" + channel.getChannel();
             String value = String.format("%.2f",availability);
             result.addResult(key, value);
**/

           }// end foreach channel

     }
}

