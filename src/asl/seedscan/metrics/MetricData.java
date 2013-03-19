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

import asl.seedscan.database.*;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.EpochData;
import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;
import asl.metadata.meta_new.ChannelMeta;
import asl.security.MemberDigest;
import asl.seedsplitter.BlockLocator;
import asl.seedsplitter.ContiguousBlock;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.SeedSplitter;
import asl.seedsplitter.IllegalSampleRateException;
import asl.seedsplitter.Sequence;
import asl.seedsplitter.SequenceRangeException;
import asl.util.Hex;

import timeutils.Timeseries;
import freq.Cmplx;

import seed.Blockette320;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Logger;
import java.util.Calendar;

public class MetricData
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.MetricData");

    private Hashtable<String, ArrayList<DataSet>> data;
    private Hashtable<String, ArrayList<Integer>> qualityData;
    private Hashtable<String, ArrayList<Blockette320>> randomCal;
    private StationMeta metadata;
    private Hashtable<String, String> synthetics;
    private MetricReader metricReader;

    private MetricData nextMetricData;

// Attach nextMetricData here for windows that span into next day
    public void setNextMetricData( MetricData nextMetricData ) {
        this.nextMetricData = nextMetricData;
    }
    public MetricData getNextMetricData() {
        return nextMetricData;
    }
    public void setNextMetricDataToNull( ) {
        this.nextMetricData = null;
    }

  //constructor(s)
    public MetricData(	MetricReader metricReader, Hashtable<String,
    					ArrayList<DataSet>> data, StationMeta metadata,
    					Hashtable<String, String> synthetics)
    {
    	this.metricReader = metricReader;
        this.data = data;
        this.metadata = metadata;
        this.synthetics = synthetics;
    }

    public MetricData(	MetricReader metricReader, Hashtable<String,
    					ArrayList<DataSet>> data, StationMeta metadata)
    {
    	this.metricReader = metricReader;
        this.data = data;
        this.metadata = metadata;
    }

    public MetricData(	MetricReader metricReader, Hashtable<String,ArrayList<DataSet>> data, 
                        Hashtable<String,ArrayList<Integer>> qualityData, StationMeta metadata,
                        Hashtable<String,ArrayList<Blockette320>> randomCal)
    {
    	this.metricReader = metricReader;
        this.data         = data;
        this.qualityData  = qualityData;
        this.randomCal    = randomCal;
        this.metadata     = metadata;
    }

    public MetricData(	Hashtable<String,ArrayList<DataSet>> data, 
                        Hashtable<String,ArrayList<Integer>> qualityData, StationMeta metadata,
                        Hashtable<String,ArrayList<Blockette320>> randomCal)
    {
    	this.metricReader = null;
        this.data         = data;
        this.qualityData  = qualityData;
        this.randomCal    = randomCal;
        this.metadata     = metadata;
    }


    public MetricData(	MetricReader metricReader, Hashtable<String,ArrayList<DataSet>> data, 
                        Hashtable<String,ArrayList<Integer>> qualityData, StationMeta metadata)
    {
    	this.metricReader = metricReader;
        this.data         = data;
        this.qualityData  = qualityData;
        this.metadata     = metadata;
    }

    public MetricData(	MetricReader metricReader,
    					Hashtable<String, ArrayList<DataSet>> data)
    {
    	this.metricReader = metricReader;
        this.data = data;
    }

    // MTH: Added simple constructor for AvailabilityMetric when there is NO data
    public MetricData( StationMeta metadata)
    {
        this.metadata = metadata;
    }

    public StationMeta getMetaData()
    {
        return metadata;
    }

    public Boolean hasChannelArrayData(ChannelArray channelArray)
    {
        for (Channel channel : channelArray.getChannels() ) {
            if (!hasChannelData(channel) )
                return false;
        }
        return true;
    }

    public Boolean hasChannelData(Channel channel)
    {
        return hasChannelData( channel.getLocation(), channel.getChannel() );
    }

    public Boolean hasChannelData(String location, String name)
    {
        if (data == null) { return false; }

        Boolean hasChannel = false;
        String locationName = location + "-" + name;
        Set<String> keys = data.keySet();
        for (String key : keys){          // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
            if (key.contains(locationName) ){
                hasChannel = true;
            }
        }
        return hasChannel;           
    }

/**
 *
 * @return ArrayList<DataSet> = All DataSets for a given channel (e.g., "00-BHZ")
 *
 */
    public ArrayList<DataSet> getChannelData(String location, String name)
    {
        String locationName = location + "-" + name;
        Set<String> keys = data.keySet();
        for (String key : keys){          // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
           if (key.contains(locationName) ){
            //System.out.format(" key=%s contains locationName=%s\n", key, locationName);
              return data.get(key);       // return ArrayList<DataSet>
           }
        }
        return null;           
    }

    public ArrayList<DataSet> getChannelData(Channel channel)
    {
        return getChannelData(channel.getLocation(), channel.getChannel() );           
    }

 // Random calibration info

    public Boolean hasCalibrationData() {
        if (randomCal == null) {
            return false;
        }
        else {
            return true;
        }
    }

    public ArrayList<Blockette320> getChannelCalData(Channel channel)
    {
        return getChannelCalData(channel.getLocation(), channel.getChannel() );           
    }
    public ArrayList<Blockette320> getChannelCalData(String location, String name)
    {
        if (!hasCalibrationData()) return null; // randomCal was never created --> Probably not a calibration day

        String locationName = location + "-" + name;
        Set<String> keys = randomCal.keySet();
        for (String key : keys){          // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
           if (key.contains(locationName) ){
              return randomCal.get(key); 
           }
        }
        return null;           
    }

 // Timing quality info:

    public ArrayList<Integer> getChannelQualityData(Channel channel)
    {
        return getChannelQualityData(channel.getLocation(), channel.getChannel() );           
    }

    public ArrayList<Integer> getChannelQualityData(String location, String name)
    {
        String locationName = location + "-" + name;
        Set<String> keys = qualityData.keySet();
        for (String key : keys){          // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
           if (key.contains(locationName) ){
              return qualityData.get(key); 
           }
        }
        return null;           
    }

