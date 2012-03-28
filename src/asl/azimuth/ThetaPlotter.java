package asl.azimuth;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

/**
 * Plots the azimuth angle offset data and correlation values
 * 
 * @author fshelly
 * 
 */
public class ThetaPlotter extends JPanel
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ThetaPlotter(String newLabel, 
            String refStation, String refNetwork, String refChannel, String refLocation,
            String northStation, String northNetwork, String northChannel, String northLocation,
            String eastStation, String eastNetwork, String eastChannel, String eastLocation,
            double _bestTheta, double _stdDeviation, double _bestCorrelation)
    {
        label = newLabel;
        mRefStation = refStation;
        mRefNetwork = refNetwork;
        mRefChannel = refChannel;
        mRefLocation = refLocation;
        mNorthStation = northStation;
        mNorthNetwork = northNetwork;
        mNorthChannel = northChannel;
        mNorthLocation = northLocation;
        mEastStation = eastStation;
        mEastNetwork = eastNetwork;
        mEastChannel = eastChannel;
        mEastLocation = eastLocation;

        bestTheta = _bestTheta;
        stdDeviation = _stdDeviation;
        bestCorrelation = _bestCorrelation;

        TimeZone.setDefault(timeZone);
    }

    /**
     * Creates a new JFreeChart and sets up the plot
     * @return JFreeChart implementing the plot
     */
    private JFreeChart createChartThetaCorl()
    {
        datasetTheta = new TimeSeriesCollection(new TimeSeries("Offset Angle"));
        datasetCorl = new TimeSeriesCollection(new TimeSeries("Correlation"));

        String chanStr = String.format(
                "Ref %s %s %2.2s/%3.3s   North %s %s %2.2s/%3.3s  East %s %s %2.2s/%3.3s", 
                mRefStation, mRefNetwork, mRefLocation, mRefChannel,
                mNorthStation, mNorthNetwork, mNorthLocation, mNorthChannel,
                mEastStation, mEastNetwork, mEastLocation, mEastChannel);

        chart = ChartFactory.createTimeSeriesChart(
                "UTC Time", // chart title
                chanStr, // x axis label
                label,      // y axis label				
                datasetTheta,    // data
                false,      // legend
                true,       // tooltips
                false       // url
                );

        chart.setBackgroundPaint(Color.white);
        XYPlot plot = chart.getXYPlot();

        rangeAxisTheta = (NumberAxis) plot.getRangeAxis();
        rangeAxisTheta.setLowerMargin(0.40);  // to leave room for Correlation bars
        DecimalFormat formatTheta = new DecimalFormat("000.00");
        rangeAxisTheta.setNumberFormatOverride(formatTheta);
        rangeAxisTheta.setAutoRange(true);
        rangeAxisTheta.setAutoRangeMinimumSize(1.0);
        XYItemRenderer rendererTheta = plot.getRenderer();
        rendererTheta.setBaseToolTipGenerator(new StandardXYToolTipGenerator(
                    StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                    new SimpleDateFormat("HHmm"), new DecimalFormat("000.00")));

        rangeAxisCorl = new NumberAxis("Correlation");
        rangeAxisCorl.setAutoRange(true);
        rangeAxisCorl.setAutoRangeMinimumSize(0.1);
        DecimalFormat formatCorl = new DecimalFormat("0.00");
        rangeAxisCorl.setNumberFormatOverride(formatCorl);

        plot.setDataset(1,datasetCorl);
        plot.setRangeAxis(1, rangeAxisCorl);
        plot.mapDatasetToRangeAxis(1, 1);

        XYLineAndShapeRenderer rendererCorl = new XYLineAndShapeRenderer();
        rendererCorl.setBaseShapesVisible(false);
        rendererCorl.setBaseToolTipGenerator(
                new StandardXYToolTipGenerator(
                    StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                    new SimpleDateFormat("HHmm"),
                    new DecimalFormat("0.00")));
        ChartUtilities.applyCurrentTheme(chart);
        plot.setRenderer(1, rendererCorl);

        // get a reference to the plot for further customization...
        String corlMsg = String.format("15%% best correlations %.3f", bestCorrelation);
        String thetaMsg = String.format("Offset = %.3f +/- %.3f deg", bestTheta, stdDeviation*3.0);
        LegendItemCollection items = new LegendItemCollection();
        items.add(new LegendItem(thetaMsg, null, null, null,
                    new Rectangle2D.Double(-6.0, -3.0, 12.0, 6.0),
                    Color.blue));
        items.add(new LegendItem(corlMsg, null, null, null,
                    new Rectangle2D.Double(-6.0, -3.0, 12.0, 6.0), Color.red));
        plot.setFixedLegendItems(items);
        plot.setInsets(new RectangleInsets(5, 5, 5, 20));
        LegendTitle legend = new LegendTitle(plot);
        legend.setPosition(RectangleEdge.BOTTOM);
        chart.addSubtitle(legend);

        return chart;

    } // createChartTime()

    /**
     * Creates a new JPanel containing the view of the Chart
     * @return new JPanel containing the view of the Chart
     */
    public JPanel createTimePanel()
    {
        JFreeChart chart = createChartThetaCorl();
        return new EHChartPanel(chart);
    }

    /**
     * Add the Theta data to be plotted
     * @param newData    Time series data to be plotted
     * @param interval   How long between each data point (microseconds)
     * @param startTime  Start time for the plot
     * @param countsPer  How many data points per plot point
     */
    public void AddNewTheta(double newData[], double interval,
            Date startTime, int countsPer)
    {
        int i;
        int iRow = 0;
        TimeSeries series = datasetTheta.getSeries(iRow);
        tPlotStartHour = new Date(startTime.getTime());

        if (newData.length > 0)
            System.err.println("Time " + startTime + "; Start "
                    + tPlotStartHour + "; Data " + newData[0] + " " + newData[1] 
                    + "; Count " + series.getItemCount());

        // Handle any change in time span
        if (series.getMaximumItemCount() != 1000)
        {
            series.setMaximumItemCount(1000);
        }

        for (i=0; i < newData.length; i++)
        {
            series.add(new Second(startTime), 
                    newData[i]);

            startTime = new Date(startTime.getTime() + (long)(interval*1000));
        }  // loop through each new data item

        chart.setTitle(tPlotStartHour.toString());
    } // AddNewTheta()

    /**
     * Add the correlation data to be plotted
     * @param newData    New correlation data
     * @param interval   Time between data points (microseconds)
     * @param startTime  Start time of data
     * @param countsPer  How many data points per plot point
     */
    public void AddNewCorl(double newData[], double interval,
            Date startTime, int countsPer)
    {
        int i;
        int iRow = 0;
        TimeSeries series = datasetCorl.getSeries(iRow);
        tPlotStartHour = new Date(startTime.getTime());

        if (newData.length > 0)
            System.err.println("Time " + startTime + "; Start "
                    + tPlotStartHour + "; Data " + newData[0] + " " + newData[1] 
                    + "; Count " + series.getItemCount());

        // Limit how much data we'll handle
        if (series.getMaximumItemCount() != 1000)
        {
            series.setMaximumItemCount(1000);
        }

        for (i=0; i < newData.length; i++)
        {
            series.add(new Second(startTime), 
                    newData[i]); 

            startTime = new Date(startTime.getTime() + (long)(interval*1000));
        }  // loop through each new data item

    } // AddNewCorl()

    private Date                         tPlotStartHour;
    private String                       label;
    private String                       mRefStation;
    private String                       mNorthStation;
    private String                       mEastStation;
    private String                       mRefNetwork;
    private String                       mNorthNetwork;
    private String                       mEastNetwork;
    private String                       mRefChannel;
    private String                       mNorthChannel;
    private String                       mEastChannel;
    private String                       mRefLocation;
    private String                       mNorthLocation;
    private String                       mEastLocation;

    private double                       bestTheta;
    private double                       bestCorrelation;
    private double                       stdDeviation;

    private NumberAxis                   rangeAxisTheta;
    private NumberAxis                   rangeAxisCorl;
    private TimeSeriesCollection         datasetTheta=null;
    private TimeSeriesCollection         datasetCorl=null;
    private JFreeChart                   chart;
    private TimeZone                     timeZone         = TimeZone
        .getTimeZone("GMT");
} // class SegmentPlotter
