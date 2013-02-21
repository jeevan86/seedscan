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

    private static final double SMALLEST_PERIOD = 80;
    //private static final double SMALLEST_PERIOD = 100;
    //private static final double  LARGEST_PERIOD = 1000;
    //private static final double  LARGEST_PERIOD = 200;
    private static final double  LARGEST_PERIOD = 500;
    private static Hashtable<String, EventCMT> eventCMTs = null;

    private static final double fmin = 1./LARGEST_PERIOD;
    private static final double fmax = 1./SMALLEST_PERIOD;

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

        eventCMTs = getEventTable();
        if (eventCMTs == null) {
            logger.info(String.format("No Event CMTs found for Day=[%s] --> Skip EventCompareSynthetic Metric", getDay()) );
            return;
        }

        String[] locs  = {"00", "10"};
        String[] chans = {"LHZ", "LHND", "LHED"};

        ByteBuffer[] digestArray = new ByteBuffer[locs.length * chans.length];
        Channel[] channels       = new Channel[locs.length * chans.length];
/**
 *      channels[0] = 00-LHZ  
 *      channels[1] = 00-LHND
 *      channels[2] = 00-LHED
 *      channels[3] = 10-LHZ 
 *      channels[4] = 10-LHND
 *      channels[5] = 10-LHED
 */

        Boolean computeEventMetric = false;

        int iChan=0;
        for (int j=0; j < locs.length; j++) {
            for (int i=0; i < chans.length; i++) {
                Channel channel   = new Channel(locs[j],chans[i]);
                ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel));
                if (digest != null) computeEventMetric = true;

                channels[iChan]   = channel;
                digestArray[iChan]= digest;
                iChan++;
            }
        }
// This is a little wonky: For instance, the digest for channel 00-LHND will be computed using only metadata + data
//                         for this channel (i.e., it will not explicitly include 00-LHED) and for the current day
//                         (i.e., if an event window extends into the next day, those data will not form part of
//                         the digest.  Also, valueDigestChanged --> checkForRotatedChannels --> createRotatedChannelData
//                         will create rotated data + metadata for both horizontals (e.g., 00-LHND, 00-LHED) but
//                         how do we get the rotated data for the next day ?

        //if (computeEventMetric) {
        //}

        int nChannels = 6; // Hard-wire for now
        double[] results = new double[nChannels];
        int nEvents = 0;

       // Loop over Events for this day

        SortedSet<String> eventKeys = new TreeSet<String>(eventCMTs.keySet());
        for (String key : eventKeys){
            long eventStartTime = (eventCMTs.get(key)).getTimeInMillis();
            Hashtable<String, SacTimeSeries> synthetics = getEventSynthetics(key);
            if (synthetics == null) {
                System.out.format("== %s: No synthetics found for key=[%s] for this station\n", getName(), key);
            }
            else {
            // We do have synthetics for this station for this event --> Compare to data

            // 1. Load up 3-comp synthetics x 2 = 6
                SacTimeSeries[] sacSynthetics = new SacTimeSeries[nChannels];
                String[] kcmp = {"Z","N","E"};
                for (int i=0; i<3; i++){
                    String fileKey = getStation() + ".XX.LX" + kcmp[i] + ".modes.sac"; // e.g., "ANMO.XX.LXZ.modes.sac"
                    if (synthetics.containsKey(fileKey)) {
                        sacSynthetics[i]   = synthetics.get(fileKey); 
                        sacSynthetics[i+3] = synthetics.get(fileKey); 
                    }
                    else {
                        logger.severe(String.format("Error: Did not find sac synthetic=[%s] in Hashtable", fileKey) );
                    }
                }

                SacHeader hdr    = sacSynthetics[0].getHeader();
                long eventDurationMilliSecs = (long)(hdr.getNpts() * hdr.getDelta() * 1000);
                long eventEndTime = eventStartTime + eventDurationMilliSecs;

            // 2. Load up Displacement Array
                ArrayList<double[]> dataDisp  = metricData.getZNE("00", "LH", eventStartTime, eventEndTime, fmin, fmax);
                ArrayList<double[]> dataDisp2 = metricData.getZNE("10", "LH", eventStartTime, eventEndTime, fmin, fmax);
                dataDisp.addAll(dataDisp2);

            // 3. Calc RMS difference for each channel
                for (int i=0; i<nChannels; i++){
                    results[i] += rmsDiff(dataDisp.get(i), sacSynthetics[i]);
                    SacTimeSeries sac = new SacTimeSeries(hdr, dataDisp.get(i));
                    //String filename = "sac." + channels[i].toString();
                    String filename = key + "." + getStation() + "." + channels[i].toString() + ".sac";
                    try {
                        sac.write(filename);
                    }
                    catch (Exception E) {
                    }
                }

                nEvents++;

            } // else: process this event

        } // eventKeys: end loop over events

        for (int i=0; i<nChannels; i++) {
            Channel channel = channels[i];
            double result = results[i] / (double)nEvents;
            ByteBuffer digest = digestArray[i];
            metricResult.addResult(channel, result, digest);
            //System.out.format("== metricResult.addResult(%s, %12.6f, %s)\n", channel, result, Hex.byteArrayToHexString(digest.array()) );
        }

    } // end process()

    private double rmsDiff(double[] data, SacTimeSeries sac) {
        float[] synth = sac.getY(); 

        if (data.length != synth.length) {
            logger.severe( String.format("Error: data npts=%d != synth npts=%d --> don't compute RMS", data.length, synth.length) );
            return NO_RESULT;
        }
        double rms=0.;
        for (int i=0; i<data.length; i++){
            rms += Math.pow( (data[i] - synth[i]), 2 ); 
        }
        rms /= (double)data.length;
        rms  =  Math.sqrt(rms);

        return rms;
    }


    private double computeMetric(Channel channel) {

        System.out.format("\n== computeMetric: channel=%s\n", channel);

        return (double)0.;

    } // end computeMetric()

}