/**
 *  Return the 3 component displacement (ZNE) in MICRONS for this location + band (e.g., "00-LH") for this time+freq window
 */
    public ArrayList<double[]> getZNE(String location, String band, long windowStartEpoch, long windowEndEpoch,
                                      double freqMin, double freqMax) 
    {
        ArrayList<double[]> dispZNE = new ArrayList<double[]>();

        Channel vertChannel = new Channel(location, (band + "Z") ); // e.g., "00-LHZ"
        double[] z = getFilteredDisplacement(vertChannel, windowStartEpoch, windowEndEpoch, freqMin, freqMax);
        dispZNE.add(z);

        Channel channel1 = metadata.getChannel(location, band, "1"); // e.g., could be "00-LH1" -or- "00-LHN"
        Channel channel2 = metadata.getChannel(location, band, "2"); // e.g., could be "00-LH2" -or- "00-LHE"

        if (channel1 == null) {
        }
        if (channel2 == null) {
        }

    // Metadata: <channel1, channel2> --> Data: <x, y>  ===> Get displ and rotate <x,y> to <N,E>

        double[] x = getFilteredDisplacement(channel1, windowStartEpoch, windowEndEpoch, freqMin, freqMax);
        double[] y = getFilteredDisplacement(channel2, windowStartEpoch, windowEndEpoch, freqMin, freqMax);

        if (x.length != y.length) {
        }
        int ndata = x.length;

        double srate1 = metadata.getChanMeta(channel1).getSampleRate();
        double srate2 = metadata.getChanMeta(channel2).getSampleRate();

        if (srate1 != srate2) {
            throw new RuntimeException("MetricData.createRotatedChannels(): Error: srate1 != srate2 !!");
        }

        double[] n = new double[ndata];
        double[] e = new double[ndata];

        double az1 = (metadata.getChanMeta( channel1 )).getAzimuth(); 
        double az2 = (metadata.getChanMeta( channel2 )).getAzimuth(); 

System.out.format("==== rotate_xy_to_ne: az1=%f az2=%f\n", az1, az2);
        rotate_xy_to_ne(az1, az2, x, y, n, e);

        dispZNE.add(n);
        dispZNE.add(e);

        return dispZNE;

    }

    private void rotate_xy_to_ne(double az1, double az2, double[] x, double[] y, double[] n, double[] e) {

    // INITIALLY: Lets assume the horizontal channels are PERPENDICULAR and use a single azimuth to rotate
    //  We'll check the azimuths and flip signs to put channel1 to +N half and channel 2 to +E

        int quadrant=0;
        double  azimuth=-999;
        int sign1 = 1;
        int sign2 = 1;
        if (az1 >= 0 && az1 < 90) {
            quadrant = 1;
            azimuth  = az1;
        }
        else if (az1 >= 90 && az1 < 180) {
            quadrant = 2;
            azimuth  = az1 - 180;
            sign1    =-1;
        }
        else if (az1 >= 180 && az1 < 270) {
            quadrant = 3;
            azimuth  = az1 - 180;
            sign1    =-1;
        }
        else if (az1 >= 270 && az1 < 360) {
            quadrant = 4;
            azimuth  = az1 - 360;
        }
        else { // ?? 
            System.out.format("== OOPS: MetricData.createRotatedChannels(): Don't know how to rotate az1=%f\n", az1);
        }

        sign2 = 1;
        if (az2 >= 0 && az2 < 180) {
            sign2    = 1;
        }
        else if (az2 >= 180 && az2 < 360) {
            sign2    =-1;
        }
        else { // ?? 
            System.out.format("== OOPS: MetricData.createRotatedChannels(): Don't know how to rotate az2=%f\n", az2);
        }

        double cosAz   = Math.cos( azimuth * Math.PI/180 );
        double sinAz   = Math.sin( azimuth * Math.PI/180 );

        for (int i=0; i<x.length; i++){
            n[i] = sign1 * x[i] * cosAz - sign2 * y[i] * sinAz;
            e[i] = sign1 * x[i] * sinAz + sign2 * y[i] * cosAz;
        }

    } // end rotate_xy_to_ne


    public double[] getFilteredDisplacement(Channel channel, long windowStartEpoch, long windowEndEpoch,
                                            double freqMin, double freqMax) 
    {
        if (!metadata.hasChannel(channel)) {
            logger.severe( String.format("Error: Metadata NOT found for channel=[%s] --> Can't return Displacement",channel) );
            return null;
        }
        double[] timeseries = getWindowedData(channel, windowStartEpoch, windowEndEpoch);
        if (timeseries == null) {
            logger.severe( String.format("Error: Did not get requested window for channel=[%s] --> Can't return Displacement",channel) );
            return null;
        }
        double filtered[] = removeInstrumentAndFilter(channel, timeseries, freqMin, freqMax);

        return filtered;

    }

    public double bpass(int n,int n1,int n2,int n3,int n4) {

             if (n<=n1 || n>=n4) return(0.);
        else if (n>=n2 && n<=n3) return(1.);
        else if (n>n1  && n<n2 ) return( .5*(1-Math.cos(Math.PI*(n-n1)/(n2-n1))) );
        else if (n>n3  && n<n4 ) return( .5*(1-Math.cos(Math.PI*(n4-n)/(n4-n3))) );
        else return(-9999999.);
    }


    public double[] removeInstrumentAndFilter(Channel channel, double[] timeseries, double freqMin, double freqMax){

        if (freqMax <= freqMin) {
            logger.severe( String.format("Error: freqMax (=%f) <= freqMin (=%f)", freqMax, freqMin) );
            return null;
        }

        ChannelMeta chanMeta = metadata.getChanMeta(channel);
        double srate = chanMeta.getSampleRate();
        int ndata    = timeseries.length; 

        if (srate == 0) throw new RuntimeException("Error: Got srate=0");

     // Find smallest power of 2 >= ndata:
        int nfft=1;
        while (nfft < ndata) nfft = (nfft << 1);

     // We are going to do an nfft point FFT which will return 
     //   nfft/2+1 +ve frequencies (including  DC + Nyq)
        int nf=nfft/2 + 1;

        double dt = 1./srate;
        double df = 1./(nfft*dt);

        Cmplx[]  xfft = new Cmplx[nf];

        double[] data = new double[timeseries.length];
        for (int i=0; i<timeseries.length; i++){
            data[i] = timeseries[i];
        }
        Timeseries.detrend(data);
        Timeseries.debias(data);
        double wss = Timeseries.costaper(data,.01);

        double[] freq = new double[nf];
        for(int k = 0; k < nf; k++){
            freq[k] = (double)k * df;
        }
     // Get the instrument response for Displacement
        Cmplx[]  instrumentResponse = chanMeta.getResponse(freq, 1);
        //Cmplx[]  instrumentResponse = chanMeta.getResponse(freq, 2);

        // fft2 returns just the (nf = nfft/2 + 1) positive frequencies
        xfft = Cmplx.fft2(data);
        double f1 = .008;
        double f2 = .01;
        double f3 = .4;
        double f4 = .5;

/**
        double f1 = freqMin * .9;
        double f2 = freqMin;
        double f3 = freqMax;
        double f4 = freqMax * 1.1;
**/
        double fNyq = (double)(nf-1)*df;

        if (f4 > fNyq) {
            f4 = fNyq;
        }

        int k1=(int)(f1/df); int k2=(int)(f2/df);
        int k3=(int)(f3/df); int k4=(int)(f4/df);

        for(int k = 0; k < nf; k++){
            double taper = bpass(k,k1,k2,k3,k4);
            xfft[k] = Cmplx.div(xfft[k],instrumentResponse[k]);     // Remove instrument
            xfft[k] = Cmplx.mul(xfft[k],taper);                     // Bandpass
//System.out.format("== k=%04d f=%8.4f taper=%8.4f\n", k, freq[k], taper);
        }

        Cmplx[] cfft = new Cmplx[nfft];
        cfft[0]    = new Cmplx(0.0, 0.0);     // DC
        //cfft[0]    = xfft[0];     // DC
        cfft[nf-1] = xfft[nf-1];  // Nyq
        for(int k = 1; k < nf-1; k++){      // Reflect spec about the Nyquist to get -ve freqs
            cfft[k]        = xfft[k];
            cfft[2*nf-2-k] = xfft[k].conjg();
        }

        float[] foo = Cmplx.fftInverse(cfft, ndata);

        double[] dfoo=new double[ndata];
        for (int i=0; i<foo.length; i++){
            dfoo[i] = (double)foo[i] * 1000000.;  // Convert meters --> micrometers (=microns)
        }
        return dfoo;

/**
 * N input time samples --> Pad in real array of length 2N --> four1 outputs N (+ve + -ve) Complex spectra
 *     stored in real array of length 2N.
 * fft2 then returns the (nf = N/2 + 1) +ve freqs + DC + Nyq as nf Complex values: 
 * 
 * nf Complex spec = +ve f's + DC + Nyq ... numbered [0],...[nf-1]
 *    Real            Imag         f
 * xfft[0].re      xfft[0].im      DC
 * xfft[1].re      xfft[1].im      df  
 * xfft[2].re      xfft[2].im      2df
 *     :               :            :
 * xfft[nf-1].re   xfft[nf-1].im  fNyq = (nf-1)df = (N/2)df
 */

/**
 * In order to use fftInverse (which uses four1), I need to repack this complex array into a larger
 * one with the -ve freqs reflected about the Nyquist:
 *    Real            Imag         f
 * xfft[0].re      xfft[0].im      DC
 * xfft[1].re      xfft[1].im      df  
 * xfft[2].re      xfft[2].im      2df
 * xfft[3].re      xfft[3].im      3df
 *     :               :            :
 * xfft[nf-3].re   xfft[nf-3].im  (nf-3)df
 * xfft[nf-2].re   xfft[nf-2].im  (nf-2)df
 * xfft[nf-1].re   xfft[nf-1].im  (nf-1)df = (N/2)df = fNyq
 * xfft[nf  ].re   xfft[nf  ].im -(nf-2)df
 * xfft[nf+1].re   xfft[nf+1].im -(nf-3)df
 *     :               :            :
 * xfft[2nf-5].re  xfft[2nf-5].im  -3df
 * xfft[2nf-4].re  xfft[2nf-4].im  -2df
 * xfft[2nf-3].re  xfft[2nf-3].im  -df

 * N/2 - 1 +ve f's
 *     +
 * N/2 - 1 -ve f's
 *     +
 *     2   =DC + Nyq
 *==================
 *  = N freqs total --> nf = N/2+1 --> (N = 2nf-2), indexed from [0], [1], ..., [2nf-3]
 */


    }


