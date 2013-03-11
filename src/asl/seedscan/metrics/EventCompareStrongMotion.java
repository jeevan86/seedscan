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
import java.util.List;
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
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauP_Time;

public class EventCompareStrongMotion
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.EventCompareStrongMotion");

    //private static final double SMALLEST_PERIOD = 4;
    private static final double  LARGEST_PERIOD = 500;
    private static final double SMALLEST_PERIOD = 1;
    //private static final double  LARGEST_PERIOD = 100;
    private static Hashtable<String, EventCMT> eventCMTs = null;

    private static final double fmin = 1./LARGEST_PERIOD;
    private static final double fmax = 1./SMALLEST_PERIOD;

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

        System.out.format("== %s: Day=[%s]\n", getName(), getDay() );

        eventCMTs = getEventTable();
        if (eventCMTs == null) {
            logger.info(String.format("No Event CMTs found for Day=[%s] --> Skip EventCompareStrongMotion Metric", getDay()) );
            return;
        }

        String[] locs  = {"00", "10"};
        String[] chans = {"LHZ", "LHND", "LHED"};

        int nChannels = 9;

        ByteBuffer[] digestArray = new ByteBuffer[nChannels];
        Channel[] channels       = new Channel[nChannels];
/**
 *      channels[0] = 00-LHZ  
 *      channels[1] = 00-LHND
 *      channels[2] = 00-LHED
 *      channels[3] = 10-LHZ 
 *      channels[4] = 10-LHND
 *      channels[5] = 10-LHED
 *      channels[6] = 20-LNZ
 *      channels[7] = 20-LNND
 *      channels[8] = 20-LNED
 */

        Boolean computeEventMetric = false;

        int iChan=0;
        for (int j=0; j < 2; j++) {
            for (int i=0; i < chans.length; i++) {
                Channel channel   = new Channel(locs[j],chans[i]);
                ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel));
                if (digest != null) computeEventMetric = true;

                channels[iChan]   = channel;
                digestArray[iChan]= digest;
                iChan++;
            }
        }
        channels[6] = new Channel("20", "LNZ");
        channels[7] = new Channel("20", "LNND");
        channels[8] = new Channel("20", "LNED");

        //if (computeEventMetric) {
        //}

        //int nChannels = 6; // Hard-wire for now
        double[] results = new double[nChannels];
        int nEvents = 0;

       // Loop over Events for this day

        SortedSet<String> eventKeys = new TreeSet<String>(eventCMTs.keySet());
        for (String key : eventKeys){
            Hashtable<String, SacTimeSeries> synthetics = getEventSynthetics(key);
            SacHeader hdr = null;
            if (synthetics == null) {
                System.out.format("== %s: No synthetics found for key=[%s] for this station\n", getName(), key);
            }
            else {
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

                hdr    = sacSynthetics[0].getHeader();
            }


            long eventStartTime = (eventCMTs.get(key)).getTimeInMillis();
            double stla  = stationMeta.getLatitude();
            double stlo  = stationMeta.getLongitude();
            double evla  = eventCMTs.get(key).getLatitude();
            double evlo  = eventCMTs.get(key).getLongitude();
            double evdep = eventCMTs.get(key).getDepth();
            double gcarc = SphericalCoords.distance(evla, evlo, stla, stlo);
            double azim  = SphericalCoords.azimuth(evla, evlo, stla, stlo);
            TauP_Time timeTool = null;
            try {
                timeTool = new TauP_Time("prem");
                timeTool.parsePhaseList("P,S");
                timeTool.depthCorrect(evdep);
                timeTool.calculate(gcarc);
            }
            catch(Exception e) {
            }

            System.out.format("== Event:%s <evla,evlo> = <%.2f, %.2f> Station:%s <%.2f, %.2f> gcarc=%f azim=%f\n",
                key, evla, evlo, getStation(), stla, stlo, gcarc, azim );

            List<Arrival> arrivals = timeTool.getArrivals();
            //ArrayList<Arrival> arrivals = (ArrayList<Arrival>)timeTool.getArrivals();
            for (int i=0; i<arrivals.size(); i++){
                Arrival arrival = arrivals.get(i);
                System.out.println(arrival.getName()+" arrives at "+ (arrival.getDist()*180.0/Math.PI)+" degrees after "+ arrival.getTime()+" seconds.");
            }
            if (arrivals.size() != 2) { // Either we don't have both P & S or we don't have just P & S
                logger.severe(String.format("Error: Expected P and/or S arrival times not found"));
            }
            long arrivalTimeP = 0;
            if (arrivals.get(0).getName().equals("P")){
                arrivalTimeP = (long)arrivals.get(0).getTime() * 1000L;
            }
            else {
                logger.severe(String.format("Error: Expected P arrival time not found"));
            }
            long arrivalTimeS = 0;
            if (arrivals.get(1).getName().equals("S")){
                arrivalTimeS = (long)arrivals.get(1).getTime() * 1000L;
            }
            else {
                logger.severe(String.format("Error: Expected S arrival time not found"));
            }

            long windowStartTime = eventStartTime + arrivalTimeP - 120*1000; // P time - 120sec, in millisecs
            long windowEndTime   = eventStartTime + arrivalTimeS + 60*1000;  // S time + 60sec, in millisecs

            //ArrayList<double[]> dataDisp00  = metricData.getZNE("00", "LH", windowStartTime, windowEndTime, fmin, fmax);
            long duration = 8000000L; // 8000 sec = 8000000 msecs

            ArrayList<double[]> dataDisp00  = metricData.getZNE("00", "LH", eventStartTime, eventStartTime + duration, fmin, fmax);
            ArrayList<double[]> dataDisp10  = metricData.getZNE("10", "LH", eventStartTime, eventStartTime + duration, fmin, fmax);
            ArrayList<double[]> dataDisp20  = metricData.getZNE("20", "LN", eventStartTime, eventStartTime + duration, fmin, fmax);

            dataDisp00.addAll(dataDisp10);
            dataDisp00.addAll(dataDisp20);

/**
            ArrayList<double[]> dataDisp00  = new ArrayList<double[]>();
            double[] data = metricData.getWindowedData(new Channel("00", "LHZ"), eventStartTime, eventStartTime + duration);
            dataDisp00.add(data);
            for (int i=0; i<1; i++){
**/

            for (int i=0; i<3; i++){
                int j=3*i;
                String kcmp = channels[j].toString();
                hdr.setKcmpnm(kcmp);
                SacTimeSeries sac = new SacTimeSeries(hdr, dataDisp00.get(j));
                String filename = key + "." + getStation() + "." + kcmp + ".sac2";
                try {
                    sac.write(filename);
                }
                catch (Exception E) {
                }
            }

            nEvents++;

        } // eventKeys: end loop over events

/**

        for (int i=0; i<nChannels; i++) {
            Channel channel = channels[i];
            double result = results[i] / (double)nEvents;
            ByteBuffer digest = digestArray[i];
            metricResult.addResult(channel, result, digest);
            //System.out.format("== metricResult.addResult(%s, %12.6f, %s)\n", channel, result, Hex.byteArrayToHexString(digest.array()) );
        }
**/

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
