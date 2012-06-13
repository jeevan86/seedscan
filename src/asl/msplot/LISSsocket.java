package asl.msplot;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class creates a background thread which connects to a server and 
 * downloads seed data from a LISS server, optionally appending it to a supplied file.
 * There are also calls which allow the calling program to keep track of the status of
 * the link.
 * @author Frank Shelly
 *
 */

public class LISSsocket extends Thread
{
    private Socket socket = null;
    private String hostname;
    private int port;
    private OutputStream outfile;
    private boolean bDone=false;
    private boolean bConnected=false;
    private boolean bUnknownHost=false;
    private int recordCount=0;
    private boolean bCancel=false;

    public static final int			SEED_QUEUE_SIZE = 100;

    private BlockingQueue<SeedRawRecord> queue = 
        new ArrayBlockingQueue<SeedRawRecord>(SEED_QUEUE_SIZE);

    public LISSsocket(String hostname, int port, OutputStream outfile)
    {
        // Run Thread constructor
        super();

        this.hostname = hostname;
        this.port = port;
        this.outfile = outfile;
        this.bCancel = false;
        this.bConnected = false;
        this.bUnknownHost = false;
        this.bDone =false;

    } // SeedSocket() constructor

    public boolean Done()
    {
        return bDone;
    }

    public int GetRecordCount()
    {
        return recordCount;
    }

    public void run()
    {
        byte seedrecord[] = new byte[512];
        int  byteCount;
        int  readCount;

        try
        {
            socket = new Socket(hostname, port);
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + hostname + ":" + port);
            bUnknownHost = true;
            bDone = true;
            return;
        } catch (IOException e) {
            System.err.println("Unable to connect to: " + hostname + ":" + port);
            bDone = true;
            return;
        } // End try/catch block

        // Get seed records
        recordCount = 0;
        byteCount = 0;
        bConnected = true;
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
                    // Queue new record up
                    SeedRawRecord record = new SeedRawRecord(seedrecord);
                    try
                    {
                        queue.put(record);
                    } catch (InterruptedException e)
                    {
                        // Error adding record to the queue
                        System.err.println("Unexpected Exception adding data to queue, ABORT!");
                        e.printStackTrace();
                        break;
                    } 

                    // We have a full record so save it
                    byteCount = 0;
                    recordCount++;
                    if (outfile != null)
                        outfile.write(seedrecord);
                } // we have read a full record
            } // while we are successfully reading data
            if (outfile != null)
                outfile.flush();
        } catch (IOException e1)
        {
            System.err.println("Unexpected IOException reading data from input stream, ABORT!");
            e1.printStackTrace();
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

    public void SetCancel(boolean bCancel)
    {
        this.bCancel = bCancel;
        if (socket != null && !socket.isClosed())
        {
            try
            {
                socket.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public boolean GetCancel()
    {
        return bCancel;
    }

    public BlockingQueue<SeedRawRecord> GetQueue()
    {
        return queue;
    }

    public boolean IsConnected()
    {
        return bConnected;
    }

    public boolean IsBadHost()
    {
        return bUnknownHost;
    }
} // class LISSsocket

