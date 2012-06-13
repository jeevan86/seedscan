package asl.msplot;

import java.io.File;
import java.util.prefs.Preferences;


public class MSPreferences
{
    private static boolean initialized=false;
    private static boolean saveData;
    private static String station;
    private static String network;
    private static String hostname;
    private static String locChan1;
    private static String locChan2;
    private static String locChan3;
    private static int    minRange;
    private static int    port;
    private static int    secondsDuration;
    private static int    unitDivisor;
    private static String localDir;
    private static int    height;
    private static int    width;
    private static int    originX;
    private static int		originY;
    private Preferences node;

    public MSPreferences()
    {
        Preferences root = Preferences.userRoot();
        node = root.node("/msplot");

        if (!initialized)
        {
            saveData = node.getBoolean("saveData", false);
            station = node.get("station", "ASPX");
            network = node.get("network", "XX");
            hostname = node.get("hostname", "anmo.iu.liss.org");
            locChan1 = node.get("lochan1", "00/BHZ");
            locChan2 = node.get("lochan2", "10/BHZ");
            locChan3 = node.get("lochan3", "20/LHZ");
            minRange = node.getInt("minrange", 1000);
            unitDivisor = node.getInt("unitdivisor", 1);
            port = node.getInt("port", 4000);
            secondsDuration = node.getInt("duration", 7200);
            localDir = node.get("localDir", "." + File.pathSeparatorChar);
            height = node.getInt("height", 600);
            width  = node.getInt("width", 800);
            originX = node.getInt("originX", 0);
            originY = node.getInt("originY", 0);
            initialized = true;
        }
    } // MSPreferences()

    // Call to save any user modified preferences
    public void SavePreferences()
    {
        node.putBoolean("saveData", saveData);
        node.put("station", station);
        node.put("network", network);
        node.put("hostname", hostname);
        node.put("lochan1", locChan1);
        node.put("lochan2", locChan2);
        node.put("lochan3", locChan3);
        node.putInt("minrange", minRange);
        node.putInt("port", port);
        node.putInt("duration", secondsDuration);
        node.putInt("unitdivisor", unitDivisor);
        node.put("localDir", localDir);
        node.putInt("height", height);
        node.putInt("width", width);
        node.putInt("originX", originX);
        node.putInt("originY", originY);
    } // dispose()

    public String GetStation()
    {
        return station;
    }

    public String GetNetwork()
    {
        return network;
    }

    public String GetHostname()
    {
        return hostname;
    }

    public String GetLocChan1()
    {
        return locChan1;
    }

    public String GetLocChan2()
    {
        return locChan2;
    }

    public String GetLocChan3()
    {
        return locChan3;
    }

    public String GetLocalDir()
    {
        return localDir;
    }

    public int GetPort()
    {
        return port;
    }

    public int GetMinRange()
    {
        return minRange;
    }
    public int GetSecondsDuration()
    {
        return secondsDuration;
    }

    public int GetUnitDivisor()
    {
        return unitDivisor;
    }

    public boolean GetSaveData()
    {
        return saveData;
    }

    public int GetHeight()
    {
        return height;
    }

    public int GetWidth()
    {
        return width;
    }

    public int GetOriginX()
    {
        return originX;
    }

    public int GetOriginY()
    {
        return originY;
    }

    public void SetStation(String newStation)
    {
        station = newStation;
    }

    public void SetNetwork(String newNetwork)
    {
        network = newNetwork;
    }

    public void SetHostname(String newHostname)
    {
        hostname = newHostname;
    }

    public void SetLocChan1(String newLocChan)
    {
        locChan1 = newLocChan;
    }

    public void SetLocChan2(String newLocChan)
    {
        locChan2 = newLocChan;
    }

    public void SetLocChan3(String newLocChan)
    {
        locChan3 = newLocChan;
    }

    public void SetLocalDir(String newLocalDir)
    {
        localDir = newLocalDir;
    }

    public void SetPort(int newPort)
    {
        port = newPort;
    }

    public void SetMinRange(int newMinRange)
    {
        minRange = newMinRange;
    }

    public void SetSecondsDuration(int newSecondsDuration)
    {
        secondsDuration = newSecondsDuration;
    }

    public void SetUnitDivisor(int newUnitDivisor)
    {
        unitDivisor = newUnitDivisor;
    }

    public void SetHeight(int newHeight)
    {
        height = newHeight;
    }

    public void SetWidth(int newWidth)
    {
        width = newWidth;
    }

    public void SetOriginX(int x)
    {
        originX = x;
    }

    public void SetOriginY(int y)
    {
        originY = y;
    }

    public void SetSaveData(boolean newSaveData)
    {
        saveData = newSaveData;
    }

} // class MSPreferences
