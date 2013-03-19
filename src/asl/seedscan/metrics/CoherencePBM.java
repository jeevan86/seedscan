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

import freq.Cmplx;

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
import asl.util.Hex;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Calendar;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.Station;

import timeutils.Timeseries;

public class CoherencePBM
extends PowerBandMetric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.CoherencePBM");

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getBaseName()
    {
        return "CoherencePBM";
    }

    private final String outputDir = "outputs";

    public void process()
    {
        System.out.format("\n              [ == Metric %s == ]\n", getName() ); 

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

            if (digest == null) { 
                System.out.format("%s INFO: Data and metadata have NOT changed for channelX=%s + channelY=%s --> Skipping\n"
                        ,getName(), channelX, channelY);
                continue;
            }

            double result = computeMetric(channelX, channelY);
            if (result == NO_RESULT) {
                // Do nothing --> skip to next channel
            }
            else {
                metricResult.addResult(channelX, channelY, result, digest);

            }

        }// end foreach channel

    } // end process()


    private double computeMetric(Channel channelX, Channel channelY) {

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
            throw new RuntimeException("CoherencePBM Error: dfX != dfY --> Can't continue");
        }
        double df      = dfX;

        if (Gxx.length != Gyy.length || Gxx.length != Gxy.length) {  // Something's wrong ...
            throw new RuntimeException("CoherencePBM Error: Gxx.length != Gyy.length --> Can't continue");
        }
     // nf = number of positive frequencies + DC (nf = nfft/2 + 1, [f: 0, df, 2df, ...,nfft/2*df] )
        int nf        = Gxx.length;
        double freq[] = new double[nf];
        double gamma[]= new double[nf];

        // Compute gamma[f] and fill freq array
        for ( int k = 0; k < nf; k++){
            freq[k] = (double)k * df;
            gamma[k]= (Gxy[k]*Gxy[k]) / (Gxx[k]*Gyy[k]);
            gamma[k]= Math.sqrt(gamma[k]);
        }
        gamma[0]=0;
        //Timeseries.timeoutXY(freq, gamma, "Gamma");
        //Timeseries.timeoutXY(freq, Gxx, "Gxx");
        //Timeseries.timeoutXY(freq, Gyy, "Gyy");
        //Timeseries.timeoutXY(freq, Gxy, "Gxy");

        // Convert gamma[f] to gamma[T]
        // Reverse freq[] --> per[] where per[0]=shortest T and per[nf-2]=longest T:

        double[] per      = new double[nf];
        double[] gammaPer = new double[nf];
        // per[nf-1] = 1/freq[0] = 1/0 = inf --> set manually:
        per[nf-1] = 0;  
        for (int k = 0; k < nf-1; k++){
            per[k]     = 1./freq[nf-k-1];
            gammaPer[k]  = gamma[nf-k-1];
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

        // Compute average Coherence within the requested period band:
        double averageValue = 0;
        int nPeriods = 0;
        for (int k = 0; k < per.length; k++){
            if (per[k] >  highPeriod){
                break;
            }
            else if (per[k] >= lowPeriod){
                averageValue += gammaPer[k];
                nPeriods++;
            }
        }

        if (nPeriods == 0) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("CoherencePBM Error: Requested band [%f - %f] contains NO periods --> divide by zero!\n"
                        ,lowPeriod, highPeriod) );
            throw new RuntimeException(message.toString());
        }
        averageValue /= (double)nPeriods;

        if (getMakePlots()){
            plotCoherence(channelX, channelY, per, gammaPer);
        }

        return averageValue;

    } // end computeMetric()


    private void plotCoherence(Channel channelX, Channel channelY, double[] period, double[] gamma) {

        // See if outputDir exists. If not, then try to make it and if that
        //   fails then return

        File dir = new File(outputDir);
        Boolean allIsOkay = true;
        if (dir.exists()) {           // Dir exists --> check write permissions
            if (!dir.isDirectory()) {
                allIsOkay = false;        // The filename exists but it is NOT a directory
            }
            else {
                allIsOkay = dir.canWrite();
            }
        }
        else {                      // Dir doesn't exist --> try to make it
            allIsOkay = dir.mkdir();
        }

        Station station        = metricResult.getStation();
        Calendar date          = metricResult.getDate();
        final String plotTitle = String.format("%04d%03d.%s.%s-%s", date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channelX, channelY);
        final String pngName   = String.format("%s/%04d%03d.%s.%s-%s.png", outputDir, date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channelX, channelY);

        File outputFile = new File(pngName);
        if (outputFile.exists()) {
            if (!outputFile.canWrite()) {
                allIsOkay = false;
            }
        }

        if (!allIsOkay) {
            System.out.format("== plotPSD: request to output plot=[%s] but we are unable to create it "
                              + " --> skip plot\n", pngName );
            return;
        }

        final String legend    = String.format("%s--%s",channelX, channelY);
        final XYSeries series1 = new XYSeries(legend);

        for (int k = 0; k < gamma.length; k++){
            series1.add( period[k], gamma[k] );
        }

        //final XYItemRenderer renderer1 = new StandardXYItemRenderer();
        final XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
        Rectangle rectangle = new Rectangle(3, 3);
        renderer1.setSeriesShape(0, rectangle);
        renderer1.setSeriesShapesVisible(0, true);
        renderer1.setSeriesLinesVisible(0, false);

        Paint[] paints = new Paint[] { Color.red, Color.black };
        renderer1.setSeriesPaint(0, paints[0]);

        final NumberAxis rangeAxis1 = new NumberAxis("Coherence, Gamma");
        rangeAxis1.setRange( new Range(0, 1.2));
        rangeAxis1.setTickUnit( new NumberTickUnit(0.1) );

        final LogarithmicAxis horizontalAxis = new LogarithmicAxis("Period (sec)");
        horizontalAxis.setRange( new Range(0.05 , 10000) );

        final XYSeriesCollection seriesCollection = new XYSeriesCollection();
        seriesCollection.addSeries(series1);

        final XYPlot xyplot = new XYPlot((XYDataset)seriesCollection, horizontalAxis, rangeAxis1, renderer1);

        xyplot.setDomainGridlinesVisible(true);  
        xyplot.setRangeGridlinesVisible(true);  
        xyplot.setRangeGridlinePaint(Color.black);  
        xyplot.setDomainGridlinePaint(Color.black);  

        final JFreeChart chart = new JFreeChart(xyplot);
        chart.setTitle( new TextTitle(plotTitle) );

        try { 
            ChartUtilities.saveChartAsPNG(outputFile, chart, 500, 300);
        } catch (IOException e) { 
            System.err.println("Problem occurred creating chart.");

        }
    } // end plotCoherence

} // end class
