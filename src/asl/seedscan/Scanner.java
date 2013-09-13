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

import java.util.SortedSet;
import java.util.TimeZone;
import java.util.Set;
import java.util.TreeSet;
import java.util.Enumeration;
import java.io.FilenameFilter;
import java.io.PrintWriter;

import java.nio.ByteBuffer;
import asl.util.Hex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import org.apache.log4j.Logger;

import asl.concurrent.FallOffQueue;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.SeedSplitProgress;
import asl.seedsplitter.SeedSplitter;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.database.MetricInjector;
import asl.seedscan.database.MetricReader;
import asl.seedscan.event.*;
import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.seedscan.metrics.*;

import seed.Blockette320;
import sac.*;

public class Scanner
    implements Runnable
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.Scanner");
    public long dayMilliseconds = 1000 * 60 * 60 * 24;

    private Station station;
    private MetricInjector injector;
    private MetricReader reader;
    private Scan scan;
    //private MetaGenerator metaGen;
    private MetaServer metaServer;

    private MetricData currentMetricData = null;
    private MetricData nextMetricData = null;

    private FallOffQueue<SeedSplitProgress> progressQueue;

    private static String eventsDir = null;
    private static Hashtable<String, EventCMT> oneDayEventCMTs = null;

    private static long MB = 1048576L; // 1024*1024 - for java Runtime outputs

    public Scanner(MetricReader reader, MetricInjector injector, Station station, Scan scan)
    {
        this.reader = reader;
        this.injector = injector;
        this.station  = station;
        this.scan = scan;
        this.progressQueue = new FallOffQueue<SeedSplitProgress>(8);
    }

    //public Scanner(MetricReader reader, MetricInjector injector, Station station, Scan scan, MetaGenerator metaGen)
    public Scanner(MetricReader reader, MetricInjector injector, Station station, Scan scan, MetaServer metaServer)
    {
        this.reader = reader;
        this.injector = injector;
        this.station  = station;
        //this.metaGen  = metaGen;
        this.metaServer  = metaServer;
        this.scan = scan;
        this.progressQueue = new FallOffQueue<SeedSplitProgress>(8);
    }


    public void run() {
        scan();
    }

    public void scan()
    {
        logger.info("Enter scan()");

        GregorianCalendar timestamp = new GregorianCalendar(TimeZone.getTimeZone("GMT") );

        Runtime runtime = Runtime.getRuntime();

        // Look for cfg:start_date first:
        if (scan.getStartDate() > 1990001 && scan.getStartDate() < 2014365) {
            timestamp.set(Calendar.YEAR, scan.getStartDate() / 1000);
            timestamp.set(Calendar.DAY_OF_YEAR, scan.getStartDate() % 1000);
        }
        else { // Use cfg:start_day
            timestamp.setTimeInMillis(timestamp.getTimeInMillis() - (scan.getStartDay() * dayMilliseconds));
        }

     // timestamp is now set to current time - (24 hours x StartDay). What we really want is to set it
     //   to the start (hh:mm=00:00) of the first day we want to scan
        timestamp.set(Calendar.HOUR_OF_DAY, 0);      timestamp.set(Calendar.MINUTE, 0);
        timestamp.set(Calendar.SECOND, 0);      timestamp.set(Calendar.MILLISECOND, 0);

     // CMT Event loader - use to load events for each day
        EventLoader eventLoader = new EventLoader( scan.getEventsDir() );

     // Loop over days to scan, from most recent (currentDay=startDay) to oldest (currentDay=startDay - daysToScan - 1)
     // e.g.,
     // i   currentDay  nextDay
     // -----------------------
     // 0      072        073
     // 1      071        072
     // 2      070        071
     // :
     // daysToScan - 1

        for (int i=0; i < scan.getDaysToScan(); i++) {
            if (i != 0) {
                timestamp.setTimeInMillis(timestamp.getTimeInMillis() - dayMilliseconds);
            }
            GregorianCalendar nextDayTimestamp = (GregorianCalendar)timestamp.clone();
            nextDayTimestamp.setTimeInMillis( timestamp.getTimeInMillis() + dayMilliseconds);

// [1] Get all the channel metadata for this station, for this day
            //StationMeta stnMeta = metaGen.getStationMeta(station, timestamp); 
            StationMeta stnMeta = metaServer.getStationMeta(station, timestamp); 
            if (stnMeta == null) {                       // No Metadata found for this station + this day --> skip day
               //System.out.format("== Scanner: No Metadata found for Station:%s_%s + Day:%s --> Skipping\n", 
                                  //station.getNetwork(), station.getStation(), EpochData.epochToDateString(timestamp) );
               logger.warn(String.format("== Scanner: No Metadata found for Station:%s_%s + Day:%s --> Skipping\n", 
                                  station.getNetwork(), station.getStation(), EpochData.epochToDateString(timestamp) ));
               continue;
            }

            if (i == 0) {
                stnMeta.printStationInfo();
            }

            logger.info(String.format("Scan Station=%s Day=%s", station, EpochData.epochToDateString(timestamp) ));

            Hashtable<String, EventCMT> eventCMTs = eventLoader.getDayEvents( timestamp );
            Hashtable<String, Hashtable<String, SacTimeSeries>> eventSynthetics = null;
            //Hashtable<String, SacTimeSeries> eventSynthetics = null;
            if (eventCMTs != null) {
                SortedSet<String> keys = new TreeSet<String>(eventCMTs.keySet());
                for (String key : keys){
                    //System.out.format("== Scanner: Got EventCMT key=[%s] --> [%s]\n",key, eventCMTs.get(key) ); 
                }
                eventSynthetics = eventLoader.getDaySynthetics( timestamp, station );
            }
            else {
                //System.out.format("== Scanner: NO CMTs FOUND for this day\n");
            }


// [2] Read in all the seed files for this station, for this day & for the next day
//     If this isn't the first day of the scan then simply copy current into next so we
//     don't have to reread all of the seed files in

            if (i == 0) {
                nextMetricData    = getMetricData(nextDayTimestamp);
            }
            else {
              // Need to null out ref to next day before passing currentMetricData to avoid chaining refs
                if (currentMetricData != null) {
                    currentMetricData.setNextMetricDataToNull();
                    nextMetricData    = currentMetricData;
                }
            }
            currentMetricData = null;
            currentMetricData = getMetricData(timestamp);

            if (currentMetricData != null) {     // This doesn't mean nextMetricData isn't null!
                currentMetricData.setNextMetricData(nextMetricData);
            }

            //System.out.format("== Scanner: totalMemory=[%d] freeMemory=[%d] usedMemory=[%d]\n", runtime.totalMemory()/MB,
                                  //runtime.freeMemory()/MB, (runtime.totalMemory() - runtime.freeMemory())/MB );

// [3] Loop over Metrics to compute, for this station, for this day

            Hashtable<CrossPowerKey, CrossPower> crossPowerMap = null;

            for (MetricWrapper wrapper: scan.getMetrics()) {
                Metric metric = wrapper.getNewInstance();

                if (currentMetricData != null) {
                    metric.setData(currentMetricData);

                    if (eventCMTs != null) {
                        metric.setEventTable( eventCMTs );
                        if (eventSynthetics != null) {
                            metric.setEventSynthetics( eventSynthetics );
                        }
                    }

   // Hand off the crossPowerMap from metric to metric, adding to it each time
                    if (crossPowerMap != null) {
                        metric.setCrossPowerMap(crossPowerMap);
                    }
                    metric.process();
   // Save the current crossPowerMap for the next metric:
                    crossPowerMap = metric.getCrossPowerMap();
                }
                else if (metric.getClass().getName().contains("AvailabilityMetric")){
                    metric.setData( new MetricData(stnMeta) );
                    metric.process();
                }
                else { // No data for this station + day
                    continue;
                }

   // This is a little convoluted: calibration.getResult() returns a MetricResult, which may contain many values
   //   in a Hashtable<String,String> = map.
   //   MetricResult.getResult(id) returns value = String
                
                MetricResult results = metric.getMetricResult();
                //System.out.format("Results for %s:\n", metric.getClass().getName());
                if (results == null){
                }
                else {
                    for (String id: results.getIdSortedSet()) {
                        double value = results.getResult(id);
                        ByteBuffer digest = results.getDigest(id);
                        logger.info(String.format("%s [%7s] [%s] %15s:%6.2f [%s]", results.getMetricName(), 
                            results.getStation(), EpochData.epochToDateString(results.getDate()), id, value, 
                            Hex.byteArrayToHexString(digest.array()) ));

                        if (Double.isNaN(value)){
                            logger.warn(String.format("%s [%s] [%s] %s: ERROR: metric value = [ NaN ] !!\n", 
                              results.getMetricName(), results.getStation(), EpochData.epochToDateString(results.getDate()),
                              id ));
                        }
                        if (Double.isInfinite(value)){
                            logger.warn(String.format("%s [%s] [%s] %s: ERROR: metric value = [ Infinity ] !!\n", 
                              results.getMetricName(), results.getStation(), EpochData.epochToDateString(results.getDate()),
                              id ));
                        }
                    }
                    if (injector.isConnected()) {
                        try {
                    	    injector.inject(results);
                        } catch (InterruptedException ex) {
                    	    logger.warn(String.format("Interrupted while trying to inject metric [%s]", metric.toString()));
                        }
                    }
                    else {
                        logger.warn("Injector *IS NOT* connected --> Don't inject");
                    }
                }
            } // end loop over metrics

        } // end loop over day to scan
    } // end scan()

