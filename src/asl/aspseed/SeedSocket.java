package asl.aspseed;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This class creates a background thread which connects to a server and 
 * downloads a seed data, appending it to the supplied file.  There are also 
 * calls which allow the calling program to keep track of the status of the 
 * download.
 * @author fshelly
 *
 */

public class SeedSocket extends Thread
{
    // Maximum number of records that we allow to be transfered at one time
    public static final int MAX_RECORDS = 4000000;

    private Socket socket = null;
    private PrintWriter out = null;
    private String hostname;
    private int port;
    private String id;
    private String date;
    private String time;
    private int duration;
    private OutputStream outfile;
    private boolean bDone=false;
    private int recordCount=0;
    private boolean bCancel=false;

    public SeedSocket(String hostname, int port, String id, String date, 
            String time, int duration, OutputStream outfile)
    {
        // Run Thread constructor
        super();

        this.hostname = hostname;
        this.port = port;
        this.id = id;
        this.date = date;
        this.time = time;
        this.duration = duration;
        this.outfile = outfile;
        this.bCancel = false;
    } // SeedSocket() constructor

    public void run()
    {
        byte seedrecord[] = new byte[512];
        char charfield[] = new char[5];
        String field;
        int  byteCount;
        int  readCount;

        try
        {
            socket = new Socket(hostname, port);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + hostname + ":" + port);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + hostname + ":" + port);
            System.exit(1);
        } // End try/catch block

        // Send DATREQ to server
        String datreq = "DATREQ " + id + " " + date + " " + time + " " 
            + duration + '\u0000';
        out.write(datreq);
        out.flush();

        // Get return data
        recordCount = 0;
        byteCount = 0;
        try
        {
            while ((readCount = socket.getInputStream().read(seedrecord, byteCount, 512-byteCount)) > 0)
            {
                if (bCancel)
                {
                    socket.close();
                    bDone = true;
                    return;
                }
                byteCount += readCount;
                if (byteCount == 512)
                {
                    // See if this is a trailer record
                    for (int i=13; i < 18; i++)
                    {
                        charfield[i-13] = (char) seedrecord[i];
                    }
                    field = String.copyValueOf(charfield);
                    if (field.toString().compareTo("RQLOG") == 0)
                    {
                        // We see the terminating record, so terminate
                        break;
                    }

                    // We have a full record so save it
                    byteCount = 0;
                    recordCount++;
                    outfile.write(seedrecord);
                }
            } // while we are successfully reading data
            outfile.flush();
        } catch (IOException e1)
        {
            System.err.println("Unexpected IOException reading data from input stream, ABORT!");
            e1.printStackTrace();
            System.exit(1);
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

    public boolean Done()
    {
        return bDone;
    }

    public int GetRecordCount()
    {
        return recordCount;
    }

    public void SetCancel(boolean bCancel)
    {
        this.bCancel = bCancel;
    }

} // class SeedSocket
