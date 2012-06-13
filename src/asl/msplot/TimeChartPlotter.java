package asl.msplot;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

/**
 * Plots seismic time series data
 */

/**
 * @author fshelly
 * 
 */
public class TimeChartPlotter extends JPanel
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public TimeChartPlotter(int rowCount, int timeSpanSeconds, int newMinRange,
            String [] newChanList, String newStation, String newNetwork)
    {
        iRowCount = rowCount;
        minRange = newMinRange;
        iTimeSpanSeconds = timeSpanSeconds;
        station = newStation;
        network = newNetwork;
        chanList = newChanList;

        TimeZone.setDefault(timeZone);
    } // TimeChartPlotter()

    private XYDataset createDataset()
    {
        dataset = new TimeSeriesCollection(timeZone);
        for (int i = 1; i <= iRowCount; i++)
        {
            TimeSeries series = new TimeSeries(chanList[i-1], Millisecond.class);
            dataset.addSeries(series);
            series.setMaximumItemCount(MAX_PLOTPOINTS);
        }
        return dataset;
    } // createDataset()

    private JFreeChart createChartTime(XYDataset dataset)
    {
        chart = ChartFactory.createTimeSeriesChart(
                station +" "+ network, // chart title
                "UTC Time", // x axis label
                "Response", // y axis label				
                dataset, // data
                true,    // legend
                true,    // tooltips
                false    // url
                );

        chart.setBackgroundPaint(Color.white);
        XYPlot plot = chart.getXYPlot();

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for(int i=0; i < iRowCount; i++)
        {
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, false);
            //      if (i == 2)
            //        renderer.setSeriesPaint(i, paint);
        }
        plot.setRenderer(renderer);

        rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setAutoRange(true);
        rangeAxis.setAutoRangeMinimumSize(minRange);

        date_axis = (DateAxis) plot.getDomainAxis();
        if (iTimeSpanSeconds < 900)
            date_axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        else
            date_axis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));

        date_axis = (DateAxis) plot.getDomainAxis();
        if (iTimeSpanSeconds < 900)
            date_axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        else
            date_axis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
        Calendar cal = Calendar.getInstance(timeZone);
        Date endDate = cal.getTime();
        cal.add(Calendar.HOUR, -iTimeSpanSeconds/3600);
        cal.add(Calendar.SECOND, -(iTimeSpanSeconds % 3600));
        cal.set(Calendar.MILLISECOND, 0);
        Date startDate = cal.getTime();
        date_axis.setRange(startDate, endDate);

        return chart;
    } // createChartTime()

    public JPanel createTimePanel()
    {
        JFreeChart chart = createChartTime(createDataset());
        return new EHChartPanel(chart);
    }

    public JPanel recreateTimePanel()
    {
        JFreeChart chart = createChartTime(dataset);
        return new EHChartPanel(chart);
    }

    public void SetTitle(String newStation,String newNetwork)
    {
        station = newStation;
        network = newNetwork;
        chart.setTitle(station +" "+ network);
    }

    public void SetTimeSpan(int newTimeSpanSeconds)
    {
        if (iTimeSpanSeconds != newTimeSpanSeconds)
        {
            for(int i=0; i < iRowCount; i++)
                dataset.getSeries(i).clear();
        }
        iTimeSpanSeconds = newTimeSpanSeconds;
        if (iTimeSpanSeconds < 900)
            date_axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        else
            date_axis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
    }

    public void SetMinRange(int newMinRange)
    {
        minRange = newMinRange;
        rangeAxis.setAutoRange(true);
        rangeAxis.setAutoRangeMinimumSize(minRange);
    }

    public void AddNewData(int newData[], int iRow, double dRate,
            GregorianCalendar tStartTime)
    {

        int i;
        Date startTime = tStartTime.getTime();
        TimeSeries series = dataset.getSeries(iRow);
        Date sampleTime = new Date(startTime.getTime());

        // Figure how much we have to compress data to keep plot performance up
        iCountsPer[iRow] = (int)(2.0*dRate*iTimeSpanSeconds) / MAX_PLOTPOINTS;
        if (iCountsPer[iRow] < 2)
            iCountsPer[iRow] = 2;  

        // See if this is the first time we've added data
        if (timeLastSum[iRow] == null)
        {
            iCount[iRow]=0;
            iMin[iRow] = 0;
            minVal[iRow] = newData[0];
            maxVal[iRow] = newData[0];
            timeLastSum[iRow] = startTime;
            midTime[iRow] = startTime;
        }
        else
        {
            // Make any gaps in continuous data show up as blanks
            if (startTime.getTime() - timeLastSum[iRow].getTime() 
                    > 1000 + iCountsPer[iRow]*1000/dRate)
            {
                double gap = (startTime.getTime() - timeLastSum[iRow].getTime()) / 500;
                String gapstr = String.format("DEBUG gap %.1f sec in data starting at %s",
                        gap, timeLastSum[iRow].toString());
                System.err.println(gapstr);


                // Putting a null value at the start of the gap period creates a blank region
                series.addOrUpdate(new Millisecond(timeLastSum[iRow]), null); 
            } // Handle gap in normally continuous data stream

        } // adding data to existing data

        for (i = 0; i < newData.length; i++)
        {
            sampleTime = new Date(startTime.getTime() + (long) (i*1000.0 / dRate));

            // Where we are in iCountsPer
            iCount[iRow]++;

            // Get min and max values, order, and midpoint timetag
            if (iCount[iRow] == 1)
            {
                iMin[iRow] = 0;
                minVal[iRow] = newData[i];
                maxVal[iRow] = newData[i];
            } else
            {
                if (newData[i] < minVal[iRow])
                {
                    iMin[iRow] = 1;
                    minVal[iRow] = newData[i];
                } else if (newData[i] > maxVal[iRow])
                {
                    iMin[iRow] = 0;
                    maxVal[iRow] = newData[i];
                }
            }
            if (iCount[iRow] == iCountsPer[iRow] / 2)
                midTime[iRow] = new Date(sampleTime.getTime());

            if (iCount[iRow] >= iCountsPer[iRow])
            {
                if (iCountsPer[iRow] > 1)
                {
                    if (iMin[iRow] == 0)
                    {
                        series.addOrUpdate(new Millisecond(midTime[iRow]), minVal[iRow]);
                        series.addOrUpdate(new Millisecond(sampleTime), maxVal[iRow]);
                    } else
                    {
                        series.addOrUpdate(new Millisecond(midTime[iRow]), maxVal[iRow]);
                        series.addOrUpdate(new Millisecond(sampleTime), minVal[iRow]);
                    }
                } else
                {
                    series.addOrUpdate(new Millisecond(sampleTime), (double) newData[i]);
                }

                iCount[iRow] = 0;
                timeLastSum[iRow] = new Date(sampleTime.getTime());
            } // if we've merged the correct number of data points

        } // loop through each new data item

        if (tPlotEndSec.getTime()/1000 < sampleTime.getTime()/1000)
        {
            tPlotEndSec = new Date(sampleTime.getTime());
            tPlotStartSec = new Date(tPlotEndSec.getTime() - (iTimeSpanSeconds*1000));
            date_axis.setRange(tPlotStartSec, tPlotEndSec);

            String title = String.format("%s %s %s %s",
                    station, network, chanList[iRow], 
                    tPlotEndSec.toString());
            chart.setTitle(title);
        }
    } // AddNewData()

    private final int   MAX_PLOTPOINTS=1200;
    private int         iRowCount;
    private int         iTimeSpanSeconds;
    private int         minRange;
    private String      station;
    private String      network;
    private String []   chanList;

    private TimeSeriesCollection dataset;
    private JFreeChart  chart;
    private TimeZone    timeZone = TimeZone.getTimeZone("GMT");
    private DateAxis    date_axis;
    private NumberAxis  rangeAxis;

    private Date        tPlotStartSec;
    private Date        tPlotEndSec = new Date(0);
    private int []      iCountsPer = {1, 1, 1};
    private int []      iCount = {0, 0, 0};
    private Date []     timeLastSum = {null, null, null};
    private Date []     midTime = {null, null, null};
    private double []   minVal = {0, 0, 0};
    private double []   maxVal = {0, 0, 0};
    private int []      iMin = {0, 0, 0};

} // class TimeChartPlotter
