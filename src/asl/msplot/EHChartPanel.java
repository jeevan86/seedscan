package asl.msplot;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.ui.ExtensionFileFilter;

public class EHChartPanel
    extends ChartPanel
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    JFileChooser _fileChooser;
    JFreeChart _chart;

    public EHChartPanel(JFreeChart chart)
    {
        super(chart);
        _chart = chart;
    }

    public void doSaveAs() throws IOException
    {
        _fileChooser = new JFileChooser();
        ExtensionFileFilter filterPNG = new ExtensionFileFilter("PNG Image Files",
                ".png");
        ExtensionFileFilter filterJPG = new ExtensionFileFilter("JPG Image Files",
                ".jpg");
        _fileChooser.addChoosableFileFilter(filterPNG);
        _fileChooser.addChoosableFileFilter(filterJPG);
        _fileChooser
            .addChoosableFileFilter(new ExtensionFileFilter("All files", ""));

        int option = _fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            String filename = _fileChooser.getSelectedFile().getPath();
            if (isEnforceFileExtensions())
            {
                if (_fileChooser.getFileFilter() == filterPNG)
                {
                    if (!(filename.endsWith(".png")))
                    {
                        filename = filename + ".png";
                    }
                } else if (_fileChooser.getFileFilter() == filterJPG)
                {
                    if (!(filename.endsWith(".jpg")))
                    {
                        filename = filename + ".jpg";
                    }
                }
            }
            if (filename.endsWith(".png"))
            {
                ChartUtilities.saveChartAsPNG(new File(filename), _chart, getWidth(),
                        getHeight());
            } else
            {
                ChartUtilities.saveChartAsJPEG(new File(filename), _chart, getWidth(),
                        getHeight());
            }
        }
    }
}
