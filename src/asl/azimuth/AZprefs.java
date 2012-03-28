package asl.azimuth;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Class to load/save user preferences between program invocations.
 *
 * @author fshelly
 *                   
 */

public class AZprefs
{
    private static boolean initialized = false;
    private static String localDir;
    private static String imageDir;
    private static int    mainHeight;
    private static int    mainWidth;
    private static int    mainOriginX;
    private static int    mainOriginY;
    private static int    angleHeight;
    private static int    angleWidth;
    private static int    angleOriginX;
    private static int    angleOriginY;
    private Preferences node;

    /**
     * Loads previous preference values, or initializes them to defaults on very first run
     */
    public AZprefs()
    {
        Preferences root = Preferences.userRoot();
        node = root.node("/azimuth");
        mainHeight = node.getInt("mainHeight", AZdisplay.DEFAULT_HEIGHT);
        mainWidth  = node.getInt("mainWidth", AZdisplay.DEFAULT_WIDTH);
        mainOriginX = node.getInt("mainOriginX", 50);
        mainOriginY = node.getInt("mainOriginY", 50);
        angleHeight = node.getInt("angleHeight", AzAngleDisplay.DEFAULT_HEIGHT);
        angleWidth  = node.getInt("angleWidth", AzAngleDisplay.DEFAULT_WIDTH);
        angleOriginX = node.getInt("angleOriginX", 50);
        angleOriginY = node.getInt("angleOriginY", 50);
        localDir = node.get("localDir", "." + File.separatorChar);
        imageDir = node.get("imageDir", "." + File.separatorChar);
        if (!initialized)
        {
            initialized = true;
        }
    } // AZprefs()

    /**
     *  Call to permanently save any user modified preferences
     */
    public void SavePrefs()
    {
        node.put("localDir", localDir);
        node.put("imageDir", imageDir);
        node.putInt("mainHeight", mainHeight);
        node.putInt("mainWidth", mainWidth);
        node.putInt("mainOriginX", mainOriginX);
        node.putInt("mainOriginY", mainOriginY);
        node.putInt("angleHeight", angleHeight);
        node.putInt("angleWidth", angleWidth);
        node.putInt("angleOriginX", angleOriginX);
        node.putInt("angleOriginY", angleOriginY);
    } // SavePrefs()

    /**
     * Get value of directory used to load seed files from
     * @return directory used to load seed files from
     */
    public String GetLocalDir()
    {
        return localDir;
    }

    /**
     * Get value of directory used to save plot images to
     * @return value of directory used to save plot images to
     */
    public String GetImageDir()
    {
        return imageDir;
    }

    /**
     * Get height of main display window
     * @return height of main display window
     */
    public int GetMainHeight()
    {
        return mainHeight;
    }

    /**
     * Get width of main display window
     * @return width of main display window
     */
    public int GetMainWidth()
    {
        return mainWidth;
    }

    /**
     *  Get x value of left side of main window
     * @return x value of left side of main window
     */
    public int GetMainOriginX()
    {
        return mainOriginX;
    }

    /**
     * Get y value of top edge of main window
     * @return y value of top edge of main window
     */
    public int GetMainOriginY()
    {
        return mainOriginY;
    }

    /**
     * Get height of Angle popup display
     * @return height of Angle popup display
     */
    public int GetAngleHeight()
    {
        return angleHeight;
    }

    /**
     * Get width of Angle popup display
     * @return width of Angle popup display
     */
    public int GetAngleWidth()
    {
        return angleWidth;
    }

    /**
     * Get x value of left side of Angle popup display
     * @return x value of left side of Angle popup display
     */
    public int GetAngleOriginX()
    {
        return angleOriginX;
    }

    /**
     * Get y value of top edge of Angle popup display
     * @return y value of top edge of Angle popup display
     */
    public int GetAngleOriginY()
    {
        return angleOriginY;
    }

    /**
     * Set the value of directory used to save plot images to
     * @param newLocalDir value of directory used to save plot images to
     */
    public void SetLocalDir(String newLocalDir)
    {
        localDir = newLocalDir;
    }

    /**
     * Set the value of directory used to save plot images to
     * @param newImageDir value of directory used to save plot images to
     */
    public void SetImageDir(String newImageDir)
    {
        imageDir = newImageDir;
    }

    /**
     * Set the height of main display window
     * @param newHeight height of main display window
     */
    public void SetMainHeight(int newHeight)
    {
        mainHeight = newHeight;
    }

    /**
     * Set the width of main display window
     * @param newWidth width of main display window
     */
    public void SetMainWidth(int newWidth)
    {
        mainWidth = newWidth;
    }

    /**
     * Set the x value of left side of main window
     * @param x value of left side of main window
     */
    public void SetMainOriginX(int x)
    {
        mainOriginX = x;
    }

    /**
     * Set the y value of top edge of main window
     * @param y value of top edge of main window
     */
    public void SetMainOriginY(int y)
    {
        mainOriginY = y;
    }

    /**
     * Set the height of Angle popup display
     * @param newHeight height of Angle popup display
     */
    public void SetAngleHeight(int newHeight)
    {
        angleHeight = newHeight;
    }

    /**
     * Set the width of Angle popup display
     * @param newWidth width of Angle popup display
     */
    public void SetAngleWidth(int newWidth)
    {
        angleWidth = newWidth;
    }

    /**
     * Set the x value of left side of Angle popup display
     * @param x value of left side of Angle popup display
     */
    public void SetAngleOriginX(int x)
    {
        angleOriginX = x;
    }

    /**
     * Set the y value of top edge of Angle popup display
     * @param y value of top edge of Angle popup display
     */
    public void SetAngleOriginY(int y)
    {
        angleOriginY = y;
    }
} // class AZprefs

