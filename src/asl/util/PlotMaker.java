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

package asl.util;

import asl.metadata.Station;
import asl.metadata.Channel;
import java.util.ArrayList;
import java.util.Calendar;
import java.awt.Font;

import java.io.IOException;
import java.io.File;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.data.xy.*;
import org.jfree.data.Range;
import org.jfree.util.ShapeUtilities;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Paint;

/** 
 * @author Mike Hagerty    <hagertmb@bc.edu>
 */
public class PlotMaker 
{
    private Station station;
    private Channel channel, channelX, channelY;
    private Channel[] channels;
    private Calendar date;
    private final String outputDir = "outputs";

    // constructor(s)
    public PlotMaker(Station station, Channel channel, Calendar date)
    {
        this.station = station;
        this.channel = channel;
        this.date    = date;
    }
    public PlotMaker(Station station, Channel channelX, Channel channelY, Calendar date)
    {
        this.station  = station;
        this.channelX = channelX;
        this.channelY = channelY;
        this.date     = date;
    }
    public PlotMaker(Station station, Channel[] channels, Calendar date)
    {
        this.station = station;
        this.channels= channels;
        this.date    = date;
    }

    public void plotZNE_3x3(ArrayList<double[]> channelData, double[] xsecs, int nstart, int nend, String eventString, String plotString) {

        // Expecting 9 channels packed like:            Panel   Trace1  Trace2  Trace3
        // channels[0] = 00-LHZ                           1     00-LHZ   10-LHZ   20-LHZ
        // channels[1] = 00-LHND                          2     00-LHND  10-LHND  20-LHND
        // channels[2] = 00-LHED                          3     00-LHED  10-LHED  20-LHED
        // channels[3] = 10-LHZ                           
        // channels[4] = 10-LHND                          
        // channels[5] = 10-LHED                          
        // channels[6] = 20-LHZ                           
        // channels[7] = 20-LHND                         
        // channels[8] = 20-LHED                        

        final String plotTitle = String.format("%04d%03d [Stn:%s] [Event:%s] %s", date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, eventString, plotString);
        final String pngName   = String.format("%s/%s.%s.%s.png", outputDir, eventString, station, plotString);
        File outputFile = new File(pngName);

        // Check that we will be able to output the file without problems and if not --> return
        if (!checkFileOut(outputFile)) {
            System.out.format("== plotZNE_3x3: request to output plot=[%s] but we are unable to create it "
                              + " --> skip plot\n", pngName );
            return;
        }

        if (channelData.size() != channels.length) {
            System.out.format("== plotZNE_3x3: Error: We have [%d channels] but [%d channelData]\n", channels.length, channelData.size() );
            return;
        }
    
        XYSeries[] series = new XYSeries[ channels.length ];
        for (int i=0; i<channels.length; i++) {
            series[i] = new XYSeries(channels[i].toString());
            double[] data = channelData.get(i);
            //for (int k = 0; k < xsecs.length; k++){
            for (int k = 0; k < data.length; k++){
                series[i].add( xsecs[k], data[k] );
            }
        }

// I. Panel I = Verticals

// Use the first data array, within the plotted range (nstart - nend) to scale the plots:
        double[] data = channelData.get(0);
        double ymax = 0;
        for (int k = nstart; k < nend; k++){
            if (data[k] > ymax) ymax=data[k];
        }

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        Paint[] paints = new Paint[] { Color.red, Color.blue , Color.green};
        for (int i=0; i<paints.length; i++){
            renderer.setSeriesPaint(i, paints[i]);
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, false);
        }

        final NumberAxis verticalAxis = new NumberAxis("Displacement (m)");
        verticalAxis.setRange( new Range(-ymax, ymax));
        //verticalAxis.setTickUnit( new NumberTickUnit(5) );

        final NumberAxis horizontalAxis = new NumberAxis("Time (s)");
        horizontalAxis.setRange( new Range(nstart , nend) );
        //horizontalAxis.setRange( new Range(0.00009 , 110) );
        final NumberAxis hAxis = new NumberAxis("Time (s)");
        hAxis.setRange( new Range(nstart , nend) );

