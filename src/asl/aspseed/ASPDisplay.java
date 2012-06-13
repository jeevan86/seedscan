package asl.aspseed;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import dcctime.DeltaTime;
import dcctime.StdTime;

/**
 * Encapsulates the entire GUI to keep it separate from the command line
 * interface implementation.
 */
public class ASPDisplay
{
    private TransferFrame frame;

    public ASPDisplay()
    {
        WindowListener listener = new Terminator();
        frame = new TransferFrame();
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.addWindowListener(listener);
        frame.setVisible(true);
    } // ASPDisplay() constructor

    /**
     * Called when the GUI exit button is clicked.
     * Allows GUI to save persistent state information for next launch.
     */
    class Terminator extends WindowAdapter
    {
        public void windowClosing(WindowEvent e)
        {
            frame.SavePrefs();
        }
    } // class Terminator

} // class ASPDisplay

/**
 * Top level frame for seed transfer GUI, everything else fits inside.
 */
class TransferFrame extends JFrame implements ActionListener, FocusListener 
{
    private static final long serialVersionUID = 2L;
    public TransferFrame()
    {
        prefs = new ASPPreferences();
        setTitle("ASP seed data Transfer V" + serialVersionUID);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        // Create panel for channel list and file list
        movePanel = createMovePanel();

        // Create panel for text fields
        entryPanel = createEntryPanel();

        // Create panel for time span
        timeSpanPanel = createTimeSpanPanel();

        // Status message box
        msgBox = new JTextArea();
        //msgBox.setFont(new Font("Serif", Font.PLAIN, 16));
        msgBox.setLineWrap(true);
        msgBox.setWrapStyleWord(true);
        msgBox.setEditable(false);
        scrollMsgBox = new JScrollPane(msgBox);
        scrollMsgBox.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollMsgBox.setPreferredSize(new Dimension(250, 250));
        scrollMsgBox.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Status display"),
                        BorderFactory.createEmptyBorder(5,5,5,5)),
                    scrollMsgBox.getBorder()));

        // Build panel
        JPanel panel = new JPanel();
        SpringLayout layout = new SpringLayout();
        panel.setLayout(layout);

        Spring s = Spring.constant(0, 20, 10000);
        Spring s0 = Spring.constant(0, 0, 0);

        layout.putConstraint(SpringLayout.EAST, timeSpanPanel, s0, 
                SpringLayout.EAST, panel);
        layout.putConstraint(SpringLayout.EAST, scrollMsgBox, s0,
                SpringLayout.EAST, panel);
        layout.putConstraint(SpringLayout.EAST, movePanel, s0,
                SpringLayout.EAST, panel);

        layout.putConstraint(SpringLayout.WEST, entryPanel, s0,
                SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.WEST, movePanel, s0, 
                SpringLayout.WEST, entryPanel);
        layout.putConstraint(SpringLayout.WEST, scrollMsgBox, s0,
                SpringLayout.WEST, movePanel);

        layout.putConstraint(SpringLayout.NORTH, movePanel, s,
                SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.NORTH, entryPanel, s,
                SpringLayout.SOUTH, movePanel);

        layout.putConstraint(SpringLayout.NORTH, timeSpanPanel, s0,
                SpringLayout.NORTH, entryPanel);
        layout.putConstraint(SpringLayout.NORTH, scrollMsgBox, s,
                SpringLayout.SOUTH, timeSpanPanel);
        layout.putConstraint(SpringLayout.SOUTH, panel, s, 
                SpringLayout.SOUTH, scrollMsgBox);
        panel.add(movePanel);
        panel.add(entryPanel);
        panel.add(timeSpanPanel);
        panel.add(scrollMsgBox);

        getContentPane().add(panel);
    } // constructor TransferFrame()

    protected JComponent createListPanel()
    {
        JPanel panel = new JPanel(new SpringLayout());

        // Channel list window
        channelListModel = new DefaultListModel();
        channelListBox = new JList(channelListModel);
        channelListBox.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        channelListBox.setLayoutOrientation(JList.VERTICAL);
        channelListBox.setVisibleRowCount(12);
        channelListBox.setPrototypeCellValue(
                "00-BHZ 2008/01/01 00:00:00 2008/12/31 24:00:00 200000");

        JScrollPane scrollChannelListBox = new JScrollPane(channelListBox);
        scrollChannelListBox.setName("Channel List");
        scrollChannelListBox.setPreferredSize(new Dimension(350, 210));
        scrollChannelListBox.setVisible(true);

        // ListAll button
        ActionListener listAction = this;
        listAllButton = new JButton("List All");
        listAllButton.addActionListener(listAction);

        // ListFiltered button
        listFilteredButton = new JButton("List Filtered");
        listFilteredButton.addActionListener(listAction);

        SpringLayout layout = (SpringLayout) panel.getLayout();
        Spring s = Spring.constant(0, 20, 10000);
        Spring s0 = Spring.constant(0, 0, 0);
        layout.putConstraint(SpringLayout.WEST, listAllButton, s, 
                SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.WEST, scrollChannelListBox, s0,
                SpringLayout.WEST, listAllButton);
        layout.putConstraint(SpringLayout.EAST, listFilteredButton, s0, 
                SpringLayout.EAST, scrollChannelListBox);
        layout.putConstraint(SpringLayout.EAST, panel, s,
                SpringLayout.EAST, listFilteredButton);

        layout.putConstraint(SpringLayout.NORTH, scrollChannelListBox, s,
                SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.NORTH, listAllButton, s,
                SpringLayout.SOUTH, scrollChannelListBox);
        layout.putConstraint(SpringLayout.NORTH, listFilteredButton, s0,
                SpringLayout.NORTH, listAllButton);
        layout.putConstraint(SpringLayout.SOUTH, panel, s, 
                SpringLayout.SOUTH, listFilteredButton);
        layout.putConstraint(SpringLayout.SOUTH, panel, s, 
                SpringLayout.SOUTH, listAllButton);		

        panel.add(scrollChannelListBox);
        panel.add(listAllButton);
        panel.add(listFilteredButton);

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Channel List"),
                        BorderFactory.createEmptyBorder(5,5,5,5)),
                    panel.getBorder()));
        return panel;
    } // createListPanel()

    protected JComponent createActionPanel()
    {
        JPanel panel = new JPanel(new SpringLayout());
        ActionListener buttonAction = this;

        // Transfer button
        transferButton = new JButton("Transfer ");
        transferButton.addActionListener(buttonAction);

        // Cancel button
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(buttonAction);

        // Quit button
        quitButton = new JButton("Quit");
        quitButton.addActionListener(buttonAction);

        // Extended filename format checkbox
        extendFilenameCheckBox = new JCheckBox("Old names");
        extendFilenameCheckBox.setSelected(prefs.GetExtendFilename()==0);
        extendFilenameCheckBox.addActionListener(buttonAction);

        panel.add(quitButton);
        panel.add(cancelButton);
        panel.add(extendFilenameCheckBox);
        panel.add(transferButton);

        SpringLayout layout = (SpringLayout) panel.getLayout();

        // Set button width to max of all items
        Spring maxWidthSpring = layout.getConstraints(quitButton).getWidth();
        maxWidthSpring = Spring.max(maxWidthSpring, 
                layout.getConstraints(cancelButton).getWidth());
        maxWidthSpring = Spring.max(maxWidthSpring, 
                layout.getConstraints(extendFilenameCheckBox).getWidth());
        maxWidthSpring = Spring.max(maxWidthSpring, 
                layout.getConstraints(transferButton).getWidth());
        layout.getConstraints(quitButton).setWidth(maxWidthSpring);
        layout.getConstraints(cancelButton).setWidth(maxWidthSpring);
        layout.getConstraints(extendFilenameCheckBox).setWidth(maxWidthSpring);
        layout.getConstraints(transferButton).setWidth(maxWidthSpring);

        // Now link up the buttons
        Spring s = Spring.constant(8, 20, 20);
        Spring s0 = Spring.constant(0, 0, 0);
        Spring s1 = Spring.constant(10, 40, 40);
        layout.putConstraint(SpringLayout.NORTH, quitButton, s1,
                SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.NORTH, cancelButton, s1,
                SpringLayout.SOUTH, quitButton);
        layout.putConstraint(SpringLayout.NORTH, extendFilenameCheckBox, s1,
                SpringLayout.SOUTH, cancelButton);
        layout.putConstraint(SpringLayout.NORTH, transferButton, s1,
                SpringLayout.SOUTH, extendFilenameCheckBox);
        layout.putConstraint(SpringLayout.SOUTH, panel, s1, 
                SpringLayout.SOUTH, transferButton);

        layout.putConstraint(SpringLayout.EAST, quitButton, s0, 
                SpringLayout.EAST, transferButton);
        layout.putConstraint(SpringLayout.EAST, cancelButton, s0, 
                SpringLayout.EAST, quitButton);
        layout.putConstraint(SpringLayout.EAST, extendFilenameCheckBox, s0, 
                SpringLayout.EAST, cancelButton);
        layout.putConstraint(SpringLayout.EAST, panel, s0, 
                SpringLayout.EAST, extendFilenameCheckBox);

        layout.putConstraint(SpringLayout.WEST, transferButton, s0, 
                SpringLayout.WEST, quitButton);
        layout.putConstraint(SpringLayout.WEST, quitButton, s0, 
                SpringLayout.WEST, cancelButton);
        layout.putConstraint(SpringLayout.WEST, cancelButton, s0, 
                SpringLayout.WEST, extendFilenameCheckBox);
        layout.putConstraint(SpringLayout.WEST, extendFilenameCheckBox, s, 
                SpringLayout.WEST, panel);

        return panel;
    } // createActionPanel()

    protected JComponent createMovePanel()
    {
        JPanel panel = new JPanel(new SpringLayout());

        // List panel
        listPanel = createListPanel();

        // Action panel
        actionPanel = createActionPanel();

        // Set up local file system window to show transfered seed data
        localSeedFileFrame = new JFileChooser();
        localSeedFileFrame.setCurrentDirectory(new File(prefs.GetLocalDir()));

        // This is a navigate and list only file frame
        localSeedFileFrame.setControlButtonsAreShown(false);
        localSeedFileFrame.getComponent(localSeedFileFrame.
                getComponentCount()-1).setVisible(false);

        panel.add(listPanel);
        panel.add(actionPanel);
        panel.add(localSeedFileFrame);

        SpringLayout layout = (SpringLayout) panel.getLayout();
        Spring s0 = Spring.constant(0, 0, 0);
        layout.putConstraint(SpringLayout.WEST, listPanel, s0, 
                SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.WEST, actionPanel, s0, 
                SpringLayout.EAST, listPanel);
        layout.putConstraint(SpringLayout.WEST, localSeedFileFrame, s0, 
                SpringLayout.EAST, actionPanel);
        layout.putConstraint(SpringLayout.EAST, panel, s0, 
                SpringLayout.EAST, localSeedFileFrame);

        layout.putConstraint(SpringLayout.NORTH, localSeedFileFrame, s0,
                SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.NORTH, actionPanel, s0,
                SpringLayout.NORTH, localSeedFileFrame);
        layout.putConstraint(SpringLayout.NORTH, listPanel, s0,
                SpringLayout.NORTH, actionPanel);
        layout.putConstraint(SpringLayout.SOUTH, listPanel, s0,
                SpringLayout.SOUTH, actionPanel);
        layout.putConstraint(SpringLayout.SOUTH, actionPanel, s0,
                SpringLayout.SOUTH, localSeedFileFrame);
        layout.putConstraint(SpringLayout.SOUTH, panel, s0, 
                SpringLayout.SOUTH, localSeedFileFrame);

        return panel;
    } // createMovePanel()

    protected JComponent createTimeSpanPanel()
    {
        JPanel panel = new JPanel(new SpringLayout());

        startDateField = new JFormattedTextField();
        startDateField.setValue(prefs.GetStartDate());
        startDateField.setColumns(8);
        startDateField.setName("startDateField");
        startDateField.addFocusListener(this);

        startTimeField = new JFormattedTextField();
        startTimeField.setValue(prefs.GetStartTime());
        startTimeField.setColumns(8);
        startTimeField.setName("startTimeField");
        startTimeField.addFocusListener(this);

        finishDateField = new JFormattedTextField();
        finishDateField.setValue(prefs.GetFinishDate());
        finishDateField.setColumns(8);
        finishDateField.setName("finishDateField");
        finishDateField.addFocusListener(this);

        finishTimeField = new JFormattedTextField();
        finishTimeField.setValue(prefs.GetFinishTime());
        finishTimeField.setColumns(8);
        finishTimeField.setName("finishTimeField");
        finishTimeField.addFocusListener(this);

        JLabel blankLabel = new JLabel("",	JLabel.TRAILING);
        JLabel dateLabel = new JLabel("Date YYYY/MM/DD",	JLabel.TRAILING);
        JLabel timeLabel = new JLabel("Time HH:MM:SS",	JLabel.TRAILING);
        JLabel startLabel = new JLabel("Start",	JLabel.TRAILING);
        JLabel finishLabel = new JLabel("Finish",	JLabel.TRAILING);

        panel.add(blankLabel);
        panel.add(dateLabel);
        panel.add(timeLabel);
        panel.add(startLabel);
        panel.add(startDateField);
        panel.add(startTimeField);
        panel.add(finishLabel);
        panel.add(finishDateField);
        panel.add(finishTimeField);

        SpringUtilities.makeCompactGrid(panel,
                3, 3,
                GAP, GAP, //init x,y
                GAP, GAP/2);//xpad, ypad

        return panel;
    } // createTimeSpanPanel()

    protected JComponent createEntryPanel() 
    {
        JPanel panel = new JPanel(new SpringLayout());

        hostField = new JFormattedTextField();
        hostField.setValue(prefs.GetHostname());
        hostField.setColumns(10);
        hostField.setName("hostField");
        JLabel labelHost = new JLabel("Host:",	JLabel.TRAILING);
        labelHost.setLabelFor(hostField);
        panel.add(labelHost);
        panel.add(hostField);
        panel.add(labelHost);
        panel.add(hostField);
        hostField.addFocusListener(this);

        stationField = new JFormattedTextField();
        stationField.setValue(prefs.GetStation());
        stationField.setColumns(5);
        stationField.setName("stationField");
        JLabel labelStation = new JLabel("Station:",	JLabel.TRAILING);
        labelStation.setLabelFor(stationField);
        panel.add(labelStation);
        panel.add(stationField);
        stationField.addFocusListener(this);

        portField = new JFormattedTextField();
        portField.setValue(Integer.toString(prefs.GetPort()));
        portField.setColumns(5);
        portField.setName("portField");
        JLabel labelPort = new JLabel("Port:",	JLabel.TRAILING);
        labelPort.setLabelFor(portField);
        panel.add(labelPort);
        panel.add(portField);
        portField.addFocusListener(this);

        channelField = new JFormattedTextField();
        channelField.setValue("*");
        channelField.setColumns(3);
        channelField.setName("channelField");
        JLabel labelChannel = new JLabel("Channel:",	JLabel.TRAILING);
        labelChannel.setLabelFor(channelField);
        panel.add(labelChannel);
        panel.add(channelField);
        channelField.addFocusListener(this);

        maxRecordField = new JFormattedTextField();
        maxRecordField.setValue(Integer.toString(prefs.GetMaxRecords()));
        maxRecordField.setColumns(5);
        maxRecordField.setName("maxRecordField");
        JLabel labelMaxRecord = new JLabel("MaxRecord:",	JLabel.TRAILING);
        labelMaxRecord.setLabelFor(maxRecordField);
        panel.add(labelMaxRecord);
        panel.add(maxRecordField);
        maxRecordField.addFocusListener(this);

        locationField = new JFormattedTextField();
        locationField.setValue("*");
        locationField.setColumns(2);
        locationField.setName("locationField");
        JLabel labelLocation = new JLabel("Location:",	JLabel.TRAILING);
        labelLocation.setLabelFor(locationField);
        panel.add(labelLocation);
        panel.add(locationField);
        locationField.addFocusListener(this);

        SpringUtilities.makeCompactGrid(panel,
                3, 4,
                GAP, GAP, //init x,y
                GAP, GAP/2);//xpad, ypad
        return panel;
    } // createEntryPanel()

    /**
     * Saves persistent state information so that it can be retrieved the
     * next time the program is run.
     */
    public void SavePrefs()
    {
        prefs.SetLocalDir(localSeedFileFrame.getCurrentDirectory().getAbsolutePath());
        prefs.SetHostname(hostField.getText());
        prefs.SetPort(Integer.valueOf(portField.getText()).intValue());
        prefs.SetMaxRecords(Integer.valueOf(maxRecordField.getText()).intValue());
        prefs.SetExtendFilename((extendFilenameCheckBox.getSelectedObjects() == null)?1:0);
        prefs.SetStation(stationField.getText());
        prefs.SetStartDate(startDateField.getText());
        prefs.SetStartTime(startTimeField.getText());
        prefs.SetFinishDate(finishDateField.getText());
        prefs.SetFinishTime(finishTimeField.getText());

        // Remember any changes to preferences
        prefs.SavePreferences();			System.exit(0);
    } // SavePrefs()

    public void listAll()
    {

        // Get directory listing
        String hostname = hostField.getText();
        int port = Integer.parseInt(portField.getText());
        String station = stationField.getText();
        String id = station +".*-*";
        String chanList[]= null;

        // Get data
        msgBox.append("Connecting to host " + hostname + ":" + port + "\n");
        DirSocket getDirThread = new DirSocket(hostname, port, id, "*", "*"); 
        getDirThread.start();

        while (!getDirThread.Done() && getDirThread.isAlive())
        {
            try
            {
                Thread.sleep(1000);
            } catch (InterruptedException e)
            {
                msgBox.append("Error listAll(): Sleep failed waiting for DirSocket to end\n");
            }
        } // loop until data collection thread says it is done

        while (channelListModel.getSize() > 0)
            channelListModel.remove(0);

        if (getDirThread.GetErrMsg() != null)
        {
            msgBox.append(getDirThread.GetErrMsg() + "\n");
            return;
        }

        chanList = getDirThread.GetChannelList();
        if (chanList == null)
        {
            msgBox.append("No channels returned\n");
            return;
        }

        for (int i = 0; i < chanList.length; i++)
        {
            for (iSort=0; iSort < channelListModel.getSize(); iSort++)
            {
                if (chanList[i].compareTo((String)channelListModel.getElementAt(iSort)) < 0)
                {
                    break;
                }
            } // loop to sort channel into list
            channelListModel.add(iSort, chanList[i]);
        } // loop to add each new channel to list

        msgBox.append("Complete list of " + chanList.length + " channels finished.\n");

    } // listAll()

    public void listFiltered()
    {

        // Get directory listing
        String hostname = hostField.getText();
        int port = Integer.parseInt(portField.getText());
        String station = stationField.getText();
        String channel = channelField.getText();
        String location = locationField.getText();
        String id;
        String startDate = startDateField.getText();
        String startTime = startTimeField.getText();
        String finishDate = finishDateField.getText();
        String finishTime = finishTimeField.getText();
        String chanList[]= null;

        if (location.compareTo("  ") == 0)
        {
            id = station + '.' + channel;
        } // blank location
        else
        {
            id = station + '.' + location + '-' + channel;
        } // not blank location

        // Get data
        msgBox.append("Connecting to host " + hostname + ":" + port + "\n");

        DirSocket getDirThread = new DirSocket(hostname, port, id, 
                startDate + ' ' + startTime, finishDate + ' ' + finishTime); 
        getDirThread.start();

        while (!getDirThread.Done() && getDirThread.isAlive())
        {
            try
            {
                Thread.sleep(1000);
            } catch (InterruptedException e)
            {
                msgBox.append("Error listFiltered(): Sleep failed waiting for DirSocket to end\n");
                return;
            } catch (IllegalMonitorStateException e)
            {
                msgBox.append("Error listFiltered(): wait failed IllegalMonitorStateException\n");
                return;
            }
        } // loop until data collection thread says it is done

        while (channelListModel.getSize() > 0)
            channelListModel.remove(0);

        if (getDirThread.GetErrMsg() != null)
        {
            msgBox.append(getDirThread.GetErrMsg() + "\n");
            return;
        }

        chanList = getDirThread.GetChannelList();
        if (chanList == null)
        {
            msgBox.append("No channels match filter\n");
            return;
        }
        for (int i = 0; i < chanList.length; i++)
        {
            for (iSort=0; iSort < channelListModel.getSize(); iSort++)
            {
                if (chanList[i].compareTo((String)channelListModel.getElementAt(iSort)) < 0)
                {
                    break;
                }
            } // loop to sort channel into list
            channelListModel.add(iSort, chanList[i]);
        } // loop to add each new channel to list
        msgBox.append("Filtered list of " + chanList.length + " channels finished.\n");

    } // listFiltered()

    class TransferTask extends Thread
    {
        public TransferTask()
        {
            // Run Thread constructor
            super();
        } // TransferTask()

        /* 
         * Starts a background thread which connects up to a server and gets a
         * listing of seed records matching the filter passed in via the DirSocket 
         * constructor. 
         * @see java.lang.Thread#run()
         */
        public void run() 
        {
            Transfer();
            EventQueue.invokeLater(new
                    Runnable()
                    {

                    public void run()
                    {
                    CleanUp();
                    }
                    });

            return;
        } // run()

        /*
         * To be called after task is done running
         */
        public void CleanUp() {
            quitButton.setEnabled(true);
            transferButton.setEnabled(true);
            listAllButton.setEnabled(true);
            listFilteredButton.setEnabled(true);
            stationField.setEnabled(true);
            channelField.setEnabled(true);
            locationField.setEnabled(true);
            startDateField.setEnabled(true);
            startTimeField.setEnabled(true);
            finishDateField.setEnabled(true);
            finishTimeField.setEnabled(true);
            hostField.setEnabled(true);
            portField.setEnabled(true);
            maxRecordField.setEnabled(true);
            localSeedFileFrame.rescanCurrentDirectory();
            setCursor(null); //turn off the wait cursor
        } // CleanUp()
    } // class TransferTask

    public void Transfer()
    {
        //
        // First get a filtered directory listing for error checking
        //
        String hostname = hostField.getText();
        int port = Integer.parseInt(portField.getText());
        String station = stationField.getText();
        String channel = "*";
        String location = "*";
        String id;
        String startDate = startDateField.getText();
        String startTime = startTimeField.getText();
        String finishDate = finishDateField.getText();
        String finishTime = finishTimeField.getText();
        String chanList[]= null;
        String savedir = localSeedFileFrame.getCurrentDirectory().getAbsolutePath();
        String filename;
        int iCount;
        int expected;
        int duration;
        String selected;
        OutputStream outfile=null;
        final char hourChar[] = 
        {'A','B','C','D','E','F','G','H','I','J','K','L',
            'M','N','O','P','Q','R','S','T','U','V','W','X'};

        // Make user user has selected some channels
        Object[] list = channelListBox.getSelectedValues();
        if (list == null || list.length == 0)
        {
            msgBox.append("User must select channels to transfer.\n");
            return;
        }

        if (location.compareTo("  ") == 0)
        {
            id = station + '.' + channel;
        } // blank location
        else
        {
            id = station + '.' + location + '-' + channel;
        } // not blank location

        // Get data
        msgBox.append("Connecting to host " + hostname + ":" + port + "\n");
        DirSocket getDirThread = new DirSocket(hostname, port, id, 
                startDate + ' ' + startTime, finishDate + ' ' + finishTime); 
        getDirThread.start();

        while (!getDirThread.Done() && getDirThread.isAlive())
        {
            try
            {
                Thread.sleep(1000);
            } catch (InterruptedException e)
            {
                msgBox.append("Error Transfer: Sleep failed waiting for DirThread to end\n");
                return;
            }
        } // loop until data collection thread says it is done

        if (getDirThread.GetErrMsg() != null)
        {
            msgBox.append(getDirThread.GetErrMsg() + "\n");
            return;
        }

        chanList = getDirThread.GetChannelList();

        // Now loop through selection list
        // This first pass is for error checking only
        boolean okay = (list != null  && chanList != null);
        if (!okay)
        {
            msgBox.append("User must select channels to transfer.\n");
            return;
        }
        iCount = 0;
        selected = "";
        for (int i=0; okay && i < list.length; i++)
        {
            // Find matching entry in filtered list
            int j;
            for (j=0; okay && j < chanList.length; j++)
            {
                selected = (String)list[i];
                if (selected.substring(0,6).compareTo(chanList[j].substring(0,6)) == 0)
                {
                    // We found the entry in the filtered list
                    iCount++;
                    // Make sure that the number of records is acceptable
                    int iMaxRecords = Integer.valueOf(maxRecordField.getText()).intValue();
                    int iRecords = 
                        Integer.valueOf(chanList[j].substring(chanList[j].lastIndexOf(' ')+1)).intValue();
                    if (iRecords > iMaxRecords)
                    {
                        okay = false;
                        msgBox.append("Channel " + selected.substring(0,6) 
                                + " has too many records, " + iRecords + " > " + iMaxRecords + '\n');
                    }
                    break;
                } // location/channel matches
            } // for each filtered item
            if (j == chanList.length && okay)
            {
                msgBox.append("Channel " + selected.substring(0,6) 
                        + " has no data for the given time span, skipping!\n");
            } // selected item not found
        } // for each selected item

        if (okay)
        {
            msgBox.append(
                    "Transfering " + iCount + " channels of 512 byte seed records.\n");

            for (int i=0; okay && i < list.length; i++)
            {
                selected = (String)list[i];
                // Find matching entry in filtered list
                int j;
                for (j=0; okay && j < chanList.length; j++)
                {
                    if (selected.substring(0,6).compareTo(chanList[j].substring(0,6)) == 0)
                    {
                        if (bCancel)
                        {
                            msgBox.append("Transfer canceled\n");
                            return;
                        }

                        location = selected.substring(0,2);
                        channel = selected.substring(3,6);
                        if (location.compareTo("  ") == 0)
                        {
                            id = station + '.' + channel;
                        } // blank location
                        else
                        {
                            id = station + '.' + location + '-' + channel;
                        } // not blank location

                        // Do some syntax checks on return string
                        if (chanList[j].length() < 48) continue;
                        if (chanList[j].charAt(2) != '/') continue;
                        if (chanList[j].charAt(11) != '/') continue;
                        if (chanList[j].charAt(14) != '/') continue;
                        if (chanList[j].charAt(20) != ':') continue;
                        if (chanList[j].charAt(23) != ':') continue;
                        if (chanList[j].charAt(31) != '/') continue;
                        if (chanList[j].charAt(34) != '/') continue;
                        if (chanList[j].charAt(40) != ':') return;
                        if (chanList[j].charAt(43) != ':') return;

                        if (!Character.isDigit(chanList[j].charAt(47)))
                        {
                            return;
                        }
                        expected = Integer.valueOf(chanList[j].substring(47)).intValue();

                        if (extendFilenameCheckBox.getSelectedObjects() == null)
                        {
                            // user wants new filename format
                            if (location.charAt(0) != ' ')
                                filename = savedir + File.separatorChar
                                    + location + channel
                                    + startDate.substring(2, 4)
                                    + startDate.substring(5,7)
                                    + startDate.substring(8,10)
                                    + startTime.substring(0,2)
                                    + startTime.substring(3,5)
                                    + '.' + "seed";
                            else
                                filename = savedir + File.separatorChar
                                    + channel
                                    + startDate.substring(2, 4)
                                    + startDate.substring(5,7)
                                    + startDate.substring(8,10)
                                    + startTime.substring(0,2)
                                    + startTime.substring(3,5)
                                    + '.' + "seed";
                        } // if extended filename
                        else
                        {
                            // user wants old filename format
                            int hour = Integer.valueOf(startTime.substring(0,2)).intValue();
                            filename = savedir + File.separatorChar
                                + 'C'
                                + channel.charAt(0)
                                + startDate.substring(2, 4)
                                + startDate.substring(5,7)
                                + startDate.substring(8,10)
                                + '.'
                                + hourChar[hour]
                                + startTime.substring(3,5);

                        } // old filename format

                        try
                        {
                            outfile = new FileOutputStream(filename, true);
                        } catch (FileNotFoundException e)
                        {
                            msgBox.append("Failed to open file " + savedir + ", aborting transfers!\n");
                            return;
                        }

                        StdTime tFinish = new StdTime(finishDate, finishTime);
                        StdTime tStart = new StdTime(startDate, startTime);
                        DeltaTime tDelta = new DeltaTime(tFinish, tStart);
                        if (tDelta.ExceedsMaxSeconds())
                        {
                            // This case should be extremely difficult to reach
                            msgBox.append("Duration exceeds limit of " + DeltaTime.MaxDeltaInt
                                    + " seconds for " + location + '/' + channel + "\n");
                            duration = DeltaTime.MaxDeltaInt;
                        }
                        else
                            duration = tDelta.toSeconds();
                        if (duration < 1) duration = 1;

                        // We found the entry in the filtered list
                        msgBox.append("Transfering data from channel " 
                                + selected.substring(0,6) + " to file " + filename + "\n");
                        EventQueue.invokeLater(new
                                Runnable()
                                {
                                public void run()
                                {
                                msgBox.append("\r");
                                localSeedFileFrame.rescanCurrentDirectory();
                                }
                                });

                        // Get data for this channel and time period
                        SeedSocket getSeedThread = new SeedSocket(hostname, port, id, startDate,
                                startTime, duration, outfile); 
                        getSeedThread.start();

                        while (!getSeedThread.Done() && getSeedThread.isAlive())
                        {
                            try
                            {
                                Thread.sleep(1000);
                                if (bCancel)
                                    getSeedThread.SetCancel(true);
                            } catch (InterruptedException e)
                            {
                                msgBox.append("Error Transfer(): Sleep failed waiting for SeedThread\n");
                                return;
                            }
                        } // loop until data collection thread says it is done

                        if (expected != getSeedThread.GetRecordCount())
                        {
                            msgBox.append("Collected " + getSeedThread.GetRecordCount()
                                    + " of " + expected + " expected records\n");
                        }

                        try
                        {
                            outfile.close();
                        } catch (IOException e)
                        {
                            msgBox.append("Error closing seed data file.\n");
                        }
                        if (getSeedThread.GetRecordCount() < 1)
                        {
                            // No data so delete output file
                            msgBox.append("Warning, empty seed output file " + filename + "\n");
                        }
                    } // location/channel matches
                } // for each filtered item
            } // for each selected item

            if (bCancel)
            {
                EventQueue.invokeLater(new
                        Runnable()
                        {
                        public void run()
                        {
                        msgBox.append("Transfer canceled\n");
                        }
                        });
                return;
            }
            EventQueue.invokeLater(new
                    Runnable()
                    {
                    public void run()
                    {
                    msgBox.append("Transfer complete\n");
                    }
                    });
        } // if request did not have any errors

    } // Transfer()

    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();
        if (command.compareTo("Transfer ") == 0)
        {
            bCancel = false;
            quitButton.setEnabled(false);
            transferButton.setEnabled(false);
            listAllButton.setEnabled(false);
            listFilteredButton.setEnabled(false);
            stationField.setEnabled(false);
            channelField.setEnabled(false);
            locationField.setEnabled(false);
            startDateField.setEnabled(false);
            startTimeField.setEnabled(false);
            finishDateField.setEnabled(false);
            finishTimeField.setEnabled(false);
            hostField.setEnabled(false);
            portField.setEnabled(false);
            maxRecordField.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            transferTask = new TransferTask();
            transferTask.start();
        }
        else if (command.compareTo("List All") == 0)
        {
            listAll();
        }
        else if (command.compareTo("List Filtered") == 0)
        {
            listFiltered();		
        }
        else if (command.compareTo("Old names") == 0)
        {
            // Don't really do anything special when box gets changed
        }
        else if (command.compareTo("Quit") == 0)
        {
            bCancel = true;
            SavePrefs();
            System.exit(0);
        }
        else if (command.compareTo("Cancel") == 0)
        {
            // Remember that user pressed the cancel button
            bCancel = true;
        }
        else
        {
            System.err.println("Unmanaged actionPerformed " + command);
        }
    } // actionPerformed()

    public void focusGained(FocusEvent e)
    {
        JFormattedTextField field = (JFormattedTextField) e.getComponent();

        // Save the current field value in case the user botches up the edit.
        // This allows us to restore the prior value upon field exit
        saveFocusString = field.getText();		
    } // focusGained

    public void focusLost(FocusEvent e)
    {
        JComponent field = (JComponent) e.getComponent();

        if (field.getName().compareTo("portField") == 0)
        {
            try
            {
                if (Integer.parseInt(portField.getText()) < 1)
                {
                    portField.setText("1");
                    msgBox.append("Reset port to minimum value of 1\n");
                    Toolkit.getDefaultToolkit().beep();
                }

                if (Integer.parseInt(portField.getText()) > 65535)
                {
                    portField.setText(Integer.toString(65535));
                    msgBox.append("Reset port to maximum value of 65535\n");
                    Toolkit.getDefaultToolkit().beep();
                }
            }
            catch (NumberFormatException e1)
            {
                msgBox.append("Non integer '" + portField.getText() 
                        + "' in port field, restoring former value\n");
                portField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }
        } // if portField
        else if (field.getName().compareTo("maxRecordField") == 0)
        {
            try
            {
                if (Integer.parseInt(maxRecordField.getText()) < 1)
                {
                    msgBox.append("Reset Max Record to minimum value of 1\n");
                    maxRecordField.setText("1");
                    Toolkit.getDefaultToolkit().beep();
                }

                if (Integer.parseInt(maxRecordField.getText()) > SeedSocket.MAX_RECORDS)
                {
                    maxRecordField.setText(Integer.toString(SeedSocket.MAX_RECORDS));
                    msgBox.append("Reset Max Record to maximum value of " + SeedSocket.MAX_RECORDS + "\n");
                    Toolkit.getDefaultToolkit().beep();
                }
            }
            catch (NumberFormatException e1)
            {
                msgBox.append("Non integer '" + maxRecordField.getText() 
                        + "' in Max Record field, restoring former value\n");
                maxRecordField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }
        } // if maxRecordField
        else if (field.getName().compareTo("stationField") == 0)
        {
            if (stationField.getText().length() < 1 ||
                    stationField.getText().length() > 5)
            {
                msgBox.append("Station name '" + stationField.getText() 
                        + "' must be between 1 and 5 characters, restoring prior name.\n");
                stationField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();				
            }
        } // if stationField
        else if (field.getName().compareTo("channelField") == 0)
        {
            if (channelField.getText().length() < 1 ||
                    channelField.getText().length() > 3)
            {
                msgBox.append("Channel name '" + channelField.getText() 
                        + "' must be between 1 and 3 characters, restoring prior name.\n");
                channelField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();				
            }
        } // if channelField
        else if (field.getName().compareTo("locationField") == 0)
        {
            if (locationField.getText().length() < 1 ||
                    locationField.getText().length() > 2)
            {
                msgBox.append("Location name '" + locationField.getText() 
                        + "' must be between 1 and 2 characters, restoring prior name.\n");
                locationField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();				
            }
        } // if locationField
        else if (field.getName().compareTo("startDateField") == 0)
        {
            if (startDateField.getText().length() != 10 ||
                    startDateField.getText().charAt(4) != '/' ||
                    startDateField.getText().charAt(7) != '/' ||
                    !Character.isDigit(startDateField.getText().charAt(0)) ||
                    !Character.isDigit(startDateField.getText().charAt(1)) ||
                    !Character.isDigit(startDateField.getText().charAt(2)) ||
                    !Character.isDigit(startDateField.getText().charAt(3)) ||
                    !Character.isDigit(startDateField.getText().charAt(5)) ||
                    !Character.isDigit(startDateField.getText().charAt(6)) ||
                    !Character.isDigit(startDateField.getText().charAt(8)) ||
                    !Character.isDigit(startDateField.getText().charAt(9)))
            {
                msgBox.append("Invalid start date '" + startDateField.getText() 
                        + "', format is yyyy/mm/dd, restoring prior date.\n");
                startDateField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();				
            }
        } // if startDateField
        else if (field.getName().compareTo("startTimeField") == 0)
        {
            if (startTimeField.getText().length() != 8 ||
                    startTimeField.getText().charAt(2) != ':' ||
                    startTimeField.getText().charAt(5) != ':' ||
                    !Character.isDigit(startTimeField.getText().charAt(0)) ||
                    !Character.isDigit(startTimeField.getText().charAt(1)) ||
                    !Character.isDigit(startTimeField.getText().charAt(3)) ||
                    !Character.isDigit(startTimeField.getText().charAt(4)) ||
                    !Character.isDigit(startTimeField.getText().charAt(6)) ||
                    !Character.isDigit(startTimeField.getText().charAt(7)))
            {
                msgBox.append("Invalid start time '" + startTimeField.getText() 
                        + "', format is HH:MM:SS, restoring prior time.\n");
                startTimeField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();				
            }
        } // if startTimeField
        else if (field.getName().compareTo("finishDateField") == 0)
        {
            if (finishDateField.getText().length() != 10 ||
                    finishDateField.getText().charAt(4) != '/' ||
                    finishDateField.getText().charAt(7) != '/' ||
                    !Character.isDigit(finishDateField.getText().charAt(0)) ||
                    !Character.isDigit(finishDateField.getText().charAt(1)) ||
                    !Character.isDigit(finishDateField.getText().charAt(2)) ||
                    !Character.isDigit(finishDateField.getText().charAt(3)) ||
                    !Character.isDigit(finishDateField.getText().charAt(5)) ||
                    !Character.isDigit(finishDateField.getText().charAt(6)) ||
                    !Character.isDigit(finishDateField.getText().charAt(8)) ||
                    !Character.isDigit(finishDateField.getText().charAt(9)))
            {
                msgBox.append("Invalid finish date '" + finishDateField.getText() 
                        + "', format is yyyy/mm/dd, restoring prior date.\n");
                finishDateField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();				
            }
        } // if finishDateField
        else if (field.getName().compareTo("finishTimeField") == 0)
        {
            if (finishTimeField.getText().length() != 8 ||
                    finishTimeField.getText().charAt(2) != ':' ||
                    finishTimeField.getText().charAt(5) != ':' ||
                    !Character.isDigit(finishTimeField.getText().charAt(0)) ||
                    !Character.isDigit(finishTimeField.getText().charAt(1)) ||
                    !Character.isDigit(finishTimeField.getText().charAt(3)) ||
                    !Character.isDigit(finishTimeField.getText().charAt(4)) ||
                    !Character.isDigit(finishTimeField.getText().charAt(6)) ||
                    !Character.isDigit(finishTimeField.getText().charAt(7)))
            {
                msgBox.append("Invalid finish time '" + finishTimeField.getText() 
                        + "', format is HH:MM:SS, restoring prior time.\n");
                finishTimeField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();				
            }
        } // if finishTimeField
    } // focusLost()

    final static int GAP = 10;
    public static final int DEFAULT_WIDTH = 1150;
    public static final int DEFAULT_HEIGHT = 640;
    private int iSort;
    private boolean bCancel=false;
    private String saveFocusString;
    private JFileChooser localSeedFileFrame;
    private JButton transferButton;
    private JButton listAllButton;
    private JButton listFilteredButton;
    private JButton cancelButton;
    private JButton quitButton;
    private JCheckBox extendFilenameCheckBox;
    private ASPPreferences prefs;
    private TransferTask transferTask;
    private JList channelListBox;
    private DefaultListModel channelListModel;
    private JTextArea msgBox;
    private JScrollPane scrollMsgBox;
    private JFormattedTextField stationField;
    private JFormattedTextField channelField;
    private JFormattedTextField locationField;
    private JFormattedTextField startDateField;
    private JFormattedTextField startTimeField;
    private JFormattedTextField finishDateField;
    private JFormattedTextField finishTimeField;
    private JFormattedTextField hostField;
    private JFormattedTextField portField;
    private JFormattedTextField maxRecordField;

    private JComponent listPanel;
    private JComponent movePanel;
    private JComponent actionPanel;
    private JComponent entryPanel;
    private JComponent timeSpanPanel;

} // class TransferFrame