/**
 *
 */
    public double[] getWindowedData(Channel channel, long windowStartEpoch, long windowEndEpoch) 
    {
        if (windowStartEpoch > windowEndEpoch) {
            System.out.format("== getWindowedData ERROR: Requested window Epoch [%d - %d] is NOT VALID "
              + "(windowStartEpoch > windowEndEpoch\n", 
                 windowStartEpoch, windowEndEpoch );
            return null;
        }

        if (!hasChannelData(channel)){
            System.out.format("== getWindowedData ERROR: We have NO data for channel=[%s]\n", channel);
            return null;
        }
        ArrayList<DataSet>datasets = getChannelData(channel);
        DataSet data = null;
        Boolean windowFound = false;
        int nSet=0;
        for (int i=0; i<datasets.size(); i++) {
            data = datasets.get(i);
            long startEpoch     = data.getStartTime() / 1000;  // Convert microsecs --> millisecs
            long endEpoch       = data.getEndTime()   / 1000;  // ...
            if (windowStartEpoch >= startEpoch && windowStartEpoch < endEpoch) {
                windowFound = true;
                nSet = i;
                break;
            }
        }

        if (!windowFound) {
            System.out.format("== getWindowedData ERROR: Requested window Epoch [%d - %d] was NOT FOUND "
              + "within DataSet for channel=[%s]\n", windowStartEpoch, windowEndEpoch, channel);
            return null;
        }
        else {
            //System.out.format("== getWindowedData: Requested window Epoch [%d - %d] WAS FOUND "
              //+ "within DataSet[i=%d] for channel=[%s]\n", windowStartEpoch, windowEndEpoch, nSet, channel);
        }

        long dataStartEpoch     = data.getStartTime() / 1000;  // Convert microsecs --> millisecs
        long dataEndEpoch       = data.getEndTime()   / 1000;  // ...
        long interval           = data.getInterval()  / 1000;  // Convert microsecs --> millisecs (dt = sample interval)
        double srate1           = data.getSampleRate(); 

System.out.format("== windowStartEpoch=[%d] dataStartEpoch=[%d]\n", windowStartEpoch, dataStartEpoch);

    // Requested Window must start in Day 1 (taken from current dataset(0))
        if (windowStartEpoch < dataStartEpoch || windowStartEpoch > dataEndEpoch) {
            System.out.format("== getWindowedData ERROR: Requested window Epoch [%d - %d] does NOT START "
              + "in current day data window Epoch [%d - %d] for channel=[%s]\n", 
                 windowStartEpoch, windowEndEpoch, dataStartEpoch, dataEndEpoch, channel );
            return null;
        }

        Boolean spansDay = false;
        DataSet nextData = null;

        if (windowEndEpoch > dataEndEpoch) { // Window appears to span into next day
            if (nextMetricData == null) {
                logger.severe( String.format("== getWindowedData: Requested Epoch window[%d-%d] spans into next day, but we have NO data "
                                 +"for channel=[%s] for next day\n", windowStartEpoch, windowEndEpoch, channel) );
                return null;
            }
            if (!nextMetricData.hasChannelData(channel)){
                System.out.format("== getWindowedData ERROR: Requested Epoch window spans into next day, but we have NO data "
                                 +"for channel=[%s] for next day\n", channel);
                return null;
            }

            datasets = nextMetricData.getChannelData(channel);
            nextData = datasets.get(0);

            long nextDataStartEpoch     = nextData.getStartTime() / 1000;  // Convert microsecs --> millisecs
            long nextDataEndEpoch       = nextData.getEndTime()   / 1000;  // ...
            double srate2               = nextData.getSampleRate(); 

            if (srate2 != srate1) {
                System.out.format("== getWindowedData ERROR: Requested window Epoch [%d - %d] extends into "
                + "nextData window Epoch [%d - %d] for channel=[%s] but srate1[%f] != srate2[%f]\n", 
                    windowStartEpoch, windowEndEpoch, nextDataStartEpoch, nextDataEndEpoch, srate1, srate2 );
                return null;
            }

    // Requested Window must end in Day 2 (taken from next day dataset(0))

            if (windowEndEpoch > nextDataEndEpoch) {
                System.out.format("== getWindowedData ERROR: Requested window Epoch [%d - %d] extends BEYOND "
                + "found nextData window Epoch [%d - %d] for channel=[%s]\n", 
                    windowStartEpoch, windowEndEpoch, nextDataStartEpoch, nextDataEndEpoch );
                return null;
            }

            spansDay = true;

        }

        long windowMilliSecs = windowEndEpoch - windowStartEpoch;
        int  nWindowPoints   = (int)(windowMilliSecs / interval);

/**
        System.out.format("== getWindowedData: Requested window Epoch [%d - %d] = [%d millisecs] = [%d points]\n",
            windowStartEpoch, windowEndEpoch, windowMilliSecs, nWindowPoints);
        System.out.format("== getWindowedData: DataEpoch [%d - %d] data srate1=%f interval=[%d] msecs\n",
            dataStartEpoch, dataEndEpoch, srate1, interval);
**/

        double[] dataArray  = new double[nWindowPoints];

        int[] series1 = data.getSeries();
        int[] series2 = null;
        if (spansDay) {
             series2 = nextData.getSeries();
        }
        int j=0;

        //int  istart   = (int)((windowStartEpoch - dataStartEpoch) / interval);
        // MTH: this seems to line it up better with rdseed output window but doesn't seem right ...
        int  istart   = (int)((windowStartEpoch - dataStartEpoch) / interval) + 1;

        for (int i=0; i<nWindowPoints; i++){
            if( (istart + i) < data.getLength() ) {
                dataArray[i] = (double)series1[i + istart];
            }
            else if (j < nextData.getLength()) {
                dataArray[i] = (double)series2[j++];
            }
            else {
                // We should never be here!
            }
        }

        return dataArray;

    } // end getWindowedData



