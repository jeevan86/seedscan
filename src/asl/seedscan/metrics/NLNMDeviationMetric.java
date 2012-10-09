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

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.seedsplitter.DataSet;

import timeutils.Timeseries;

public class NLNMDeviationMetric
extends PowerBandMetric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.NLNMDeviationMetric");

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getBaseName()
    {
        return "NLNMDeviationMetric";
    }

    private double[] NLNMPeriods;
    private double[] NLNMPowers;

    public void process()
    {
        System.out.format("\n              [ == Metric %s == ]\n", getName() ); 

   // Grab station metadata for all channels for this day:
        StationMeta stnMeta = metricData.getMetaData();

   // Create a 3-channel array to use for loop
        ChannelArray channelArray = new ChannelArray("00","LHZ", "LH1", "LH2");
        //ChannelArray channelArray = new ChannelArray("00","LHZ", "LH1", "LHZ");
        //ChannelArray channelArray = new ChannelArray("10","BHZ", "BH1", "BH2");
        //ChannelArray channelArray = new ChannelArray("00","BHZ", "BH1", "BH2");

        ArrayList<Channel> channels = channelArray.getChannels();

        metricResult = new MetricResult();

   // Read in the NLNM
        readNLNM();

   // Loop over channels, get metadata & data for channel and Calculate Metric

        for (Channel channel : channels){

            ChannelMeta chanMeta = stnMeta.getChanMeta(channel);
            if (chanMeta == null){ // Skip channel, we have no metadata for it
                System.out.format("%s Error: metadata not found for requested channel:%s --> Skipping\n"
                                  ,getName(), channel.getChannel());
                continue;
            }

            ArrayList<DataSet>datasets = metricData.getChannelData(channel);
            String dataHashString = null;

            if (datasets == null){ // Skip channel, we have no data for it
                System.out.format("%s Error: No data for requested channel:%s --> Skipping\n"
                                  ,getName(), channel.getChannel());
                continue;
            }
         // Temp hack to get data hash:
            else {
                dataHashString = datasets.get(0).getDigestString();
            }

            if (!metricData.hashChanged(channel)) { // Skip channel, we don't need to recompute the metric
                System.out.format("%s INFO: Data and metadata have NOT changed for this channel:%s --> Skipping\n"
                                  ,getName(), channel.getChannel());
                continue;
            }

         // If we're here, it means we need to (re)compute the metric for this channel:

         // Compute/Get the 1-sided psd[f] using Peterson's algorithm (24 hrs, 13 segments, etc.)

            CrossPower crossPower = getCrossPower(channel, channel);
            double[] psd  = crossPower.getSpectrum();
            double df     = crossPower.getSpectrumDeltaF();

         // nf = number of positive frequencies + DC (nf = nfft/2 + 1, [f: 0, df, 2df, ...,nfft/2*df] )
            int nf        = psd.length;
            double freq[] = new double[nf];

         // Convert the psd to dB and fill freq array

            for ( int k = 0; k < nf; k++){
                freq[k] = (double)k * df;
                psd[k]  = 10 * Math.log10(psd[k]);
            }

         // Convert psd[f] to psd[T]
         // Reverse freq[] --> per[] where per[0]=shortest T and per[nf-2]=longest T:

            double[] per    = new double[nf];
            double[] psdPer = new double[nf];
         // per[nf-1] = 1/freq[0] = 1/0 = inf --> set manually:
            per[nf-1] = 0;  
            for (int k = 0; k < nf-1; k++){
                per[k]     = 1./freq[nf-k-1];
                psdPer[k]  = psd[nf-k-1];
            }
            double Tmin  = per[0];    // Should be = 1/fNyq = 2/fs = 0.1 for fs=20Hz
            double Tmax  = per[nf-2]; // Should be = 1/df = Ndt

         // Average over each octave, starting with shortest period

            double T1 = Tmin;
            double T2 = 2.*T1;
            double Tc = Math.sqrt(T1*T2);
            double powerInOctave;
            int    npersInOctave;

            ArrayList<Double> Tcs    = new ArrayList<Double>();
            ArrayList<Double> Powers = new ArrayList<Double>();

            while (T2 < Tmax) {
                powerInOctave = 0;
                npersInOctave = 0;
                for (int k = 0; k < nf; k++){
                    if ( (per[k] >= T1) && (per[k] <= T2) ) {
                        powerInOctave += psdPer[k];
                        npersInOctave++;
                    }
                }
                powerInOctave = powerInOctave / (double)npersInOctave;
                Tcs.add(Tc);
                Powers.add(powerInOctave);

                T1 *= Math.pow(2.0, .125);
                T2  = 2*T1;
                Tc  = Math.sqrt(T1*T2);
            }
            Double[] octavePowers  = Powers.toArray(new Double[]{});
            Double[] octavePeriods = Tcs.toArray(new Double[]{});

            //Timeseries.timeoutXY(octavePeriods, octavePowers, "psd.00-lhz");
 
         // Interpolate the psd octave-average to the periods of the NLNM Model:
            double[] psdInterp;
            int npers  = octavePeriods.length;
            double[] X = new double[npers];
            double[] Y = new double[npers];
            for (int k = 0; k < npers; k++){
                X[k] = octavePeriods[k];
                Y[k] = octavePowers[k];
            }
            psdInterp = Timeseries.interpolate(X, Y, NLNMPeriods);

            //Timeseries.timeoutXY(NLNMPeriods, psdInterp, "psd.00-bhz.interp");

            PowerBand band    = getPowerBand();
            double lowPeriod  = band.getLow();
            double highPeriod = band.getHigh();

            if (lowPeriod >= highPeriod) {
                StringBuilder message = new StringBuilder();
                message.append(String.format("NLNMDeviation Error: Requested band [%f - %f] has lowPeriod >= highPeriod\n"
                    ,lowPeriod, highPeriod) );
                throw new RuntimeException(message.toString());
            }
        // We should really only compare to NLNM within the range of useable periods/frequencies for this channel
            if (lowPeriod < Tmin) {
                StringBuilder message = new StringBuilder();
                message.append(String.format("NLNMDeviation Error: Requested band [%f - %f] has lowPeriod < Nyquist Tmin=%f\n"
                    ,lowPeriod, highPeriod, Tmin) );
                throw new RuntimeException(message.toString());
            }
            if (highPeriod > Tmax) {
                StringBuilder message = new StringBuilder();
                message.append(String.format("NLNMDeviation Error: Requested band [%f - %f] has highPeriod > Tmax=%f\n"
                    ,lowPeriod, highPeriod, Tmax) );
                throw new RuntimeException(message.toString());
            }

        // Compute deviation from NLNM within the requested period band:
            double deviation = 0;
            int nPeriods = 0;
            for (int k = 0; k < NLNMPeriods.length; k++){
                if (NLNMPeriods[k] >  highPeriod){
                    break;
                }
                else if (NLNMPeriods[k] >= lowPeriod){
                    double difference = psdInterp[k] - NLNMPowers[k];
                    //System.out.format("== NLNMPeriods[k=%d]=%.2f psdInterp[k]=%.2f NLNMPowers[k]=%.2f difference=%.2f\n",
                    //   k, NLNMPeriods[k], psdInterp[k], NLNMPowers[k], difference);
                    deviation += Math.sqrt( Math.pow(difference, 2) );
                    nPeriods++;
                }
            }

            if (nPeriods == 0) {
                StringBuilder message = new StringBuilder();
                message.append(String.format("NLNMDeviation Error: Requested band [%f - %f] contains NO periods within NLNM\n"
                    ,lowPeriod, highPeriod) );
                throw new RuntimeException(message.toString());
            }
            deviation = deviation/(double)nPeriods;

            String key   = getName() + "+Channel(s)=" + channel.getLocation() + "-" + channel.getChannel();
            String value = String.format("%.2f",deviation);
            metricResult.addResult(key, value);

            System.out.format("%s-%s [%s] %s %s-%s ", stnMeta.getStation(), stnMeta.getNetwork(),
              EpochData.epochToDateString(stnMeta.getTimestamp()), getName(), chanMeta.getLocation(), chanMeta.getName() );
            System.out.format("nPeriods:%d deviation=%.2f) %s %s\n", nPeriods, deviation, chanMeta.getDigestString(), dataHashString); 

        }// end foreach channel

    } // end process()


    private void readNLNM() {

   // Read in the NLNM from local file

        String fileName = "./NLNM.ascii";
        String path     = "/Users/mth/mth/Projects/asl/src/asl/seedscan/metrics/";
        fileName = path + fileName;

   // First see if the file exists
        if (!(new File(fileName).exists())) {
            System.out.format("=== %s: NLNM file=%s does NOT exist!\n", getName(), fileName);
            System.exit(0);
        }
   // Temp ArrayList(s) to read in unknown number of (x,y) pairs:
        ArrayList<Double> tmpPers = new ArrayList<Double>();
        ArrayList<Double> tmpPows = new ArrayList<Double>();
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                String[] args = line.trim().split("\\s+") ;
                if (args.length != 2) {
                    String message = "==Error reading NLNM: got " + args.length + " args on one line!";
                    throw new RuntimeException(message);
                }
                tmpPers.add( Double.valueOf(args[0].trim()).doubleValue() );
                tmpPows.add( Double.valueOf(args[1].trim()).doubleValue() );
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
        Double[] modelPeriods  = tmpPers.toArray(new Double[]{});
        Double[] modelPowers   = tmpPows.toArray(new Double[]{});

        NLNMPeriods = new double[modelPeriods.length];
        NLNMPowers  = new double[modelPowers.length];

        for (int i=0; i<modelPeriods.length; i++){
            NLNMPeriods[i] = modelPeriods[i];
            NLNMPowers[i]  = modelPowers[i];
        }

    } // end readNLNM


} // end class

