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
import java.util.Calendar;

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


    public void plotPSD(double per[], double[] model, double[] psd, String modelName, String plotString) {

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

        final XYSeries series1 = new XYSeries(modelName);
        final XYSeries series2 = new XYSeries(channel.toString());

        for (int k = 0; k < per.length; k++){
            series1.add( per[k], model[k] );
            series2.add( per[k], psd[k] );
        }

        //final XYItemRenderer renderer = new StandardXYItemRenderer();
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        Rectangle rectangle = new Rectangle(3, 3);

        renderer.setSeriesShape(0, rectangle);
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesLinesVisible(0, false);

        renderer.setSeriesShape(1, rectangle);
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesLinesVisible(1, false);

        Paint[] paints = new Paint[] { Color.black, Color.red };
        renderer.setSeriesPaint(0, paints[0]);
        renderer.setSeriesPaint(1, paints[1]);

        final NumberAxis rangeAxis1 = new NumberAxis("PSD 10log10(m**2/s**4)/Hz dB");
        rangeAxis1.setRange( new Range(-190, -120));
        rangeAxis1.setTickUnit( new NumberTickUnit(5.0) );

        final LogarithmicAxis horizontalAxis = new LogarithmicAxis("Period (sec)");
        horizontalAxis.setRange( new Range(0.05 , 10000) );

        final XYSeriesCollection seriesCollection = new XYSeriesCollection();
        seriesCollection.addSeries(series1);
        seriesCollection.addSeries(series2);

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

        for (int k = 0; k < freq.length; k++){
            double dB = 20. * Math.log10( amp[k] );
            series1.add( freq[k], dB );
            series2.add( freq[k], phase[k] );
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

        final NumberAxis verticalAxis = new NumberAxis("Spec Amp (dB)");
        verticalAxis.setRange( new Range(-40, 10));
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
            allIsOkay = dir.mkdir();
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