/**
 *  Return a full day (86400 sec) array of data assembled from a channel's DataSets
 *  Zero pad any gaps between DataSets
 */
    public double[] getPaddedDayData(Channel channel) 
    {
        if (!hasChannelData(channel)){
            System.out.format("== MetricData.getPaddedDayData() ERROR: We have NO data for channel=[%s]\n", channel);
            return null;
        }
        ArrayList<DataSet>datasets = getChannelData(channel);

        long dayStartTime = metadata.getTimestamp().getTimeInMillis() * 1000; // epoch microsecs since 1970
        long interval     = datasets.get(0).getInterval();                    // sample dt in microsecs

        int nPointsPerDay = (int)(86400000000L/interval);

        double[] data     = new double[nPointsPerDay];

        long lastEndTime = 0;
        int k=0;

        for (int i=0; i<datasets.size(); i++) {
            DataSet dataset= datasets.get(i);
            long startTime = dataset.getStartTime();  // microsecs since Jan. 1, 1970
            long endTime   = dataset.getEndTime();
            int length     = dataset.getLength();
            int[] series   = dataset.getSeries();

            if (i == 0) {
                lastEndTime = dayStartTime;
            }
            int npad = (int)( (startTime - lastEndTime) / interval ) - 1;

            for (int j=0; j<npad; j++){
                if (k < data.length) {
                    data[k] = 0.;
                }
                k++;
            }
            for (int j=0; j<length; j++){
                if (k < data.length) {
                    data[k] = (double)series[j];
                }
                k++;
            }

            lastEndTime = endTime;
        }
        //System.out.format("== fullDayData: nDataSets=%d interval=%d nPointsPerDay%d k=%d\n", datasets.size(),
                          //interval, nPointsPerDay, k );
        return data;
    }


