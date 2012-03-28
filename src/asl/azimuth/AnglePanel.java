package asl.azimuth;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.dial.DialPlot;
import org.jfree.chart.plot.dial.DialPointer;
import org.jfree.chart.plot.dial.DialTextAnnotation;
import org.jfree.chart.plot.dial.StandardDialFrame;
import org.jfree.chart.plot.dial.StandardDialScale;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;


/**
 * Creates an panel for displaying a Circle with lines indicating azimuth angle offsets
 * 
 * @author fshelly
 */
public class AnglePanel
{

    /**
     * 1  Initial version
     */
    private static final long serialVersionUID = 1L;

    private JFreeChart          m_chart;
    private double              offset=0;
    private double              reference=0;

    /**
     * Angles in degrees
     */
    public static final double  DELTA_MAX = 20.0;
    public static final double  DELTA_WARN = 5.0;
    public static final double  DELTA_GOOD = 1.0;


    /**
     * Creates a new Circle plot.
     * 
     * @param _reference 
     *        instrument angle in degrees
     * @param _offset 
     *        relative angle of test instrument in degrees    
     */
    public AnglePanel(double _reference, double _offset)
    {
        offset = AzAngleDisplay.Normalize180(_offset);
        reference = AzAngleDisplay.Normalize360(_reference);

        m_chart = createChart(offset,
                new DefaultValueDataset(reference),
                new DefaultValueDataset(AzAngleDisplay.Normalize360(_offset + _reference)));
    }

    /**
     * Creates a chart.
     * 
     * @param offset
     *          degrees rotation difference between instrument and reference
     * 
     * @param datasetReference
     *          plot dataset for the Reference Instrument
     *          plot dataset for the final angle
     *          
     * @return A chart.
     */
    private JFreeChart createChart(double offset,
            DefaultValueDataset datasetReference,
            DefaultValueDataset datasetFinal)
    {

        DialPlot plot = new DialPlot();
        plot.setView(0.0, 0.0, 1.0, 1.0);
        plot.setDataset(0, datasetReference);
        plot.setDataset(1, datasetFinal);
        StandardDialFrame dialFrame = new StandardDialFrame();
        plot.setDialFrame(dialFrame);    

        StandardDialScale angleScale = new StandardDialScale(0, 360, 
                90, -360, 30.0, 5);
        angleScale.setFirstTickLabelVisible(false);
        angleScale.setTickRadius(0.88);
        angleScale.setTickLabelOffset(0.15);
        angleScale.setTickLabelFont(new Font("Dialog", Font.PLAIN, 14));
        plot.addScale(0, angleScale);
        plot.mapDatasetToScale(1, 0);

        DialPointer needleReference = new DialPointer.Pin(0);
        needleReference.setRadius(0.60);
        plot.addLayer(needleReference);

        DialPointer needleFinal = new DialPointer.Pointer(1);
        needleFinal.setRadius(0.55);
        plot.addLayer(needleFinal);


        String RefStr = String.format("   Ref %.3f", reference);
        String OffStr = String.format("Offset %.3f", offset);
        String ResultStr = String.format("Result %.3f", 
                AzAngleDisplay.Normalize360(reference + offset));
        DialTextAnnotation annotationRef = new DialTextAnnotation(
                RefStr);
        annotationRef.setFont(new Font("Dialog", Font.BOLD, 12));
        annotationRef.setRadius(0.40);
        annotationRef.setPaint(Color.red);
        plot.addLayer(annotationRef);
        DialTextAnnotation annotationOff = new DialTextAnnotation(
                OffStr);
        annotationOff.setFont(new Font("Dialog", Font.BOLD, 12));
        annotationOff.setRadius(0.50);
        annotationOff.setPaint(Color.blue);
        plot.addLayer(annotationOff);
        DialTextAnnotation annotationResult = new DialTextAnnotation(
                ResultStr);
        annotationResult.setFont(new Font("Dialog", Font.BOLD, 14));
        annotationResult.setRadius(0.60);
        plot.addLayer(annotationResult);

        JFreeChart chart = new JFreeChart("Azimuth Offset",
                JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        String refMsg = String.format("Reference Angle %.3f + Offset Angle %.3f = %.3f degrees", 
                reference, offset, AzAngleDisplay.Normalize360(reference + offset));
        TextTitle tt = new TextTitle(refMsg,
                new Font("Dialog", Font.PLAIN, 11));
        tt.setPosition(RectangleEdge.BOTTOM);
        tt.setHorizontalAlignment(HorizontalAlignment.CENTER);
        tt.setMargin(0.0, 4.0, 0.0, 4.0);
        chart.addSubtitle(tt);

        ChartUtilities.applyCurrentTheme(chart);

        return chart;

    }

    /**
     * Creates a JPanel needed to display an AnglePanel plot
     * @return a JPanel representing AnglePanel plot
     */
    public JPanel CreatePanel()
    {
        return new ChartPanel(m_chart);
    }

} // class AnglePanel
