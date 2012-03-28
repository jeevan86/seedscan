package asl.azimuth;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.dial.DialPlot;
import org.jfree.ui.ExtensionFileFilter;

/**
 * Popup display plot of the Azimuth angle correction, and plots of angle offset and correlation values.
 *
 * @author fshelly
 */
public class AzAngleDisplay 
extends JDialog 
implements ActionListener, ComponentListener
{

    /**
     * 1 Initial Version
     */
    private static final long serialVersionUID = 1L;

    private AZprefs prefs;

    private JButton             saveButton;
    private JButton             doneButton;
    private JFormattedTextField notesField;

    private JPanel              displayPanel = null;
    private AnglePanel          anglePlot = null;
    //private MeterPanel          meterPlot = null;
    private JPanel              anglePanel;
    private JPanel              sizingPanel = null;

    private JPanel              thetaViewBufferJPanel;
    private JPanel              thetaViewJPanel;
    private ThetaPlotter        thetaPlot;

    public static final int     DEFAULT_WIDTH  = 700;
    public static final int     DEFAULT_HEIGHT = 900; 
    public static final int     MINIMUM_WIDTH  = 450;
    public static final int     MINIMUM_HEIGHT = 800; 
    public static final int     MAXIMUM_DIAL_SIZE = 400; 

    /**
     * Constructor for creating a popup window displaying Angle offset and theta/correlation plot
     * 
     * @param owner			JFrame of the parent display window
     * @param refStation		Reference station code name
     * @param refNetwork		Reference station network code
     * @param refChannel		Reference channel code
     * @param refLocation		Reference channel location code
     * @param northStation	North station code name
     * @param northNetwork	North station network code
     * @param northChannel	North channel code
     * @param northLocation	North channel location code
     * @param eastStation		East station code name
     * @param eastNetwork		East station network code
     * @param eastChannel		East channel code
     * @param eastLocation	East channel location code
     * @param reference		Angle in degrees of known reference instrument orientation
     * @param offset			Offset angle in degrees from reference instrument
     * @param theta			Array of theta angle offsets calculated over time
     * @param correlation		Array of correlation values corresponding to each theta
     * @param std_deviation	The standard deviation in the theta array
     * @param best_correlation The correlation value of the chosen offset
     * @param firstTheta		The date for the first theta array value
     * @param interval_sec	The delta time between each theta array value in seconds
     */
    public AzAngleDisplay(JFrame owner,
            String refStation, String refNetwork, String refChannel, String refLocation,
            String northStation, String northNetwork, String northChannel, String northLocation,
            String eastStation, String eastNetwork, String eastChannel, String eastLocation,
            double reference, double offset,
            double [] theta, double [] correlation, 
            double std_deviation, double best_correlation,
            Date firstTheta, double interval_sec)
    {
        super(owner, "Display Azimuth offset angle", true);
        setIconImage(Resources.getAsImageIcon("resources/icons/chart.png", 128, 128).getImage());
        prefs = new AZprefs();

        // Normalize angles to +/0 180 degrees
        reference = Normalize360(reference);
        for (int i=0; i < theta.length; i++)
        {
            if (Math.abs(offset) < MeterPanel.DELTA_MAX)
                theta[i] = Normalize180(theta[i]);
            else
                theta[i] = Normalize360(theta[i]);
        }

        // ======== this ========
        setLayout(new BorderLayout());
        setSize(prefs.GetAngleWidth(), prefs.GetAngleHeight());
        setBounds(prefs.GetAngleOriginX(), prefs.GetAngleOriginY(),
                prefs.GetAngleWidth(), prefs.GetAngleHeight());
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout()); 
        setMinimumSize(new Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT)); // Minimum Window Size

        displayPanel = new JPanel();
        displayPanel.setLayout(new BorderLayout());
        add(displayPanel, BorderLayout.CENTER);

        sizingPanel = new JPanel();
        offset = Normalize360(offset);
        anglePlot = new AnglePanel(reference, offset);
        anglePanel = anglePlot.CreatePanel();
        sizingPanel.addComponentListener(this);
        sizingPanel.setBorder(new EmptyBorder(10,10,10,10));
        anglePanel.setMinimumSize(new Dimension(300, 300));
        anglePanel.setPreferredSize(new Dimension(MAXIMUM_DIAL_SIZE, MAXIMUM_DIAL_SIZE));
        anglePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createRaisedBevelBorder(),
                    BorderFactory.createLoweredBevelBorder()));
        sizingPanel.add(anglePanel);
        displayPanel.add(sizingPanel, BorderLayout.NORTH);

        thetaPlot = new ThetaPlotter("Offset Angle",
                refStation, refNetwork, refChannel, refLocation,
                northStation, northNetwork, northChannel, northLocation,
                eastStation, eastNetwork, eastChannel, eastLocation,
                offset, std_deviation, best_correlation);
        thetaViewJPanel = thetaPlot.createTimePanel();
        thetaViewJPanel.setPreferredSize(new Dimension(prefs.GetAngleWidth(), 350));
        thetaViewJPanel.setMinimumSize(new Dimension(DEFAULT_WIDTH, 200));
        thetaViewJPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        thetaViewBufferJPanel = new JPanel();
        thetaViewBufferJPanel.setLayout(new BorderLayout(5,5));
        thetaViewBufferJPanel.add(thetaViewJPanel, BorderLayout.CENTER);
        thetaViewBufferJPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        displayPanel.add(thetaViewBufferJPanel, BorderLayout.CENTER);

        thetaPlot.AddNewTheta(theta, interval_sec, firstTheta, 1);
        thetaPlot.AddNewCorl(correlation, interval_sec, firstTheta, 1);


        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        displayPanel.add(controlPanel, BorderLayout.SOUTH);

        JPanel notesPanel = new JPanel();
        notesPanel.setLayout(new BoxLayout(notesPanel, BoxLayout.X_AXIS));
        notesField = new JFormattedTextField();
        notesField.setValue("Notes:");
        notesField.setName("notesField");
        notesPanel.setMinimumSize(new Dimension(DEFAULT_WIDTH, 40));    
        notesPanel.setMaximumSize(new Dimension(prefs.GetMainWidth()+500, 40));    
        notesPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
        notesPanel.add(notesField);
        controlPanel.add(notesPanel, BorderLayout.NORTH);

        saveButton = new JButton("Save Image", Resources.getAsImageIcon("resources/icons/save.png", 20, 20));
        saveButton.addActionListener(this);
        doneButton = new JButton("   Done   ", Resources.getAsImageIcon("resources/icons/ok.png", 20, 20));
        doneButton.addActionListener(this);
        JPanel panelButton = new JPanel();
        panelButton.setLayout(new BorderLayout(5,5));
        panelButton.setBorder(new EmptyBorder(10, 10, 5, 10));    
        panelButton.add(saveButton, BorderLayout.WEST);
        panelButton.add(doneButton, BorderLayout.EAST);
        panelButton.setMinimumSize(new Dimension(DEFAULT_WIDTH, 45));
        panelButton.setMaximumSize(new Dimension(prefs.GetMainWidth()+500, 45));
        controlPanel.add(panelButton, BorderLayout.SOUTH);

    } // AzAngleDisplay() constructor

    /**
     * Implements action listener to perform the functions of all buttons
     * 
     * @param e	The action which caused this routine to be called
     */
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if (source == saveButton)
        {
            Dimension size = this.getSize();
            BufferedImage image = (BufferedImage) this.createImage(size.width,
                    size.height);
            Graphics g = image.getGraphics();
            this.paint(g);
            g.dispose();

            JFileChooser fileChooser = new JFileChooser();
            ExtensionFileFilter filterPNG = new ExtensionFileFilter(
                    "PNG Image Files", ".png");
            ExtensionFileFilter filterJPG = new ExtensionFileFilter(
                    "JPG Image Files", ".jpg");
            fileChooser.addChoosableFileFilter(filterPNG);
            fileChooser.addChoosableFileFilter(filterJPG);
            fileChooser.setFileFilter(filterJPG);
            fileChooser.setCurrentDirectory(new File(prefs.GetImageDir()));
            int option = fileChooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION)
            {
                String filename = fileChooser.getSelectedFile().getPath();
                if (fileChooser.getFileFilter() == filterPNG)
                {
                    if (!(filename.endsWith(".png")))
                    {
                        filename = filename + ".png";
                    }
                } else if (fileChooser.getFileFilter() == filterJPG)
                {
                    if (!(filename.endsWith(".jpg")))
                    {
                        filename = filename + ".jpg";
                    }
                }

                try
                {
                    prefs.SetImageDir(fileChooser.getCurrentDirectory().getAbsolutePath());
                    if (filename.endsWith(".png"))
                    {
                        ImageIO.write(image, "png", new File(filename));
                    } else
                    {
                        ImageIO.write(image, "jpg", new File(filename));
                    }
                } catch (IOException exc)
                {
                    System.err.println("Unable to save window image to " + filename);
                    exc.printStackTrace();
                }
            } // User said save
        } // SaveButton pushed
        else if (source == doneButton)
        {
            prefs.SetAngleOriginX(this.getX());
            prefs.SetAngleOriginY(this.getY());
            prefs.SetAngleHeight(this.getHeight());
            prefs.SetAngleWidth(this.getWidth());
            prefs.SavePrefs();
            setVisible(false);
        }
    } // actionPerformed()

    /**
     * Utility routine to keep an angle between 0 and 360 degrees
     * @param degrees Input angle
     * @return		Corrected angle between 0 and 360 degrees
     */
    public final static double Normalize360(double degrees)
    {
        double normal;
        int circles;

        circles = (int)Math.floor(degrees / 360.0);
        normal = degrees - (circles*360);
        return normal;
    }

    /**
     * Utility routine to keep an angle between +/- 180 degrees
     * @param degrees  Input angle
     * @return		 Corrected angle between +/- 180 degrees
     */
    public final static double Normalize180(double degrees)
    {
        double normal;
        int circles;

        circles = (int)Math.floor(degrees / 360.0);
        normal = degrees - (circles*360);
        if (normal > 180.0)
            normal -= 360.0;
        return normal;
    }


    public void componentHidden(ComponentEvent event)
    {;}
    public void componentMoved(ComponentEvent event)
    {;}
    public void componentResized(ComponentEvent event)
    {
        correctSize();
    }
    public void componentShown(ComponentEvent event)
    {
        correctSize();
    }

    public void correctSize()
    {
        int height = sizingPanel.getHeight();
        int width = sizingPanel.getWidth();
        int minAxis = height > width ? width : height;
        minAxis = minAxis > MAXIMUM_DIAL_SIZE ? MAXIMUM_DIAL_SIZE : minAxis;
        anglePanel.setPreferredSize(new Dimension(minAxis, minAxis));
    }

} // class AzAngleDisplay
