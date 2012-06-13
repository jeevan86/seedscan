package asl.aspseed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This class creates a background thread which connects to a server and 
 * gets a list of seed channels that match filter parameters.  There are also
 * calls which allow the calling program to keep track of the status of the
 * background thread.
 * @author fshelly
 */

public class DirSocket extends Thread
{
    private Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private String hostname;
    private int port;
    private String id;
    private String startTime;
    private String endTime;
    private String chanList[] = null;
    private boolean bDone=false;
    private int channelCount=0;
    private String errMsg;

    public DirSocket(String hostname, int port, String id,
            String startTime, String endTime)
    {
        // Run Thread constructor
        super();

        this.hostname = hostname;
        this.port = port;
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.errMsg = null;
    } // SeedSocket() constructor

    /* 
     * Starts a background thread which connects up to a server and gets a
     * listing of seed records matching the filter passed in via the DirSocket 
     * constructor. 
     * @see java.lang.Thread#run()
     */
    public void run()
    {
        int  readCount;
        String nextLine;

        try
        {
            socket = new Socket(hostname, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + hostname + ":" + port);
            errMsg = "Unknown host: " + hostname + ":" + port;
            bDone = true;
            return;
        } catch (IOException e) {
            System.err.println("Unable to connect to: " + hostname + ":" + port);
            errMsg = "Unable to connect to: " + hostname + ":" + port;
            bDone = true;
            return;
        } // End try/catch block

        System.out.println("Socket connection established to " + hostname + ":" + port);

        // Send DATREQ to server
        String datreq = "DIRREQ " + id + " " + startTime + " " + endTime + '\u0000';
        out.write(datreq);
        out.flush();

        // Get return data
        channelCount = 0;
        try
        {
            // Get line listing count of channels
            nextLine = in.readLine();
            if (nextLine != null)
            {
                if (nextLine.substring(0,5).compareTo("Error") == 0)
                {
                    System.err.println(nextLine);
                    while ((nextLine = in.readLine()) != null)
                    {
                        if (nextLine.compareTo("FINISHED") != 0)
                        {
                            System.err.println(nextLine);
                            errMsg = nextLine;
                        }
                    }
                } // If there was an error string returned
                else
                {
                    String countStr = nextLine.substring(0, nextLine.indexOf(' ', 0));
                    if (Character.isDigit(countStr.charAt(0)))
                        channelCount = Integer.valueOf(countStr).intValue();
                    else
                        channelCount = 0;

                    if (channelCount > 0)
                        chanList = new String[channelCount];

                } // Not an error return
            } // We have lines of text
            else
            {
                channelCount = 0;
            }
            readCount = 0;
            while ((nextLine = in.readLine()) != null && readCount < channelCount)
            {
                if (nextLine.compareTo("FINISHED") == 0)
                    break;
                chanList[readCount++] = nextLine;
            } // while we are successfully reading data
        } 
        catch (IOException e1)
        {
            System.err.println("Unexpected IOException reading data from input stream, ABORT!");
            e1.printStackTrace();
            errMsg = "Unexpected IOException reading data from input stream, ABORT!";
            bDone = true;
            return;
        }
        try
        {
            socket.close();
        } catch (IOException e)
        {
            ; // Don't care if I get an exception on a close
        }
        socket = null;

        bDone = true;
    } // run()

    /**
     * @return True when the thread started by run() has finished getting its
     *  results.
     */
    public boolean Done()
    {
        return bDone;
    }

    /**
     * @return How many seed files matched last finished query.  Note that 
     *  Done() method should return true before this value means something.
     */
    public int GetChannelCount()
    {
        return channelCount;
    }

    /**
     * @return String array containing list of seed channels that match the filter
     * arguments from the class constructor.  Only valid after Done() method
     * returns true.
     * String array contains lines of the following format:
     * <location>-<channel> <start date> <end date> <number of seed records>
     *    Where <date> is of the format yyyy/mm/dd hh:mm:ss
     * 
     * Example:
     * 00-BHZ 2008/09/03 09:55:57  2008/09/08 13:54:17  2599
     * 00-BHN 2008/09/03 09:55:57  2008/09/08 13:54:17  2599
     * 00-BHE 2008/09/03 09:55:57  2008/09/08 13:54:17  2600
     * 
     */
    public String[] GetChannelList()
    {
        return chanList;
    }

    public String GetErrMsg()
    {
        return errMsg;
    }
} // class DirSocket
