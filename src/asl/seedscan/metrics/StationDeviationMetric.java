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

import org.jfree.chart.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.data.xy.*;
import org.jfree.data.Range;
import org.jfree.util.ShapeUtilities;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Paint;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.nio.ByteBuffer;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.Calendar;

import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.security.MemberDigest;
import asl.seedscan.ArchivePath;
import asl.seedsplitter.DataSet;

import asl.util.Hex;

import timeutils.Timeseries;

public class StationDeviationMetric
extends PowerBandMetric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.StationDeviationMetric");

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getBaseName()
    {
        return "StationDeviationMetric";
    }

    private double[] ModelPeriods;
    private double[] ModelPowers;
    private String   ModelDir;

    public StationDeviationMetric(){
        super();
        addArgument("modelpath");
    }

    public void process()
    {
        System.out.format("\n              [ == Metric %s == ]\n", getName() ); 

   // Get the path to the station models that was read in from config.xml
   //  <cfg:argument cfg:name="modelpath">/Users/mth/mth/Projects/xs0/stationmodel/${NETWORK}_${STATION}/</cfg:argument>
        String pathPattern = null;
        try {
            pathPattern = get("modelpath");
        } catch (NoSuchFieldException ex) {
            System.out.format("Error: Station Model Path ('modelpath') was not specified!\n");
            return; // Without the modelpath we can't compute the metric --> return
        }
        ArchivePath pathEngine = new ArchivePath(new Station(stationMeta.getNetwork(), stationMeta.getStation() ) );
        ModelDir  = pathEngine.makePath(pathPattern);

        ArrayList<Channel> channels = stationMeta.getChannelArray("LH"); // Get all LH channels in metadata

   // Loop over channels, get metadata & data for channel and Calculate Metric

        for (Channel channel : channels){

         // Check to see that we have data + metadata & see if the digest has changed wrt the database:

            ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel));
            //logger.fine(String.format("%s: digest=%s\n", getName(), (digest == null) ? "null" : Hex.byteArrayToHexString(digest.array())));

            if (digest == null) {
                System.out.format("%s INFO: Data and metadata have NOT changed for this channel:%s --> Skipping\n"
                                  ,getName(), channel);
                continue;
            }

            double result = computeMetric(channel);
            if (result == NO_RESULT) {
                // Metric computation failed --> do nothing
            }
            else {
                metricResult.addResult(channel, result, digest);
            }

        }// end foreach channel

    } // end process()


    private double computeMetric(Channel channel) {

    // Read in specific noise model for this station+channel          // ../ANMO.00.LH1.90
        String modelFileName = stationMeta.getStation() + "." + channel.getLocation() + "." + channel.getChannel() + ".90";
        if (!readModel(modelFileName)) {
            System.out.format("%s Error: ModelFile=%s not found for requested channel:%s --> Skipping\n"
                              ,getName(), modelFileName, channel.getChannel());
            return NO_RESULT;
        }

     // Compute/Get the 1-sided psd[f] using Peterson's algorithm (24 hrs, 13 segments, etc.)

        CrossPower crossPower = getCrossPower(channel, channel);
        double[] psd  = crossPower.getSpectrum();
        double df     = crossPower.getSpectrumDeltaF();

     // nf = number of positive frequencies + DC (nf = nfft/2 + 1, [f: 0, df, 2df, ...,nfft/2*df] )
        int nf        = psd.length;
        double freq[] = new double[nf];

     // Fill freq array & Convert spectrum to dB
        for ( int k = 0; k < nf; k++){
            freq[k] = (double)k * df;
            psd[k]  = 10.*Math.log10(psd[k]);
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

        String outFile; // Use for outputting spectra arrays (in testing)

        outFile = channel.toString() + ".psd.Fsmooth.T";
        //outFile = channel.toString() + ".psd.T";
        //Timeseries.timeoutXY(per, psdPer, outFile);

     // Interpolate the smoothed psd to the periods of the Station/Channel Noise Model:
        double psdInterp[] = Timeseries.interpolate(per, psdPer, ModelPeriods);

        outFile = channel.toString() + ".psd.Fsmooth.T.Interp";
        //Timeseries.timeoutXY(NLNMPeriods, psdInterp, outFile);

        PowerBand band    = getPowerBand();
        double lowPeriod  = band.getLow();
        double highPeriod = band.getHigh();

        if (!checkPowerBand(lowPeriod, highPeriod, Tmin, Tmax)){
            System.out.format("%s powerBand Error: Skipping channel:%s\n", getName(), channel);
            return NO_RESULT;
        }

    // Compute deviation from The Model within the requested period band:
        double deviation = 0;
        int nPeriods = 0;
        for (int k = 0; k < ModelPeriods.length; k++){
            if (ModelPeriods[k] >  highPeriod){
                break;
            }
            else if (ModelPeriods[k] >= lowPeriod){
                double difference = psdInterp[k] - ModelPowers[k];
                deviation += Math.sqrt( Math.pow(difference, 2) );
                nPeriods++;
            }
        }

        if (nPeriods == 0) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("%s Error: Requested band [%f - %f] contains NO periods within station model\n"
                ,getName(),lowPeriod, highPeriod) );
            throw new RuntimeException(message.toString());
        }

        deviation = deviation/(double)nPeriods;
        return deviation;

    } // end computeMetric()


    private Boolean readModel(String fName) {

   // ../stationmodel/IU_ANMO/ANMO.00.LHZ.90
        String fileName = ModelDir + fName;

   // First see if the file exists
        if (!(new File(fileName).exists())) {
            //System.out.format("=== %s: ModelFile=%s does NOT exist!\n", getName(), fileName);
            return false;
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
// MTH: This is hard-wired for Adam's station model files which have 7 columns:
                if (args.length != 7) {
                    String message = "==Error reading Station Model File: got " + args.length + " args on one line!";
                    throw new RuntimeException(message);
                }
                try {
                tmpPers.add( Double.valueOf(args[0].trim()).doubleValue() );
                tmpPows.add( Double.valueOf(args[2].trim()).doubleValue() );
                }
                catch (NumberFormatException e) {
                    System.out.format("== StationDeviation: Error reading modelFile=[%s]: %s\n", fName, e);
                    return false;
                }
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
        Double[] tmpPeriods  = tmpPers.toArray(new Double[]{});
        Double[] tmpPowers   = tmpPows.toArray(new Double[]{});

        ModelPeriods = new double[tmpPeriods.length];
        ModelPowers  = new double[tmpPowers.length];

        for (int i=0; i<tmpPeriods.length; i++){
            ModelPeriods[i] = tmpPeriods[i];
            ModelPowers[i]  = tmpPowers[i];
        }

        return true;

    } // end readModel


} // end class