/**
 *  Return a MetricData object for the station + timestamp
 *  If a StationMeta is passed in, then this must be for the current Day so 
 *  attach a MetricReader to the MetricData, otherwise don't
 */
    private MetricData getMetricData(GregorianCalendar timestamp) {

      //System.out.format("== getMetricData: request data for Station=[%s] Day=[%s]\n", station, EpochData.epochToDateString(timestamp));

        //StationMeta stationMeta = metaGen.getStationMeta(station, timestamp); 
        StationMeta stationMeta = metaServer.getStationMeta(station, timestamp); 
        if (stationMeta == null) {
            return null;
        }

        ArchivePath pathEngine = new ArchivePath(timestamp, station);
        String path = pathEngine.makePath(scan.getPathPattern());
        File dir = new File(path);
        File[] files = null;
        boolean dataExists = true;

/** MTH: There are some non-seed files (e.g., data_avail.txt) included in files[].
 **      For some reason the file netday.index causes the splitter to hang.
 **      Either restrict the file list to .seed files (as I do below) -or-
 **      Debug splitter so it drops non-seed/miniseed files.
**/
        FilenameFilter textFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                File file = new File(dir + "/" + name);
                if (lowercaseName.endsWith(".seed") && (file.length() > 0) ) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        if (!dir.exists()) {
            logger.info("Path '" +dir+ "' does not exist.");
            dataExists = false;
        }
        else if (!dir.isDirectory()) {
            logger.info("Path '" +dir+ "' is not a directory.");
            dataExists = false;
        }
        else { // The dir exists --> See if we have any useful seed files in it:

            files = dir.listFiles(textFilter);
            if (files == null) {
                dataExists = false;
            }
            else if (files.length == 0) {
                dataExists = false;
            }
        } // end else dir exists

//      if (dataExists) {   // See if this is a calibration day (i.e., if files BC{0,1}.512.seed exist)
//          for (File file : files){
//              if (file.getName().equals("BC0.512.seed") || file.getName().equals("BC1.512.seed")) {
//                  System.out.format("== Scanner: found Calibration file=[%s]\n", file);
//              }
//          }
//      }

        if (!dataExists) {
            //System.out.format("== getMetricData: No data found for Day=[%s] Station=[%s]\n", EpochData.epochToDateString(timestamp), station);
            return null;
        }

        Hashtable<String,ArrayList<DataSet>> table = null;
        logger.info(dir.getPath() + " contains " +files.length+ " files.");
        progressQueue.clear();

        SeedSplitter splitter = new SeedSplitter(files, progressQueue);
        table = splitter.doInBackground();

        Hashtable<String,ArrayList<Integer>> qualityTable = null;
        qualityTable = splitter.getQualityTable();

        Hashtable<String,ArrayList<Blockette320>> calibrationTable = null;
        calibrationTable = splitter.getCalTable();

        return new MetricData(reader, table, qualityTable, stationMeta, calibrationTable);

    } // end getMetricData()


}
