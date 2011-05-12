import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;

/**
 * 
 */
public class SeedScan
{
    public static void main(String[] args)
    {
        String tr1PathPattern = "/tr1/telemetry_days/${NETWORK}_${STATION}/%1$tY/%1$tY_%1$tj";
        String xs0PathPattern = "/xs0/seed/${NETWORK}_${STATION}/%1$tY/%1$tY_%1$tj_${NETWORK}_${STATION}";
        String lockFile = "/qcwork/seedscan.lock";

        LockFile lock = new LockFile(lockFile);
        if (!lock.acquire()) {
            System.out.println("Could not acquire lock.");
            System.exit(1);
        }

        int startDepth = 1;
        int scanDepth  = 2; // Number of days to look back.
        boolean scanXS0 = false;

        StationDatabase database = null;
        //Station[] stations = null;

        // TEST LIST (TODO: Remove once working)
        Station[] stations = {
            new Station("IU", "ANMO"),
            new Station("IU", "COR"),
            new Station("IU", "SJG"),
            new Station("IU", "MA2"),
            new Station("IU", "YSS")
        };

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
            Scanner scanner = new Scanner(database, station, tr1PathPattern);
            scanner.scan();
            if (scanXS0) {
                scanner = new Scanner(database, station, tr1PathPattern);
                scanner.scan();
            }
        }

        try {
            lock.release();
        } catch (IOException e) {
            ;
        } finally {
            lock = null;
        }
    }
}