/*
 *  Rotate/Create new derived channels: (chan1, chan2) --> (chanN, chanE)
 *  And add these to StationData
 *  Channels we can derive end in H1,H2 (e.g., LH1,LH2 or HH1,HH2) --> LHND,LHED or HHND,HHED
 *                             or N1,N2 (e.g., LN1,LN2 or HN1,HN2) --> LNND,LNED or HNND,HNED
 */
    public void createRotatedChannelData(String location, String channelPrefix)
    {
        Boolean use12 = true; // Use ?H1,?H2 to rotate, else use ?HN,?HE

    // Raw horizontal channels used for rotation
        Channel channel1 = new Channel(location, String.format("%s1", channelPrefix) );
        Channel channel2 = new Channel(location, String.format("%s2", channelPrefix) );

    // If we can't find ?H1,?H2 --> try for ?HN,?HE
        if (hasChannelData(channel1)==false || hasChannelData(channel2)==false){
            channel1.setChannel(String.format("%sN", channelPrefix));
            channel2.setChannel(String.format("%sE", channelPrefix));
            use12 = false;
        }
    // If we still can't find 2 horizontals to rotate then give up
        if (hasChannelData(channel1)==false || hasChannelData(channel2)==false){
            System.out.format("== createRotatedChannelData: Error -- Unable to find data "
            + "for channel1=[%s] and/or channel2=[%s] --> Unable to Rotate!\n",channel1, channel2);
            return;
        }

        if (metadata.hasChannel(channel1)==false || metadata.hasChannel(channel2)==false){
            System.out.format("== createRotatedChannelData: Error -- Unable to find metadata "
            + "for channel1=[%s] and/or channel2=[%s] --> Unable to Rotate!\n",channel1, channel2);
            return;
        }

    // Rotated (=derived) channels (e.g., 00-LHND,00-LHED -or- 10-BHND,10-BHED, etc.)
        Channel channelN = new Channel(location, String.format("%sND", channelPrefix) );
        Channel channelE = new Channel(location, String.format("%sED", channelPrefix) );

    // Get overlapping data for 2 horizontal channels and confirm equal sample rate, etc.
        long[] foo = new long[1];
        double[][] channelOverlap = getChannelOverlap(channel1, channel2, foo);
    // The startTime of the largest overlapping segment
        long startTime = foo[0]; 

        double[]   chan1Data = channelOverlap[0];
        double[]   chan2Data = channelOverlap[1];
    // At this point chan1Data and chan2Data should have the SAME number of (overlapping) points

        int ndata = chan1Data.length;

        double srate1 = getChannelData(channel1).get(0).getSampleRate();
        double srate2 = getChannelData(channel2).get(0).getSampleRate();
        if (srate1 != srate2) {
            throw new RuntimeException("MetricData.createRotatedChannels(): Error: srate1 != srate2 !!");
        }

        double[]   chanNData = new double[ndata];
        double[]   chanEData = new double[ndata];

    // INITIALLY: Lets assume the horizontal channels are PERPENDICULAR and use a single azimuth to rotate
    //  We'll check the azimuths and flip signs to put channel1 to +N half and channel 2 to +E
        double az1 = (metadata.getChanMeta( channel1 )).getAzimuth(); 
        double az2 = (metadata.getChanMeta( channel2 )).getAzimuth(); 

        int quadrant=0;
        double  azimuth=-999;
        int sign1 = 1;
        int sign2 = 1;
        if (az1 >= 0 && az1 < 90) {
            quadrant = 1;
            azimuth  = az1;
        }
        else if (az1 >= 90 && az1 < 180) {
            quadrant = 2;
            azimuth  = az1 - 180;
            sign1    =-1;
        }
        else if (az1 >= 180 && az1 < 270) {
            quadrant = 3;
            azimuth  = az1 - 180;
            sign1    =-1;
        }
        else if (az1 >= 270 && az1 < 360) {
            quadrant = 4;
            azimuth  = az1 - 360;
        }
        else { // ?? 
            System.out.format("== OOPS: MetricData.createRotatedChannels(): Don't know how to rotate az1=%f\n", az1);
        }

        sign2 = 1;
        if (az2 >= 0 && az2 < 180) {
            sign2    = 1;
        }
        else if (az2 >= 180 && az2 < 360) {
            sign2    =-1;
        }
        else { // ?? 
            System.out.format("== OOPS: MetricData.createRotatedChannels(): Don't know how to rotate az2=%f\n", az2);
        }

        System.out.format("== MetricData.createRotatedChannels for [%s-%s]: az1=%.2f --> azimuth=%.2f az2=%.2f"
          + " Quadrant=%d (sign1=%d, sign2=%d)\n", location, channelPrefix, az1, azimuth, az2, quadrant, sign1, sign2);

        double cosAz   = Math.cos( azimuth * Math.PI/180 );
        double sinAz   = Math.sin( azimuth * Math.PI/180 );

        for (int i=0; i<ndata; i++){
            chanNData[i] = sign1 * chan1Data[i] * cosAz - sign2 * chan2Data[i] * sinAz;
            chanEData[i] = sign1 * chan1Data[i] * sinAz + sign2 * chan2Data[i] * cosAz;
        }

/**
    // az1 = azimuth of the H1 channel/vector.  az2 = azimuth of the H2 channel/vector
    // Find the smallest (<= 180) angle between them --> This *should* be 90 (=orthogonal channels)
        double azDiff = Math.abs(az1 - az2);
        if (azDiff > 180) azDiff = Math.abs(az1 - az2 - 360);

        if ( Math.abs( azDiff - 90. ) > 0.2 ) {
            System.out.format("== createRotatedChannels: channels are NOT perpendicular! az1-az2 = %f\n",
                               Math.abs(az1 - az2) );
        }
**/

// Here we need to convert the Series intArray[] into a DataSet with header, etc ...

// Make new channelData keys based on existing ones

        String northKey = null;
        String eastKey  = null;

        // keys look like "IU_ANMO 00-BH1 (20.0 Hz)"
        //             or "IU_ANMO 10-BH1 (20.0 Hz)"
        String lookupString = null;
        if (use12) {
            lookupString = location + "-" + channelPrefix + "1";  // e.g., "10-BH1"
        }
        else {
            lookupString = location + "-" + channelPrefix + "N";  // e.g., "10-BHN"
        }

        String northString  = location + "-" + channelPrefix + "ND"; // e.g., "10-BHND"
        String eastString   = location + "-" + channelPrefix + "ED"; // e.g., "10-BHED"

        Set<String> keys = data.keySet();
        for (String key : keys){   
           if (key.contains(lookupString)) { // "LH1" --> "LHND" and "LHED"
                northKey = key.replaceAll(lookupString, northString);
                eastKey  = key.replaceAll(lookupString, eastString);
           }
        }
        //System.out.format("== MetricData.createRotatedChannels(): channel1=%s, channelPrefex=%s\n", channel1, channelPrefix);
        //System.out.format("== MetricData.createRotatedChannels(): northKey=[%s] eastKey=[%s]\n", northKey, eastKey);

        DataSet ch1Temp = getChannelData(channel1).get(0);
        String network  = ch1Temp.getNetwork();
        String station  = ch1Temp.getStation();
        //String location = ch1Temp.getLocation();

        DataSet northDataSet = new DataSet();
        northDataSet.setNetwork(network);
        northDataSet.setStation(station);
        northDataSet.setLocation(location);
        northDataSet.setChannel(channelN.getChannel());
        northDataSet.setStartTime(startTime);
        try {
            northDataSet.setSampleRate(srate1);
        } catch (IllegalSampleRateException e) {
            logger.finer(String.format("MetricData.createRotatedChannels(): Invalid Sample Rate = %f", srate1) );
        }

        int[] intArray = new int[ndata];
        for (int i=0; i<ndata; i++){
            intArray[i] = (int)chanNData[i];
        }
        northDataSet.extend(intArray, 0, ndata);

        ArrayList<DataSet> dataList = new ArrayList<DataSet>();
        dataList.add(northDataSet);
        data.put(northKey, dataList);

        DataSet eastDataSet = new DataSet();
        eastDataSet.setNetwork(network);
        eastDataSet.setStation(station);
        eastDataSet.setLocation(location);
        eastDataSet.setChannel(channelE.getChannel());
        eastDataSet.setStartTime(startTime);
        try {
            eastDataSet.setSampleRate(srate1);
        } catch (IllegalSampleRateException e) {
            logger.finer(String.format("MetricData.createRotatedChannels(): Invalid Sample Rate = %f", srate1) );
        }

        for (int i=0; i<ndata; i++){
            intArray[i] = (int)chanEData[i];
        }
        eastDataSet.extend(intArray, 0, ndata);

        dataList = new ArrayList<DataSet>();
        dataList.add(eastDataSet);
        data.put(eastKey, dataList);

    } // end createRotatedChannels()


