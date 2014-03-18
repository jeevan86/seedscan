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

import freq.Cmplx;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Calendar;

import java.awt.BasicStroke;
import java.awt.Color;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.Station;
import asl.util.Hex;
import asl.util.PlotMaker2;
import asl.util.Trace;

import timeutils.Timeseries;

//New metric for PSD Differences in 90-110, 200-500 second period ranges
public class DifferencePBM 
extends PowerBandMetric
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.DifferencePBM.class);

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getBaseName()
    {
        return "DifferencePBM";
    }

    private PlotMaker2 plotMaker = null;

    public void process()
    {
        logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

        String station = getStation();
        String day = getDay();
        String metric = getName();

        if (!weHaveChannels("00", "LH") || !weHaveChannels("10", "LH") ){
            logger.info(String.format("== %s: Day=[%s] Stn=[%s] - metadata + data NOT found for EITHER loc=00 -OR- loc=10 + band=LH --> Skip Metric",
                        getName(), getDay(), getStation()) );
            return;
        }

        for (int i=0; i < 3; i++) {
            Channel channelX = null;
            Channel channelY = null;

            if (i==0) {
                channelX = new Channel("00", "LHZ");
                channelY = new Channel("10", "LHZ");
            }
            else if (i==1) {
                channelX = new Channel("00", "LHND");
                channelY = new Channel("10", "LHND");
            }
            else if (i==2) {
                channelX = new Channel("00", "LHED");
                channelY = new Channel("10", "LHED");
            }

            ChannelArray channelArray = new ChannelArray(channelX, channelY);

            ByteBuffer digest = metricData.valueDigestChanged(channelArray, createIdentifier(channelX, channelY), getForceUpdate());

            if (digest == null) { // means oldDigest == newDigest and we don't need to recompute the metric 
                logger.warn("Digest unchanged station:[{}] channelX=[{}] channelY=[{}]--> Skip metric", 
                           getStation(), channelX, channelY);
                continue;
            }

            double result = computeMetric(channelX, channelY, station, day, metric);
            if (result == NO_RESULT) {
                // Do nothing --> skip to next channel
            }
            else {
                metricResult.addResult(channelX, channelY, result, digest);
            }
        }// end foreach channel

        if (getMakePlots()) {
            final String pngName   = String.format("%s.%s.png", getOutputDir(), "diff" );
            plotMaker.writePlot(pngName);
        }
    } // end process()


    private double computeMetric(Channel channelX, Channel channelY, String station, String day, String metric) {

     // Compute/Get the 1-sided psd[f] using Peterson's algorithm (24 hrs, 13 segments, etc.)

        CrossPower crossPower = getCrossPower(channelX, channelX);
        double[] Gxx   = crossPower.getSpectrum();
        double dfX     = crossPower.getSpectrumDeltaF();

        crossPower     = getCrossPower(channelY, channelY);
        double[] Gyy   = crossPower.getSpectrum();
        double dfY     = crossPower.getSpectrumDeltaF();

        crossPower     = getCrossPower(channelX, channelY);
        double[] Gxy   = crossPower.getSpectrum();

        if (dfX != dfY) {  // Oops - spectra have different frequency sampling!
            StringBuilder message = new StringBuilder();
            message.append(String.format("DifferencePBM Error: station=[{}] channelX[{}] channelY=[{}] day=[{}] metric=[{}]: dfX != dfY --> Can't continue\n", station, channelX, channelY, day, metric));
            RuntimeException e = new RuntimeException(message.toString());
            logger.error("DifferencePBM RuntimeException:", e);
            return NO_RESULT;
        }
        double df = dfX;

        if (Gxx.length != Gyy.length || Gxx.length != Gxy.length) {  // Something's wrong ...
            StringBuilder message = new StringBuilder();
            message.append(String.format("DifferencePBM Error: station=[{}] channelX[{}] channelY=[{}] day=[{}] metric=[{}]: Gxx.length != Gyy.length --> Can't continue\n", station, channelX, channelY, day, metric));
            RuntimeException e = new RuntimeException(message.toString());
       	    logger.error("DifferencePBM RuntimeException:", e); 
       	    return NO_RESULT;
        }
        
        int nf = Gxx.length;
        double freq[] = new double[nf];
        double diff[] = new double[nf];	

        // Compute diff[f] and fill freq array
        for ( int k = 0; k < nf; k++){
            freq[k] = (double)k * df;
            diff[k] = 10*Math.log10(Gxx[k]) - 10*Math.log10(Gyy[k]); 
        }
       	diff[0] = 0; 

        double[] per      = new double[nf];
      	double[] diffPer = new double[nf]; 

      	// per[nf-1] = 1/freq[0] = 1/0 = inf --> set manually:
        per[nf-1] = 0;  
        for (int k = 0; k < nf-1; k++){
            per[k] = 1./freq[nf-k-1];
       	    diffPer[k] = diff[nf-k-1]; 
        }
        double Tmin  = per[0];    // Should be = 1/fNyq = 2/fs = 0.1 for fs=20Hz
        double Tmax  = per[nf-2]; // Should be = 1/df = Ndt

        PowerBand band    = getPowerBand();
        double lowPeriod  = band.getLow();
        double highPeriod = band.getHigh();

        if (!checkPowerBand(lowPeriod, highPeriod, Tmin, Tmax)){
            System.out.format("%s powerBand Error: Skipping channel:%s\n", getName(), channelX);
            return NO_RESULT;
        }

        // Compute average Difference within the requested period band:
        double averageValue = 0;
        int nPeriods = 0;
        for (int k = 0; k < per.length; k++) {
            if (per[k] >  highPeriod) {
                break;
            }
            else if (per[k] >= lowPeriod){
                averageValue += diffPer[k]; 
                nPeriods++;
            }
        }

        if (nPeriods == 0) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("DifferencePBM Error: station=[{}] channelX=[{}] channelY=[{}] day=[{}] metric=[{}]: Requested band [%f - %f] contains NO periods --> divide by zero!\n", station, channelX, channelY, day, metric, lowPeriod, highPeriod) );
            RuntimeException e = new RuntimeException(message.toString());
       	    logger.error("DifferencePBM RuntimeException:", e); 
       	    return NO_RESULT;
        }
        averageValue /= (double)nPeriods;
