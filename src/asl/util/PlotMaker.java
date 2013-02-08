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
    private Channel channel;
    private Calendar date;

    // constructor(s)
    public PlotMaker(Station station, Channel channel, Calendar date)
    {
        this.station = station;
        this.channel = channel;
        this.date    = date;
    }

    public void plotSpecAmp(double freq[], double[] amp, String plotString) {

        String outputDir = ".";

        // plotTitle = "2012074.IU_ANMO.00-BHZ " + plotString
        final String plotTitle = String.format("%04d%03d.%s.%s %s", date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channel, plotString);
        // plot filename = "2012074.IU_ANMO.00-BHZ" + plotString + ".png"
        final String pngName   = String.format("%s/%04d%03d.%s.%s.%s.png", outputDir, date.get(Calendar.YEAR), date.get(Calendar.DAY_OF_YEAR)
                                                ,station, channel, plotString);

        Boolean allIsOkay = true;

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

        //final XYSeries series1 = new XYSeries(channel.toString());
        //final XYSeries series2 = new XYSeries("NLNM");

        final XYSeries series1 = new XYSeries("foo");

        for (int k = 0; k < freq.length; k++){
            double dB = 20. * Math.log10( amp[k] );
            series1.add( freq[k], dB );
            //series1.add( freq[k], amp[k] );
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

        Paint[] paints = new Paint[] { Color.red, Color.black };
        renderer.setSeriesPaint(0, paints[0]);
        renderer.setSeriesPaint(1, paints[1]);

        final NumberAxis verticalAxis = new NumberAxis("Spec Amp dB");
        verticalAxis.setRange( new Range(-40, 10));
        verticalAxis.setTickUnit( new NumberTickUnit(5) );

        //final LogarithmicAxis verticalAxis = new LogarithmicAxis("Amplitude Response");
        //verticalAxis.setRange( new Range(0.01 , 10) );

        final LogarithmicAxis horizontalAxis = new LogarithmicAxis("Frequency (Hz)");
        horizontalAxis.setRange( new Range(0.001 , 100) );

        final XYSeriesCollection seriesCollection = new XYSeriesCollection();
        seriesCollection.addSeries(series1);
        //seriesCollection.addSeries(series2);

        final XYPlot xyplot = new XYPlot((XYDataset)seriesCollection, horizontalAxis, verticalAxis, renderer);

        xyplot.setDomainGridlinesVisible(true);  
        xyplot.setRangeGridlinesVisible(true);  
        xyplot.setRangeGridlinePaint(Color.black);  
        xyplot.setDomainGridlinePaint(Color.black);  

        final JFreeChart chart = new JFreeChart(xyplot);
        chart.setTitle( new TextTitle(plotTitle) );

// Here we need to see if test dir exists and create it if necessary ...
        try { 
            //ChartUtilities.saveChartAsJPEG(new File("chart.jpg"), chart, 500, 300);
            ChartUtilities.saveChartAsPNG(outputFile, chart, 500, 300);
        } catch (IOException e) { 
            System.err.println("Problem occurred creating chart.");

        }
    } // end plotResp

}

