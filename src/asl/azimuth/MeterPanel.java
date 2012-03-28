package asl.azimuth;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DialShape;
import org.jfree.chart.plot.MeterInterval;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

/**
 * Obsolete class that creates a plot for displaying small angle offsets.
 * 
 * @author fshelly
 * 
 */
public class MeterPanel
{

    /**
     * 1  Initial version
     */
    private static final long serialVersionUID = 1L;

    JPanel                      panel = null;
    JFreeChart                  m_chart;

    private static double       m_offset;
    private static double       m_reference;

    public static final double  DELTA_MAX =  5.0;
    public static final double  DELTA_WARN = 3.0;
    public static final double  DELTA_GOOD = 1.0;

    public static final int     DEFAULT_WIDTH  = 500;
    public static final int     DEFAULT_HEIGHT = 400; 

    public MeterPanel(double reference, double offset)
    {
        m_reference = AzAngleDisplay.Normalize360(reference);
        m_offset = offset;
        m_chart = createChart(createDataset(AzAngleDisplay.Normalize180(m_offset + m_reference)));
    }

    /**
     * Creates a sample dataset.
     * 
     * @return A sample dataset.
     */
    private static DefaultValueDataset createDataset(double offset)
    {
        DefaultValueDataset dataset = new DefaultValueDataset(offset);

        return dataset;
    }

    /**
     * Creates a chart.
     * 
     * @param dataset
     *          the dataset.
     * 
     * @return A chart.
     */
    private static JFreeChart createChart(DefaultValueDataset dataset)
    {

        MeterPlot plot = new MeterPlot(dataset);
        plot.setRange(new Range(-DELTA_MAX, DELTA_MAX));
        plot.addInterval(new MeterInterval("Critical", 
                    new Range(-DELTA_MAX, -DELTA_WARN),
                    Color.lightGray, new BasicStroke(2.0f),
                    new Color(255, 0, 0, 128)));
        plot.addInterval(new MeterInterval("Warning", 
                    new Range(-DELTA_WARN, -DELTA_GOOD),
                    Color.lightGray, new BasicStroke(2.0f), new Color(255, 255, 0, 64)));
        plot.addInterval(new MeterInterval("Good", 
                    new Range(-DELTA_GOOD, DELTA_GOOD),
                    Color.lightGray, new BasicStroke(2.0f),
                    new Color(0, 255, 0, 64)));
        plot.addInterval(new MeterInterval("Warning", 
                    new Range(DELTA_GOOD, DELTA_WARN),
                    Color.lightGray, new BasicStroke(2.0f), new Color(255, 255, 0, 64)));
        plot.addInterval(new MeterInterval("Critical", 
                    new Range(DELTA_WARN, DELTA_MAX),
                    Color.lightGray, new BasicStroke(2.0f),
                    new Color(255, 0, 0, 128)));
        plot.setNeedlePaint(Color.darkGray);
        plot.setDialBackgroundPaint(Color.white);
        plot.setDialOutlinePaint(Color.gray);
        plot.setDialShape(DialShape.CHORD);
        plot.setMeterAngle(260);
        plot.setTickLabelsVisible(true);
        plot.setTickLabelFont(new Font("Dialog", Font.BOLD, 10));
        plot.setTickLabelPaint(Color.darkGray);
        plot.setTickSize(5.0);
        plot.setTickPaint(Color.lightGray);

        plot.setValuePaint(Color.black);
        plot.setValueFont(new Font("Dialog", Font.BOLD, 14));
        plot.setUnits("Degrees");

        JFreeChart chart = new JFreeChart("Azimuth Offset",
                JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        String RefStr = String.format("   Ref %.3f", m_reference);
        String OffStr = String.format("Offset %.3f", m_offset);
        String ResultStr = String.format("Result %.3f", 
                AzAngleDisplay.Normalize360(m_reference + m_offset));

        TextTitle tRef = new TextTitle(RefStr,
                new Font("Dialog", Font.BOLD, 12));
        tRef.setPosition(RectangleEdge.TOP);
        tRef.setHorizontalAlignment(HorizontalAlignment.CENTER);
        tRef.setMargin(0.0, 4.0, 0.0, 4.0);
        tRef.setPaint(Color.red);
        chart.addSubtitle(tRef);

        TextTitle tOff = new TextTitle(OffStr,
                new Font("Dialog", Font.BOLD, 12));
        tOff.setPosition(RectangleEdge.TOP);
        tOff.setHorizontalAlignment(HorizontalAlignment.CENTER);
        tOff.setMargin(0.0, 4.0, 0.0, 4.0);
        tOff.setPaint(Color.blue);
        chart.addSubtitle(tOff);

        TextTitle tResult = new TextTitle(ResultStr,
                new Font("Dialog", Font.BOLD, 14));
        tResult.setPosition(RectangleEdge.TOP);
        tResult.setHorizontalAlignment(HorizontalAlignment.CENTER);
        tResult.setMargin(0.0, 4.0, 0.0, 4.0);
        tResult.setPaint(Color.red);
        chart.addSubtitle(0, tResult);

        /*
           String refMsg = String.format(
           "Reference Angle %.3f + Offset Angle %.3f = %.3f degrees", 
           m_reference, AzAngleDisplay.Normalize180(m_offset), 
           AzAngleDisplay.Normalize360(m_reference + m_offset));
           TextTitle tt = new TextTitle(refMsg,
           new Font("Dialog", Font.PLAIN, 11));
           tt.setPosition(RectangleEdge.BOTTOM);
           tt.setHorizontalAlignment(HorizontalAlignment.CENTER);
           tt.setMargin(0.0, 4.0, 0.0, 4.0);
           chart.addSubtitle(tt);
         */
        ChartUtilities.applyCurrentTheme(chart);

        return chart;
    }

    public JPanel CreatePanel()
    {
        return new ChartPanel(m_chart);
    }
} // class AnglePanel