/**
 *  getChannelOverlap - find the overlapping samples between 2+ channels
 *
 */
    public double[][] getChannelOverlap(Channel channelX, Channel channelY) {
        // Dummy var to hold startTime of overlap
        // Call the 3 param version below if you want the startTime back
        long[] foo = new long[1];
        return getChannelOverlap(channelX, channelY, foo);
    }

    public double[][] getChannelOverlap(Channel channelX, Channel channelY, long[] startTime) {

        ArrayList<ArrayList<DataSet>> dataLists = new ArrayList<ArrayList<DataSet>>();

        ArrayList<DataSet> channelXData = getChannelData(channelX);
        ArrayList<DataSet> channelYData = getChannelData(channelY);
        if (channelXData == null) {
            System.out.format("== getChannelOverlap: Error --> No DataSets found for Channel=%s\n", channelX);
        }
        if (channelYData == null) {
            System.out.format("== getChannelOverlap: Error --> No DataSets found for Channel=%s\n", channelY);
        }
        dataLists.add(channelXData);
        dataLists.add(channelYData);

        //System.out.println("Locating contiguous blocks...");

        ArrayList<ContiguousBlock> blocks = null;
        BlockLocator locator = new BlockLocator(dataLists);
        //Thread blockThread = new Thread(locator);
        //blockThread.start();
        locator.doInBackground();
        blocks = locator.getBlocks();

        //System.out.println("Found " + blocks.size() + " Contiguous Blocks");

        ContiguousBlock largestBlock = null;
        ContiguousBlock lastBlock = null;
        for (ContiguousBlock block: blocks) {
            if ((largestBlock == null) || (largestBlock.getRange() < block.getRange())) {
                largestBlock = block;
            }
            if (lastBlock != null) {
                System.out.println("    Gap: " + ((block.getStartTime() - lastBlock.getEndTime()) / block.getInterval()) + " data points (" + (block.getStartTime() - lastBlock.getEndTime()) + " microseconds)");
            }
            //System.out.println("  Time Range: " + Sequence.timestampToString(block.getStartTime()) + " - " + Sequence.timestampToString(block.getEndTime()) + " (" + ((block.getEndTime() - block.getStartTime()) / block.getInterval() + 1) + " data points)");
            lastBlock = block;
        }

        double[][] channels = {null, null};
        int[] channel = null;

        for (int i = 0; i < 2; i++) {
            boolean found = false;
            for (DataSet set: dataLists.get(i)) {
                if ((!found) && set.containsRange(largestBlock.getStartTime(), largestBlock.getEndTime())) {
                    try {
                        //System.out.println("  DataSet[" +i+ "]: " + Sequence.timestampToString(set.getStartTime()) + " - " + Sequence.timestampToString(set.getEndTime()) + " (" + ((set.getEndTime() - set.getStartTime()) / set.getInterval() + 1) + " data points)");
                        channel = set.getSeries(largestBlock.getStartTime(), largestBlock.getEndTime());
                        channels[i] = intArrayToDoubleArray(channel);
                    } catch (SequenceRangeException e) {
                        //System.out.println("SequenceRangeException");
                        e.printStackTrace();
                    }
                    found = true;
                    break;
                }
            }
        }

    // See if we have a problem with the channel data we are about to return:
        if (channels[0].length == 0 || channels[1].length == 0 || channels[0].length != channels[1].length){
            System.out.println("== getChannelOverlap: WARNING --> Something has gone wrong!");
        }

    // MTH: hack to return the startTime of the overlapping length of data points
        startTime[0] = largestBlock.getStartTime();

        return channels;

    } // end getChannelOverlap


    /**
     * Converts an array of type int into an array of type double.
     *
     * @param   source     The array of int values to be converted.
     * 
     * @return  An array of double values.
     */
    static double[] intArrayToDoubleArray(int[] source) 
    {
        double[] dest = new double[source.length];
        int length = source.length;
        for (int i = 0; i < length; i++) {
            dest[i] = source[i];
        }
        return dest;
    }


