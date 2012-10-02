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

import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.seedsplitter.DataSet;

import freq.Cmplx;
import timeutils.Timeseries;

import java.io.IOException;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.logging.Logger;

public abstract class Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.Metric");

    private Hashtable<String, String> arguments;
    private Hashtable<CrossPowerKey, CrossPower> crossPowers;

    protected MetricData   metricData   = null;
    protected MetricResult metricResult = null;

    public Metric()
    {
        arguments = new Hashtable<String, String>();
        crossPowers = new Hashtable<CrossPowerKey, CrossPower>();
    }

    protected CrossPower getCrossPower(Channel channelA, Channel channelB)
    {
        CrossPowerKey key = new CrossPowerKey(channelA, channelB);
        CrossPower crossPower;
        if (crossPowers.containsKey(key)) {
            crossPower = crossPowers.get(key);
        }
        else {
            double[] psd = null;
            double[] df  = new double[1];            // Dummy array to get params out of computePSD()
            for (int i=0; i<df.length; i++) df[i]=0;
            try {
                psd = computePSD(channelA, channelB, df);
            }
            catch (NullPointerException e) {
            }
            crossPower = new CrossPower(psd, df[0]);
            crossPowers.put(key, crossPower);
        }
        return crossPower;
    }

    public void setData(MetricData metricData)
    {
        this.metricData = metricData;
    }

    public MetricResult getResult()
    {
        return metricResult;
    }

    public abstract long getVersion();
    public abstract String getName();
    public abstract void process();

// Dynamic argumemnt managment
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
    }

    public final String get(String name)
    throws NoSuchFieldException
    {
        if (!arguments.containsKey(name)) {
            throw new NoSuchFieldException("Argument '" +name+ "' is not recognized.");
        }
        String metricResult = arguments.get(name);
        if ((metricResult == null) || (metricResult.equals(""))) {
            metricResult = null;
        }
        return metricResult;
    }

    public final Enumeration<String> names()
    {
        return arguments.keys();
    }


 // Compute/Get the psd[f] using Peterson's algorithm (24 hrs, 13 segments with 75% overlap, etc.)

 // TODO: Convert this to handle 2 channels to give crosspectrum when the aren't equal

    private final double[] computePSD(Channel channelX, Channel channelY, double[] params) {

        int ndata      = 0;
        double srate   = 0;  // srate = sample frequency, e.g., 20Hz
        int[] intArray = null;
        String dataHashString = null;

        Channel channel = channelX;

   // Grab station metadata for all channels for this day:
        StationMeta stnMeta        = metricData.getMetaData();
        ChannelMeta chanMeta       = stnMeta.getChanMeta(channel);
        ArrayList<DataSet>datasets = metricData.getChannelData(channel);

        for (DataSet dataset : datasets) {
            ndata     += dataset.getLength();
            intArray   = dataset.getSeries(); // Need to handle multiple datasets (gaps) !
            srate      = dataset.getSampleRate();
            dataHashString = dataset.getDigestString();
        } // end for each dataset

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
        while (nfft < nseg_pnts) nfft = (nfft << 1);

     // We are going to do an nfft point FFT which will return 
     //   nfft/2+1 +ve frequencies (including  DC + Nyq)
        int nf=nfft/2 + 1;

        if (srate == 0) throw new RuntimeException("Error: Got srate=0");
        double dt = 1./srate;
        double df = 1./(nfft*dt);

        params[0] = df;

        double[] xseg = new double[nseg_pnts];
        Cmplx[]  xfft = null;
        double[] psd  = new double[nf];
        double   wss  = 0.;

        int iwin=0;
        int ifst=0;
        int ilst=nseg_pnts-1;
        int offset = 0;

        while (ilst < ndata) // ndata needs to come from largest dataset
        {
           for(int k=0; k<nseg_pnts; k++) {     // Load current window
            xseg[k]=(double)intArray[k+offset]; 
           }
           //Timeseries.timeout(xseg,"xseg");
           Timeseries.detrend(xseg);
           //Timeseries.timeout(xseg,"xseg.detrend");
           Timeseries.debias(xseg);
           //Timeseries.timeout(xseg,"xseg.debias");

           wss = Timeseries.costaper(xseg,.10);
           //Timeseries.timeout(xseg,"xseg.taper");

        // fft2 returns just the (nf = nfft/2 + 1) positive frequencies
           xfft = Cmplx.fft2(xseg);

        // Load up the 1-sided PSD:
           for(int k = 0; k < nf; k++){
               psd[k]= psd[k] +  Math.pow(xfft[k].mag(),2);
           }

           iwin ++;
           offset += noff;
           ilst   += noff;
           ifst   += noff;
        } //end while
        int nwin = iwin;    // Should have nwin = 13


     // Divide the summed psd[]'s by the number of windows (=13) AND
     //   Normalize the PSD ala Bendat & Piersol, to units of (time series)^2 / Hz AND
     //   At same time, correct for loss of power in window due to 10% cosine taper

        double psdNormalization = 2.0 * dt / (double)nfft;
        double windowCorrection = wss / (double)nseg_pnts;  // =.875 for 10% cosine taper
        psdNormalization = psdNormalization / windowCorrection;
        psdNormalization = psdNormalization / (double)nwin; 

        double[] freq = new double[nf];

        for(int k = 0; k < nf; k++){
            psd[k]  = psd[k]*psdNormalization;
            freq[k] = (double)k * df;
        }

     // Get the instrument response for Acceleration and remove it from the PSD
        Cmplx[]  instrumentResponse = chanMeta.getResponse(freq, 3);
        double[] responseMag        = new double[nf];

     // We're computing the squared magnitude as we did with the FFT above
     //   Start from k=1 to skip DC (k=0) where the response=0
        psd[0]=0; 
        for(int k = 1; k < nf; k++){
            responseMag[k]  = Math.pow(instrumentResponse[k].mag(),2);
            if (responseMag[k] == 0) {
                throw new RuntimeException("NLNMDeviation Error: responseMag[k]=0 --> divide by zero!");
            }
            else {   // Divide out (squared)instrument response 
                //psd[k] = 10*Math.log10(psd[k]/responseMag[k]);
                psd[k] = psd[k]/responseMag[k];
            }
        }

        return psd;

    } // end computePSD


}
