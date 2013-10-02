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

import asl.metadata.Channel;
import asl.metadata.EpochData;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.StationMeta;
import asl.metadata.meta_new.ChannelMeta.ResponseUnits;

import asl.seedscan.database.MetricValueIdentifier;
import asl.seedscan.event.EventCMT;

import freq.Cmplx;
import timeutils.PSD;

import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sac.SacTimeSeries;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;


public abstract class Metric
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.Metric.class);

    private Hashtable<String, String> arguments;
    private Hashtable<CrossPowerKey, CrossPower> crossPowerMap;

    private boolean forceUpdate = false;
    private boolean makePlots   = false;
    private String outputDir    = null;

    protected final double NO_RESULT = -999.999; 

    protected StationMeta  stationMeta  = null;
    protected MetricData   metricData   = null;
    protected MetricResult metricResult = null;

    private Hashtable<String, EventCMT> eventTable = null;
    private Hashtable<String, Hashtable<String, SacTimeSeries>> eventSynthetics = null;

    public Metric()
    {
        arguments = new Hashtable<String, String>();
        crossPowerMap = new Hashtable<CrossPowerKey, CrossPower>();

        // MTH: 03-18-13: Added to allow these optional arguments to each cfg:metric in config.xml
        addArgument("makeplots");
        addArgument("forceupdate");
    }

    public void setData(MetricData metricData)
    {
        this.metricData = metricData;
        stationMeta = metricData.getMetaData();
        metricResult = new MetricResult(stationMeta, getName());
    }

    public abstract long getVersion();
    public abstract String getName();
    public abstract void process();

    public MetricValueIdentifier createIdentifier(Channel channel)
    {
    	return new MetricValueIdentifier(	metricResult.getDate(), metricResult.getMetricName(),
    										metricResult.getStation(), channel);
    }
    
    public MetricValueIdentifier createIdentifier(Channel channelA, Channel channelB)
    {
    	return createIdentifier(MetricResult.createChannel(MetricResult.createResultId(channelA, channelB)));
    }

    public Hashtable<CrossPowerKey, CrossPower> getCrossPowerMap()
    {
        return crossPowerMap;
    }

    public void setCrossPowerMap(Hashtable<CrossPowerKey, CrossPower> crossPowerMap)
    {
        this.crossPowerMap = crossPowerMap;
    }

    protected CrossPower getCrossPower(Channel channelA, Channel channelB)
    {
        CrossPowerKey key = new CrossPowerKey(channelA, channelB);
        CrossPower crossPower;

        if (crossPowerMap.containsKey(key)) {
            crossPower = crossPowerMap.get(key);
        }
        else {
            double[] psd = null;
            double[] df  = new double[1];            // Dummy array to get params out of computePSD()
            for (int i=0; i<df.length; i++) df[i]=0;
            try {
                psd = computePSD(channelA, channelB, df);
            }
            catch (NullPointerException e) {
                System.out.println("== Metric.getCrossPower NullPointerException = " + e);
            }
            crossPower = new CrossPower(psd, df[0]);
            crossPowerMap.put(key, crossPower);
        }
        return crossPower;
    }

    public Hashtable<String, SacTimeSeries> getEventSynthetics(String eventIdString)
    {
            if (eventSynthetics.containsKey( eventIdString ) ){
                return eventSynthetics.get( eventIdString );
            }
            else {
                System.out.format("== Metric.getEventSynthetics - Synthetics not found for eventIdString=[%s]\n", eventIdString);
                return null;
            }
    }

    public Hashtable<String, EventCMT> getEventTable()
    {
            return eventTable;
    }

    public void setEventSynthetics(Hashtable<String, Hashtable<String, SacTimeSeries>> eventSynthetics)
    {
        this.eventSynthetics = eventSynthetics;
    }

    public void setEventTable(Hashtable<String, EventCMT> events)
    {
        eventTable = events;
    }

    public MetricResult getMetricResult()
    {
        return metricResult;
    }

    public void setBaseOutputDir(String outputDir){
        this.outputDir = outputDir;
    }

    public String getBaseOutputDir(){
        return outputDir;
    }
    public String getOutputDir(){
        // e.g., "outputs/2012/2012160/2012160.IU_ANMO"
        return new String( String.format("%s/%4s/%4s%3s/%4s%3s.%s", getBaseOutputDir(), getYear(), getYear(), getDOY(),
                           getYear(), getDOY(), getStation())  );
    }

    public String getDay()
    {   // returns yyyy:ddd:hh:mm
        return (EpochData.epochToDateString( stationMeta.getTimestamp() ) );
    }
    public String getDOY()
    {
        String[] dateArray = getDay().split(":");
        return dateArray[1];
    }
    public String getYear()
    {
        String[] dateArray = getDay().split(":");
        return dateArray[0];
    }
    public String getStn()      // e.g., will return "ANMO"
    {
        return stationMeta.getStation();
    }
    public String getStation()  // e.g., will return "IU_ANMO"
    {
        return metricResult.getStation().toString();
    }

    private void setForceUpdate(){
        this.forceUpdate = true;
    }
    public boolean getForceUpdate(){
        return forceUpdate;
    }
    private void setMakePlots(){
        this.makePlots = true;
    }
    public boolean getMakePlots(){
        return makePlots;
    }

    public boolean weHaveChannels(String location, String band) {
        if (!stationMeta.hasChannels(location, band)) {
            return false;
        }
        if (!metricData.hasChannels(location, band)) {
            return false;
        }
        return true;
    }


