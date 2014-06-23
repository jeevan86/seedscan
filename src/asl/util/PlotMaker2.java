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

import java.awt.Color;
import java.awt.Font;
import java.io.File;
//import java.awt.BasicStroke;
import java.io.IOException;
import java.util.ArrayList;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Hagerty <hagertmb@bc.edu>
 */
public class PlotMaker2 {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.util.PlotMaker2.class);
	private String plotTitle;

	private ArrayList<Panel> panels;

	// constructor(s)
	public PlotMaker2(String title) {
		this.plotTitle = title;
	}

	public void addTraceToPanel(Trace trace, int iPanel)
			throws PlotMakerException {
		if (panels == null) {
			throw new PlotMakerException(
					"== addTraceToPanel: panels == null !!");
		}
		if (iPanel >= panels.size()) {
			throw new PlotMakerException(
					"== addTraceToPanel: Requested iPanel > panels.size() !!");
		}
		Panel panel = panels.get(iPanel);
		panel.addTrace(trace);
	}

	public void initialize3Panels(String subTitle1, String subTitle2,
			String subTitle3) {
		if (panels == null) {
			panels = new ArrayList<Panel>(3);
		}
		Panel panel1 = new Panel(subTitle1);
		panels.add(panel1);
		Panel panel2 = new Panel(subTitle2);
		panels.add(panel2);
		Panel panel3 = new Panel(subTitle3);
		panels.add(panel3);
	}

	public void writePlot(String fileName) {
		// System.out.format("== plotTitle=[%s] fileName=[%s]\n", plotTitle,
		// fileName);

		File outputFile = new File(fileName);

		// Check that we will be able to output the file without problems and if
		// not --> return
		if (!checkFileOut(outputFile)) {
			// System.out.format("== plotMaker: request to output plot=[%s] but we are unable to create it "
			// + " --> skip plot\n", fileName );
			logger.warn(
					"== Request to output plot=[%s] but we are unable to create it "
							+ " --> skip plot\n", fileName);
			return;
		}

		NumberAxis horizontalAxis = new NumberAxis("x-axis default"); // x =
																		// domain

		if (fileName.contains("nlnm") || fileName.contains("coher")
				|| fileName.contains("stn")) { // NLNM or StationDeviation
			horizontalAxis = new LogarithmicAxis("Period (sec)");
			horizontalAxis.setRange(new Range(1, 11000));
			horizontalAxis.setTickUnit(new NumberTickUnit(5.0));
		} else { // EventCompareSynthetics/StrongMotion
			horizontalAxis = new NumberAxis("Time (s)");
			double x[] = panels.get(0).getTraces().get(0).getxData();
			horizontalAxis.setRange(new Range(x[0], x[x.length - 1]));
		}

		CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(
				horizontalAxis);
		combinedPlot.setGap(15.);

		// Loop over (3) panels for this plot:

		for (Panel panel : panels) {

			NumberAxis verticalAxis = new NumberAxis("y-axis default"); // y =
																		// range

			if (fileName.contains("nlnm") || fileName.contains("stn")) { // NLNM
																			// or
																			// StationDeviation
				verticalAxis = new NumberAxis("PSD 10log10(m**2/s**4)/Hz dB");
				verticalAxis.setRange(new Range(-190, -95));
				verticalAxis.setTickUnit(new NumberTickUnit(5.0));
			} else if (fileName.contains("coher")) { // Coherence
				verticalAxis = new NumberAxis("Coherence, Gamma");
				verticalAxis.setRange(new Range(0, 1.2));
				verticalAxis.setTickUnit(new NumberTickUnit(0.1));
			} else { // EventCompareSynthetics/StrongMotion
				verticalAxis = new NumberAxis("Displacement (m)");
			}

			Font fontPlain = new Font("Verdana", Font.PLAIN, 14);
			Font fontBold = new Font("Verdana", Font.BOLD, 18);
			verticalAxis.setLabelFont(fontBold);
			verticalAxis.setTickLabelFont(fontPlain);
			horizontalAxis.setLabelFont(fontBold);
			horizontalAxis.setTickLabelFont(fontPlain);

			XYSeriesCollection seriesCollection = new XYSeriesCollection();
			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
			XYPlot xyplot = new XYPlot((XYDataset) seriesCollection,
					horizontalAxis, verticalAxis, renderer);
			xyplot.setDomainGridlinesVisible(true);
			xyplot.setRangeGridlinesVisible(true);
			xyplot.setRangeGridlinePaint(Color.black);
			xyplot.setDomainGridlinePaint(Color.black);

			// Plot each trace on this panel:
			int iTrace = 0;
			for (Trace trace : panel.getTraces()) {

				XYSeries series = new XYSeries(trace.getName());

				double xdata[] = trace.getxData();
				double ydata[] = trace.getyData();
				for (int k = 0; k < xdata.length; k++) {
					series.add(xdata[k], ydata[k]);
				}

				renderer.setSeriesPaint(iTrace, trace.getColor());
				renderer.setSeriesStroke(iTrace, trace.getStroke());
				renderer.setSeriesLinesVisible(iTrace, true);
				renderer.setSeriesShapesVisible(iTrace, false);

				seriesCollection.addSeries(series);

				iTrace++;
			}

			// Add Annotations for each trace - This is done in a separate loop
			// so that
			// the upper/lower limits for this panel will be known
			double xmin = horizontalAxis.getRange().getLowerBound();
			double xmax = horizontalAxis.getRange().getUpperBound();
			double ymin = verticalAxis.getRange().getLowerBound();
			double ymax = verticalAxis.getRange().getUpperBound();
			double delX = Math.abs(xmax - xmin);
			double delY = Math.abs(ymax - ymin);

			// Annotation (x,y) in normalized units - where upper-right corner =
			// (1,1)
			double xAnn = 0.97; // Right center coords of the trace name (e.g.,
								// "00-LHZ")
			double yAnn = 0.95;

			double yOff = 0.05; // Vertical distance between different trace
								// legends

			iTrace = 0;
			for (Trace trace : panel.getTraces()) {
				if (!trace.getName().contains("NLNM")
						&& !trace.getName().contains("NHNM")) {
					// x1 > x2 > x3, e.g.:
					// o-------o 00-LHZ
					// x3 x2 x1

					double scale = .01; // Controls distance between trace label
										// and line segment
					double xL = .04; // Length of trace line segment in legend

					double xAnn2 = xAnn - scale * trace.getName().length();
					double xAnn3 = xAnn - scale * trace.getName().length() - xL;

					double x1 = xAnn * delX + xmin; // Right hand x-coord of
													// text in range units
					double x2 = xAnn2 * delX + xmin; // x-coord of line segment
														// end in range units
					double x3 = xAnn3 * delX + xmin; // x-coord of line segment
														// end in range units

					double y = (yAnn - (iTrace * yOff)) * delY + ymin;

					if (horizontalAxis instanceof LogarithmicAxis) {
						double logMin = Math.log10(xmin);
						double logMax = Math.log10(xmax);
						delX = logMax - logMin;
						x1 = Math.pow(10, xAnn * delX + logMin);
						x2 = Math.pow(10, xAnn2 * delX + logMin);
						x3 = Math.pow(10, xAnn3 * delX + logMin);
					}
					xyplot.addAnnotation(new XYLineAnnotation(x3, y, x2, y,
							trace.getStroke(), trace.getColor()));
					XYTextAnnotation xyText = new XYTextAnnotation(
							trace.getName(), x1, y);
					xyText.setFont(new Font("Verdana", Font.BOLD, 18));
					xyText.setTextAnchor(TextAnchor.CENTER_RIGHT);
					xyplot.addAnnotation(xyText);
				}
				iTrace++;
			}

			combinedPlot.add(xyplot, 1);

		} // panel

		final JFreeChart chart = new JFreeChart(combinedPlot);
		chart.setTitle(new TextTitle(plotTitle, new Font("Verdana", Font.BOLD,
				18)));
		chart.removeLegend();

		try {
			ChartUtilities.saveChartAsPNG(outputFile, chart, 1400, 1400);
		} catch (IOException e) {
			// System.err.println("Problem occurred creating chart.");
			logger.error("IOException:", e);
		}

	} // writePlot()

	private Boolean checkFileOut(File file) {

		// Check that dir either exists or can be created

		File dir = file.getParentFile();

		Boolean allIsOkay = true;

		if (dir.exists()) { // Dir exists --> check write permissions
			if (!dir.isDirectory()) {
				allIsOkay = false; // The filename exists but it is NOT a
									// directory
			} else {
				allIsOkay = dir.canWrite();
			}
		} else { // Dir doesn't exist --> try to make it
			allIsOkay = dir.mkdirs();
		}

		if (!allIsOkay) { // We were unable to make output dir --> return false
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
