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

import java.util.Set;
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
import asl.seedscan.scan.Scan;
import asl.seedscan.database.StationDatabase;
import asl.metadata.*;
import asl.metadata.meta_new.*;

public class Scanner
    implements Runnable
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.Scanner");
    public long dayMilliseconds = 1000 * 60 * 60 * 24;

    private Station station;
    private StationDatabase database;
    private Scan scan;

    private FallOffQueue<SeedSplitProgress> progressQueue;

    public Scanner(StationDatabase database, Station station, Scan scan)
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
        GregorianCalendar timestamp = new GregorianCalendar();
        if (scan.getStartDay() > 0) {
            timestamp.setTimeInMillis(timestamp.getTimeInMillis() - (scan.getStartDay() * dayMilliseconds));
        }

        for (int i=0; i < scan.getDaysToScan(); i++) {
            if (i != 0) {
                timestamp.setTimeInMillis(timestamp.getTimeInMillis() - dayMilliseconds);
            }
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

// Here we go - Let's read in all the .seed files for this station, for this day

            File[] files = dir.listFiles(textFilter);
            int seedCount = files.length;

            Hashtable<String,ArrayList<DataSet>> table = null;
            logger.info(dir.getPath() + " contains " +seedCount+ " files.");
            progressQueue.clear();

            SeedSplitter splitter = new SeedSplitter(files, progressQueue);
            table = splitter.doInBackground();

// MTH: This is just for testing, if we want to see what we loaded into table
            ArrayList<DataSet> datasets = new ArrayList<DataSet>();
            DataSet dataset;
            Set<String> keys = table.keySet();
            for (String key : keys){
               //logger.info("** table key=" + key); 
               datasets = table.get(key);          // Will normally be = 1 for contiguous data
               dataset  = table.get(key).get(0);
               String knet = dataset.getNetwork(); String kstn = dataset.getStation();
               String locn = dataset.getLocation();String kchn = dataset.getChannel();
               double srate= dataset.getSampleRate();
               String out = String.format("NET=%s KSTN=%s KCHN=%s KLOC=%s srate=%.2f",knet,kstn,kchn,locn,srate);
               //logger.info("<===== PROCESSING: " + out); 
            }

            Runtime runtime = Runtime.getRuntime();
            System.out.println(" Java total memory=" + runtime.totalMemory() );

// MTH: Now we're going to load in MetaData for this station, for this day
// Really, we are requesting the metadata for a time exactly 24 hours before now
// e.g., timestamp --> 2012:159:02:44
// but the seed files all start at 00:00 and we are processing 1 day at a time,
// so we should request metadata for the 24-hr period starting at 00:00
// e.g., 2012:159:00 - 2012:160:00
// We should also set a flag to true for each channel where the metadata changed
//    sometime during the epoch (day) requested.

            MetaGenerator metaGen = new MetaGenerator();
            StationMeta stnMeta = metaGen.getStationMeta(station, timestamp); 
// At this point we should have the data and the metadata for all channels of this station,
//  for this day and we can hand them off to the metrics.
// Everything below is just for testing.

            stnMeta.print();
            ChannelMeta chanMeta = null;
            if (stnMeta.hasChannel("00","BHZ") ) {
               chanMeta = stnMeta.getChanMeta(new ChannelKey("00","BHZ") );
               if (chanMeta == null){
                 System.out.println("Scanner Error: stnMeta.getChannel returned null!");
               }
               else {
                 chanMeta.print();
               }
            }
            else {
               System.out.println("Scanner Error: chanMeta not found!");
            }

            runtime = Runtime.getRuntime();
            System.out.println(" Java total memory=" + runtime.totalMemory() );

/**
 ** Here's how Adam's code might use the metadata:
            double latitude  = stnMeta.getLatitude();
            double longitude = stnMeta.getLongitude();
            ChanMeta chnMeta = stnMeta.getChan("00","VHZ");
            double azimuth   = chnMeta.getAzimuth();
            double sampRate  = chnMeta.getSampleRate();
            Complex[] Response = chnMeta.getResponse(double freq[]); // Return complex response at freq[]

  [2] Data (table) get/return methods:
            DataSet vertical    = stnData.getChan("00","VHZ");
            double startTime    = vertical.getStartTime();
            long numberOfPoints = vertical.getlength();
            double sampleRate   = vertical.getSampleRate();
**/

//  From here we would hand off the data table + stnMeta for this Station(=name + net) + Day to Adam's Metrics.

        }
    }
}
