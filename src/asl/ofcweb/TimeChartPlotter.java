package asl.ofcweb;

import java.awt.Color;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
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
public class TimeChartPlotter
    extends  JPanel
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public TimeChartPlotter(String newStation,
            String newNetwork, String newChannel)
    {
        station = newStation;
        network = newNetwork;
        channel = newChannel;

        iSumAvg = 0;
        iSumCount=0;
        tLastSum = null;

        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    private XYDataset createDataset()
    {
        SeriesID = 0;
        dataset = new TimeSeriesCollection(timeZone);
        seriesMap = new HashMap<Integer, TimeSeries>();

        TimeSeries minSeries = new TimeSeries("Min", Minute.class);
        dataset.addSeries(minSeries);
        seriesMap.put(SeriesID++, minSeries);

        TimeSeries maxSeries = new TimeSeries("Max", Minute.class);
        dataset.addSeries(maxSeries);
        seriesMap.put(SeriesID++, maxSeries);

        TimeSeries avgSeries = new TimeSeries("Avg", Minute.class);
        dataset.addSeries(avgSeries);
        seriesMap.put(SeriesID++, avgSeries);

        return dataset;
    }

    private JFreeChart createChartTime(XYDataset dataset)
    {
        chart = ChartFactory.createTimeSeriesChart(
                station +" "+ network +" "+ channel, // chart title
                "UTC Time", // x axis label
                "Counts", // y axis label				
                dataset, // data
                true,    // legend
                true,    // tooltips
                false    // url
                );

        chart.setBackgroundPaint(Color.white);
        XYPlot plot = chart.getXYPlot();

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for(int i=0; i < SeriesID; i++){
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, false);
        }
        plot.setRenderer(renderer);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        //		date_axis = (DateAxis) plot.getDomainAxis();
        //		date_axis.setDateFormatOverride(new SimpleDateFormat("HHmm"));
        iSumAvg = 0;
        iSumCount=0;
        tLastSum = null;

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

    public void removeDataSet(int setID)
    {
        dataset.removeSeries(seriesMap.get(setID));
    }

    public void addDataSet(int setID)
    {
        dataset.addSeries(seriesMap.get(setID));
    }

    public void clearDataSet()
    {
        dataset.getSeries(0).clear();
        dataset.getSeries(1).clear();
        dataset.getSeries(2).clear();
    }

    public void SetTitle(String newStation,
            String newNetwork, String newChannel)
    {
        station = newStation;
        network = newNetwork;
        channel = newChannel;
        chart.setTitle(station +" "+ network +" "+ channel);
    }

    public void AddNewData(int minData[], int maxData[], int avgData[],
            GregorianCalendar tStartTime, int countsPer)
    {

        int i;
        TimeSeries minSeries = dataset.getSeries(0);
        TimeSeries maxSeries = dataset.getSeries(1);
        TimeSeries avgSeries = dataset.getSeries(2);

        tStartTime.setTimeZone(timeZone);

        // See if this is the first time we've added data
        if (tLastSum == null)
        {
            iSumAvg = 0;
            iSumCount=0;
        }
        else
        {
            // Make any gaps in continuous data show up as blanks
            if (tStartTime.getTimeInMillis() - tLastSum.getTimeInMillis() > countsPer*60000)
            {
                System.err.println("DEBUG gap in data starting at: " + tLastSum.getTime());
                System.err.println("Time " + tStartTime.getTime()
                        + "; Count " + minSeries.getItemCount());

                // Putting a null value at the start of the gap period works
                minSeries.addOrUpdate(new Minute(tLastSum.getTime(), timeZone), 
                        null); 
                maxSeries.addOrUpdate(new Minute(tLastSum.getTime(), timeZone), 
                        null); 
                avgSeries.addOrUpdate(new Minute(tLastSum.getTime(), timeZone), 
                        null); 
            } // Handle gap in normally continuous data stream

        } // adding data to existing data

        // make sure we have the current time correct
        tLastSum = new GregorianCalendar(timeZone);
        tLastSum.setTimeInMillis(tStartTime.getTimeInMillis());

        // Handle any change in time span
        if (minSeries.getMaximumItemCount() != MAX_COUNT)
        {
            minSeries.setMaximumItemCount(MAX_COUNT);
            maxSeries.setMaximumItemCount(MAX_COUNT);
            avgSeries.setMaximumItemCount(MAX_COUNT);
        }

        for (i=0; i < avgData.length; i++)
        {
            // Set min and max values
            if (iSumCount == 0)
            {
                iMin = minData[i];
                iMax = maxData[i];
            }
            else
            {
                iMin = iMin > minData[i] ? minData[i] : iMin;
                iMax = iMax > maxData[i] ? iMax : maxData[i];
            }

            // Average last iCountsPer
            iSumCount++;
            iSumAvg += avgData[i];
            if (iSumCount >= countsPer)
            {
                Date timetag = new Date(tStartTime.getTimeInMillis() + ((i-iSumCount) * 60000));
                minSeries.addOrUpdate(new Minute(timetag, timeZone), iMin);
                maxSeries.addOrUpdate(new Minute(timetag, timeZone), iMax);
                avgSeries.addOrUpdate(new Minute(timetag, timeZone), 
                        (double)iSumAvg /(double)iSumCount);		

                iSumCount=0;
                iSumAvg = 0;
            } // if we've averaged up correct number of data points
            tLastSum.add(Calendar.MINUTE, 1);	
        }  // loop through each new data item

        long lower = (long)dataset.getDomainLowerBound(true);
        //		long upper = (long)dataset.getDomainUpperBound(true);

        //		System.err.printf("Domain bounds %d %d\n", lower, upper);
        chart.setTitle(station +" "+ network + " "+ channel 
                + " from " + new Date(lower).toString()
                + " to " + tLastSum.getTime().toString());

    } // AddNewData()

    private String station;
    private String network;
    private String channel;

    private Integer SeriesID = 0;
    private TimeSeriesCollection dataset;
    private HashMap<Integer, TimeSeries> seriesMap;
    private JFreeChart chart;
    private TimeZone timeZone = TimeZone.getTimeZone("GMT");

    private int iMin = 0;
    private int iMax = 0;
    private int iSumAvg = 0;
    private int iSumCount = 0;
    private GregorianCalendar tLastSum = null;

    public final int MAX_COUNT = 10000;

} // class TimeChartPlotter
