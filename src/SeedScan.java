import java.io.File;
import java.util.GregorianCalendar;

/**
 * 
 */
public class SeedScan
{
    public void dayMilliseconds = 1000 * 60 * 60 * 24;

    public static void main(String[] args)
    {
        String tr1PathPattern = "/tr1/telemetry_days/${NETWORK}_${STATION}/%Y/%Y_%j";
        String xs0PathPattern = "/xs0/seed/${NETWORK}_${STATION}/%Y/%Y_%j_${NETWORK}_${STATION}";

        int scanDepth = 2; // Number of days to look back.
        boolean scanXS0 = false;

        GregorianCalendar timestamp = new GregorianCalendar();

        StationDatabase database = null;
        Station[] stations = null;

        // Get a list of stations
        
        // Get a list of files  (do we want channels too?)

        // For each day ((yesterday - scanDepth) to yesterday)
        // scan for these channel files, only process them if
        // they have not yet been scanned, or if changes have
        // occurred to the file since its last scan. Do this for
        // each scan type. Do not re-scan data for each type,
        // launch processes for each scan and use the same data set
        // for each. If we can pipe the data as it is read, do so.
        // If we need to push all of it at once, do these in sequence
        // in order to preserve overall system memory resources.

        for (Station station: stations) {
            Scanner scanner = new Scanner(database, station);
            scanner.scan();
        }
    }
}