        final XYSeriesCollection seriesCollection1 = new XYSeriesCollection();
        seriesCollection1.addSeries(series[0]);
        seriesCollection1.addSeries(series[3]);
        seriesCollection1.addSeries(series[6]);
        //final XYPlot xyplot1 = new XYPlot((XYDataset)seriesCollection1, null, verticalAxis, renderer);
        //final XYPlot xyplot1 = new XYPlot((XYDataset)seriesCollection1, horizontalAxis, verticalAxis, renderer);
        final XYPlot xyplot1 = new XYPlot((XYDataset)seriesCollection1, hAxis, verticalAxis, renderer);
        double x = .95 * xsecs[nend];
        double y = .90 * ymax;
        XYTextAnnotation annotation1 = new XYTextAnnotation("Vertical", x, y);
        annotation1.setFont(new Font("SansSerif", Font.PLAIN, 14));
        xyplot1.addAnnotation(annotation1);

// II. Panel II = North

// Use the first data array, within the plotted range (nstart - nend) to scale the plots:
        data = channelData.get(1);
        ymax = 0;
        for (int k = nstart; k < nend; k++){
            if (data[k] > ymax) ymax=data[k];
        }
        final NumberAxis verticalAxisN = new NumberAxis("Displacement (m)");
        verticalAxisN.setRange( new Range(-ymax, ymax));

        final XYSeriesCollection seriesCollection2 = new XYSeriesCollection();
        seriesCollection2.addSeries(series[1]);
        seriesCollection2.addSeries(series[4]);
        seriesCollection2.addSeries(series[7]);
        final XYPlot xyplot2 = new XYPlot((XYDataset)seriesCollection2, null, verticalAxisN, renderer);
        XYTextAnnotation annotation2 = new XYTextAnnotation("North-South", x, y);
        annotation2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        xyplot2.addAnnotation(annotation2);

// III. Panel III = East

// Use the first data array, within the plotted range (nstart - nend) to scale the plots:
        data = channelData.get(2);
        ymax = 0;
        for (int k = nstart; k < nend; k++){
            if (data[k] > ymax) ymax=data[k];
        }
        final NumberAxis verticalAxisE = new NumberAxis("Displacement (m)");
        verticalAxisE.setRange( new Range(-ymax, ymax));

        final XYSeriesCollection seriesCollection3 = new XYSeriesCollection();
        seriesCollection3.addSeries(series[2]);
        seriesCollection3.addSeries(series[5]);
        seriesCollection3.addSeries(series[8]);
        final XYPlot xyplot3 = new XYPlot((XYDataset)seriesCollection3, null, verticalAxisE, renderer);
        XYTextAnnotation annotation3 = new XYTextAnnotation("East-West", x, y);
        annotation3.setFont(new Font("SansSerif", Font.PLAIN, 14));
        xyplot3.addAnnotation(annotation3);

        //CombinedXYPlot combinedPlot = new CombinedXYPlot( horizontalAxis, CombinedXYPlot.VERTICAL );
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot( horizontalAxis );
        combinedPlot.add(xyplot1,1);
        combinedPlot.add(xyplot2,1);
        combinedPlot.add(xyplot3,1);
        combinedPlot.setGap(15.);

        final JFreeChart chart = new JFreeChart(combinedPlot);
        chart.setTitle( new TextTitle(plotTitle) );