/**
        if (getMakePlots()) {   // Output files like 2012160.IU_ANMO.00-LHZ.png = psd
            PlotMaker plotMaker = new PlotMaker(metricResult.getStation(), channelX, channelY, metricResult.getDate());
            plotMaker.plotCoherence(per, gammaPer, "coher");
        }
**/

        if (getMakePlots()) {   // Output files like 2012160.IU_ANMO.00-LHZ.png = psd

            if (plotMaker == null) {
                String plotTitle = String.format("%04d%03d [ %s ] Difference",  metricResult.getDate().get(Calendar.YEAR), metricResult.getDate().get(Calendar.DAY_OF_YEAR), metricResult.getStation() );
                plotMaker = new PlotMaker2(plotTitle);
                plotMaker.initialize3Panels("LHZ", "LHND", "LHED");
            }
            int iPanel = 0;
            Color color = Color.red;

            BasicStroke stroke = new BasicStroke(2.0f);

            if  (channelX.getChannel().equals("LHZ")) {
                iPanel = 0;
            }
            else if (channelX.getChannel().equals("LHND") ) {
                iPanel = 1;
            }
            else if (channelX.getChannel().equals("LHED") ) {
                iPanel = 2;
            }
            else { // ??
            }
            String channelLabel = MetricResult.createResultId(channelX, channelY);
            //plotMaker.addTraceToPanel( new Trace(per, gammaPer, channelLabel, color, stroke), iPanel);
       	    plotMaker.addTraceToPanel(new Trace(per, diffPer, channelLabel, color, stroke), iPanel); 
        }
        return averageValue;
    } // end computeMetric()
} // end class
