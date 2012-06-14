package asl.ofcweb;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import seed.IllegalSeednameException;
import seed.MiniSeed;
import seed.Utility;

/**
 * @author fshelly
 *Applet to display OFC records on a web page
 */

/**
 * Top level frame for display GUI, everything else fits inside.
 */
public class OFCWeb
    extends     JApplet
    implements  ActionListener
{
    private static final long serialVersionUID = 1L;
    private URL ofcURL;

    public final int  DEFAULT_X_SIZE = 600;
    public final int  DEFAULT_Y_SIZE = 350;

    public static void main(String[] args)
    {
        System.err.println("Dummy main routine");
        System.exit(1);
    } // dummy main

    public void init()
    {
        panel = new JPanel();

        falconChannel = getParameter("channel");
        if (falconChannel == null)
        {
            falconChannel = "OFC";
            System.err.printf("Parameter channel undefined, defaulting to %s\n",
                    falconChannel);
        }

        String ticks = getParameter("minutespertick");
        if (ticks == null)
        {
            ticks = "1";
            System.err.printf(
                    "Parameter minutespertick undefined, defaulting to %s\n",
                    ticks);
        }
        minutesPerTick = Integer.parseInt(ticks);

        ofcdataFileName = getParameter("ofcdata");
        if (ofcdataFileName == null)
        {
            ofcdataFileName = new String("HRV_ofc.seed");
            System.err.printf(
                    "Parameter ofcdata undefined, defaulting to %s\n",
                    ofcdataFileName);
        }
        ofcURL = OFCWeb.class.getResource(ofcdataFileName);
        if (ofcURL == null)
        {
            System.err.printf("Unable to open resource %s, aborting!\n", 
                    ofcdataFileName);
            System.exit(1);
        }

        this.getContentPane().setLayout(new FlowLayout());
        panel.setPreferredSize(new Dimension(this.getWidth(), this.getHeight()));

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        getContentPane().add(panel);


        // ======== graphViewJPanel ========
        timeChartPlot = new TimeChartPlotter(station,
                network, falconChannel);
        graphViewJPanel = timeChartPlot.createTimePanel();
        graphViewJPanel.setMinimumSize(new Dimension(600, 300));
        graphViewJPanel.setPreferredSize(
                new Dimension(this.getWidth(), this.getHeight()));
        graphViewJPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(graphViewJPanel, BorderLayout.CENTER);

        //======== labelJPanel ========
        labelJPanel = new JPanel();
        labelJPanel.setMaximumSize(new Dimension(600, 30));
        labelJPanel.setMinimumSize(new Dimension(600, 30));
        labelJPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        labelJPanel.setPreferredSize(new Dimension(600, 30));
        labelJPanel.setLayout(new GridLayout(1, 4));
        panel.add(labelJPanel);

        showCheckBox = new JCheckBox[3];
        for (int i=0; i < 3; i++)
        {
            if (i==0) showCheckBox[0] = new JCheckBox("Min " + falconChannel);
            if (i==1) showCheckBox[1] = new JCheckBox("Max " + falconChannel);
            if (i==2) showCheckBox[2] = new JCheckBox("Avg " + falconChannel);
            showCheckBox[i].setSelected(true);
            showCheckBox[i].setAlignmentX(CENTER_ALIGNMENT);
            showCheckBox[i].addActionListener(this);
            labelJPanel.add(showCheckBox[i]);
        }

        dateButton = new JButton("Date Range");
        dateButton.addActionListener(this);
        dateButton.setAlignmentX(CENTER_ALIGNMENT);
        Dimension size = dateButton.getSize();
        size.setSize(size.getWidth()*2/3, size.getHeight());
        dateButton.setSize((dateButton.getWidth()*2)/3, dateButton.getHeight());
        labelJPanel.add(dateButton);

        fileDateStart=null;
        fileDateEnd=null;
        displayDateStart=null;
        displayDateEnd=null;

        // Plot the data
        ofcPlot();

    }// init()

    public void ofcPlot()
    {
        //
        // First get a filtered directory listing for error checking
        //
        byte seedBytes[] = new byte[512];
        int byteCount;
        int readCount;
        int iSize;
        int iDataOffset;
        boolean bContinue = false;
        String fchan;

        iTotalDataCount = 0;

        try
        {
            System.err.println("Opening " + ofcURL);
            ofcFile = ofcURL.openStream();
        } catch (FileNotFoundException e)
        {
            System.err.println("Unable to open OFC file " + ofcURL);
            System.exit(1);
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        // Read records until file is empty
        try
        {
            System.err.printf("Loading %s data from %s, avail=%d\n", 
                    falconChannel, ofcdataFileName, ofcFile.available());
            showStatus("Downloading " + falconChannel + " data from " + ofcURL);				

            byteCount = 0;
            readCount = 0;

            while (readCount >= 0)
            {
                if (byteCount >= 512)
                    byteCount = 0;

                readCount = ofcFile.read(seedBytes, byteCount, 512 - byteCount);
                byteCount += readCount;

                if (byteCount == 512)
                {
                    wantCCCLL = "OFC90";

                    // Pull the seed records off of the queue
                    try
                    {
                        seedRecord = new MiniSeed(seedBytes);
                    } catch (IllegalSeednameException e1)
                    {
                        // Problems with seed record, warn and skip
                        continue;
                    }

                    // We have a new seed record see if it is the right channel
                    String newCCCLL = seedRecord.getSeedName().substring(7);
                    if (wantCCCLL.compareToIgnoreCase(newCCCLL) == 0)
                    {
                        // Time to pull out the opaque blockettes
                        int iBlocketteCount = seedRecord.getNBlockettes();
                        int blocketteOffset;
                        int blocketteType;
                        boolean bFoundB2000=false;
                        for (int ib = 0; ib < iBlocketteCount; ib++)
                        {
                            blocketteType = seedRecord.getBlocketteType(ib);

                            if (blocketteType == 2000)
                            {
                                // We have an opaque blockette type
                                blocketteOffset = seedRecord.getBlocketteOffset(ib);
                                iSize = Utility.uBytesToInt(seedBytes[blocketteOffset+4],
                                        seedBytes[blocketteOffset+5], !seedRecord.isBigEndian());
                                iDataOffset = Utility.uBytesToInt(seedBytes[blocketteOffset+6],
                                        seedBytes[blocketteOffset+7], !seedRecord.isBigEndian());
                                iDataOffset += blocketteOffset;

                                byte[] b2000 = new byte[iSize];
                                System.arraycopy(seedBytes, blocketteOffset, b2000, 0, iSize);

                                if (!bFoundB2000)
                                {
                                    fchan = new String(b2000).substring(15,20);
                                    if (fchan.compareTo("FALC~") != 0)
                                    {
                                        System.err.printf("Found unexpected opaque channel type %s\n",
                                                fchan);
                                        continue; // Not the right type of opaque blockette
                                    }
                                    bFoundB2000 = true;
                                }

                                if (getOFCdescription(b2000, !seedRecord.isBigEndian()).
                                        compareTo(falconChannel) != 0)
                                    continue;  // only interested in specific falcon channel

                                if (!bContinue)
                                {
                                    ofcBlock = new OFCb2000(b2000);
                                }
                                else
                                {
                                    System.err.printf("Continuation blockette %s %s, needs debuging\n",
                                            falconChannel, new Date(ofcBlock.get_start_time()).toString());
                                    ofcBlock.Continue(b2000);
                                }
                                bContinue = ofcBlock.get_Continuation();

                                if (bContinue)
                                {
                                    continue;
                                }

                                if (ofcBlock.get_average_data() == null ||
                                        ofcBlock.get_average_data().length == 0)
                                    continue;  // no data to plot

                                if (fileDateStart == null)
                                    fileDateStart = new Date(ofcBlock.get_start_time()*1000);
                                fileDateEnd = new Date(ofcBlock.get_end_time()*1000);

                                if (displayDateStart != null)
                                {
                                    if (displayDateStart.getTime()/1000 > ofcBlock.get_end_time())
                                        continue;
                                }

                                if (displayDateEnd != null)
                                {
                                    if (displayDateEnd.getTime()/1000 + 86400 < ofcBlock.get_start_time())
                                        continue;
                                }
                                /*
                                   System.err.printf(
                                   "DEBUG: %s ; type %d; length %d; offset %d; record %d; order %d; flags %02x\n",
                                   ofcBlock.get_header_fields(), ofcBlock.get_type(), ofcBlock.get_length(), 
                                   ofcBlock.get_data_offset(), ofcBlock.get_record_number(),
                                   ofcBlock.get_word_order(), ofcBlock.get_data_flags());

                                   System.err.printf("DEBUG description: %s\n", ofcBlock.get_name());
                                   System.err.printf("DEBUG lines:       %d\n", ofcBlock.get_average_data().length);
                                   System.err.printf("DEBUG              Time Average Low High\n");

                                   long current_time = ofcBlock.get_start_time();
                                   for (int i=0; i < ofcBlock.get_average_data().length; i++)
                                   { 
                                   System.err.printf(" DEBUG %s[%d]:  %s %d %d %d\n", ofcBlock.get_name(), i+1, 
                                   new Date(current_time*1000), ofcBlock.get_average_data()[i], 
                                   ofcBlock.get_low_data()[i], ofcBlock.get_high_data()[i]);
                                   current_time += 60;
                                   }
                                 */
                                if (station.length() == 0 || network.length() == 0)
                                {
                                    // We need to label the title according to the station
                                    network = seedRecord.getSeedName().substring(0, 2);
                                    station = seedRecord.getSeedName().substring(2, 7).trim();
                                    timeChartPlot.SetTitle(station, network,
                                            falconChannel);
                                }

                                iTotalDataCount += ofcBlock.get_average_data().length;
                                GregorianCalendar startTime =
                                    new GregorianCalendar(TimeZone.getTimeZone("GMT"));
                                startTime.setTimeInMillis(ofcBlock.get_start_time()*1000);
                                timeChartPlot.AddNewData(ofcBlock.get_low_data(),
                                        ofcBlock.get_high_data(), ofcBlock.get_average_data(),
                                        startTime, minutesPerTick);
                            } // blockette 2000
                        } // loop through all blockettes
                    } // Seed record was an OFC channel
                } // we have read a full record
            } // while we are successfully reading data

            System.err.printf("Done %s data from %s, %d values\n", 
                    falconChannel, ofcdataFileName, iTotalDataCount);
            ofcFile.close();

            if (fileDateStart != null)
            {
                if (displayDateStart == null)
                    displayDateStart = (Date) fileDateStart.clone();
                if (displayDateEnd == null)
                    displayDateEnd = (Date) fileDateEnd.clone();
            }
            else
            {
                showStatus("Failed to retrieve data from " + ofcURL + ", try refresh");				
            }

        } catch (IOException e1)
        {
            System.err
                .println("Unexpected IOException reading data from input stream, ABORT!");
            e1.printStackTrace();
        }
    } // ofcPlot()

    private Frame findParentFrame(){ 
        Container c = this; 
        while(c != null){ 
            if (c instanceof Frame) 
                return (Frame)c; 

            c = c.getParent(); 
        } 
        return (Frame)null; 
    } 

    private String getOFCdescription(byte[] b2000, boolean bSwapBytes)
    {
        String description="";

        int desc_len;
        int data_offset;
        byte [] desc_bytes;

        data_offset = Utility.uBytesToInt(b2000[6], 
                b2000[7], bSwapBytes);

        desc_len = (int)(b2000[38+data_offset]);

        if ((desc_len < 1) || (desc_len > 8))
        {
            return description;
        }

        desc_bytes = new byte[desc_len];
        for (int i=0; i < desc_len; i++)
        {
            desc_bytes[i] = b2000[data_offset + 39+i];
        }
        description = new String(desc_bytes);

        return description;
    } // getOFCdescription()

    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        for (int i=0; i < 3; i++)
        {
            if (source == showCheckBox[i])
            {
                // user hit checkbox
                if(!showCheckBox[i].isSelected())
                {
                    timeChartPlot.removeDataSet(i);
                }
                else
                {
                    timeChartPlot.addDataSet(i);
                }				
            }
        } // loop through each line check box

        if (source == dateButton)
        {
            if (fileDateStart != null)
            {
                dateSelect = new DateSelect(
                        findParentFrame(), 
                        fileDateStart, 
                        fileDateEnd,
                        displayDateStart, 
                        displayDateEnd,
                        daysBack);			
                dateSelect.setVisible(true);

                if (!dateSelect.bCancel)
                {
                    // User hit OK button so lets plot again
                    timeChartPlot.clearDataSet();
                    displayDateStart.setTime(dateSelect.getStartDate().getTime());
                    displayDateEnd.setTime(dateSelect.getEndDate().getTime());

                    minutesPerTick = (int)
                        ((displayDateEnd.getTime() - displayDateStart.getTime())/60000) /
                        2000;
                    if (minutesPerTick < 1)
                        minutesPerTick = 1;
                    System.err.printf("ofcplot(%s .. %s, ticks %d\n", 
                            displayDateStart.toString(), displayDateEnd.toString(), minutesPerTick);
                    ofcPlot();
                } // User hit OK button in dialog
            } // There is data present in file
        } // dateButton
    } // actionPerformed()

    /**
     * Called when the GUI exit button is clicked. Allows GUI to save persistent
     * state information for next launch.
     */
    final static int            GAP            = 10;

    private String							station="";
    private String							network="";
    private String              falconChannel="ITS";
    private String							ofcdataFileName;
    private int									minutesPerTick=1;

    private int									iTotalDataCount;

    private JPanel							panel;
    private JPanel              graphViewJPanel;
    private JPanel 							labelJPanel;
    private TimeChartPlotter    timeChartPlot;
    private JCheckBox						showCheckBox[];
    private JButton							dateButton;
    private DateSelect					dateSelect;

    private Date								fileDateStart=null;
    private Date								fileDateEnd=null;
    private Date								displayDateStart=null;
    private Date								displayDateEnd=null;
    private int									daysBack=0;

    private String 							wantCCCLL;

    private MiniSeed            seedRecord;

    private InputStream     		ofcFile;

    private OFCb2000						ofcBlock;

} // class OFCWeb

