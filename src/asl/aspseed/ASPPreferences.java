/**
 * Class to load/save user preferences between program invocations.
 * Program using this create an ASPPreferences object and then use
 * the various Get/Set calls to get/store the preferences.
 * Somebody should call SavePreferences before exiting the program.
 */
package asl.aspseed;

/**
 * @author fshelly
 *
 */

import java.io.File;
import java.util.prefs.Preferences;

public class ASPPreferences
{
    private static boolean initialized=false;
    private static String station;
    private static String hostname;
    private static String localDir;
    private static String startDate;
    private static String startTime;
    private static String finishDate;
    private static String finishTime;
    private static int    port;
    private static int    maxRecords;
    private static int    extendFilename;
    private Preferences node;

    public ASPPreferences()
    {
        Preferences root = Preferences.userRoot();
        node = root.node("/aspseed");

        if (!initialized)
        {
            station = node.get("station", "");
            hostname = node.get("hostname", "");
            localDir = node.get("localDir", "." + File.pathSeparatorChar);
            startDate = node.get("startDate", "2000/01/01");
            startTime = node.get("startTime", "00:00:00");
            finishDate = node.get("finishDate", "2050/01/01");
            finishTime = node.get("finishTime", "00:00:00");
            port = node.getInt("port", 4004);
            maxRecords = node.getInt("maxRecords", 20000);
            extendFilename = node.getInt("extendFilename", 0);
            initialized = true;
        }
    } // ASPPreferences()

    // Call to save any user modified preferences
    public void SavePreferences()
    {
        node.put("station", station);
        node.put("hostname", hostname);
        node.put("localDir", localDir);
        node.put("startDate", startDate);
        node.put("startTime", startTime);
        node.put("finishDate", finishDate);
        node.put("finishTime", finishTime);
        node.putInt("port", port);
        node.putInt("maxRecords", maxRecords);
        node.putInt("extendFilename", extendFilename);
    } // dispose()

    public String GetStation()
    {
        return station;
    }

    public String GetHostname()
    {
        return hostname;
    }

    public String GetLocalDir()
    {
        return localDir;
    }

    public String GetStartDate()
    {
        return startDate;
    }

    public String GetStartTime()
    {
        return startTime;
    }

    public String GetFinishDate()
    {
        return finishDate;
    }

    public String GetFinishTime()
    {
        return finishTime;
    }

    public int GetPort()
    {
        return port;
    }

    public int GetMaxRecords()
    {
        return maxRecords;
    }

    public int GetExtendFilename()
    {
        return extendFilename;
    }

    public void SetStation(String newStation)
    {
        station = newStation;
    }

    public void SetHostname(String newHostname)
    {
        hostname = newHostname;
    }

    public void SetLocalDir(String newLocalDir)
    {
        localDir = newLocalDir;
    }

    public void SetStartDate(String newStartDate)
    {
        startDate = newStartDate;
    }

    public void SetStartTime(String newStartTime)
    {
        startTime = newStartTime;
    }

    public void SetFinishDate(String newFinishDate)
    {
        finishDate = newFinishDate;
    }

    public void SetFinishTime(String newFinishDate)
    {
        finishTime = newFinishDate;
    }

    public void SetPort(int newPort)
    {
        port = newPort;
    }

    public void SetMaxRecords(int newMaxRecords)
    {
        maxRecords = newMaxRecords;
    }

    public void SetExtendFilename(int newExtendFilename)
    {
        extendFilename = newExtendFilename;
    }

} // class ASPPreferences
