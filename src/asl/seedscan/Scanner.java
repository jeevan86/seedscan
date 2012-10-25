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

import java.util.TimeZone;
import java.util.Set;
import java.util.Enumeration;
import java.io.FilenameFilter;

import java.io.File;
import java.io.IOException;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.concurrent.BlockingQueue;

import asl.concurrent.FallOffQueue;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.SeedSplitProgress;
import asl.seedsplitter.SeedSplitter;
import asl.seedscan.database.MetricDatabase;
import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.seedscan.metrics.*;

public class Scanner
    implements Runnable
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.Scanner");
    public long dayMilliseconds = 1000 * 60 * 60 * 24;

    private Station station;
    private MetricDatabase database;
    private Scan scan;

    private FallOffQueue<SeedSplitProgress> progressQueue;

    public Scanner(MetricDatabase database, Station station, Scan scan)
    {
        this.station  = station;
        this.database = database;
        this.scan = scan;
        this.progressQueue = new FallOffQueue<SeedSplitProgress>(8);
    }

    public void run() {
        scan();
    }

    public void scan()
    {

    //  GregorianCalendar timestamp = new GregorianCalendar();

    // This has to be done so that EpochData.epochToDateString will return the correct times
    //   since seedsplitter/SeedSplitProcessor sets the TimeZone to GMT.
    // Otherwise, all calls to epochToDateString will print local time (GMT - 4 hours).
    // Update: There's more to it: If this is run after 8:00pm EST then it thinks it's the next GMT day ...

        GregorianCalendar timestamp = new GregorianCalendar(TimeZone.getTimeZone("GMT") );

        if (scan.getStartDay() > 0) {
            timestamp.setTimeInMillis(timestamp.getTimeInMillis() - (scan.getStartDay() * dayMilliseconds));
        }
     // timestamp is now set to current time - (24 hours x StartDay). What we really want is to set it
     //   to the start (hh:mm=00:00) of the first day we want to scan
        timestamp.set(Calendar.HOUR_OF_DAY, 0);      timestamp.set(Calendar.MINUTE, 0);
        timestamp.set(Calendar.SECOND, 0);      timestamp.set(Calendar.MILLISECOND, 0);

     // Read in all metadata for this station (all channels + all days):
        //MetaGenerator metaGen = new MetaGenerator(station);
        String datalessDir    = scan.getDatalessDir();
        MetaGenerator metaGen = new MetaGenerator(station, datalessDir);
        if (!metaGen.isLoaded()) {    // No Metadata found for this station --> Skip station == End thread ??
            System.out.format("Scanner Error: No Metadata found for Station:%s_%s --> Skip this Station\n", station.getNetwork(), station.getStation());
            return;
        }

     // Loop over days to scan

        for (int i=0; i < scan.getDaysToScan(); i++) {
            if (i != 0) {
                timestamp.setTimeInMillis(timestamp.getTimeInMillis() - dayMilliseconds);
            }

            System.out.format("\n==Scanner: scan Day=%s Station=%s\n", EpochData.epochToDateString(timestamp), station);

// [1] Get all the channel metadata for this station, for this day
            StationMeta stnMeta = metaGen.getStationMeta(station, timestamp); 
            if (stnMeta == null) { // No Metadata found for this station + this day --> skip day
               System.out.format("Scanner Error: No Metadata found for Station:%s_%s + Day:%s --> Skipping\n", station.getNetwork(), station.getStation(),
                                  EpochData.epochToDateString(timestamp) );
               continue;
            }

// [2] Read in all the seed files for this station, for this day

            ArchivePath pathEngine = new ArchivePath(timestamp, station);
            String path = pathEngine.makePath(scan.getPathPattern());
            File dir = new File(path);
            if (!dir.exists()) {
                logger.info("Path '" +dir+ "' does not exist.");
                continue;
            }
            else if (!dir.isDirectory()) {
                logger.info("Path '" +dir+ "' is not a directory.");
                continue;
            }

/** MTH: There are some non-seed files (e.g., data_avail.txt) included in files[].
 **      For some reason the file netday.index causes the splitter to hang.
 **      Either restrict the file list to .seed files (as I do below) -or-
 **      Debug splitter so it drops non-seed/miniseed files.
 **
 **         File[] files = dir.listFiles();
**/
            FilenameFilter textFilter = new FilenameFilter() {
              public boolean accept(File dir, String name) {
                  String lowercaseName = name.toLowerCase();
                  if (lowercaseName.endsWith(".seed")) {
                      return true;
                  } else {
                      return false;
                  }
              }
            };

            File[] files = dir.listFiles(textFilter);
            int seedCount = files.length;

            Hashtable<String,ArrayList<DataSet>> table = null;
            logger.info(dir.getPath() + " contains " +seedCount+ " files.");
            progressQueue.clear();

            SeedSplitter splitter = new SeedSplitter(files, progressQueue);
            table = splitter.doInBackground();

/** These digests are empty:
            String DigestStrings[] = splitter.getDigests();
            System.out.format("==Scanner: got %d digestString(s)\n", DigestStrings.length );

            for (String digestString : DigestStrings) {
              System.out.format("==Scanner: digestString=%s\n", digestString );
            }
**/

            Runtime runtime = Runtime.getRuntime();
            System.out.println(" Java total memory=" + runtime.totalMemory() );

// [3] Loop over Metrics to compute, for this station, for this day
            MetricData metricData = new MetricData(table, stnMeta);

            Hashtable<CrossPowerKey, CrossPower> crossPowerMap = null;

            for (MetricWrapper wrapper: scan.getMetrics()) {
                Metric metric = wrapper.getNewInstance();
                metric.setData(metricData);

   // Hand off the crossPowerMap from metric to metric, adding to it each time
                if (crossPowerMap != null) {
                    metric.setCrossPowerMap(crossPowerMap);
                }

                metric.process();

   // Save the current crossPowerMap for the next metric:
                crossPowerMap = metric.getCrossPowerMap();

   // This is a little convoluted: calibration.getResult() returns a MetricResult, which may contain many values
   //   in a Hashtable<String,String> = map.
   //   MetricResult.getResult(id) returns value = String
                
                MetricResult result = metric.getResult();
                System.out.format("Results for %s:\n", metric.getClass().getName());
                if (result == null){
                }
                else {
                    for (String id: result.getIdSet()) {
                        String value = result.getResult(id);
                        System.out.format("  %s : %s\n", id, value);
                    }
                }
            } // end loop over metrics

        } // end loop over day to scan
    } // end scan()
}
