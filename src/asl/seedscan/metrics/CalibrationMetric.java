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
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;

import java.nio.ByteBuffer;
import asl.util.Hex;

import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.seedsplitter.*;
import asl.util.PlotMaker;

import seed.Blockette320;

import freq.Cmplx;
import timeutils.PSD;
import timeutils.Timeseries;


public class CalibrationMetric
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.CalibrationMetric");

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getName()
    {
        return "CalibrationMetric";
    }


    public void process()
    {
        System.out.format("\n              [ == Metric %s == ]    [== Station %s ==]    [== Day %s ==]\n", 
                          getName(), getStation(), getDay() );


        if (metricData.hasCalibrationData() == false) {
            System.out.format("== No Calibrations loaded for this station for this day --> return\n");
            return;
        }

    // Get all BH? channels for this stationMeta:
        ArrayList<Channel> channels = stationMeta.getChannelArray("BH");

        for (Channel channel : channels){

            ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel));

        // At this point we KNOW we have metadata so we WILL compute a digest.  If the digest is null
        //  then nothing has changed and we don't need to recompute the metric
            if (digest == null) { 
                System.out.format("%s INFO: Data and metadata have NOT changed for this channel:%s --> Skipping\n"
                                ,getName(), channel);
                continue;
            }

            double result = computeMetric(channel);

            if (result == NO_RESULT) {
                // Do nothing --> skip to next channel
            }
            else {
                metricResult.addResult(channel, result, digest);
            }

        }// end foreach channel
    } // end process()

    private double computeMetric(Channel channel) {

        if (!metricData.hasChannelData(channel)) {
            return NO_RESULT;
        }

        ArrayList<Blockette320> calBlocks = metricData.getChannelCalData(channel);

        if (calBlocks == null) {
            System.out.format("== %s: No cal blocks found for channel=[%s]\n", getName(), channel);
            return NO_RESULT;
        }

        if (calBlocks.size() > 1) {
            System.out.format("== %s: Found n=%d Random Calibration Blockettes! --> What to do ?\n", getName(), calBlocks.size() );
        }

        Blockette320 blockette320 = calBlocks.get(0);
        blockette320.print();
        long calStartEpoch      = blockette320.getCalibrationEpoch();   // Epoch millisecs
        long calDuration        = blockette320.getCalibrationDuration();// Duration in millisecs
        long calEndEpoch        = calStartEpoch + calDuration;
        String channelExtension = blockette320.getCalInputChannel();  // e.g., "BC0" or "BC1"

        ArrayList<DataSet> data = metricData.getChannelData(channel);
        long dataStartEpoch     = data.get(0).getStartTime() / 1000;  // Convert microsecs --> millisecs
        long dataEndEpoch       = data.get(0).getEndTime()   / 1000;  // ...
        double srate            = data.get(0).getSampleRate();

        System.out.format("== %s: channel=[%s] calStartTime=[%s] calDuration=[%d]\n", getName(), channel, 
                           EpochData.epochToDateString(blockette320.getCalibrationCalendar()), calDuration/1000);

        if ( blockette320.getCalibrationCalendar().get(Calendar.HOUR) == 0 ){
            // This appears to be the 2nd half of a cal that began on the previous day --> Skip
            System.out.format("== %s: cal appears to be the 2nd half of a cal from previous day --> Skip\n", getName());
            return NO_RESULT;
        }

        if ( calEndEpoch > dataEndEpoch ) {
            // Look for cal to span into next day

            System.out.format("== %s: channel=[%s] calEndEpoch > dataEndEpoch --> Cal appears to span day\n", getName(), channel);

            //calBlocks = nextMetricData.getChannelCalData(channel);
            calBlocks = metricData.getNextMetricData().getChannelCalData(channel);

            if (calBlocks == null) {
                System.out.format("== %s: No DAY 2 cal blocks found for channel=[%s]\n", getName(), channel);
            }
            else {
                System.out.format("== %s: channel=[%s] Found matching blockette320 on 2nd day:\n", getName(), channel);
                blockette320 = calBlocks.get(0);
                blockette320.print();
                long nextCalStartEpoch      = blockette320.getCalibrationEpoch();
                long nextCalDuration        = blockette320.getCalibrationDuration();
                String nextChannelExtension = blockette320.getCalInputChannel();  // e.g., "BC0"
            // Compare millisecs between end of previous cal and start of this cal
                if ( Math.abs(nextCalStartEpoch - calEndEpoch) < 1800000 ) { // They are within 1800 (?) secs of each other
                    Boolean calSpansNextDay = true;
                    calDuration += nextCalDuration; 
                }
            }
    
        }

    // We have the cal startTime and duration --> window both the input (BC?) and output (=channel data) and 
    //    compute the PSD of each
    //    Calibration input channel seed files (e.g., BC0.512.seed) do not have location code so it defaults to "--":

        double[] outData = metricData.getWindowedData(channel, calStartEpoch, calStartEpoch + calDuration);
        double[] inData  = metricData.getWindowedData(new Channel("--",channelExtension), calStartEpoch, calStartEpoch + calDuration);
        //Timeseries.writeSacFile(outData, srate, "dataOut", getStation(), channel.getChannel());  
        //Timeseries.writeSacFile(inData,  srate, "dataIn",  getStation(), channelExtension);  

     // Compute/Get the 1-sided psd[f] using Peterson's algorithm (24 hrs, 13 segments, etc.)

        double[] params = new double[1];
        double[] Gxx   = getSpectrum(inData, srate, params);
        double[] Gyy   = getSpectrum(outData, srate, params);
        //double[] Gxy   = getSpectrum(inData, outData);

        int nf        = Gxx.length;
        double[] freq = new double[nf];
        double[] freqResponse = new double[nf];
        double df     = params[0];
        //System.out.format("== %s: df=%f\n", getName(), df);
        //Timeseries.writeSacFile(Gxx, df, "Gxx", getStation(), channelExtension);  
        //Timeseries.writeSacFile(Gyy, df, "Gyy", getStation(), channel.getChannel());  

        double[] ampCalibration = new double[nf];
        double[] phsCalibration = new double[nf];
        for (int k=0; k<nf; k++) {
            freq[k] = (double)k * df;
            freqResponse[k] = Gyy[k] / Gxx[k];
            //freqResponse[k] = Math.sqrt(freqResponse[k]);
            freqResponse[k] = Math.sqrt(freqResponse[k]) * 2. * Math.PI * freq[k];
            ampCalibration[k] = freqResponse[k];
            phsCalibration[k] = 0.;
        }
Timeseries.writeSacFile(freqResponse, df, "Hf", getStation(), channel.getChannel());  

        ChannelMeta chanMeta = stationMeta.getChanMeta(channel);
        Cmplx[] instResponse = chanMeta.getPoleZeroResponse(freq);
        double[] ampResponse = new double[nf];
        double[] phsResponse = new double[nf];
        for (int k=0; k<nf; k++) {
            ampResponse[k] = instResponse[k].mag();
            phsResponse[k] = instResponse[k].phs() * 180./Math.PI;
        }
Timeseries.writeSacFile(ampResponse, df, "If", getStation(), channel.getChannel());  

        System.out.format("== %s: [%d] windowed points outData\n", getName(), outData.length);
chanMeta.print();
//chanMeta = stationMeta.getChanMeta( new Channel("--", "BC0") );
//chanMeta.print();

        PSD psdX         = new PSD(inData, inData, srate);
        Cmplx[] Gx       = psdX.getSpectrum();
        PSD psdXY        = new PSD(inData, outData, srate);
        Cmplx[] Gxy      = psdXY.getSpectrum();
        Cmplx[] Hf       = new Cmplx[Gxy.length];
        double[] calAmp  = new double[Gxy.length];
        double[] calPhs  = new double[Gxy.length];
        for (int k=0; k<Gxy.length; k++) {
            Hf[k]     = Cmplx.div( Gxy[k], Gx[k] );
            calAmp[k] = Hf[k].mag();
            calPhs[k] = Hf[k].phs() * 180./Math.PI;
        }
Timeseries.writeSacFile(calAmp, df, "calAmp", getStation(), channel.getChannel());  
Timeseries.writeSacFile(calPhs, df, "calPhs", getStation(), channel.getChannel());  

System.out.format("== Computation complete --> now plot\n");

        if (getMakePlots()){
            PlotMaker plotMaker = new PlotMaker(metricResult.getStation(), channel, metricResult.getDate());
            plotMaker.plotSpecAmp2(freq, ampResponse, phsResponse, calAmp, calPhs, "CalibrationMetric");
            //plotMaker.plotSpecAmp2(freq, ampResponse, phsResponse, ampCalibration, phsCalibration, "CalibrationMetric");
            //plotMaker.plotSpecAmp(freq, ampResponse, phsResponse, "CalibrationMetric");
        }

System.exit(0);

        return 0.0;

    } // end computeMetric()


    public double[] getSpectrum( double[] data, double srate, double[] params) {

        if (data == null || data.length <= 0 || srate == 0) {
        } 

        int ndata = data.length; 

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
        double[] yseg = new double[nseg_pnts];

        Cmplx[]  xfft = null;
        Cmplx[]  yfft = null;
        double[] psd  = new double[nf];
        Cmplx[]  psdC = new Cmplx[nf];
        double   wss  = 0.;

        int iwin=0;
        int ifst=0;
        int ilst=nseg_pnts-1;
        int offset = 0;

// Initialize the Cmplx array
       for(int k = 0; k < nf; k++){
            psdC[k] = new Cmplx(0., 0.);
        }

        while (ilst < ndata) // ndata needs to come from largest dataset
        {
           for(int k=0; k<nseg_pnts; k++) {     // Load current window
            xseg[k] = (double)data[k+offset]; 
            yseg[k] = (double)data[k+offset]; 
           }
           //Timeseries.timeout(xseg,"xseg");
           Timeseries.detrend(xseg);
           Timeseries.detrend(yseg);
           Timeseries.debias(xseg);
           Timeseries.debias(yseg);

           wss = Timeseries.costaper(xseg,.10);
           wss = Timeseries.costaper(yseg,.10);
// MTH: Maybe want to assert here that wss > 0 to avoid divide-by-zero below ??

        // fft2 returns just the (nf = nfft/2 + 1) positive frequencies
           xfft = Cmplx.fft2(xseg);
           yfft = Cmplx.fft2(yseg);

        // Load up the 1-sided PSD:
           for(int k = 0; k < nf; k++){
            // when X=Y, X*Y.conjg is Real and (X*Y.conjg).mag() simply returns the Real part as a double 
                psd[k] = psd[k] + Cmplx.mul(xfft[k], yfft[k].conjg()).mag() ;
                psdC[k]= Cmplx.add(psdC[k], Cmplx.mul(xfft[k], yfft[k].conjg()) );
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
            psdC[k]  = Cmplx.mul(psdC[k], psdNormalization);
            freq[k] = (double)k * df;
        }

/**
     // We still have psd[f] so this is a good point to do any smoothing over neighboring frequencies:
        int nsmooth = 11;
        int nhalf   = 5;
        int nw = nf - nsmooth;
        double[] psdFsmooth = new double[nf];
        Cmplx[] psdCFsmooth = new Cmplx[nf];

        int iw=0;

        for (iw = 0; iw < nhalf; iw++) {
            psdFsmooth[iw] = psd[iw];
            psdCFsmooth[iw]= psdC[iw];
        }

        // iw is really icenter of nsmooth point window
        for (; iw < nf - nhalf; iw++) {
            int k1 = iw - nhalf;
            int k2 = iw + nhalf;

            double sum = 0;
            Cmplx sumC = new Cmplx(0., 0.);
            for (int k = k1; k < k2; k++) {
                sum  = sum + psd[k];
                sumC = Cmplx.add(sumC, psdC[k]);
            }
            psdFsmooth[iw] = sum / (double)nsmooth;
            psdCFsmooth[iw]= Cmplx.div(sumC, (double)nsmooth);
        }

     // Copy the remaining point into the smoothed array
        for (; iw < nf; iw++) {
            psdFsmooth[iw] = psd[iw];
            psdCFsmooth[iw]= psdC[iw];
        }

     // Copy Frequency smoothed spectrum back into psd[f] and proceed as before
        for ( int k = 0; k < nf; k++){
            //psd[k]  = psdFsmooth[k];
            psd[k]  = psdCFsmooth[k].mag();
            //psd[k]  = psdC[k].mag();
        }
        psd[0]=0; // Reset DC

**/

        return psd;

    } // end computePSD


}
