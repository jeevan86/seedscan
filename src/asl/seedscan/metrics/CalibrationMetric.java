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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import java.nio.ByteBuffer;
import asl.util.Hex;

import asl.metadata.Channel;
import asl.metadata.EpochData;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.ResponseStage;
import asl.seedsplitter.DataSet;
import asl.util.PlotMaker;

import seed.Blockette320;

import freq.Cmplx;
import timeutils.PSD;
import timeutils.Timeseries;


public class CalibrationMetric
extends Metric
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.CalibrationMetric.class);

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getName()
    {
        return "CalibrationMetric";
    }

    public CalibrationMetric() {
        super();
        addArgument("instrument-calibration-file");
    }

    public void process()
    {
        System.out.format("\n              [ == Metric %s == ]    [== Station %s ==]    [== Day %s ==]\n", 
                          getName(), getStation(), getDay() );

        if (metricData.hasCalibrationData() == false) {
            logger.info("No Calibration loaded for station=[{}] day=[{}] --> Skip Metric", getStation(), getDay());
            return;
        }

    // Get all BH? channels for this stationMeta:
        List<Channel> channels = stationMeta.getChannelArray("BH");

        for (Channel channel : channels){

            if ( !(channel.getChannel().equals("BHZ")) && stationMeta.getChanMeta(channel).getInstrumentType().contains("STS-2")) {
            // Skip STS-2/STS-2.5 Horizontal Channels
                logger.info("InstrumentType = STS-2/2.5 --> Skip horizontal channel={}", channel);
                continue;
            }

            ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel));

        // At this point we KNOW we have metadata so we WILL compute a digest.  If the digest is null
        //  then nothing has changed and we don't need to recompute the metric
            if (digest == null) { 
                logger.info("Data and metadata have NOT changed for channel=[" + channel + "] --> Skip Metric");
                continue;
            }

        // We're computing 2 results (amp + phase diff) but we don't actually have a way yet to load
        // 2 responses for a single metric (= single channel + powerband, etc.) into the database
            double[] results = computeMetric(channel);

            //if (result == NO_RESULT) {
            if (results == null) {
                // Do nothing --> skip to next channel
            }
            else {
                metricResult.addResult(channel, results[0], digest);
                //for (int i=0; i<results.length; i++) {
                    //metricResult.addResult(channel, results[i], digest);
                //}
            }

        }// end foreach channel
    } // end process()

    private double[] computeMetric(Channel channel) {

        double[] result = new double[2];

        if (!metricData.hasChannelData(channel)) {
            return null;
            //return NO_RESULT;
        }

        List<Blockette320> calBlocks = metricData.getChannelCalData(channel);

        if (calBlocks == null) {
            logger.info("No cal blocks found for [{}/{}/{}] --> Skip Metric",getStation(),channel,getDay());
            return null;
            //return NO_RESULT;
        }

        if (calBlocks.size() > 1) {
            logger.error("Found more than 1 calibration blockette! --> What to do ?");
        }

        Blockette320 blockette320 = calBlocks.get(0);
        //blockette320.print();
        long calStartEpoch      = blockette320.getCalibrationEpoch();   // Epoch millisecs
        long calDuration        = blockette320.getCalibrationDuration();// Duration in millisecs
        long calEndEpoch        = calStartEpoch + calDuration;
        String channelExtension = blockette320.getCalInputChannel();  // e.g., "BC0" or "BC1"

        List<DataSet> data = metricData.getChannelData(channel);
        long dataStartEpoch     = data.get(0).getStartTime() / 1000;  // Convert microsecs --> millisecs
        long dataEndEpoch       = data.get(0).getEndTime()   / 1000;  // ...
        double srate            = data.get(0).getSampleRate();

        logger.info("channel=[{}] calChannel=[{}] calStartDate=[{}] calDuration=[{}] sec", channel, channelExtension, 
                           EpochData.epochToTimeString(blockette320.getCalibrationCalendar()), calDuration/1000);
        logger.info(blockette320.toString());

        if ( blockette320.getCalibrationCalendar().get(Calendar.HOUR) == 0 ){
            // This appears to be the 2nd half of a cal that began on the previous day --> Skip
            logger.warn("** cal appears to be the 2nd half of a cal from previous day --> Skip");
            return null;
        }

        if ( calEndEpoch > dataEndEpoch ) {
            // Look for cal to span into next day

            logger.info("channel=[{}] calEndEpoch > dataEndEpoch --> Cal appears to span day", channel); 

            calBlocks = metricData.getNextMetricData().getChannelCalData(channel);

            if (calBlocks == null) {
                logger.warn("No DAY 2 cal blocks found for channel=[{}]", channel); 
            }
            else {
                logger.info("Found matching blockette320 on 2nd day for channel=[{}]", channel); 
                blockette320 = calBlocks.get(0);
                long nextCalStartEpoch      = blockette320.getCalibrationEpoch();
                long nextCalDuration        = blockette320.getCalibrationDuration();
                String nextChannelExtension = blockette320.getCalInputChannel();  // e.g., "BC0"
            // Compare millisecs between end of previous cal and start of this cal
                if ( Math.abs(nextCalStartEpoch - calEndEpoch) < 1800000 ) { // They are within 1800 (?) secs of each other
                    boolean calSpansNextDay = true;
                    calDuration += nextCalDuration; 
                }
                logger.info("channel=[{}] calChannel=[{}] calStartDate=[{}] calDuration=[{}] sec", channel, nextChannelExtension, 
                                EpochData.epochToTimeString(blockette320.getCalibrationCalendar()), nextCalDuration/1000);
                //logger.info(blockette320.toString());
                logger.info("channel=[{}] total calDuration=[{}] sec", channel, calDuration/1000);
            }
    
        }

    // We have the cal startTime and duration --> window both the input (BC?) and output (=channel data) and 
    //    compute the PSD of each
    //    Calibration input channel seed files (e.g., BC0.512.seed) do not have location code so it defaults to "--":

        double[] outData = metricData.getWindowedData(channel, calStartEpoch, calStartEpoch + calDuration);
        double[] inData  = metricData.getWindowedData(new Channel("--",channelExtension), calStartEpoch, calStartEpoch + calDuration);

        if (inData == null || inData.length <= 0) {
            logger.error("We have no data for reported cal input channel=[{}] for station=[{}] --> Skip metric", channelExtension, getStation());
            return null;
        }
        if (outData == null || outData.length <= 0) {
            logger.error("We have no data for reported cal output channel=[{}] for station=[{}] --> Skip metric", channel, getStation());
            return null;
        }

//MTH
/**
        String fileName1 = getStation() + "." + channel + ".sac";
        String fileName2 = getStation() + "." + channelExtension + ".sac";
        Timeseries.writeSacFile(outData, srate, fileName1, getStation(), channel.getChannel());  
        Timeseries.writeSacFile(inData,  srate, fileName2, getStation(), channelExtension);  
**/

     // Compute/Get the 1-sided psd[f] using Peterson's algorithm (24 hrs, 13 segments, etc.)

        double dt = 1.0/srate;
        PSD psdX         = new PSD(inData, inData, dt);
        Cmplx[] Gx       = psdX.getSpectrum();
        double df        = psdX.getDeltaF();
        double[] freq    = psdX.getFreq();
        int nf           = freq.length;

        ChannelMeta chanMeta = stationMeta.getChanMeta(channel);
        ResponseStage stage  = chanMeta.getStage(1);
        double s=0;
        if (stage.getStageType() == 'A') {
            s = 2. * Math.PI;
        }
        else if (stage.getStageType() == 'B') {
            s = 1.;
        }
        else {
            throw new RuntimeException("Error: Unrecognized stage1 type != {'A' || 'B'} --> can't compute!");
        }

        PSD psdXY        = new PSD(inData, outData, dt);
        Cmplx[] Gxy      = psdXY.getSpectrum();
        Cmplx[] Hf       = new Cmplx[Gxy.length];
        double[] calAmp  = new double[Gxy.length];
        double[] calPhs  = new double[Gxy.length];
        Cmplx ic         = new Cmplx(0.0 , 1.0);
        for (int k=0; k<Gxy.length; k++) {
          // Cal coils generate an ACCERLATION but we want the intrument response to VELOCITY:
          // Note that for metadata stage 1 = 'A' [Laplace rad/s] so that    s=i*2pi*f
          //   most II stations have stage1 = 'B' [Analog Hz] and should use s=i*f
            Cmplx iw  = Cmplx.mul(ic , s*freq[k]);
            Hf[k]     = Cmplx.div( Gxy[k], Gx[k] );
            Hf[k]     = Cmplx.mul( Hf[k], iw );
            //calAmp[k] = Hf[k].mag();
            calAmp[k] = 20. * Math.log10( Hf[k].mag() );
            calPhs[k] = Hf[k].phs() * 180./Math.PI;
        }

        Cmplx[] instResponse = chanMeta.getPoleZeroResponse(freq);
        double[] ampResponse = new double[nf];
        double[] phsResponse = new double[nf];
        for (int k=0; k<nf; k++) {
            ampResponse[k] = 20. * Math.log10( instResponse[k].mag() );
            phsResponse[k] = instResponse[k].phs() * 180./Math.PI;
        }

        String sensorType = chanMeta.getInstrumentType();
        double Tmin = 60;
        double Tmax = 400;
        double Tnorm= 80;

        if (sensorType.contains("STS-1")) {
            logger.info("This is an STS-1 Seismometer");
            Tmin = 10;
            Tmax = 800;
            Tnorm = 11;
        }
        else if (sensorType.contains("KS-54000")) {
            logger.info("This is a KS-54000 Seismometer");
            Tmin = 20;
            Tmax = 800;
            Tnorm = 251;
        }

        logger.info("InstrumentType=[{}] Tmin={} Tmax={} Tnorm={}", sensorType, Tmin, Tmax, Tnorm);

        double Fmin = 1.0/Tmin;
        double Fmax = 1.0/Tmin;
        double Fnorm= 1.0/Tnorm;

        int iMin  = (int)(Fmin/df);
        int iMax  = (int)(Fmax/df);
        int iNorm = (int)(Fnorm/df);

        double midAmp   = ampResponse[iNorm];
        double magDiff  = calAmp[iNorm] - midAmp;
        double phsDiff  = phsResponse[iNorm] - calPhs[iNorm];

        // Scale calAmp to = ampResponse at the mid-band frequency
        for (int k=0; k<nf; k++) {
            calAmp[k] -= magDiff;  // subtract offset from the decibel spectrum
            phsDiff  = phsResponse[k] - calPhs[k];
            if (phsDiff > 130) {   // This is just a guess ...
                calPhs[k] += 180.;
            } 
            else if (phsDiff < -130) {
                calPhs[k] -= 180.;
            } 
        }

    // Compute average mag/phase difference within the band Tmin to Tmax:
        double avgMagDiff=0;
        double avgPhsDiff=0;
        int nFreq = 0;
        for (int k=iMin; k<=iMax; k++){
            avgMagDiff += (ampResponse[k] - calAmp[k]);
            avgPhsDiff += (phsResponse[k] - calPhs[k]);
            nFreq++;
        }
        avgMagDiff /= (double)nFreq;
        avgPhsDiff /= (double)nFreq;

        // Get cornerFreq = Freq where ampResponse falls by -3dB below midAmp

        double cornerFreq = 0.;
        for (int k=iNorm; k>=0; k--) {
            if (Math.abs(midAmp - ampResponse[k]) >= 3) {
                cornerFreq = freq[k];
                break;
            }
        }

        logger.info("station={} channel={} avgMagDiff={} avgPhsDiff={} cornerFreq={}", getStation(), channel, avgMagDiff, avgPhsDiff, cornerFreq);

        if (cornerFreq <= 0.) {
            logger.warn("Corner freq == 0 --> There is likely a problem with this Cal!");
            //throw new RuntimeException("CalibrationMetric: Error - cornerFreq == 0!");
        }

        if (getMakePlots()){
            PlotMaker plotMaker = new PlotMaker(metricResult.getStation(), channel, metricResult.getDate());
            plotMaker.plotSpecAmp2(freq, ampResponse, phsResponse, calAmp, calPhs, "calib");
        }

    // diff = average absolute diff (in dB) between calAmp and ampResponse in (per) octave containing Tc: Ts < Tc < Tl
    // phaseDiff = average absolute diff (in deg) between calPhs and phsResponse over all periods from Nyq to Tl
        result[0]=avgMagDiff;
        result[1]=avgPhsDiff;

        return result;

    } // end computeMetric()

}
