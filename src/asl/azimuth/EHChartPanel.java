package asl.azimuth;

import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import java.io.File;
import java.io.IOException;

import java.util.Collection;
import java.util.Date;

import javax.swing.JFileChooser;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.ExtensionFileFilter;

/**
 * Extension wrapper to ChartPanel that implements SaveAs right click.
 * 
 */

public class EHChartPanel 
extends ChartPanel
{

    private static final long serialVersionUID = 1L;
    JFileChooser              _fileChooser;
    JFreeChart                _chart;
    private Collection<EHChartPanel>  associates = null;

    /**
     * Constructor that extends ChartPanel
     * @param chart The chart that we are extending
     */
    public EHChartPanel(JFreeChart chart)
    {
        super(chart);
        _chart = chart;
        setRangeZoomable(false);
        setVerticalAxisTrace(true);
        setHorizontalAxisTrace(true);
    }

    /**
     * Display's a cross hair at the given time point
     * @param newTimePos  time point we want a crosshair to appear
     */
    public void setCrossHairTime(Date newTimePos)
    {
        if (getChart() != null) 
        {
            XYPlot plot = (XYPlot) getChart().getPlot();
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
        if (getChart() != null) 
        {
            XYPlot plot = (XYPlot) getChart().getPlot();
            double xx = plot.getDomainCrosshairValue();
            return new Date((long)xx);
        }
        else
            return new Date(0);
    } // getCrossHairTime

    public void setAssociates(Collection<EHChartPanel> associates)
    {
        this.associates = associates;
    }

    public Collection<EHChartPanel> getAssociates()
    {
        return this.associates;
    }

    public void mouseEntered(MouseEvent event)
    {
        setVerticalAxisTrace(true);
        setHorizontalAxisTrace(true);
        super.mouseEntered(event);
    }

    public void mouseExited(MouseEvent event)
    {
        setVerticalAxisTrace(false);
        setHorizontalAxisTrace(false);
        super.mouseExited(event);
    }

    public void mouseMoved(MouseEvent event)
    {
        // TODO: Draw vertical traces and selections on associate windows
        super.mouseMoved(event);
        if (this.associates != null)
        {
            Line2D vtl = getVerticalTraceLine();
            //Date date = getCrossHairTime();
            for (EHChartPanel associate: associates) {
                if (associate != this) {
                    //associate.setHorizontalAxisTrace(false);
                    //associate.setVerticalAxisTrace(true);
                    associate.setVerticalTraceLine(vtl);
                    //associate.setCrossHairTime(date);
                    associate.getChart().fireChartChanged();
                }
            }
        }
    }

    public void mouseClicked(MouseEvent event)
    {
        if (event.getButton() == MouseEvent.BUTTON2) {
            getChart().getXYPlot().getDomainAxis().setAutoRange(true);
            getChart().getXYPlot().configureDomainAxes();
            getChart().getXYPlot().getRangeAxis().setAutoRange(true);
            getChart().getXYPlot().configureRangeAxes();
            if (this.associates != null)
            {
                for (EHChartPanel associate: associates) {
                    if (associate != this) {
                        associate.getChart().getXYPlot().getDomainAxis().setAutoRange(true);
                        associate.getChart().getXYPlot().configureDomainAxes();
                        associate.getChart().getXYPlot().getRangeAxis().setAutoRange(true);
                        associate.getChart().getXYPlot().configureRangeAxes();
                    }
                }
            }
        }
        else {
            super.mouseClicked(event);
        }
    }

    public void zoomSuper(Rectangle2D selection)
    {super.zoom(selection);}
    public void zoomInBothSuper(double x, double y)
    {super.zoomInBoth(x,y);}
    public void zoomInDomainSuper(double x, double y)
    {super.zoomInDomain(x,y);}
    public void zoomInRangeSuper(double x, double y)
    {super.zoomInRange(x,y);}
    public void zoomOutBothSuper(double x, double y)
    {super.zoomOutBoth(x,y);}
    public void zoomOutDomainSuper(double x, double y)
    {super.zoomOutDomain(x,y);}
    public void zoomOutRangeSuper(double x, double y)
    {super.zoomOutRange(x,y);}

    // TODO: Implement a zoom the re-loads data
    //       - We need to calculate the Date window from the
    //         indices we are given in each of these ranges
    public void zoomData(Rectangle2D selection)
    {;}
    public void zoomInBothData(double x, double y)
    {;}
    public void zoomInDomainData(double x, double y)
    {;}
    public void zoomInRangeData(double x, double y)
    {;}
    public void zoomOutBothData(double x, double y)
    {;}
    public void zoomOutDomainData(double x, double y)
    {;}
    public void zoomOutRangeData(double x, double y)
    {;}


    public void zoom(Rectangle2D selection)
    {
        super.zoom(selection);
        if (this.associates != null)
        {
            for (EHChartPanel associate: associates) {
                if (associate != this) {
                    associate.zoomSuper(selection);
                }
            }
        }
    }

    public void zoomInBoth(double x, double y)
    {
        super.zoomInBoth(x, y);
        if (this.associates != null)
        {
            for (EHChartPanel associate: associates) {
                if (associate != this) {
                    associate.zoomInBothSuper(x, y);
                }
            }
        }
    }

    public void zoomInDomain(double x, double y)
    {
        super.zoomInDomain(x, y);
        if (this.associates != null)
        {
            for (EHChartPanel associate: associates) {
                if (associate != this) {
                    associate.zoomInDomainSuper(x, y);
                }
            }
        }
    }

    public void zoomInRange(double x, double y)
    {
        super.zoomInRange(x, y);
        if (this.associates != null)
        {
            for (EHChartPanel associate: associates) {
                if (associate != this) {
                    associate.zoomInRangeSuper(x, y);
                }
            }
        }
    }

    public void zoomOutBoth(double x, double y)
    {
        super.zoomOutBoth(x, y);
        if (this.associates != null)
        {
            for (EHChartPanel associate: associates) {
                if (associate != this) {
                    associate.zoomOutBothSuper(x, y);
                }
            }
        }
    }

    public void zoomOutDomain(double x, double y)
    {
        super.zoomOutDomain(x, y);
        if (this.associates != null)
        {
            for (EHChartPanel associate: associates) {
                if (associate != this) {
                    associate.zoomOutDomainSuper(x, y);
                }
            }
        }
    }

    public void zoomOutRange(double x, double y)
    {
        super.zoomOutRange(x, y);
        if (this.associates != null)
        {
            for (EHChartPanel associate: associates) {
                if (associate != this) {
                    associate.zoomOutRangeSuper(x, y);
                }
            }
        }
    }

    /**
     * Implements the SaveAs right click popup
     * @exception IOException propagates up IOException rather than handle it
     */
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