        try {
            ChartUtilities.saveChartAsPNG(outputFile, chart, 1400, 800);
        } catch (IOException e) {
            System.err.println("Problem occurred creating chart.");

        }

    }


    public void plotPSD(double per[], double[] model, double[] psd, String modelName, String plotString) {
        plotPSD(per, model, null, null, psd, modelName, plotString);
    }
    public void plotPSD(double per[], double[] model, double[] nhnmPer, double[] nhnm, double[] psd, String modelName, String plotString) {

        // plotTitle = "2012074.IU_ANMO.00-BHZ " + plotString
        final String plotTitle = String.format("%04d%03d.%s.%s %s", date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channel, plotString);
        // plot filename = "2012074.IU_ANMO.00-BHZ" + plotString + ".png"
        final String pngName   = String.format("%s/%04d%03d.%s.%s.%s.png", outputDir, date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channel, plotString);
        File outputFile = new File(pngName);

        // Check that we will be able to output the file without problems and if not --> return
        if (!checkFileOut(outputFile)) {
            System.out.format("== plotPSD: request to output plot=[%s] but we are unable to create it "
                              + " --> skip plot\n", pngName );
            return;
        }

        Boolean plotNHNM = false;
        //if (nhnm.length > 0) {
        if (nhnm != null) {
            plotNHNM = true;
        }

        final XYSeries series1 = new XYSeries(modelName);
        final XYSeries series2 = new XYSeries(channel.toString());
        final XYSeries series3 = new XYSeries("NHNM");

        for (int k = 0; k < per.length; k++){
            series1.add( per[k], model[k] );
            series2.add( per[k], psd[k] );
        }

        if (plotNHNM){
            for (int k = 0; k < nhnmPer.length; k++){
                series3.add( nhnmPer[k], nhnm[k] );
            }
        }

        //final XYItemRenderer renderer = new StandardXYItemRenderer();
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        Rectangle rectangle = new Rectangle(3, 3);

        renderer.setSeriesShape(0, rectangle);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesLinesVisible(0, true);

        renderer.setSeriesShape(1, rectangle);
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesLinesVisible(1, false);

        renderer.setSeriesShape(2, rectangle);
        renderer.setSeriesShapesVisible(2, false);
        renderer.setSeriesLinesVisible(2, true);

        Paint[] paints = new Paint[] { Color.blue, Color.red , Color.black};
        renderer.setSeriesPaint(0, paints[0]);
        renderer.setSeriesPaint(1, paints[1]);
        renderer.setSeriesPaint(2, paints[2]);

        final NumberAxis rangeAxis1 = new NumberAxis("PSD 10log10(m**2/s**4)/Hz dB");
        //rangeAxis1.setRange( new Range(-190, -120));
        rangeAxis1.setRange( new Range(-190, -95));
        rangeAxis1.setTickUnit( new NumberTickUnit(5.0) );

        final LogarithmicAxis horizontalAxis = new LogarithmicAxis("Period (sec)");
        horizontalAxis.setRange( new Range(0.05 , 10000) );

        final XYSeriesCollection seriesCollection = new XYSeriesCollection();
        seriesCollection.addSeries(series1);
        seriesCollection.addSeries(series2);

        if (plotNHNM){
            seriesCollection.addSeries(series3);
        }

        final XYPlot xyplot = new XYPlot((XYDataset)seriesCollection, horizontalAxis, rangeAxis1, renderer);

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
    } // end plotPSD


    public void plotCoherence(double per[], double[] gamma, String plotString) {

        final String plotTitle = String.format("%04d%03d.%s.%s-%s", date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channelX, channelY);
        final String pngName   = String.format("%s/%04d%03d.%s.%s-%s.%s.png", outputDir,date.get(Calendar.YEAR),date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channelX, channelY, plotString);

        File outputFile = new File(pngName);

        // Check that we will be able to output the file without problems and if not --> return
        if (!checkFileOut(outputFile)) {
            System.out.format("== plotCoherence: request to output plot=[%s] but we are unable to create it "
                              + " --> skip plot\n", pngName );
            return;
        }

        final String legend    = String.format("%s--%s",channelX, channelY);
        final XYSeries series1 = new XYSeries(legend);

        for (int k = 0; k < gamma.length; k++){
            series1.add( per[k], gamma[k] );
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


    public void plotSpecAmp2(double freq[], double[] amp1, double[] phase1, double[] amp2, double[] phase2, String plotTitle, String pngName) {

/**
        final String plotTitle = String.format("%04d%03d.%s.%s %s", date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channel, plotString);
        final String pngName   = String.format("%s/%04d%03d.%s.%s.%s.png", outputDir, date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channel, plotString);
**/
        File outputFile = new File(pngName);

        // Check that we will be able to output the file without problems and if not --> return
        if (!checkFileOut(outputFile)) {
            System.out.format("== plotSpecAmp: request to output plot=[%s] but we are unable to create it "
                              + " --> skip plot\n", pngName );
            return;
        }
    // Plot x-axis (frequency) range
        final double XMIN = .00009;
        final double XMAX = freq[freq.length-1];

System.out.format("== plotSpecAmp2: nfreq=%d npts=%d pngName=%s\n", freq.length, amp2.length, pngName);

        final XYSeries series1 = new XYSeries("Amp_PZ");
        final XYSeries series1b= new XYSeries("Amp_Cal");

        final XYSeries series2 = new XYSeries("Phase_PZ");
        final XYSeries series2b= new XYSeries("Phase_Cal");

        double maxdB = 0.;
        for (int k = 0; k < freq.length; k++){
            double dB = amp1[k];
            //double dB = 20. * Math.log10( amp1[k] );
            //series1.add( freq[k], dB );
            //series1.add( freq[k], 20. * Math.log10( amp1[k] ) );
            //series1b.add(freq[k], 20. * Math.log10( amp2[k] ));
            series1.add( freq[k], amp1[k] );
            series1b.add(freq[k], amp2[k] );
            series2.add( freq[k], phase1[k] );
            series2b.add(freq[k], phase2[k] );
            if (dB > maxdB) { maxdB = dB;}
        }

        //final XYItemRenderer renderer = new StandardXYItemRenderer();
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        Rectangle rectangle = new Rectangle(3, 3);
        renderer.setSeriesShape(0, rectangle);
        //renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesLinesVisible(0, true);

        renderer.setSeriesShape(1, rectangle);
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesLinesVisible(1, false);

        Paint[] paints = new Paint[] { Color.red, Color.blue };
        renderer.setSeriesPaint(0, paints[0]);
        //renderer.setSeriesPaint(1, paints[1]);

	final XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
        renderer2.setSeriesPaint(0, paints[1]);
        renderer2.setSeriesShapesVisible(0, false);
        renderer2.setSeriesLinesVisible(0, true);

	// Stroke is part of Java Swing ...
	//renderer2.setBaseStroke( new Stroke( ... ) );

        double ymax;
        if (maxdB < 10) {
            ymax = 10.;
        }
        else {
            ymax = maxdB + 2;;
        }

        final NumberAxis verticalAxis = new NumberAxis("Spec Amp (dB)");
        verticalAxis.setRange( new Range(-40, ymax));
        verticalAxis.setTickUnit( new NumberTickUnit(5) );

        //final LogarithmicAxis verticalAxis = new LogarithmicAxis("Amplitude Response");
        //verticalAxis.setRange( new Range(0.01 , 10) );

        final LogarithmicAxis horizontalAxis = new LogarithmicAxis("Frequency (Hz)");
        //horizontalAxis.setRange( new Range(0.0001 , 100.5) );
        //horizontalAxis.setRange( new Range(0.00009 , 110) );
        horizontalAxis.setRange( new Range(XMIN, XMAX) );

        final XYSeriesCollection seriesCollection = new XYSeriesCollection();
        seriesCollection.addSeries(series1);
        seriesCollection.addSeries(series1b);

        final XYPlot xyplot = new XYPlot((XYDataset)seriesCollection, null, verticalAxis, renderer);
        //final XYPlot xyplot = new XYPlot((XYDataset)seriesCollection, horizontalAxis, verticalAxis, renderer);

        xyplot.setDomainGridlinesVisible(true);  
        xyplot.setRangeGridlinesVisible(true);  
        xyplot.setRangeGridlinePaint(Color.black);  
        xyplot.setDomainGridlinePaint(Color.black);  

        final NumberAxis phaseAxis = new NumberAxis("Phase (Deg)");
        phaseAxis.setRange( new Range(-180, 180));
        phaseAxis.setTickUnit( new NumberTickUnit(30) );
        final XYSeriesCollection seriesCollection2 = new XYSeriesCollection();
        seriesCollection2.addSeries(series2);
        seriesCollection2.addSeries(series2b);
        final XYPlot xyplot2 = new XYPlot((XYDataset)seriesCollection2, null, phaseAxis, renderer2);

        //CombinedXYPlot combinedPlot = new CombinedXYPlot( horizontalAxis, CombinedXYPlot.VERTICAL );
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot( horizontalAxis );
	combinedPlot.add(xyplot,1);
	combinedPlot.add(xyplot2,1);
	combinedPlot.setGap(15.);

        //final JFreeChart chart = new JFreeChart(xyplot);
        final JFreeChart chart = new JFreeChart(combinedPlot);
        chart.setTitle( new TextTitle(plotTitle) );

        try { 
            ChartUtilities.saveChartAsPNG(outputFile, chart, 1000, 800);
        } catch (IOException e) { 
            System.err.println("Problem occurred creating chart.");

        }
    } // end plotResp


    public void plotSpecAmp(double freq[], double[] amp, double[] phase, String plotString) {

        // plotTitle = "2012074.IU_ANMO.00-BHZ " + plotString
        final String plotTitle = String.format("%04d%03d.%s.%s %s", date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channel, plotString);
        // plot filename = "2012074.IU_ANMO.00-BHZ" + plotString + ".png"
        final String pngName   = String.format("%s/%04d%03d.%s.%s.%s.png", outputDir, date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channel, plotString);

        File outputFile = new File(pngName);

        // Check that we will be able to output the file without problems and if not --> return
        if (!checkFileOut(outputFile)) {
            System.out.format("== plotSpecAmp: request to output plot=[%s] but we are unable to create it "
                              + " --> skip plot\n", pngName );
            return;
        }

        final XYSeries series1 = new XYSeries("Amplitude");
        final XYSeries series2 = new XYSeries("Phase");

        double maxdB = 0.;
        for (int k = 0; k < freq.length; k++){
            double dB = 20. * Math.log10( amp[k] );
            series1.add( freq[k], dB );
            series2.add( freq[k], phase[k] );
            if (dB > maxdB) { maxdB = dB;}
        }

        //final XYItemRenderer renderer = new StandardXYItemRenderer();
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        Rectangle rectangle = new Rectangle(3, 3);
        renderer.setSeriesShape(0, rectangle);
        //renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesLinesVisible(0, true);

        renderer.setSeriesShape(1, rectangle);
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesLinesVisible(1, false);

        Paint[] paints = new Paint[] { Color.red, Color.blue };
        renderer.setSeriesPaint(0, paints[0]);
        //renderer.setSeriesPaint(1, paints[1]);

	final XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
        renderer2.setSeriesPaint(0, paints[1]);
        renderer2.setSeriesShapesVisible(0, false);
        renderer2.setSeriesLinesVisible(0, true);

	// Stroke is part of Java Swing ...
	//renderer2.setBaseStroke( new Stroke( ... ) );

        double ymax;
        if (maxdB < 10) {
            ymax = 10.;
        }
        else {
            ymax = maxdB + 2;;
        }

        final NumberAxis verticalAxis = new NumberAxis("Spec Amp (dB)");
        verticalAxis.setRange( new Range(-40, ymax));
        verticalAxis.setTickUnit( new NumberTickUnit(5) );

        //final LogarithmicAxis verticalAxis = new LogarithmicAxis("Amplitude Response");
        //verticalAxis.setRange( new Range(0.01 , 10) );

        final LogarithmicAxis horizontalAxis = new LogarithmicAxis("Frequency (Hz)");
        //horizontalAxis.setRange( new Range(0.0001 , 100.5) );
        horizontalAxis.setRange( new Range(0.00009 , 110) );

        final XYSeriesCollection seriesCollection = new XYSeriesCollection();
        seriesCollection.addSeries(series1);

        final XYPlot xyplot = new XYPlot((XYDataset)seriesCollection, null, verticalAxis, renderer);
        //final XYPlot xyplot = new XYPlot((XYDataset)seriesCollection, horizontalAxis, verticalAxis, renderer);

        xyplot.setDomainGridlinesVisible(true);  
        xyplot.setRangeGridlinesVisible(true);  
        xyplot.setRangeGridlinePaint(Color.black);  
        xyplot.setDomainGridlinePaint(Color.black);  

        final NumberAxis phaseAxis = new NumberAxis("Phase (Deg)");
        phaseAxis.setRange( new Range(-180, 180));
        phaseAxis.setTickUnit( new NumberTickUnit(30) );
        final XYSeriesCollection seriesCollection2 = new XYSeriesCollection();
        seriesCollection2.addSeries(series2);
        final XYPlot xyplot2 = new XYPlot((XYDataset)seriesCollection2, null, phaseAxis, renderer2);

        //CombinedXYPlot combinedPlot = new CombinedXYPlot( horizontalAxis, CombinedXYPlot.VERTICAL );
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot( horizontalAxis );
	combinedPlot.add(xyplot,1);
	combinedPlot.add(xyplot2,1);
	combinedPlot.setGap(15.);

        //final JFreeChart chart = new JFreeChart(xyplot);
        final JFreeChart chart = new JFreeChart(combinedPlot);
        chart.setTitle( new TextTitle(plotTitle) );

// Here we need to see if test dir exists and create it if necessary ...
        try { 
            //ChartUtilities.saveChartAsJPEG(new File("chart.jpg"), chart, 500, 300);
            //ChartUtilities.saveChartAsPNG(outputFile, chart, 500, 300);
            ChartUtilities.saveChartAsPNG(outputFile, chart, 1000, 800);
        } catch (IOException e) { 
            System.err.println("Problem occurred creating chart.");

        }
    } // end plotResp


    private Boolean checkFileOut( File file) {

        // Check that dir either exists or can be created

        File dir = file.getParentFile();

        Boolean allIsOkay = true;

        if (dir.exists()) {             // Dir exists --> check write permissions
            if (!dir.isDirectory()) {
                allIsOkay = false;      // The filename exists but it is NOT a directory
            }
            else {
                allIsOkay = dir.canWrite();
            }
        }
        else {                          // Dir doesn't exist --> try to make it
            allIsOkay = dir.mkdirs();
        }

        if (!allIsOkay) {               // We were unable to make output dir --> return false
            return false;
        }

        // Check that if file already exists it can be overwritten

        if (file.exists()) {
            if (!file.canWrite()) {
                return false;
            }
        }

        return true;

    }



}