/**
 *  valueDigestChanged - Determine if the current digest computed for a channel or channelArray
 *                       has changed from the value stored in the database.
 *  @return   null - If the digest has NOT changed or if unable to compute a digest (e.g., because
 *                   the channels don't exist or we are unable to compute rotated channels, etc.)
 *            null - Will cause the Metric that called valueDigestChanged to skip to the next channel
 *  @return digest - If the digest has changed (OR if the database is NOT connected so that we couldn't
 *                   get an old digest to compare).
 *          digest - Will cause the Metric that called valueDigestChanged to execute its computeMetric().
 */


    public ByteBuffer valueDigestChanged(Channel channel, MetricValueIdentifier id)
    {
        ChannelArray channelArray = new ChannelArray(channel.getLocation(), channel.getChannel());
        return valueDigestChanged(channelArray, id);
    }
    public ByteBuffer valueDigestChanged(ChannelArray channelArray, MetricValueIdentifier id)
    {
        return valueDigestChanged(channelArray, id, false);
    }

    public ByteBuffer valueDigestChanged(Channel channel, MetricValueIdentifier id, Boolean forceUpdate)
    {
        ChannelArray channelArray = new ChannelArray(channel.getLocation(), channel.getChannel());
        return valueDigestChanged(channelArray, id, forceUpdate);
    }

    public ByteBuffer valueDigestChanged(ChannelArray channelArray, MetricValueIdentifier id, Boolean forceUpdate)
    {
        String metricName = id.getMetricName();
        Station station   = id.getStation();
        Calendar date     = id.getDate();
        String channelId  = MetricResult.createResultId(id.getChannel());

/**
        logger.fine(String.format(
                    "MetricValueIdentifier --> date=%04d-%02d-%02d (%03d) %02d:%02d:%02d | metricName=%s station=%s channel=%s",
                    date.get(Calendar.YEAR), (date.get(Calendar.MONTH)+1), date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.DAY_OF_YEAR), 
                    date.get(Calendar.HOUR), date.get(Calendar.MINUTE), date.get(Calendar.SECOND),
                    id.getMetricName(), id.getStation(), id.getChannel()
        ));
**/

    // We need at least metadata to compute a digest. If it doesn't exist, then maybe this is a rotated
    // channel (e.g., "00-LHND") and we need to first try to make the metadata + data for it.
        if (!metadata.hasChannels(channelArray)) { 
            checkForRotatedChannels(channelArray);
        }

    // Check again for metadata. If we still don't have it (e.g., we weren't able to rotate) --> return null digest
        if (!metadata.hasChannels(channelArray)) { 
            System.out.format("MetricData.valueDigestChanged: We don't have metadata to compute the digest for this channelArray "
                              + " --> return null digest\n");
            return null;
        }

    // At this point we have the metadata but we may still not have any data for this channel(s).
    // Check for data and if it doesn't exist, then return a null digest, EXCEPT if this is the
    // AvailabilityMetric that is requesting the digest (in which case return a digest for the metadata alone)

        Boolean availabilityMetric = false;
        if (id.getMetricName().contains("AvailabilityMetric") ) {
            availabilityMetric = true;
        }

        if (!hasChannelArrayData(channelArray) && !availabilityMetric) {  // Return null digest so Metric will be skipped
            System.out.format("== valueDigestChanged: <Metric=%s> We do NOT have data for this channel(s) "
                               + "--> return null digest\n", id.getMetricName());
            return null;
        }

        ByteBuffer newDigest = getHash(channelArray);
        if (newDigest == null) {
            logger.warning("New digest is null!");
        }

        if (metricReader == null) { // This could be the case if we are computing AvailabilityMetric and there is no data
            return newDigest;
        }

        if (metricReader.isConnected()) {   // Retrieve old Digest from Database and compare to new Digest
            //System.out.println("=== MetricData.metricReader *IS* connected");
            ByteBuffer oldDigest = metricReader.getMetricValueDigest(id);
            if (oldDigest == null) {
                logger.fine("Old digest is null.");
            }
            else if (newDigest.compareTo(oldDigest) == 0) {
                logger.fine("Digests are Equal !!");
                if (forceUpdate) {  // Don't do anything --> return the digest to force the metric computation
                    String msg = String.format("== valueDigestChanged: metricName=%s Digests are Equal BUT forceUpdate=[%s]"
                    + " so compute the metric anyway!\n", metricName, forceUpdate);
                    System.out.println(msg);
                    logger.warning(msg);
                }
                else {
                    newDigest = null;
                }
            }
            logger.fine(String.format( "valueDigestChanged() --> oldDigest = getMetricValueDigest(%s, %s, %s, %s)",
                                       EpochData.epochToDateString(date), metricName, station, channelId));
        }
        else {
            //System.out.println("=== MetricData.metricReader *IS NOT* connected");
        }

        return newDigest;
    }


