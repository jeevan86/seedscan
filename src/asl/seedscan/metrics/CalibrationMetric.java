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
import java.util.Set;
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

    private static Hashtable<String, SensorInfo> sensorTable = null;

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
        logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

       	String station = getStation();	// i.e. IU_ANMO
	String day = getDay();		// yyyy:ddd:hh:mm
	
	if (metricData.hasCalibrationData() == false) {
            logger.info("No Calibration loaded for station=[{}] day=[{}] --> Skip Metric", getStation(), getDay());
            return;
        }

    // Get all BH? channels for this stationMeta:
        List<Channel> channels = stationMeta.getChannelArray("BH");

        for (Channel channel : channels){

            if (!metricData.hasChannelData(channel)){
                logger.warn("No data found for channel[{}] --> Skip metric", channel);
                continue;
            }

            if ( !(channel.getChannel().equals("BHZ")) && stationMeta.getChanMeta(channel).getInstrumentType().contains("STS-2")) {
            // Skip STS-2/STS-2.5 Horizontal Channels
                logger.info("InstrumentType = STS-2/2.5 --> Skip horizontal channel={}", channel);
                continue;
            }

            ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel));

            if (digest == null) { // means oldDigest == newDigest and we don't need to recompute the metric 
                logger.warn("Digest unchanged station:[{}] channel:[{}] --> Skip metric", getStation(), channel);
                continue;
            }

            CalibrationResult calResult = computeMetric(channel, station, day);

            if (calResult == null) {
                logger.info("CalResult == NULL for Channel=[{}] + Day=[{}]", channel, getDay());
                // Do nothing --> skip to next channel
            }
            else {
                logger.info("channel=[{}] day=[{}] CalResult Follows", channel, getDay() );
                logger.info(calResult.toString());
                logger.info(calResult.toJSONString());

                // In this special case, we don't care about the (single double) result value 
                // since everything is packed into a JSON String so we'll just directly call 
                double dummyValue = -999.;
/** Temp disable in master branch until James tests the JSON injections
                metricResult.addResult(calResult.toJSONString(), dummyValue, digest);
                //MetricResult.createChannel(calResult.toJSONString());
**/
            }

        }// end foreach channel
    } // end process()

    private CalibrationResult computeMetric(Channel channel, String station, String day) {

        if (!metricData.hasChannelData(channel)) {
            return null;
        }

        List<Blockette320> calBlocks = metricData.getChannelCalData(channel);

        if (calBlocks == null) {
            logger.info("No cal blocks found for [{}/{}/{}] --> Skip Metric", getStation(), channel, getDay());
            return null;
        }

        if (calBlocks.size() > 1) {
            logger.error("Found more than 1 calibration blockette (station=[{}] channel=[{}] day=[{}])! --> What to do ?", station, channel.toString(), day);
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

        //logger.info("channel=[{}] calChannel=[{}] calStartDate=[{}] calDuration=[{}] sec", channel, channelExtension, 
                           //EpochData.epochToTimeString(blockette320.getCalibrationCalendar()), calDuration/1000);
        logger.info(blockette320.toString());

        if ( blockette320.getCalibrationCalendar().get(Calendar.HOUR) == 0 ) {
            // This appears to be the 2nd half of a cal that began on the previous day --> Skip
            logger.warn("** cal appears to be the 2nd half of a cal from previous day --> Skip");
            return null;
        }

        // Cal channels will have (no) location = "" in the miniseed files, but this will
        //   be mapped to the default location code="00"
        Channel calChannel = new Channel("00", channelExtension);

        CalibrationResult calibration = new CalibrationResult(channel, calChannel, 
                EpochData.epochToTimeString(blockette320.getCalibrationCalendar()), calDuration/1000);

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
                //logger.info("channel=[{}] calChannel=[{}] calStartDate=[{}] calDuration=[{}] sec", channel, nextChannelExtension, 
                                //EpochData.epochToTimeString(blockette320.getCalibrationCalendar()), nextCalDuration/1000);
                //logger.info(blockette320.toString());
                //logger.info("channel=[{}] total calDuration=[{}] sec", channel, calDuration/1000);
            }
    
        }

    // We have the cal startTime and duration --> window both the input (BC?) and output (=channel data) and 
    //    compute the PSD of each
    //    Calibration input channel seed files (e.g., BC0.512.seed) do not have location code so it defaults to "--":

        double[] outData = metricData.getWindowedData(channel,    calStartEpoch, calStartEpoch + calDuration);
        double[] inData  = metricData.getWindowedData(calChannel, calStartEpoch, calStartEpoch + calDuration);

        if (inData == null || inData.length <= 0) {
            logger.error("We have no data for reported cal input channel=[{}] for station=[{}] day=[{}] --> Skip metric", channelExtension, station, day);
            return null;
        }
        if (outData == null || outData.length <= 0) {
            logger.error("We have no data for reported cal output channel=[{}] for station=[{}] day=[{}] --> Skip metric", channel, station, day);
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

    // Change of plans: Not clear that Tmin and Tmax are even going to be used (which
    // begs the question if we even needed to use a calibration-file in the first place,
    // but anyway ... I'll leave them in here in case Adam changes his mind :)

        String sensorType = chanMeta.getInstrumentType().toUpperCase();
        double Tmin = 0;
        double Tmax = 0;
        double Tnorm= 0;
        SensorInfo sensorInfo = getSensorInfo(sensorType);
        if (sensorInfo == null) {
            logger.error("Couldn't find instrument calibration info for sensorType=[{}] for station=[{}] day=[{}]", sensorType, station, day);
            logger.warn("Use default Tmin, Tmax, Tnorm");
            Tmin  = 60; 
            Tmax  = 400;
            Tnorm = 80;
        }
        else {
            Tmin  = sensorInfo.getMinPeriod();
            Tmax  = sensorInfo.getMaxPeriod();
            Tnorm = sensorInfo.getNormPeriod();
        }
        //logger.info("InstrumentType=[{}] Tmin={} Tmax={} Tnorm={}", sensorType, Tmin, Tmax, Tnorm);

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

        calibration.setPeriods(Tmin, Tmax, Tnorm);
        calibration.setSensorName(sensorType);

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

        BandAverageDiff bandDiff = new BandAverageDiff();
        //bandDiff.T1 = Tmin;
        //bandDiff.T2 = Tmax;
        bandDiff.T1 = 40;
        bandDiff.T2 = 80;
        bandDiff.ampDiff = avgMagDiff;
        bandDiff.phsDiff = avgPhsDiff;

        calibration.addBand("midBandDiff", bandDiff);

        // Get cornerFreq  from polezero response = Freq where ampResponse falls by -3dB below midAmp
        double cornerFreq = 0.;
        for (int k=iNorm; k>=0; k--) {
            if (Math.abs(midAmp - ampResponse[k]) >= 3) {
                cornerFreq = freq[k];
                break;
            }
        }
        calibration.setCornerFreq(cornerFreq);

        if (cornerFreq <= 0.) {
            logger.warn("Corner freq == 0 --> There is likely a problem with this Cal!");
            //throw new RuntimeException("CalibrationMetric: Error - cornerFreq == 0!");
        }
        else {
         // Compute diffs within an octave bandwidth centered on the corner Period, Tc
            double Tc = 1.0/cornerFreq;
            Tmin = 2./3. * Tc;
            Tmax = 2 * Tmin;
            Fmax  = 1/Tmin;
            Fmin  = 1/Tmax;
            iMax  = (int)(Fmax/df);
            iMin  = (int)(Fmin/df);
            avgMagDiff=0;
            avgPhsDiff=0;
            nFreq = 0;
            for (int k=iMin; k<=iMax; k++){
                avgMagDiff += (ampResponse[k] - calAmp[k]);
                avgPhsDiff += (phsResponse[k] - calPhs[k]);
                nFreq++;
            }
            avgMagDiff /= (double)nFreq;
            avgPhsDiff /= (double)nFreq;

            bandDiff = new BandAverageDiff();
            bandDiff.T1 = Tmin;
            bandDiff.T2 = Tmax;
            bandDiff.ampDiff = avgMagDiff;
            bandDiff.phsDiff = avgPhsDiff;

            calibration.addBand("longTDiff", bandDiff);
        }

        if (getMakePlots()){
            String date = String.format("%04d%03d", metricResult.getDate().get(Calendar.YEAR),
                                                    metricResult.getDate().get(Calendar.DAY_OF_YEAR) );
            final String plotTitle = String.format("[ Date: %s ] [ Station: %s ] [ Channel: %s ] Calibration", 
                                     date, getStation(), channel );
            final String pngName   = String.format("%s.%s.%s.png", getOutputDir(), channel, "calib" );

            PlotMaker plotMaker = new PlotMaker(metricResult.getStation(), channel, metricResult.getDate());
            plotMaker.plotSpecAmp2(freq, ampResponse, phsResponse, calAmp, calPhs, plotTitle, pngName);

        }

        return calibration;

    } // end computeMetric()


    /** 
     * I think the danger with double check synchronization is *not* that
     * the 2nd thread will try to re-create sensorTable, but rather, 
     * that for some earlier Java versions (e.g., ver < 1.5), it's possible
     * that the 2nd thread will see sensorTable != null before this (in getSensor()),
     * so it won't need the lock, and may try to use sensorTable before it is 
     * completely initialized by the 1st thread.
     */
    synchronized private static boolean readSensorTable(String fileName) {
        if (sensorTable != null) {
            return false;
        }

        sensorTable = new Hashtable<String, SensorInfo>();

   // First see if the file exists
        if (!(new File(fileName).exists())) {
            logger.error("readSensorTable: file={} does NOT exist!", fileName);
            return false;
        }

        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new FileReader(fileName));
            int i=0;
            while ((line = br.readLine()) != null) {
                if (i > 1) { // 2-line header
                    String[] args = line.trim().split("\\s+") ;
                    if (args.length != 4) {
                        logger.error("calibration-table=[{}] --> Bad format", fileName);
                    }
                    String sensorName = args[0].toUpperCase(); 
                    double Tmin=0;
                    double Tmax=0;
                    double Tnorm=0;
                    try {
                        Tmin = Double.parseDouble(args[1]);
                        Tmax = Double.parseDouble(args[2]);
                        Tnorm= Double.parseDouble(args[3]);
                    }
                    catch (Exception e) {
                        logger.error("Caught exception:", e.getMessage());
                    }
                    SensorInfo sensorInfo = new SensorInfo(sensorName, Tmin, Tmax, Tnorm);
                    sensorTable.put(sensorName, sensorInfo);
                    //logger.info("add sensorName=[{}] Tmin=[{}]", sensorName, Tmin);
                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return true;
    }

    private SensorInfo getSensorInfo(String sensorType){
        if (sensorTable == null) {
            String fileName = null;
            try {
                fileName = get("instrument-calibration-file");
            }
            catch (Exception e) {
                logger.error("Failed attempt to read instrument-calibration-file from config.xml: ", e.getMessage() );
            }
            readSensorTable(fileName);
        }
        //for (String sensorName : sensorTable.keySet() ){
            //logger.info("SensorTable: Found Sensor:" + sensorTable.get(sensorName).toString() );
        //}

        for (String sensorName : sensorTable.keySet() ){
            if (sensorType.contains(sensorName)) {
                //logger.info("sensorName=[{}] matches sensorType=[{}]", sensorName, sensorType);
                return sensorTable.get(sensorName);
            }
        }
        return null;
    }

    public static class SensorInfo
    {
        private String sensorName=null;
        private double Tmin;
        private double Tmax;
        private double Tnorm;

    // constructor
        public SensorInfo(String sensorName, double Tmin, double Tmax, double Tnorm)
        {
            this.sensorName  = sensorName;
            this.Tmin  = Tmin;
            this.Tmax  = Tmax;
            this.Tnorm = Tnorm;
        }
        public String getSensorName(){
            return sensorName;
        }
        public double getMinPeriod(){
            return Tmin;
        }
        public double getMaxPeriod(){
            return Tmax;
        }
        public double getNormPeriod(){
            return Tnorm;
        }
        public String toString() {
            return String.format("sensorName=[%s] Tmin=[%.2f] Tmax=[%.2f] Tnorm=[%.2f]",
                                  sensorName, Tmin, Tmax, Tnorm );
        }
    }

    public class BandAverageDiff
    {
        double T1;
        double T2;
        double ampDiff;
        double phsDiff;
    }

    public class CalibrationResult
    {
        private Channel channel;
        private Channel calInputChannel;
        private String  calStartDate;
        private long    calDuration;

        String sensorName;
        double Tmin;
        double Tmax;
        double Tnorm;
        double cornerFreq;
        double cornerFreqCal;
        Hashtable<String, BandAverageDiff> bandTable;

        public CalibrationResult(Channel channel, Channel calInputChannel,
                                 String calStartDate, long calDuration) {
            this.channel = channel;
            this.calInputChannel = calInputChannel;
            this.calStartDate = calStartDate;
            this.calDuration = calDuration;
            bandTable = new Hashtable<String, BandAverageDiff>();
        }
        void setSensorName(String sensorName) {
            this.sensorName = sensorName;
        }
        void setPeriods(double Tmin, double Tmax, double Tnorm) {
            this.Tmin  = Tmin;
            this.Tmax  = Tmax;
            this.Tnorm = Tnorm;
        }
        void setCornerFreq(double cornerFreq) {
            this.cornerFreq = cornerFreq;
        }
        void setCornerFreqCal(double cornerFreq) {
            this.cornerFreqCal = cornerFreq;
        }
        void addBand(String name, BandAverageDiff bandDiff) {
            bandTable.put(name, bandDiff);
        }

        public String toJSONString() {
            BandAverageDiff bandDiff = bandTable.get("midBandDiff");
            StringBuilder out = new StringBuilder();
            out.append(String.format("{\"channelId\":\"%s\",\n", channel) );
            out.append(String.format(" \"band\":{\n") );
            out.append(String.format("   \"T1\":%.2f\n", bandDiff.T1) );
            out.append(String.format("   \"T2\":%.2f\n", bandDiff.T2) );
            out.append(String.format("   \"dBDiff\":%.4f\n", bandDiff.ampDiff) );
            out.append(String.format("   \"phDiff\":%.4f\n", bandDiff.phsDiff) );
            out.append(String.format("   }\n") );

            bandDiff = bandTable.get("longTDiff");
            out.append(String.format(" \"band\":{\n") );
            out.append(String.format("   \"T1\":%.2f\n", bandDiff.T1) );
            out.append(String.format("   \"T2\":%.2f\n", bandDiff.T2) );
            out.append(String.format("   \"dBDiff\":%.4f\n", bandDiff.ampDiff) );
            out.append(String.format("   \"phDiff\":%.4f\n", bandDiff.phsDiff) );
            out.append(String.format("   }\n") );

            out.append(String.format("}") );

            return out.toString();
        }

        public String toString() {
            StringBuilder out = new StringBuilder();
            out.append(String.format("\n==CalibrationResult: channel=[%s] calInputChannel=[%s] calStart=[%s] calDuration=[%d]\n"
                                 + "                     SensorName=[%s]\n"
                                 + "                     Tmin=[%.2f] Tmax=[%.2f] Tnorm=[%.2f] corner Freq=[%f] Per=[%f]\n"
                                 + "                                                                Cal corner Per=[%f]\n"
                                    ,channel, calInputChannel, calStartDate, calDuration
                                    ,sensorName
                                    ,Tmin, Tmax, Tnorm, cornerFreq, 1./cornerFreq, 1./cornerFreqCal) );
            for (String band : bandTable.keySet()) {
                BandAverageDiff bandDiff = bandTable.get(band);
                out.append(String.format("                     Band [%s]: T1=%.2f T2=%.2f AmpDiff=[%f] PhsDiff=[%f]\n",
                                          band, bandDiff.T1, bandDiff.T2, bandDiff.ampDiff, bandDiff.phsDiff ) ); 
            }
            return out.toString();
        }
    }

}
