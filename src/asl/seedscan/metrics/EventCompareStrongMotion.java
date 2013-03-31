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
import asl.util.PlotMaker;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.ChannelMeta.ResponseUnits;
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

    private static Hashtable<String, EventCMT> eventCMTs = null;

    private static final double PERIOD1 = 25;
    private static final double PERIOD2 = 20;
    private static final double PERIOD3 = 4;
    private static final double PERIOD4 = 2;

    private static final double f1 = 1./PERIOD1;
    private static final double f2 = 1./PERIOD2;
    private static final double f3 = 1./PERIOD3;
    private static final double f4 = 1./PERIOD4;

    private SacHeader hdr = null;

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

/**  iDigest/
 *   iMetric   ChannelX                v. ChannelY
 *    0        channels[0] = 00-LHZ    v. channels[6] = 20-LNZ  
 *    1        channels[1] = 00-LHND   v. channels[7] = 20-LNND
 *    2        channels[2] = 00-LHED   v. channels[8] = 20-LNED
 *    3        channels[3] = 10-LHZ    v. channels[6] = 20-LNZ
 *    4        channels[4] = 10-LHND   v. channels[7] = 20-LNND
 *    5        channels[5] = 10-LHED   v. channels[8] = 20-LNED
 **/
        int nChannels = 9;
        int nDigests  = 6;

        ByteBuffer[] digestArray = new ByteBuffer[nDigests];
        Channel[] channels       = new Channel[nChannels];

        double[] results = new double[nDigests];

        channels[0] = new Channel("00", "LHZ"); 
        channels[1] = new Channel("00", "LHND"); 
        channels[2] = new Channel("00", "LHED"); 
        channels[3] = new Channel("10", "LHZ"); 
        channels[4] = new Channel("10", "LHND"); 
        channels[5] = new Channel("10", "LHED"); 
        channels[6] = new Channel("20", "LNZ"); 
        channels[7] = new Channel("20", "LNND"); 
        channels[8] = new Channel("20", "LNED"); 

        for (int i=0; i<nDigests; i++) {
            Channel channelX = channels[i];
            Channel channelY = null;
            if (i < 3){
                channelY = channels[i+6];
            }
            else {
                channelY = channels[i+3];
            }

            ChannelArray channelArray = new ChannelArray(channelX, channelY);
            ByteBuffer digest = metricData.valueDigestChanged(channelArray, createIdentifier(channelX, channelY), getForceUpdate());
            digestArray[i] = digest;
            results[i] = 0.;
        }

        int nEvents = 0;

       // Loop over Events for this day

        SortedSet<String> eventKeys = new TreeSet<String>(eventCMTs.keySet());
        for (String key : eventKeys){

            EventCMT eventCMT = eventCMTs.get(key);

        // Window the data from the Event (PDE) Origin.
        //   Use larger time window to do the instrument decons and trim it down later:

            long duration = 8000000L; // 8000 sec = 8000000 msecs
            long eventStartTime = eventCMT.getTimeInMillis();   // Event origin epoch time in millisecs
            long eventEndTime   = eventStartTime + duration;

            ResponseUnits units = ResponseUnits.DISPLACEMENT;
            ArrayList<double[]> dataDisp00  = metricData.getZNE(units, "00", "LH", eventStartTime, eventEndTime, f1, f2, f3, f4);
            ArrayList<double[]> dataDisp10  = metricData.getZNE(units, "10", "LH", eventStartTime, eventEndTime, f1, f2, f3, f4);
            ArrayList<double[]> dataDisp20  = metricData.getZNE(units, "20", "LN", eventStartTime, eventEndTime, f1, f2, f3, f4);
            dataDisp00.addAll(dataDisp10);
            dataDisp00.addAll(dataDisp20);

        // Use P and S arrival times to trim the window down for comparison:
            double[] arrivalTimes = getEventArrivalTimes(eventCMT);
            int nstart = (int)(arrivalTimes[0] - 120.); // P - 120 sec
            int nend   = (int)(arrivalTimes[1] + 60.);  // S + 120 sec

            if (getMakePlots()){
                double[] xsecs = new double[ dataDisp00.get(0).length ];
                for (int k=0; k<xsecs.length; k++){
                    xsecs[k] = (float)k;        // hard-wired for LH? dt=1.0
                }
                PlotMaker plotMaker = new PlotMaker(metricResult.getStation(), channels, metricResult.getDate());
                plotMaker.plotZNE_3x3(dataDisp00, xsecs, nstart, nend, key, "strongmotion");
            }

            for (int i=0; i<nDigests; i++) {
                int j = 0;
                if (i < 3){
                    j = i + 6;
                }
                else {
                    j = i + 3;
                }

        // Displacements are in meters so rmsDiff's will be small
        //   scale rmsDiffs to micrometers:
                results[i] += 1.e6 * rmsDiff( dataDisp00.get(i), dataDisp00.get(j), nstart, nend);
            }

            nEvents++;

        } // eventKeys: end loop over events

        for (int i=0; i<nDigests; i++) {
            Channel channelX = channels[i];
            Channel channelY = null;
            if (i < 3){
                channelY = channels[i+6];
            }
            else {
                channelY = channels[i+3];
            }
         // Average over all events for this day
            double result = results[i]/(double)nEvents;
            ByteBuffer digest = digestArray[i];
            metricResult.addResult(channelX, channelY, result, digest);
        }

    } // end process()

