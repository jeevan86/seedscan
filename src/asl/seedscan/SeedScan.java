/*
 * Copyright 2011, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */

package asl.seedscan;


import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.GregorianCalendar;

/**
 * 
 */
public class SeedScan
{
    private final static String allchanURLstr = "http://wwwasl/uptime/honeywell/gsn_allchan.txt";
    private static URL allchanURL;

    public static void main(String args[])
    {
        // Configuration (see SeedScanSchema.xsd):
        //
        // <?xml version="1.0"?
        // <seedscan xmlns="config.seedscan.asl.cr.usgs.gov">
        //
        //   <lock_file>/qcwork/seedscan/seedscan.lock</lock_file>
        //
        //   <log>
        //     <directory>/qcwork/seedscan/logs</directory>
        //     <prefix>seedscan.</prefix>
        //     <postfix>.log</postfix>
        //     <level>INFO</level>
        //   </log>
        //  
        //   <database>
        //     <url>jdbc:mysql://136.177.121.210:54321/seedscan"</url>
        //     <!--url>jdbc:mysql://catbox2.cr.usgs.gov/stations"</url-->
        //     <username>seedscan_write</username>
        //     <password>
        //       <ciphertext>2f9cb9a02ee92a39</ciphertext>
        //       <iv>952bf002cc030243</iv>
        //       <key>952bf002cc030243</key>
        //       <key_iv>952bf002cc030243</key_iv>
        //     </password>
        //   </database>
        //
        //   <scan id="1">
        //     <path>/xs0/seed/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}_${NETWORK}_${STATION}</path>
        //     <frequency>
        //       <days value="1">
        //         <hour value="22"/>
        //       </days>
        //     </frequency>
        //     <start_depth>1</start_depth>
        //     <scan_depth>2</scan_depth>
        //   </scan>
        //  
        //   <scan id="2">
        //     <path>/xs0/seed/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}_${NETWORK}_${STATION}</path>
        //     <frequency>
        //       <years value="1">
        //         <month value="12">
        //           <day value="24">
        //             <hour value="22">
        //               <minute value="30">
        //             </hour>
        //           </day>
        //         </month>
        //       </years>
        //     </frequency>
        //     <start_depth>1</start_depth>
        //     <scan_depth>-1</scan_depth>
        //   </scan>
        //   
        // </seedscan>

        String tr1PathPattern = "/tr1/telemetry_days/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}";
        String xs0PathPattern = "/xs0/seed/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}_${NETWORK}_${STATION}";
        String xs1PathPattern = "/xs1/seed/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}_${NETWORK}_${STATION}";
        String lockFile = "/qcwork/seedscan.lock";

        String url  = "jdbc:mysql://136.177.121.210:54321/seedscan";
        //String url  = "jdbc:oracle://<server>:<port>/database";
        String user = "seedscan_write";
        Console cons = System.console();
        //char[] pass = cons.readPassword("Password for MySQL account '%s': ", user);

        File schemaFile = new File("schemas/SeedScanConfig.xsd");
        File configFile = new File("config.xml");

        Config config = new Config(schemaFile);
        config.loadConfig(configFile);

        LockFile lock = new LockFile(lockFile);
        if (!lock.acquire()) {
            System.out.println("Could not acquire lock.");
            System.exit(1);
        }

        int startDepth = 1; // Start this many days back.
        int scanDepth  = 2; // Number of days to evaluate.
        boolean scanXS0 = false;

        //StationDatabase database = new StationDatabase(url, user, pass);
        StationDatabase database = null;
        //Station[] stations = null;

        // Get the list of active channels from a web page where they are currently maintained.
        try 
        {
            allchanURL = new URL(allchanURLstr);
        } catch (MalformedURLException e1) 
        {
            System.err.println("Exception opening URL %s.");
            e1.printStackTrace();
            allchanURL = null;
        }
        if (allchanURL == null)
        {
            System.err.printf("Unable to open resource %s, aborting!\n", 
                    allchanURLstr);
            System.exit(1);
        }

        System.out.printf("We seem to have found %s\n", allchanURLstr);

        // TEST LIST (TODO: Remove once configurations are working)
        Station[] stations = {
            new Station("IU", "ANMO"),
            new Station("IU", "COR"),
            new Station("IU", "SJG"),
            new Station("IU", "MA2"),
            new Station("IU", "YSS"),
            new Station("IU", "CCM")
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

        /*
           for (int i=0; i < pass.length; i++) {
           pass[i] = ' ';
           }
         */
    } // main()
} // class SeedScan
