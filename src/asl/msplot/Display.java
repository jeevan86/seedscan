package asl.msplot;

import java.awt.BorderLayout;
import java.awt.Container;
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
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import seed.IllegalSeednameException;
import seed.MiniSeed;
import seed.SteimException;

public class Display
{
    private PlotFrame frame;

    public Display()
    {
        WindowListener listener = new Terminator();
        frame = new PlotFrame();
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.addWindowListener(listener);
        frame.setVisible(true);
    } // Display() constructor

    /**
     * Called when the GUI exit button is clicked. Allows GUI to save persistent
     * state information for next launch.
     */
    class Terminator extends WindowAdapter
    {
        public void windowClosing(WindowEvent e)
        {
            frame.SavePrefs(); // SavePrefs also exits program
        }
    } // class Terminator
} // class Display

/**
 * Top level frame for Display GUI, everything else fits inside.
 */
class PlotFrame extends JFrame implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private Logger logger = Logger.getLogger("asl.msplot.PlotFrame");

    public PlotFrame()
    {
        prefs = new MSPreferences();
        setTitle("Station Seismic Data LISS plotter");
        setPreferredSize(new Dimension(prefs.GetWidth(), prefs.GetHeight()));
        setBounds(prefs.GetOriginX(), prefs.GetOriginY(), 
                prefs.GetWidth(), prefs.GetHeight());

        // Set up saved preference fields
        localSeedFileFrame = new JFileChooser();
        localSeedFileFrame.setCurrentDirectory(new File(prefs.GetLocalDir()));
        hostField = prefs.GetHostname();
        portField = prefs.GetPort();
        minRange = prefs.GetMinRange();
        secondsDuration = prefs.GetSecondsDuration();
        unitDivisor = prefs.GetUnitDivisor();
        locChan1 = prefs.GetLocChan1();
        locChan2 = prefs.GetLocChan2();
        locChan3 = prefs.GetLocChan3();

        // ======== this ========
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        // ======== mainMenuBar ========
        {
            mainMenuBar = new JMenuBar();
            fileJMenu = new JMenu();
            exitJMenu = new JMenuItem();
            setupJMenu = new JMenuItem();

            mainMenuBar.setPreferredSize(new Dimension(166, 21));

            // ======== fileJMenu ========
            {
                fileJMenu.setText(" File  ");

                // ---- setupJMenu ----
                setupJMenu.setText("Setup");
                setupJMenu.addActionListener(this);
                fileJMenu.add(setupJMenu);

                // ---- exitJMenu ----
                exitJMenu.setText("Exit");
                exitJMenu.addActionListener(this);
                fileJMenu.add(exitJMenu);
            } // fileJMenu
            mainMenuBar.add(fileJMenu);

            // All done setting up menu bar
            setJMenuBar(mainMenuBar);
        } // mainMenuBar

        // ======== graphViewJPanel ========
        String chanList[] = new String [] {locChan1, locChan2, locChan3};
        timeChartPlot = new TimeChartPlotter(3, secondsDuration, minRange,
                chanList, station, network);
        graphViewJPanel = timeChartPlot.createTimePanel();
        graphViewJPanel.setMinimumSize(new Dimension(600, 300));
        graphViewJPanel.setPreferredSize(new Dimension(prefs.GetWidth(),
                    prefs.GetHeight()));
        graphViewJPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(graphViewJPanel, BorderLayout.CENTER);

        displayPanel = new JPanel(new BorderLayout());
        add(displayPanel, BorderLayout.SOUTH);

        textArea = new JTextArea();
        textPane = new JScrollPane(textArea);

        displayPanel.add(textPane, BorderLayout.CENTER);

        // Status field
        statusField = new JTextField();
        statusField.setEditable(false);
        statusField.setText("Initializing...");
        displayPanel.add(statusField, BorderLayout.SOUTH);

        // Start the data Transfer going
        transferTask = new TransferTask();
        transferTask.execute();
    }// constructor PlotFrame()

    class TransferTask extends SwingWorker<Object, Object>
    {
        /*
         * Main task. Executed in background thread.
         */
        public Object doInBackground()
        {
            Transfer();
            return null;
        } // doInBackground()

        /*
         * Executed in event dispatching thread
         */
        public void done()
        {
            setCursor(null); // turn off the wait cursor
            Toolkit.getDefaultToolkit().beep();
        } // done()
    } // class TransferTask

    public void Transfer()
    {
        //
        // First get a filtered directory listing for error checking
        //
        String hostname = hostField;
        int port = portField;
        int iCount = 0;
        int iSeedCount;
        int iMatchCount=0;
        BlockingQueue<SeedRawRecord> queue;
        SeedRawRecord head;
        OutputStream outfile = null;

        // loop until transfer is canceled
        while (!bCancel)
        {
            if (bResetConnection)
            {
                // Build up an id string
                hostname = hostField;
                port = portField;
                iMatchCount = 0;
                iSeedCount = 0;

                bResetConnection = false;
            } // Somebody requested that we reset network connection

            statusField.setText("Connecting to host " + hostname + ":" + port + "\n");

            // Get data for this channel and time period
            LISSsocket getSeedThread = new LISSsocket(hostname, port, outfile);
            getSeedThread.start();

            // Give time for a connection to be attempted
            try
            {
                Thread.sleep(2000);
            } catch (InterruptedException e)
            {
                statusField.setText(
                        "Error Transfer(): Sleep failed waiting for connection");
                break;
            }			
            if (getSeedThread.IsConnected())
            {
                statusField.setText("Connected to host " + hostname + ":" + port + "\n");
            }
            else
            {
                if (getSeedThread.IsBadHost())
                {
                    statusField.setText("Unknown host " + hostname + ":" + port + "\n");
                    while (!bResetConnection)
                    {
                        try
                        {
                            Thread.sleep(5000);
                        } catch (InterruptedException e)
                        {
                            statusField.setText(
                                    "Error Transfer(): Sleep failed waiting for connection");
                            break;
                        }			
                    }
                } // hostname was bad
                else
                    statusField.setText("Failed connection " + hostname + ":" + port + "\n");
                try
                {
                    Thread.sleep(5000);
                } catch (InterruptedException e)
                {
                    statusField.setText(
                            "Error Transfer(): Sleep failed waiting for connection");
                    break;
                }			
                continue;
            }

            queue = getSeedThread.GetQueue();

            Calendar tStart = null;
            Calendar tEnd = null;
            while (!getSeedThread.Done() && getSeedThread.isAlive())
            {
                if (bCancel || bResetConnection)
                {
                    if (!getSeedThread.GetCancel())
                    {
                        statusField.setText("Closing network connection");
                        getSeedThread.SetCancel(true);
                    }
                    continue;
                }

                wantCCCLL1 = LL_CCC2CCCLL(locChan1);
                wantCCCLL2 = LL_CCC2CCCLL(locChan2);
                wantCCCLL3 = LL_CCC2CCCLL(locChan3);

                tEnd = new GregorianCalendar();
                if (tStart != null) {
                    long diff = tEnd.getTimeInMillis() - tStart.getTimeInMillis();
                    System.err.println(String.format("Composition Duration: %d ms", diff));
                }
                tStart = null;
                System.err.println(String.format("[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL]> Checking queue...", tEnd));

                try
                {
                    head = queue.poll(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e1)
                {
                    // Unexpected error removing item from queue
                    statusField.setText("Error removing record from read queue");
                    e1.printStackTrace();
                    continue;
                }


                // Check for timeout
                if (head == null)
                {
                    continue;
                }


                if (bCancel || bResetConnection)
                {
                    if (!getSeedThread.GetCancel())
                    {
                        statusField.setText("Closing network connection");
                        getSeedThread.SetCancel(true);
                    }
                    continue;
                }

                iCount++;
                iSeedCount = getSeedThread.GetRecordCount();

                // Pull the seed records off of the queue
                try
                {
                    seedRecord = new MiniSeed(head.GetRecord());
                } catch (IllegalSeednameException e1)
                {
                    // Problems with seed record, warn and skip
                    statusField.setText("Error in seed record, skipping");
                    continue;
                }

                // We have a new seed record see if it is the right channel
                String newCCCLL = seedRecord.getSeedName().substring(7);
                iRow = -1;
                if (wantCCCLL1.compareToIgnoreCase(newCCCLL) == 0)
                    iRow = 0;
                if (wantCCCLL2.compareToIgnoreCase(newCCCLL) == 0)
                    iRow = 1;
                if (wantCCCLL3.compareToIgnoreCase(newCCCLL) == 0)
                    iRow = 2;
                if ((iRow >= 0)	&& (seedRecord.getNsamp() > 0))
                {
                    tStart = new GregorianCalendar();
                    System.err.println(String.format("[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL]> Got record from queue.", tStart));

                    iMatchCount++;

                    try
                    {
                        EventQueue.invokeAndWait(new
                                Runnable()
                                {
                                @Override
                                public void run()
                                {
                                int newData[];
                                // This is what we wanted, plot it
                                try
                                {
                                newData = seedRecord.decomp();
                                }
                                catch(SteimException e)
                                {
                                newData = null;
                                statusField.setText("Steim Exception raised, aborting");
                                System.err.println("Steim Exception raised.");
                                e.printStackTrace();
                                System.exit(1);
                                }

                                if (station.length() == 0 || network.length() == 0)
                                {
                                    // We need to label the title according to the station
                                    network = seedRecord.getSeedName().substring(0,2);
                                    station = seedRecord.getSeedName().substring(2,7).trim();
                                    timeChartPlot.SetTitle(station, network);
                                }

                                //System.out.println("Collected " + newData.length + " data values, rate = " 
                                //+ seedRecord.getRate());

                                timeChartPlot.AddNewData(newData, iRow,
                                        seedRecord.getRate(), seedRecord.getGregorianCalendar());
                                } // run()

                                }); // EventQueue.invokeAndWait()
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                        System.exit(1);
                    } catch (InvocationTargetException e)
                    {
                        e.printStackTrace();
                        System.exit(1);
                    }
                } // Seed record matched the channel we are plotting
                if (iSeedCount > 0) {
                    statusField.setText("Records " + iMatchCount + "/"+ iSeedCount);
                }

            } // loop until data collection thread says it is done

            statusField.setText("Disconnected after " + getSeedThread.GetRecordCount()
                    + " records");
            try
            {
                Thread.sleep(5000);
            } catch (InterruptedException e)
            {
                statusField
                    .setText("Error Transfer(): Sleep failed waiting for SeedThread\n");
                return;
            }
        } // while not canceled
    } // Transfer()

    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();
        Object source = e.getSource();
        if (source == exitJMenu)
        {
            SavePrefs(); // SavePrefs also exits program
        } else if (source == setupJMenu)
        {
            Setup dialog = new Setup(this, 
                    prefs.GetHostname(), prefs.GetPort(), prefs.GetMinRange(),
                    prefs.GetLocChan1(), prefs.GetLocChan2(), prefs.GetLocChan3(), 
                    prefs.GetSecondsDuration(), prefs.GetUnitDivisor());
            dialog.setVisible(true);
            if (dialog.GetNetChange())
            {
                hostField = dialog.GetHostname();
                portField = dialog.GetPort();
                bResetConnection = true;
            }

            if (dialog.GetDisplayChange() || dialog.GetSmallChange() 
                    || dialog.GetNetChange())
            {
                // Something modified so save changes
                locChan1 = dialog.GetLocChan1();
                locChan2 = dialog.GetLocChan2();
                locChan3 = dialog.GetLocChan3();
                secondsDuration = dialog.GetSecondsDuration();
                unitDivisor = dialog.GetUnitDivisor();
                minRange = dialog.GetMinRange();

                // Remember any changes to preferences
                prefs.SetLocalDir(localSeedFileFrame.getCurrentDirectory()
                        .getAbsolutePath());
                prefs.SetHostname(hostField);
                prefs.SetPort(portField);
                prefs.SetLocChan1(locChan1);
                prefs.SetLocChan2(locChan2);
                prefs.SetLocChan3(locChan3);
                prefs.SetSecondsDuration(secondsDuration);
                prefs.SetUnitDivisor(unitDivisor);
                prefs.SetMinRange(minRange);
                prefs.SavePreferences();
                if (dialog.GetDisplayChange() || dialog.GetNetChange())
                {
                    station = "";
                    network = "";
                    Dimension rememberSize = getSize();
                    remove(graphViewJPanel);
                    String chanList[] = new String [] {locChan1, locChan2, locChan3};
                    timeChartPlot = new TimeChartPlotter(3, secondsDuration, minRange,
                            chanList, station, network);
                    graphViewJPanel = timeChartPlot.createTimePanel();
                    graphViewJPanel.setMinimumSize(new Dimension(600, 300));
                    graphViewJPanel.setPreferredSize(rememberSize);
                    graphViewJPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
                    remove(statusField);
                    add(graphViewJPanel, BorderLayout.CENTER);
                    add(statusField, BorderLayout.SOUTH);
                    setPreferredSize(rememberSize);
                    pack();
                    repaint();		
                }

                if (dialog.GetSmallChange())
                {
                    timeChartPlot.SetTimeSpan(secondsDuration);
                    timeChartPlot.SetMinRange(minRange);
                }
            }
        } else if (command.compareTo("Exit") == 0)
        {
            SavePrefs(); // SavePrefs also exits program
        } else
        {
            System.err.println("Unmanaged actionPerformed " + command);
        }
    } // actionPerformed()

    /**
     * Saves persistent state information so that it can be retrieved the next
     * time the program is run.
     */
    public void SavePrefs()
    {
        System.err.println("DEBUG SavePrefs entered, window: " 
                + this.getX() + "," + this.getY() + "; " 
                + this.getWidth() + "," + this.getHeight());
        prefs.SetMinRange(minRange);
        prefs.SetLocalDir(localSeedFileFrame.getCurrentDirectory()
                .getAbsolutePath());
        prefs.SetHostname(hostField);
        prefs.SetPort(portField);
        prefs.SetLocChan1(locChan1);
        prefs.SetLocChan2(locChan2);
        prefs.SetLocChan3(locChan3);
        prefs.SetSecondsDuration(secondsDuration);
        prefs.SetUnitDivisor(unitDivisor);
        prefs.SetOriginX(this.getX());
        prefs.SetOriginY(this.getY());
        prefs.SetHeight(this.getHeight());
        prefs.SetWidth(this.getWidth());

        // Remember any changes to preferences
        prefs.SavePreferences();
        bCancel = true;
        System.exit(0);
    } // SavePrefs()

    private String LL_CCC2CCCLL(String ll_ccc)
    {
        String cccll;

        if (ll_ccc.length() == 6)
            cccll = ll_ccc.substring(3,6) + ll_ccc.substring(0,2);
        else if (ll_ccc.length() == 0)
            cccll = "";
        else
            cccll = ll_ccc.substring(ll_ccc.length() - 3) + "  ";

        return cccll;
    } // LL_CCC2CCCLL()

    final static int            GAP            = 10;

    private String              locChan1;
    private String              locChan2;
    private String              locChan3;


    private String              hostField;
    private int                 portField;
    private JFileChooser        localSeedFileFrame;
    private MSPreferences         prefs;

    private String							station="";
    private String							network="";
    private int                 secondsDuration;
    private int                 unitDivisor;
    private int                 minRange;

    // Menu
    private JMenuBar            mainMenuBar;
    private JMenu               fileJMenu;
    private JMenuItem           exitJMenu;
    private JMenuItem           setupJMenu;

    private JPanel              graphViewJPanel;
    private TimeChartPlotter    timeChartPlot;
    private JPanel				displayPanel;
    private JTextArea			textArea;
    private JScrollPane 		textPane;
    private JTextField          statusField;

    private String 							wantCCCLL1;
    private String              wantCCCLL2;
    private String              wantCCCLL3;
    private int                 iRow;
    private boolean             bCancel        = false;
    private boolean							bResetConnection = false;
    private TransferTask        transferTask;

    private MiniSeed            seedRecord;

} // class PlotFrame
