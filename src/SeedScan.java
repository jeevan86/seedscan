import java.io.Console;
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
        // TODO: Write a DTD (or XML schema) for the configuration
        //
        // Configuration:
        //
        // <seedscan>
        //
        //   <lock_file>/qcwork/seedscan/seedscan.lock</lock_file>
        //
        //   <log>
        //     <directory>/qcwork/seedscan/logs</directory>
        //     <prefix>seedscan.</prefix>
        //     <postfix>.log</postfix>
        //     <!--level>SEVERE</level-->
        //     <!--level>WARNING</level-->
        //     <level>INFO</level>
        //     <!--level>DEBUG</level-->
        //     <!--level>FINE</level-->
        //     <!--level>FINER</level-->
        //     <!--level>FINEST</level-->
        //   </log>
        //  
        //   <database>
        //     <url>jdbc:mysql://136.177.121.210:54321/seedscan"</url>
        //     <username>seedscan_write</username>
        //     <password>
        //       <ciphertext>2f9cb9a02ee92a39</ciphertext>
        //       <iv>952bf002cc030243</iv>
        //     </password>
        //   </database>
        //
        //   <scan id="1">
        //     <path>/tr1/telemetry_days/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}</path>
        //     <frequency>
        //       <day value="-1"/>
        //     </frequency>
        //     <start_depth>1</start_depth>
        //     <scan_depth>2</scan_depth>
        //   </scan>
        //  
        //   <scan id="2">
        //     <path>/xs0/seed/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}_${NETWORK}_${STATION}</path>
        //     <frequency>
        //       <month value="-1">
        //         <day value="1"/>
        //       </month>
        //     </frequency>
        //     <start_depth>1</start_depth>
        //     <scan_depth>2</scan_depth>
        //   </scan>
        //   
        // </seedscan>

        String tr1PathPattern = "/tr1/telemetry_days/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}";
        String xs0PathPattern = "/xs0/seed/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}_${NETWORK}_${STATION}";
        String lockFile = "/qcwork/seedscan.lock";

        String url  = "jdbc:mysql://136.177.121.210:54321/seedscan";
        //String url  = "jdbc:oracle://<server>:<port>/database";
        String user = "seedscan_write";
        Console cons = System.console();
        char[] pass = cons.readPassword("Password for MySQL account '%s': ", user);

        LockFile lock = new LockFile(lockFile);
        if (!lock.acquire()) {
            System.out.println("Could not acquire lock.");
            System.exit(1);
        }

        int startDepth = 1; // Start this many days back.
        int scanDepth  = 2; // Number of days to evaluate.
        boolean scanXS0 = false;

        StationDatabase database = new StationDatabase(url, user, pass);
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

        for (int i=0; i < pass.length; i++) {
            pass[i] = ' ';
        }
    }
}
