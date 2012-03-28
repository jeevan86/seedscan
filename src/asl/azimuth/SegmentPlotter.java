package asl.azimuth;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

/**
 * Plots seismic time series data so user can select the segment he wants to analyze
 * 
 * @author fshelly
 * 
 */
public class SegmentPlotter extends JPanel
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for a new time series plot
     * @param newLabel    Plot title
     * @param newStation  Station code
     * @param newNetwork  Network code
     * @param newChannel  Channel code
     * @param newLocation Location code
     */
    public SegmentPlotter(String newLabel,   String newStation,
                          String newNetwork, String newChannel,
                          String newLocation)
    {
        iRowCount = 3;
        label = newLabel;
        station = newStation;
        network = newNetwork;
        channel = newChannel;
        location = newLocation;

        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Creates data structures that store data to be plotted.  Also creates cursor markers.
     * @return
     */
    private XYDataset createDataset()
    {
        dataset = new TimeSeriesCollection(timeZone);
        for (int i = 1; i <= iRowCount; i++)
        {
            plotSeries[i-1] = new TimeSeries("Row" + i);
            plotSeries[i-1].setMaximumItemCount(2500);
        }
        markSeries = new TimeSeries("Start/End");
        markSeries.setMaximumItemCount(2);
        return dataset;
    }

    /**
     * Builds a new TimeSeriesChart
     * @param dataset  The data associated with the plot
     * @return a new JFreeChart object that implements the plot
     */
    private JFreeChart createChartTime(XYDataset dataset)
    {
        chart = ChartFactory.createTimeSeriesChart(
                station +" "+ network +" "+ location +"/"+ channel, // chart title
                "UTC Time", // x axis label
                label,      // y axis label				
                dataset,    // data
                false,      // legend
                true,       // tooltips
                false       // url
                );

        chart.setBackgroundPaint(Color.white);
        XYPlot plot = chart.getXYPlot();

        renderer = new XYLineAndShapeRenderer();
        for(int i=0; i < iRowCount; i++){
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, false);
        }
        plot.setRenderer(renderer);
        plot.setDomainCrosshairVisible(false);
        plot.setDomainCrosshairLockedOnData(false);
        plot.setRangeCrosshairVisible(false);

        // Add listeners so we can tell when user presses crosshair
        //    chart.addProgressListener(this);

        rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        date_axis = (DateAxis) plot.getDomainAxis();
        date_axis.setDateFormatOverride(new SimpleDateFormat("HHmm"));
        date_axis.setAutoRange(true);		

        return chart;
    } // createChartTime()

    /**
     * Returns the viewable JPanel needed to show a plot
     * @return the viewable JPanel needed to show a plot
     */
    public JPanel createTimePanel()
    {
        JFreeChart chart = createChartTime(createDataset());
        return new EHChartPanel(chart);
    }

    /**
     *  Call this when a new set of files have been selected
     */
    public void resetTimeData()
    {
        dataset.removeAllSeries();
        for (int i=0; i < iRowCount; i++)
        {
            plotSeries[i].clear();
        }
    } // resetTimeData()

    /**
     *  Call this one when changing the subset of contiguous data being used
     */
    public void resetSelectData()
    {
        // Only clearing the last two series, not the first one
        for (int i=1; i < iRowCount; i++)
        {
            plotSeries[i].clear();
        }  
    } // resetSelectData()

    /**
     * Allows a program to add data to a plot without that data showing up before the program is done.
     * @param visible  true to display the plot lines, false to make them invisible
     */
    public void SetVisible(boolean visible)
    {
        dataset.removeAllSeries();

        for(int i=0; i < iRowCount && visible; i++)
        {
            dataset.addSeries(plotSeries[i]);
            renderer.setSeriesLinesVisible(i, visible);
        }
        dataset.addSeries(markSeries);
        renderer.setSeriesLinesVisible(iRowCount, false);
        renderer.setSeriesShapesVisible(iRowCount, true);
        renderer.setSeriesShape(iRowCount, new Rectangle2D.Float(0, -90, 1, 180)); 
    } // SetVisible()

    /**
     * Display's a cross hair at the given time point
     * @param newTimePos  time point we want a crosshair to appear
     */
    public void setCrossHairTime(Date newTimePos)
    {
        if (chart != null) 
        {
            XYPlot plot = (XYPlot) chart.getPlot();
            double xx = newTimePos.getTime();
            plot.setDomainCrosshairValue(xx);
        }
    } // setCrossHairTime()

    /**
     * Get the time associated with the cross hair
     * @return time associated with the cross hair
     */
    public Date getCrossHairTime()
    {
        if (chart != null) 
        {
            XYPlot plot = (XYPlot) chart.getPlot();
            double xx = plot.getDomainCrosshairValue();
            return new Date((long)xx);
        }
        else
            return new Date(0);
    } // getCrossHairTime

    /**
     * Places the data segment markers
     * @param startMark time for start marker
     * @param startVal  value to associated with the marker
     * @param endMark   time for end marker
     * @param endVal    value associated with the marker
     */
    public void setStartEndMarks(Date startMark, int startVal, 
            Date endMark, int endVal)
    {
        renderer.setSeriesLinesVisible(iRowCount, false);
        renderer.setSeriesShapesVisible(iRowCount, true);
        markSeries.clear();
        markSeries.add(new Second(startMark), startVal);
        markSeries.add(new Second(endMark), endVal);
    } // setStartEndMark

    /**
     * Change the plot title with new SNCL data
     * @param newStation  Station code
     * @param newNetwork  Network code
     * @param newChannel  Channel code
     * @param newLocation Location code
     */
    public void SetTitle(String newStation,
            String newNetwork, String newChannel, String newLocation)
    {
        station = newStation;
        network = newNetwork;
        channel = newChannel;
        location = newLocation;
        chart.setTitle(station +" "+ network +" "+ location +"/"+ channel);
    }

    /**
     * Work horse that plots a data time series
     * @param newData      The data to be plotted
     * @param dRate        Sample frequency
     * @param startTime    The start time for the data series
     * @param countsPer    Generate one plot point for countsPer data points
     * @param iRow         Which of the time series to plot
     */
    public void AddNewData(int newData[], double dRate,
            Date startTime, int countsPer, int iRow )
    {
        int i;
        double minVal, maxVal;
        int iMin;
        int iPeriodMs = (int)(countsPer*500 / dRate);
        if (iPeriodMs < 1000)
        {
            iPeriodMs = 1000;
        }
        Date midTime = startTime;

        TimeSeries series = plotSeries[iRow];
        tPlotStartHour = new Date(startTime.getTime());

        // make sure we have the current time correct
        timeLastSum = new Date(startTime.getTime());
        iSumCount = 0;
        iMin = 0;
        minVal = newData[0];
        maxVal = newData[0];

        // We generate two points per plot period (min/max)
        iCountsPer = countsPer * 2;
        if (iCountsPer < 1)
            iCountsPer = 1; 		

        for (i=0; i < newData.length; i++)
        {

            // Where we are in iCountsPer
            iSumCount++;

            // Get min and max values, order, and midpoint timetag
            if (iSumCount == 1)
            {
                iMin = 0;
                minVal = newData[i];
                maxVal = newData[i];
            }
            else
            {
                if (newData[i] < minVal)
                {
                    iMin = 1;
                    minVal = newData[i];
                }
                else if (newData[i] > maxVal)
                {
                    iMin = 0;
                    maxVal = newData[i];
                }
            }
            if (iSumCount == iCountsPer/2)
                midTime = new Date(startTime.getTime());

            if (iSumCount >= iCountsPer)
            {
                if (iCountsPer > 1)
                {
                    if (iMin == 0)
                    {
                        series.addOrUpdate(new Second(midTime), minVal);
                        series.addOrUpdate(new Second(startTime), maxVal);
                    }
                    else
                    {
                        series.addOrUpdate(new Second(midTime), maxVal);
                        series.addOrUpdate(new Second(startTime), minVal);
                    }
                }
                else
                {
                    series.addOrUpdate(new Second(startTime), 
                            (double)newData[i]);
                }

                iSumCount=0;
                timeLastSum = new Date(startTime.getTime());
            } // if we've merged the correct number of data points

            startTime = new Date(startTime.getTime() + (long)(1000.0/dRate));
        }  // loop through each new data item

        // Each segment is separated, so force a blank afterwards
        timeLastSum = new Date(timeLastSum.getTime() + iPeriodMs);
        series.addOrUpdate(new Second(timeLastSum), null); 

        if (iRow == 1)
        {
            chart.setTitle(station +" "+ network +" "+ location +"/"+ channel 
                    + "  " + tPlotStartHour.toString());
        }
        date_axis.setAutoRange(true);   
    } // AddNewData()

    private int iRowCount;
    private Date tPlotStartHour;
    private String label;
    private String station;
    private String network;
    private String channel;
    private String location;

    private TimeSeriesCollection dataset;
    private XYLineAndShapeRenderer renderer;
    private NumberAxis rangeAxis;
    private TimeSeries[] plotSeries = { null, null, null };
    private TimeSeries markSeries = null;
    private JFreeChart chart = null;
    private TimeZone timeZone = TimeZone.getTimeZone("GMT");
    private DateAxis date_axis;

    private int iSumCount = 0;
    private int iCountsPer;
    private Date timeLastSum = null;
} // class SegmentPlotter