/**
 *  getHash - Return the multi-buffer hash for a specified channel Array (data + metadata digest)
 */
    public ByteBuffer getHash(Channel channel)
    {
        ChannelArray channelArray = new ChannelArray(channel.getLocation(), channel.getChannel());
        return getHash(channelArray);
    }

    private ByteBuffer getHash(ChannelArray channelArray)
    {
        ArrayList<ByteBuffer> digests = new ArrayList<ByteBuffer>();

        ArrayList<Channel> channels = channelArray.getChannels();
        for (Channel channel : channels){
            ChannelMeta chanMeta  = getMetaData().getChanMeta(channel);
            if (chanMeta == null){
                System.out.format("MetricData.getHash() Error: metadata not found for requested channel:%s\n",channel);
                return null;
            }
            else {
                digests.add(chanMeta.getDigestBytes());
            }

            if (!hasChannelData(channel))  {
                   // Go ahead and pass back the digests for the metadata alone
                   // The only Metric that should get to here is the AvailabilityMetric
            }
            else { // Add in the data digests
                ArrayList<DataSet>datasets = getChannelData(channel);
                if (datasets == null){
                    System.out.format("MetricData.getHash() Error: Data not found for requested channel:%s\n",channel);
                    return null;
                }
                else {
                    for (int i=0; i<datasets.size(); i++) {
                        //digests.add(datasets.get(0).getDigestBytes());
                        digests.add(datasets.get(i).getDigestBytes());
                    }
                }
            }
        }

        return MemberDigest.multiBuffer(digests);
    }

/**
 *  We've been handed a channelArray for which valueDigestChanged() was unable to find metadata.
 *  We want to go through the channels and see if any are rotated-derived channels (e.g., "00-LHND").  
 *  If so, then try to create the rotated channel data + metadata
 */
    public void checkForRotatedChannels(ChannelArray channelArray)
    {
        ArrayList<Channel> channels = channelArray.getChannels();
        for (Channel channel : channels){
            //System.out.format("== checkForRotatedChannels: request channel=%s\n", channel);

        // channelPrefix = channel band + instrument code  e.g., 'L' + 'H' = "LH"
            String channelPrefix = null;
            if (channel.getChannel().contains("ND") ) {
                channelPrefix = channel.getChannel().replace("ND","");
            }
            else if (channel.getChannel().contains("ED") ) {
                channelPrefix = channel.getChannel().replace("ED","");
            }
            else {
                //System.out.format("== MetricData.checkForRotatedChannels: Request for UNKNOWN channel=%s\n", channel);
                return;
            }

        // Check here since each derived channel (e.g., "00-LHND") will cause us to generate
        //  Rotated channel *pairs* ("00-LHND" AND "00-LHED") so we don't need to repeat it
            if (!metadata.hasChannel(channel)) { 
                metadata.addRotatedChannelMeta(channel.getLocation(), channelPrefix);
            }
        // MTH: Only try to add rotated channel data if we were successful in adding the rotated channel
        //      metadata above since createRotatedChannelData requires it
            if (!hasChannelData(channel) && metadata.hasChannel(channel) ) { 
                createRotatedChannelData(channel.getLocation(), channelPrefix);
            }
        }
    }


}