// Dynamic argument managment
    protected final void addArgument(String name)
    {
        arguments.put(name, "");
    }

    public final void add(String name, String value)
    throws NoSuchFieldException
    {
        if (!arguments.containsKey(name)) {
            throw new NoSuchFieldException("Argument '" +name+ "' is not recognized.");
        }
        arguments.put(name, value);

        if (name.equals("forceupdate")) {
            if (value.toLowerCase().equals("true") || value.toLowerCase().equals("yes") ) {
                setForceUpdate();
            }
        }
        else if (name.equals("makeplots")) {
            if (value.toLowerCase().equals("true") || value.toLowerCase().equals("yes") ) {
                setMakePlots();
            }
        }
    }

/**
 * get(key) returns value if key is found in Hashtable arguments
 *          returns null  if key is found but value is not set (value="")
 *          throws NoSuchFieldException if key is not found
 */
    public final String get(String name)
    throws NoSuchFieldException
    {
        if (!arguments.containsKey(name)) {
            throw new NoSuchFieldException("Argument '" +name+ "' is not recognized.");
        }
        String argumentValue = arguments.get(name);
        if ((argumentValue == null) || (argumentValue.equals(""))) {
            argumentValue = null;
        }
        return argumentValue;
    }

    public final Enumeration<String> names()
    {
        return arguments.keys();
    }


/**
 * computePSD - Done here so that it can be passed from metric to metric,
 *              rather than re-computing it for each metric that needs it
 *
 * Use Peterson's algorithm (24 hrs = 13 segments with 75% overlap, etc.)
 *
 * @param channelX - X-channel used for power-spectral-density computation
 * @param channelY - Y-channel used for power-spectral-density computation
 * @param params[] - Dummy array used to pass df (frequency spacing) back up 
 * 
 * @return psd[f] - Contains smoothed crosspower-spectral density
 *                  computed for nf = nfft/2 + 1 frequencies (+ve freqs + DC + Nyq)
 * @author Mike Hagerty
*/
    private final double[] computePSD(Channel channelX, Channel channelY, double[] params) {

        int ndata      = 0;
        double srate   = 0;  // srate = sample frequency, e.g., 20Hz

// This would give us 2 channels with the SAME number of (overlapping) points, but 
//   they might not represent a complete day (e.g., could be a single block of data in the middle of the day)
//      double[][] channelOverlap = metricData.getChannelOverlap(channelX, channelY);
//      double[]   chanXData = channelOverlap[0];
//      double[]   chanYData = channelOverlap[1];

// Instead, getPaddedDayData() gives us a complete (zero padded if necessary) array of data for 1 day:
        double[] chanXData = metricData.getPaddedDayData(channelX);
        double[] chanYData = metricData.getPaddedDayData(channelY);

        double srateX = metricData.getChannelData(channelX).get(0).getSampleRate();
        double srateY = metricData.getChannelData(channelY).get(0).getSampleRate();
        ChannelMeta chanMetaX = stationMeta.getChanMeta(channelX);
        ChannelMeta chanMetaY = stationMeta.getChanMeta(channelY);

        if (srateX != srateY) {
            String message = "computePSD() ERROR: srateX (=" + srateX + ") != srateY (=" + srateY + ")";
            throw new RuntimeException(message);
        }
        srate = srateX;
        ndata = chanXData.length; 

        if (srate == 0) throw new RuntimeException("Error: Got srate=0");
        double dt = 1./srate;

        PSD psdRaw    = new PSD(chanXData, chanYData, dt);
        Cmplx[] spec  = psdRaw.getSpectrum();
        double[] freq = psdRaw.getFreq();
        double df     = psdRaw.getDeltaF();
        int nf = freq.length;

        params[0] = df;

     // Get the instrument response for Acceleration and remove it from the PSD
        Cmplx[]  instrumentResponseX = chanMetaX.getResponse(freq, ResponseUnits.ACCELERATION);
        Cmplx[]  instrumentResponseY = chanMetaY.getResponse(freq, ResponseUnits.ACCELERATION);

        Cmplx[] responseMagC        = new Cmplx[nf];

        double[] psd  = new double[nf]; // Will hold the 1-sided PSD magnitude
        psd[0]=0; 

     // We're computing the squared magnitude as we did with the FFT above
     //   Start from k=1 to skip DC (k=0) where the response=0

        for(int k = 1; k < nf; k++){
            responseMagC[k] = Cmplx.mul(instrumentResponseX[k], instrumentResponseY[k].conjg()) ;
            if (responseMagC[k].mag() == 0) {
                throw new RuntimeException("NLNMDeviation Error: responseMagC[k]=0 --> divide by zero!");
            }
            else {   // Divide out (squared)instrument response & Convert to dB:
                spec[k] = Cmplx.div(spec[k], responseMagC[k]);
                psd[k]  = spec[k].mag();
            }
        }

        return psd;

    } // end computePSD

} // end class
