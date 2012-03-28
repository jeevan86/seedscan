package asl.azimuth;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import asl.seedsplitter.ChannelSelector;
import asl.seedsplitter.ContiguousBlock;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.SequenceRangeException;

/**
 * Top level class for building the Azimuth GUI
 * 
 * @author  fshelly
 */

public class AZdisplay
{
    public static final int DEFAULT_WIDTH = 1150;
    public static final int DEFAULT_HEIGHT = 800;
    public static AZprefs prefs;
    private MainFrame frame;

    /**
     * Constructor that gets called to start the GUI
     */
    public AZdisplay()
    {
        prefs = new AZprefs();
        WindowListener listener = new Terminator();
        frame = new MainFrame();
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.addWindowListener(listener);
        frame.setVisible(true);
    } // ASPdisplay() constructor

    /**
     * Class called when the GUI exit button is clicked.
     * Allows GUI to save persistent state information for next launch.
     */
    private class Terminator extends WindowAdapter
    {
        public void windowClosing(WindowEvent e)
        {
            frame.SavePrefs();
        }
    } // class Terminator

} // class AZdisplay

/**
 * Top level frame for seed transfer GUI, everything else fits inside.
 */
class MainFrame extends JFrame implements ActionListener, FocusListener,
      ChangeListener, PropertyChangeListener
{
    private static final long serialVersionUID = 1L;
    private ArrayList<EHChartPanel> plotPanels = null;
    public static final int MINIMUM_WIDTH = 700;
    public static final int MINIMUM_HEIGHT = 850;

    /**
     * Constructor for MainFrame.  Top level Gui creation routine
     */
    public MainFrame()
    {
        setIconImage(Resources.getAsImageIcon("resources/icons/chart.png", 128, 128).getImage());

        prefs = new AZprefs();
        setTitle("Azimuth Instrument Determination");
        setMinimumSize(new Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT));
        setPreferredSize(new Dimension(prefs.GetMainWidth(), prefs.GetMainHeight()));
        setBounds(prefs.GetMainOriginX(), prefs.GetMainOriginY(), 
                prefs.GetMainWidth(), prefs.GetMainHeight());

        seedFileDir = prefs.GetLocalDir();

        // ======== this ========
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        // ======== ViewJPanel ========
        northSegmentPlot = new SegmentPlotter("North",
                "North", mRefNetwork, "LH1", mRefLocation);
        northViewJPanel = northSegmentPlot.createTimePanel();
        northViewJPanel.setMinimumSize(new Dimension(600, 160));
        northViewJPanel.setPreferredSize(new Dimension(prefs.GetMainWidth(),
                    prefs.GetMainHeight()));
        northViewJPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        northViewBufferJPanel = new JPanel();
        northViewBufferJPanel.setLayout(new BorderLayout());
        northViewBufferJPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        northViewBufferJPanel.add(northViewJPanel, BorderLayout.CENTER);
        add(northViewBufferJPanel);

        eastSegmentPlot = new SegmentPlotter("East",
                "EAST", mRefNetwork, "LH2", mRefLocation);
        eastViewJPanel = eastSegmentPlot.createTimePanel();
        eastViewJPanel.setMinimumSize(new Dimension(600, 160));
        eastViewJPanel.setPreferredSize(new Dimension(prefs.GetMainWidth(),
                    prefs.GetMainHeight()));
        eastViewJPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        eastViewBufferJPanel = new JPanel();
        eastViewBufferJPanel.setLayout(new BorderLayout());
        eastViewBufferJPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        eastViewBufferJPanel.add(eastViewJPanel, BorderLayout.CENTER);
        add(eastViewBufferJPanel);

        referenceSegmentPlot = new SegmentPlotter("Reference",
                "REF", mRefNetwork, "LHZ", mRefLocation);
        referenceViewJPanel = referenceSegmentPlot.createTimePanel();
        referenceViewJPanel.setMinimumSize(new Dimension(600, 160));
        referenceViewJPanel.setPreferredSize(new Dimension(prefs.GetMainWidth(),
                    prefs.GetMainHeight()));
        referenceViewJPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        referenceViewBufferJPanel = new JPanel();
        referenceViewBufferJPanel.setLayout(new BorderLayout());
        referenceViewBufferJPanel.setBorder(new EmptyBorder(20, 10, 10, 10));
        referenceViewBufferJPanel.add(referenceViewJPanel, BorderLayout.CENTER);
        add(referenceViewBufferJPanel);

        plotPanels = new ArrayList<EHChartPanel>(3);
        plotPanels.add((EHChartPanel)northViewJPanel);
        plotPanels.add((EHChartPanel)eastViewJPanel);
        plotPanels.add((EHChartPanel)referenceViewJPanel);
        ((EHChartPanel)northViewJPanel).setAssociates(plotPanels);
        ((EHChartPanel)eastViewJPanel).setAssociates(plotPanels);
        ((EHChartPanel)referenceViewJPanel).setAssociates(plotPanels);

        JPanel panelSlider = new JPanel();
        panelSlider.setLayout(new BoxLayout(panelSlider, BoxLayout.X_AXIS));
        panelSlider.setBorder(new EmptyBorder(20, 10, 5, 10));
        leftSlider = new JSlider(0, 1000, iLeftSliderValue);
        leftSlider.setInverted(true);
        leftSlider.addChangeListener(this);
        panelSlider.add(leftSlider);
        rightSlider = new JSlider(0, 1000, iRightSliderValue);
        rightSlider.addChangeListener(this);
        panelSlider.add(rightSlider);
        add(panelSlider);

        JPanel panelTime = new JPanel();
        panelTime.setLayout(new BoxLayout(panelTime, BoxLayout.X_AXIS));
        panelTime.setBorder(new EmptyBorder(5, 10, 10, 10));
        zoomInButton = new JButton("Zoom In ", Resources.getAsImageIcon("resources/icons/zoom-in.png", 20, 20));
        zoomInButton.addActionListener(this);
        zoomInButton.setMaximumSize(zoomInButton.getPreferredSize());
        panelTime.add(zoomInButton);
        panelTime.add(Box.createHorizontalGlue());
        startTime = new JLabel();
        startTime.setAlignmentX(LEFT_ALIGNMENT);
        startTime.setText("Segment start time");
        endTime = new JLabel();
        endTime.setAlignmentX(RIGHT_ALIGNMENT);
        endTime.setText("Segment end time");
        panelTime.add(startTime);
        panelTime.add(Box.createHorizontalStrut(20));
        panelTime.add(endTime);
        panelTime.add(Box.createHorizontalGlue());
        zoomOutButton = new JButton("Zoom Out", Resources.getAsImageIcon("resources/icons/zoom-out.png", 20, 20));
        zoomOutButton.addActionListener(this);
        zoomOutButton.setMaximumSize(zoomOutButton.getPreferredSize());
        panelTime.add(zoomOutButton);
        add(panelTime);

        segmentCombo = new JComboBox();
        segmentCombo.setEditable(false);
        segmentCombo.setBorder(new EmptyBorder(10, 10, 10, 10));
        segmentCombo.addActionListener(this);
        add(segmentCombo);

        JPanel panelProgress = new JPanel();
        panelProgress.setLayout(new BorderLayout());
        panelProgress.setBorder(new EmptyBorder(20, 10, 20, 10));
        inverterProgress = new JProgressBar(SwingConstants.HORIZONTAL);
        panelProgress.add(inverterProgress, BorderLayout.NORTH);
        add(panelProgress);

        JPanel panelReference = new JPanel();
        panelReference.setLayout(new BoxLayout(panelReference, BoxLayout.X_AXIS));
        panelReference.setBorder(new EmptyBorder(0, 10, 0, 10));
        panelReference.setPreferredSize(new Dimension(prefs.GetMainWidth(), 50));
        panelReference.setMaximumSize(new Dimension(prefs.GetMainWidth()+500, 50));    
        refAngleField = new JFormattedTextField();
        refAngleField.setValue("0.0");
        refAngleField.setColumns(8);
        refAngleField.setName("refAngleField");
        JLabel labelSeconds = new JLabel("Reference Angle: ",  JLabel.TRAILING);
        labelSeconds.setLabelFor(refAngleField);
        panelReference.add(labelSeconds);
        panelReference.add(refAngleField);
        refAngleField.addFocusListener(this);
        add(panelReference);

        JPanel panelButton = new JPanel();
        panelButton.setLayout(new BoxLayout(panelButton, BoxLayout.X_AXIS));
        panelButton.setBorder(new EmptyBorder(10, 10, 10, 10));
        panelButton.setPreferredSize(new Dimension(prefs.GetMainWidth(), 60));
        panelButton.setMaximumSize(new Dimension(prefs.GetMainWidth()+500, 60));
        filesButton = new JButton("Files", Resources.getAsImageIcon("resources/icons/folder.png", 20, 20));
        filesButton.addActionListener(this);
        filesButton.setMaximumSize(filesButton.getPreferredSize());
        panelButton.add(filesButton);
        panelButton.add(Box.createHorizontalStrut(20));
        generateButton = new JButton("Generate", Resources.getAsImageIcon("resources/icons/chart.png", 20, 20));
        generateButton.addActionListener(this);
        //generateButton.setEnabled(false);
        panelButton.add(generateButton);
        panelButton.add(Box.createHorizontalStrut(20));
        cancelButton = new JButton("Cancel", Resources.getAsImageIcon("resources/icons/cancel.png", 20, 20));
        cancelButton.addActionListener(this);
        //cancelButton.setEnabled(true);
        panelButton.add(cancelButton);

        panelButton.add(Box.createHorizontalGlue());
        quitButton = new JButton("Quit", Resources.getAsImageIcon("resources/icons/exit.png", 20, 20));
        quitButton.addActionListener(this);
        panelButton.add(quitButton);
        add(panelButton);

        // Status field
        statusField = new JTextField();
        statusField.setEditable(false);
        statusField.setText("Initializing...");
        add(statusField);

        updateGui();

        // This should never be deleted. Rather, the user will can
        // re-select files if they wish, otherwise the previous
        // selection and data is retained.
        channelSelector = new ChannelSelector(this, prefs.GetMainOriginX(), 
                prefs.GetMainOriginY(), seedFileDir,
                3 /*channelCount*/);
        channelSelector.setWindowIcon(Resources.getAsImageIcon("resources/icons/chart.png", 128, 128).getImage());
        channelSelector.setAddFilesButtonIcon(Resources.getAsImageIcon("resources/icons/add.png", 20, 20));
        channelSelector.setRemoveSelectedButtonIcon(Resources.getAsImageIcon("resources/icons/remove.png", 20, 20));
        channelSelector.setReadFilesButtonIcon(Resources.getAsImageIcon("resources/icons/load.png", 20, 20));
        channelSelector.setCancelReadButtonIcon(Resources.getAsImageIcon("resources/icons/delete.png", 20, 20));
        channelSelector.setOkButtonIcon(Resources.getAsImageIcon("resources/icons/start.png", 20, 20));
        channelSelector.setCancelButtonIcon(Resources.getAsImageIcon("resources/icons/stop.png", 20, 20));
        channelSelector.setChannelLabel(0, "1/North: ");
        channelSelector.setChannelLabel(1, "2/East: ");
        channelSelector.setChannelLabel(2, "Z/Reference: ");
        FileProcess();

        timerGraph = new GraphUpdate();
        t = new Timer(50, timerGraph);
    } // constructor MainFrame()

    /**
     * Whenever the user has selected a new set of file data to process this gets called to update plots and data structures.
     */
    private void FileProcess()
    {
        ArrayList<ContiguousBlock> fileBlocks = null;
        // Start up the file selection dialog
        channelSelector.setVisible(true);

        // Handle user hit the Cancel button
        if (channelSelector.GetCancel())
        {
            statusField.setText("File selection canceled, select again...");
        }
        else
        {
            seedFileDir = channelSelector.GetDefaultDirectory();        
        }

        if (!channelSelector.GetProcessed())  // Handle user hit the OK button
        {
            statusField.setText("Failed to process azimuth data files, select again...");
        }
        else if (channelSelector.GetBlocks() == null)
        {
            statusField.setText("Failed to find a large enough contiguous block, select again...");      
        }
        else
        {
            fileBlocks = channelSelector.GetBlocks();
            dataSets = channelSelector.GetDataSets();
        }

        // If we have data then process it
        if (fileBlocks != null && dataSets != null)
        {
            segmentCombo.removeAllItems();

            SegmentPlotter[] segPlots = { northSegmentPlot, eastSegmentPlot, referenceSegmentPlot };
            for (int i=0; i < 3; i++)
            {
                ArrayList<DataSet> dataList = dataSets.get(i);
                DataSet dataSet = dataList.get(0);
                segPlots[i].SetTitle(dataSet.getStation(), dataSet.getNetwork(),
                        dataSet.getChannel(), dataSet.getLocation());
            }
            this.setVisible(true);

            // Reinitialize in case we are doing a new set of files
            factory = null;
            result = null;
            iMarginSlider=100;
            iLeftSliderValue=1000;
            iRightSliderValue=1000;
            leftSliderDate=null;
            rightSliderDate=null;
            blocks = null;
            contBlocks = new ArrayList<FilterBlock>();
            selectBlock = null;
            finalData[0] = null;
            finalData[1] = null;
            finalData[2] = null;
            finalBlock = null;
            zoomBlock = null;

            // Only use segments that have enough data
            blocks = new ArrayList<ContiguousBlock>();
            for (ContiguousBlock block : fileBlocks)
            {
                long dataPoints = (block.getEndTime() - block.getStartTime()) 
                    / block.getInterval();

                if (dataPoints < MIN_DATAPOINTS)
                {
                    continue;
                }
                blocks.add(block);
            }
            if (blocks.size() == 0)
            {
                statusField.setText("Failed to find a large enough contiguous block, select again...");
                return;
            }

            statusField.setText("Azimuth data files contained "
                    + blocks.size() + " seperate time segments, filtering data...");

            // Fill combo box up with list of contiguous segments
            FilterBlock largestBlock = null;
            FilterBlock firstBlock = null;
            int iLargestBlock=-1;
            int blockCount=0;
            long totalDataPoints = 0;
            for (ContiguousBlock block : blocks)
            {
                long dataPoints = (block.getEndTime() - block.getStartTime()) 
                    / block.getInterval();
                totalDataPoints += dataPoints;
                Date startTime = new Date(block.getStartTime()/1000);
                Date endTime = new Date(block.getEndTime()/1000);
                blockCount++;

                int[][] channels = { null, null, null };
                for (int i = 0; i < 3; i++)
                {
                    boolean found = false;
                    for (DataSet set : dataSets.get(i))
                    {

                        if ((!found)
                                && set.containsRange(block.getStartTime(), block.getEndTime()))
                        {
                            try
                            {
                                channels[i] = set.getSeries(
                                        block.getStartTime(), block.getEndTime());
                            } catch (SequenceRangeException e)
                            {
                                ;
                            }
                            found = true;
                            break;
                        }
                    } // loop through each dataSet
                } // for each of the 3 data channels
                statusField.setText("Azimuth data files contained "
                        + blocks.size() 
                        + " seperate time segments, filtering data[" + blockCount + "] " 
                        + startTime.toString());
                FilterBlock newCont = new FilterBlock(
                        block.getStartTime(), block.getEndTime(),
                        block.getInterval(), channels);
                contBlocks.add(newCont);

                if ((largestBlock == null)
                        || (largestBlock.getRange() < block.getRange()))
                {
                    largestBlock = newCont;
                    iLargestBlock = segmentCombo.getItemCount();
                }
                segmentCombo.insertItemAt(startTime + " - " + endTime,
                        segmentCombo.getItemCount());


                if (firstBlock == null)
                {
                    firstBlock = newCont;
                }

            } // loop through list of contiguous blocks

            // Set up some plotting parameters
            ArrayList<DataSet> dataList = dataSets.get(0);
            DataSet dataSet = dataList.get(0);

            dRate = 1000000.0 / dataSet.getInterval();

            countsPerPlot = (int)(totalDataPoints / prefs.GetMainWidth());
            if (countsPerPlot < 1)
                countsPerPlot = 1;

            northSegmentPlot.SetTitle(dataSet.getStation(), dataSet.getNetwork(),
                    dataSet.getChannel(), dataSet.getLocation());
            mNorthStation = dataSet.getStation();
            mNorthNetwork = dataSet.getNetwork();
            mNorthChannel = dataSet.getChannel();
            mNorthLocation = dataSet.getLocation();

            dataSet = dataSets.get(1).get(0);
            eastSegmentPlot.SetTitle(dataSet.getStation(), dataSet.getNetwork(),
                    dataSet.getChannel(), dataSet.getLocation());
            mEastStation = dataSet.getStation();
            mEastNetwork = dataSet.getNetwork();
            mEastChannel = dataSet.getChannel();
            mEastLocation = dataSet.getLocation();

            dataSet = dataSets.get(2).get(0);
            mRefStation = dataSet.getStation();
            mRefNetwork = dataSet.getNetwork();
            mRefChannel = dataSet.getChannel();
            mRefLocation = dataSet.getLocation();
            referenceSegmentPlot.SetTitle(dataSet.getStation(), dataSet.getNetwork(),
                    dataSet.getChannel(), dataSet.getLocation());
            referenceSegmentPlot.resetTimeData();

            // Default to the largest contiguous block being selected
            selectBlock = largestBlock;
            segmentCombo.setSelectedIndex(iLargestBlock);
            //generateButton.setEnabled(true);
            //cancelButton.setEnabled(false);
            populated = true;
            updateGui();

            for (SegmentPlotter plot : segPlots)
                plot.resetTimeData();
            DisplayPlotSegments();
            for (SegmentPlotter plot : segPlots)
                plot.SetVisible(true);
            statusField.setText("Select data to generate azimuth reading from");
        } // We have contiguous segments to process

    } // FileProcess();

    /**
     * Displays all of the contiguous plot segments in the data files
     * Implements full zoom out data structure setup.
     */
    private void DisplayPlotSegments()
    {
        statusField.setText("Plotting Seismic channel data...");
        SegmentPlotter[] segPlots = { northSegmentPlot, eastSegmentPlot, referenceSegmentPlot };
        int len = selectBlock.getFilterData(0).length;
        for (int ip=0; ip < 3; ip++)
        {
            segPlots[ip].setStartEndMarks(
                    new Date(selectBlock.getStartTime()/1000), selectBlock.getFilterData(ip)[0],
                    new Date(selectBlock.getEndTime()/1000), selectBlock.getFilterData(ip)[len-1]);
        }
        for (FilterBlock block : contBlocks)
        {
            for (int i=0; i < 3; i++)
            {
                // Add the data to the plot
                if (block == selectBlock)
                {
                    // This was the block selected by the user
                    segPlots[i].AddNewData(selectBlock.getFilterData(i), dRate,
                            new Date(selectBlock.getStartTime() / 1000l), countsPerPlot, 0);
                    finalData[i] = selectBlock.getIntData(i).clone();
                    leftSliderDate = new Date(selectBlock.getStartTime() / 1000);
                    rightSliderDate = new Date(selectBlock.getEndTime() / 1000);
                    iRightSliderValue = 1000;
                    iLeftSliderValue = 1000;
                    iMarginSlider = 1 + (1000 * MIN_DATAPOINTS + 999) 
                        / selectBlock.length;
                    if (iMarginSlider > 1000)
                        iMarginSlider = 1000;
                    rightSlider.setValue(iRightSliderValue);
                    leftSlider.setValue(iLeftSliderValue);
                    startTime.setText(leftSliderDate.toString());
                    endTime.setText(rightSliderDate.toString());
                } else
                {
                    // This was one of the non selected blocks.
                    segPlots[i].AddNewData(block.getFilterData(i), dRate,
                            new Date(block.getStartTime() / 1000l), countsPerPlot, 0);
                }
            } // loop through each instrument channel
            if (block == selectBlock)
                finalBlock = new FilterBlock(selectBlock, 0, finalData.length);
        } // loop through each contiguous segment

        //zoomOutButton.setEnabled(false);
    } // DisplayPlotSegments()

    /**
     * Data files can contain multiple contiguous segments.  This routine handles book keeping when a new segment is chosen.
     */
    private void DisplaySelectSegment()
    {
        // Loop through the three channels of data
        FilterBlock viewBlock = null;

        SegmentPlotter[] segPlots = { northSegmentPlot, eastSegmentPlot, referenceSegmentPlot };
        int iSeg0Size=0;
        int iSeg1Size=0;
        int iSeg2Size=0;
        if (zoomBlock != null)
            viewBlock = zoomBlock;
        else
            viewBlock = selectBlock;

        for (int i = 0; i < 3; i++)
        {
            iSeg0Size = viewBlock.length * (1000-iLeftSliderValue) / 1000;
            iSeg2Size = viewBlock.length * (1000-iRightSliderValue) / 1000;
            iSeg1Size = viewBlock.length - (iSeg0Size + iSeg2Size);

            int segFinal[] = new int[iSeg1Size];
            System.arraycopy(viewBlock.getIntData(i), iSeg0Size, segFinal, 0, iSeg1Size);
            finalData[i] = segFinal;
        } // loop through each plot file  

        finalBlock = new FilterBlock(viewBlock, iSeg0Size, iSeg1Size);

        for (int ip=0; ip < 3; ip++)
        {
            int len = finalData[ip].length;
            segPlots[ip].setStartEndMarks(
                    (Date)leftSliderDate.clone(), finalBlock.getFilterData(ip)[0],
                    (Date)rightSliderDate.clone(), finalBlock.getFilterData(ip)[len-1]);
        }

        startTime.setText(leftSliderDate.toString());
        endTime.setText(rightSliderDate.toString());
    } // DisplaySelectSegment()

    /**
     * Handles book keeping functions when user zooms in on a section of the plot
     */
    private void DisplayZoom()
    {
        int zoomPerPlot = (zoomBlock.length / prefs.GetMainWidth());
        if (zoomPerPlot < 1)
            zoomPerPlot = 1;

        // Loop through the three channels of data
        SegmentPlotter[] segPlots = { northSegmentPlot, eastSegmentPlot, referenceSegmentPlot };
        for (int i = 0; i < 3; i++)
        {
            segPlots[i].SetVisible(false);
            segPlots[i].resetTimeData();
            segPlots[i].AddNewData(zoomBlock.getFilterData(i), dRate,
                    new Date(zoomBlock.getStartTime() / 1000l), zoomPerPlot, 0);
            segPlots[i].SetVisible(true);
        } // loop through each plot file  

        iMarginSlider = (1000 * MIN_DATAPOINTS + 999) 
            / zoomBlock.length;
        if (iMarginSlider > 1000)
            iMarginSlider = 1000;
        iLeftSliderValue = 1000;
        iRightSliderValue = 1000;
        leftSliderDate = new Date(zoomBlock.getStartTime()/1000);
        rightSliderDate = new Date(zoomBlock.getEndTime()/1000);

        for (int ip=0; ip < 3; ip++)
        {
            int len = zoomBlock.length;
            segPlots[ip].setStartEndMarks(
                    (Date)leftSliderDate.clone(), zoomBlock.getFilterData(ip)[0],
                    (Date)rightSliderDate.clone(), zoomBlock.getFilterData(ip)[len-1]);
        }

        startTime.setText(leftSliderDate.toString());
        endTime.setText(rightSliderDate.toString());
        leftSlider.setValue(iLeftSliderValue);
        rightSlider.setValue(iRightSliderValue);
    } // DisplayZoom()

    /**
     * Saves persistent state information so that it can be retrieved the
     * next time the program is run.
     */
    public void SavePrefs()
    {
        prefs.SetLocalDir(seedFileDir);

        // Remember any changes to preferences
        prefs.SetMainOriginX(this.getX());
        prefs.SetMainOriginY(this.getY());
        prefs.SetMainHeight(this.getHeight());
        prefs.SetMainWidth(this.getWidth());

        prefs.SavePrefs();
        System.exit(0);
    } // SavePrefs()

    /**
     * Timer routine that allows the cursors to accurately reflect their current position.  
     * Needed because we have a minimum amount of data that a user can select.  
     * If the user tries to push the cursor beyond that point, this timer pushes it back.
     */
    class GraphUpdate implements ActionListener
    {
        public void actionPerformed(ActionEvent event)
        {
            if (leftSlider.getValue() != iLeftSliderValue)
            {
                if (!leftSlider.getValueIsAdjusting())
                    leftSlider.setValue(iLeftSliderValue);
            }
            if (rightSlider.getValue() != iRightSliderValue)
            {
                if (!rightSlider.getValueIsAdjusting())
                    rightSlider.setValue(iRightSliderValue);
            }

            t.stop();

            DisplaySelectSegment();
        } // ActionPerformed

    } // class GraphUpdate

    /**
     * Implements listener for slider events for data selection cursor control
     * @param e	ChangeEvent representing the slider motion detected
     */
    public void stateChanged(ChangeEvent e)
    {
        FilterBlock viewBlock = zoomBlock;
        if (viewBlock == null) viewBlock = selectBlock;
        JSlider source = (JSlider)e.getSource();
        int iMargin = iMarginSlider;
        long iDelta = (viewBlock.getEndTime() - viewBlock.getStartTime()) / 1000;
        if (source == leftSlider)
        {
            iLeftSliderValue = leftSlider.getValue();
            if (iLeftSliderValue < iMargin)
            {
                iLeftSliderValue = iMargin;
            }
            leftSliderDate = new Date(viewBlock.getStartTime()/1000
                    + (1000-iLeftSliderValue)*(iDelta/1000));
            startTime.setText(leftSliderDate.toString());
            if (iLeftSliderValue + iRightSliderValue < 1000 + iMargin)
            {
                iRightSliderValue = 1000 + iMargin - iLeftSliderValue;
                rightSliderDate = new Date(viewBlock.getEndTime()/1000
                        - (1000-iRightSliderValue)*(iDelta/1000));
                endTime.setText(rightSliderDate.toString());
                rightSlider.setValue(iRightSliderValue);
            }
            t.start();
        } // leftSlider
        else if (source == rightSlider)
        {
            iRightSliderValue = rightSlider.getValue();
            if (iRightSliderValue < iMargin)
            {
                iRightSliderValue = iMargin;
            }
            rightSliderDate = new Date(viewBlock.getEndTime()/1000
                    - (1000-iRightSliderValue)*(iDelta/1000));
            endTime.setText(rightSliderDate.toString());
            if (iLeftSliderValue + iRightSliderValue < 1000 + iMargin)
            {
                iLeftSliderValue = 1000 + iMargin - iRightSliderValue;
                leftSliderDate = new Date(viewBlock.getStartTime()/1000
                        + (1000-iLeftSliderValue)*(iDelta/1000));
                startTime.setText(leftSliderDate.toString());
                leftSlider.setValue(iLeftSliderValue);
            }
            t.start();
        } // rightSlider
    } // stateChanged()

    /**
     * Implements ActionListener for this class. Performs all button push actions. 
     */
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if (source == quitButton)
        {
            SavePrefs(); // also exits
        }
        else if (source == zoomInButton)
        {
            statusField.setText("Zooming in on selected data region...");
            zoomBlock = new FilterBlock(finalBlock, 0, finalBlock.length);
            DisplayZoom();
            statusField.setText("Done zooming in");
            //zoomOutButton.setEnabled(true);
            zoomed = true;
            updateGui();
        }
        else if (source == zoomOutButton)
        {
            statusField.setText("Zooming out to full data display...");
            zoomBlock = null;
            SegmentPlotter[] segPlots = { northSegmentPlot, eastSegmentPlot, referenceSegmentPlot };
            for (SegmentPlotter plot : segPlots)
                plot.resetTimeData();
            DisplayPlotSegments();
            for (SegmentPlotter plot : segPlots)
                plot.SetVisible(true);
            statusField.setText("Zoomed out");
            zoomed = false;
            updateGui();
        }
        else if (source == filesButton)
        {
            populated = false;
            updateGui();
            northSegmentPlot.resetTimeData();
            eastSegmentPlot.resetTimeData();
            referenceSegmentPlot.resetTimeData();
            FileProcess();
        }
        else if (source == segmentCombo)
        {
            int index = segmentCombo.getSelectedIndex();
            if (index < 0)
            {
                selectBlock = null;
                northSegmentPlot.resetTimeData();
                eastSegmentPlot.resetTimeData();
                referenceSegmentPlot.resetTimeData();
            }
            else if (selectBlock != contBlocks.get(index))
            {
                System.out.println("Selected Time segment " + index);
                selectBlock = contBlocks.get(index);
                finalBlock = new FilterBlock(selectBlock, 0, selectBlock.length);
                for (int i=0; i < 3; i++)
                {
                    finalData[i] = finalBlock.getIntData(i);
                }
                leftSliderDate = new Date(selectBlock.getStartTime() / 1000);
                rightSliderDate = new Date(selectBlock.getEndTime() / 1000);
                iRightSliderValue = 1000;
                iLeftSliderValue = 1000;
                iMarginSlider = (1000 * MIN_DATAPOINTS + 999) 
                    / selectBlock.getIntData(0).length;
                rightSlider.setValue(iRightSliderValue);
                leftSlider.setValue(iLeftSliderValue);
                DisplaySelectSegment();
            } // Item selected was not the one already selected
        } // Segment list item was selected
        else if (source == generateButton)
        {
            if (selectBlock != null)
            {
                // Convert data from int to doubles
                double [] north = Azimuth.intArrayToDoubleArray(finalData[0]);
                double [] east = Azimuth.intArrayToDoubleArray(finalData[1]);
                double [] reference = Azimuth.intArrayToDoubleArray(finalData[2]);

                //generateButton.setEnabled(false);
                //cancelButton.setEnabled(true);
                generating = true;
                updateGui();

                // Now determine the Azimuth offset
                statusField.setText("Generating Azimuth offset angle...");
                factory = new AzimuthLocator(north, east, reference);   
                result = null;
                factory.addPropertyChangeListener(this);
                factory.execute();

            } // we have data to use
        } // Generate button pushed
        else if (source == cancelButton)
        {
            if (generating) {
                factory.cancel(true);
                statusField.setText("Canceled Azimuth offset angle generation");
                factory = null;
                //generateButton.setEnabled(true);
                //cancelButton.setEnabled(false);
                generating = false;
                updateGui();
                inverterProgress.setValue(0);
            }
        }
    } // actionPerformed()

    /**
     * Implements Gain part of FocusListener for this class.  Needed to remember a text field before user edits it.
     */
    public void focusGained(FocusEvent e)
    {
        JFormattedTextField field = (JFormattedTextField) e.getComponent();

        // Save the current field value in case the user botches up the edit.
        // This allows us to restore the prior value upon field exit
        saveFocusString = field.getText();  
    }

    /**
     * Implements Lost part of FocusListener for this class.  Verifies the validity of an edited field.
     */
    public void focusLost(FocusEvent e)
    {
        Object source = e.getSource();
        if (source == refAngleField)
        {
            try
            {
                if (Double.parseDouble(refAngleField.getText()) < -360.0)
                {
                    refAngleField.setText("-360.0");
                    statusField.setText("Reset reference angle to minimum value of -360.0\n");
                    Toolkit.getDefaultToolkit().beep();
                }

                if (Double.parseDouble(refAngleField.getText()) > 360.0)
                {
                    refAngleField.setText(Double.toString(360.0));
                    statusField.setText("Reset reference angle to maximum value of " 
                            + 360.0 + "\n");
                    Toolkit.getDefaultToolkit().beep();
                }

                double angle = Double.parseDouble(refAngleField.getText());
                refAngleField.setText(Double.toString(angle));
            } catch (NumberFormatException e1)
            {
                statusField.setText("Invalid reference angle '" + refAngleField.getText()
                        + "' in reference field, restoring former value\n");
                refAngleField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }
        } // if refAngleField


    } // focusLost()

    /**
     * Implements callbacks for SeedSplitter and AzimuthLocator worker events.
     * This is where the popup plots for the final results gets initiated.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        Date firstTheta;
        double interval_sec;
        if ("progress".equals(evt.getPropertyName()))
        {
            inverterProgress.setValue((Integer)evt.getNewValue());

            if (factory.isDone())
            {
                generating = false;
                updateGui();
                try
                {
                    if (factory.getSuccess())
                    {
                        result = factory.get();
                        interval_sec = InvertAzimuth.OVERLAP / dRate;
                        firstTheta = new Date(leftSliderDate.getTime()
                                + (long)(1000*InvertAzimuth.WINDOWLENGTH/2/dRate)); 
                        double refAngle = Double.parseDouble(refAngleField.getText());
                        String status = String.format(
                                "Reference %.3f + angle offset %.3f = %.3f",
                                refAngle, AzAngleDisplay.Normalize360(result.getBestTheta()), 
                                AzAngleDisplay.Normalize360(refAngle + result.getBestTheta()));
                        statusField.setText(status);
                        azAngleDisplay = new AzAngleDisplay(this,
                                mRefStation, mRefNetwork, mRefChannel, mRefLocation,
                                mNorthStation, mNorthNetwork, mNorthChannel, mNorthLocation,
                                mEastStation, mEastNetwork, mEastChannel, mEastLocation,
                                refAngle, result.getBestTheta(), result.getThetas(), 
                                result.getCorrelations(), result.getStandardDeviation(),
                                result.getMeanOfBestCorrelations(), firstTheta, interval_sec);
                        azAngleDisplay.setVisible(true);            
                        //azAngleDisplay.correctSize();
                        //generateButton.setEnabled(true);
                        //cancelButton.setEnabled(false);
                    }
                    else
                    {
                        statusField.setText("Convergence failed!");
                        inverterProgress.setValue(0);
                        //generateButton.setEnabled(true);
                        //cancelButton.setEnabled(false);
                    }
                } 
                catch (InterruptedException e)
                {
                    statusField.setText("Interrupt Exception, Convergence failed!");
                    e.printStackTrace();
                    //generateButton.setEnabled(true);
                    //cancelButton.setEnabled(false);
                } catch (ExecutionException e)
                {
                    statusField.setText("Execution Exception Convergence failed!");
                    e.printStackTrace();
                    //generateButton.setEnabled(true);
                    //cancelButton.setEnabled(false);
                }
            } // factory reports it is done
        } // "progress" event

    } // propertyChange()

    /**
     * Makes sure that each button is enabled or greyed out depending on the current data state.
     */
    private void updateGui() {
        if (generating) {
            generateButton.setEnabled(false);
            cancelButton.setEnabled(true);
            zoomOutButton.setEnabled(false);
            zoomInButton.setEnabled(false);
            filesButton.setEnabled(false);
        } else {
            cancelButton.setEnabled(false);
            filesButton.setEnabled(true);
            if (populated) {
                generateButton.setEnabled(true);
                zoomInButton.setEnabled(true);
                if (zoomed) {
                    zoomOutButton.setEnabled(true);
                } else {
                    zoomOutButton.setEnabled(false);
                }
            } else {
                generateButton.setEnabled(false);
                zoomInButton.setEnabled(false);
                zoomOutButton.setEnabled(false);
            }
        }
    }

    private AZprefs prefs;

    // Persistence variables
    private String              seedFileDir;

    private String              mRefStation = "XXXX";
    private String              mNorthStation;
    private String              mEastStation;
    private String              mRefNetwork="XX";
    private String              mNorthNetwork;
    private String              mEastNetwork;
    private String              mRefChannel = "LH?";
    private String              mNorthChannel;
    private String              mEastChannel;
    private String              mRefLocation = "00";
    private String              mNorthLocation;
    private String              mEastLocation;

    private JPanel              northViewBufferJPanel;
    private JPanel              northViewJPanel;
    private SegmentPlotter      northSegmentPlot;
    private JPanel              eastViewBufferJPanel;
    private JPanel              eastViewJPanel;
    private SegmentPlotter      eastSegmentPlot;
    private JPanel              referenceViewBufferJPanel;
    private JPanel              referenceViewJPanel;
    private SegmentPlotter      referenceSegmentPlot;
    private JTextField          statusField;

    private GraphUpdate         timerGraph = null;
    private Timer               t;
    private AzimuthLocator      factory = null;
    private AzimuthResult       result = null;

    private JSlider             leftSlider;
    private JSlider             rightSlider;
    private JLabel              startTime;
    private JLabel              endTime;
    private JComboBox           segmentCombo;
    private int                 iMarginSlider=100;
    private int                 iLeftSliderValue=1000;
    private int                 iRightSliderValue=1000;
    private Date                leftSliderDate=null;
    private Date                rightSliderDate=null;
    private JProgressBar        inverterProgress;

    private JFormattedTextField refAngleField;
    private String              saveFocusString;

    private JButton             filesButton;
    private JButton             generateButton;
    private JButton             cancelButton;
    private JButton             quitButton;
    private JButton             zoomInButton;
    private JButton             zoomOutButton;

    private ChannelSelector       channelSelector;
    private AzAngleDisplay        azAngleDisplay;
    ArrayList<ArrayList<DataSet>> dataSets = null;
    ArrayList<ContiguousBlock>    blocks = null;
    ArrayList<FilterBlock>        contBlocks = new ArrayList<FilterBlock>();
    FilterBlock                   selectBlock = null;
    FilterBlock                   finalBlock = null;
    FilterBlock                   zoomBlock = null;
    int [][]                      finalData = {null, null, null};
    double                        dRate = 1.0;
    int                           countsPerPlot;

    private boolean generating = false;
    private boolean populated  = false;
    private boolean zoomed     = false;

    public static final int MIN_DATAPOINTS = InvertAzimuth.WINDOWLENGTH + InvertAzimuth.OVERLAP*4;
} // class MainFrame