/**
 * compare 2 double[] arrays between array indices n1 and n2
 */
    private double rmsDiff(double[] data1, double[] data2, int n1, int n2) {
// if n1 < n2 or nend < data.length ...
        double rms=0.;
        int npts = n2 - n1 + 1;
        for (int i=n1; i<n2; i++){
            rms += Math.pow( (data1[i] - data2[i]), 2 ); 
        }
        rms /= (double)npts;
        rms  =  Math.sqrt(rms);

        return rms;
    }


    private double computeMetric(Channel channel) {

        System.out.format("\n== computeMetric: channel=%s\n", channel);

        return (double)0.;

    } // end computeMetric()


    private double[] getEventArrivalTimes(EventCMT eventCMT) {

            long eventStartTime = eventCMT.getTimeInMillis();   // Event origin epoch time in millisecs

            double evla  = eventCMT.getLatitude();
            double evlo  = eventCMT.getLongitude();
            double evdep = eventCMT.getDepth();
            double stla  = stationMeta.getLatitude();
            double stlo  = stationMeta.getLongitude();
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

            List<Arrival> arrivals = timeTool.getArrivals();

            //for (int i=0; i<arrivals.size(); i++){
                //Arrival arrival = arrivals.get(i);
                //System.out.println(arrival.getName()+" arrives at "+ (arrival.getDist()*180.0/Math.PI)+" degrees after "+ arrival.getTime()+" seconds.");
            //}
            if (arrivals.size() != 2) { // Either we don't have both P & S or we don't have just P & S
                logger.severe(String.format("Error: Expected P and/or S arrival times not found"));
            }

            double arrivalTimeP = 0.;
            if (arrivals.get(0).getName().equals("P")){
                arrivalTimeP = arrivals.get(0).getTime();
            }
            else {
                logger.severe(String.format("Error: Expected P arrival time not found"));
            }
            double arrivalTimeS = 0.;
            if (arrivals.get(1).getName().equals("S")){
                arrivalTimeS = arrivals.get(1).getTime();
            }
            else {
                logger.severe(String.format("Error: Expected S arrival time not found"));
            }

            System.out.format("== Event:%s <evla,evlo> = <%.2f, %.2f> Station:%s <%.2f, %.2f> gcarc=%f azim=%f eventEpoch=[%d] tP=%.3f tS=%.3f\n",
                eventCMT.getEventID(), evla, evlo, getStation(), stla, stlo, gcarc, azim, eventStartTime, arrivalTimeP, arrivalTimeS );

            double[] arrivalTimes = new double[2];
            arrivalTimes[0] = arrivalTimeP;
            arrivalTimes[1] = arrivalTimeS;

            return arrivalTimes;

    }

}
