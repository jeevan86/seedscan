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

package asl.seedscan.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import asl.metadata.Station;
import sac.*;

public class EventLoader
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.event.EventLoader.class);

    private static String  eventsDirectory = null;
    private static boolean eventsDirectoryLoaded = false;
    private static boolean eventsDirectoryValid  = false;

    private static Hashtable<String, Hashtable<String, EventCMT>> cmtTree = null;

    public EventLoader( String directoryPath ) {
        loadEventsDirectory( directoryPath );
    }

/**
 * We only want 1 (Scanner) thread at a time in here!
 * And it should only need to be entered one time for a given Scan
 */
    synchronized private static void loadEventsDirectory( String directoryPath ){

        if (eventsDirectoryLoaded) {
            logger.info(String.format( "eventsDir already initialized to:%s [valid=%s]", eventsDirectory, eventsDirectoryValid) );
            return;
        }

        eventsDirectoryLoaded = true;

        if ( directoryPath == null ) {
            logger.warn("eventsDir was NOT set in config.xml: <cfg:events_dir> --> Don't Compute Event Metrics");
            return;
        }
        else if ( !(new File(directoryPath)).exists() ) {
            logger.warn(String.format( "eventsDir=%s does NOT exist --> Skip Event Metrics", directoryPath) );
            return;
        }
        else if ( !(new File(directoryPath)).isDirectory() ) {
            logger.error(String.format( "eventsDir=%s is NOT a directory --> Skip Event Metrics", directoryPath) );
            return;
        }
        else {
            logger.info(String.format( "eventsDir=%s DOES exist --> Compute Event Metrics if asked", directoryPath) );
            eventsDirectory = directoryPath;
            eventsDirectoryValid = true;
            return;
        }

    }

    private final String makeKey(Calendar timestamp) {
        String yyyy = String.format("%4d",  timestamp.get(Calendar.YEAR) );
        String mo   = String.format("%02d", timestamp.get(Calendar.MONTH) + 1);
        String dd   = String.format("%02d", timestamp.get(Calendar.DAY_OF_MONTH));
        final String yyyymodd = yyyy + mo + dd;
        return yyyymodd;
    }

    public Hashtable<String, Hashtable<String, SacTimeSeries>> getDaySynthetics(Calendar timestamp, final Station station) {

        final String key = makeKey(timestamp);

        if (cmtTree == null) return null;       // No events loaded

        if (!cmtTree.containsKey(key)) return null;// No events loaded for this day

        Hashtable<String, EventCMT> dayCMTs = cmtTree.get(key);
        if (dayCMTs == null) return null; // Not sure why this would happen

        FilenameFilter sacFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                File file = new File(dir + "/" + name);
                //if (name.startsWith(station.getStation()) && name.endsWith(".sac") && (file.length() != 0) ) {
                if (name.startsWith(station.getStation()) && name.contains(".sac") && (file.length() != 0) ) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        Hashtable<String, Hashtable<String, SacTimeSeries>> allEventSynthetics = null;

        String year    = key.substring(0,4);
        String yearDir = eventsDirectory + "/" + year;

        SortedSet<String> keys = new TreeSet<String>(dayCMTs.keySet());
        for (String idString : keys){
            //System.out.format("== getDaySynthetics: Got EventCMT idString=[%s] --> [%s]\n",idString, dayCMTs.get(idString) );
            File eventDir = new File(yearDir + "/" + idString);

            if (!eventDir.exists()) {
                logger.warn(String.format("getDaySynthetics: eventDir=[%s] does NOT EXIST!", eventDir) );
            }

            File[] sacFiles = eventDir.listFiles(sacFilter);
            Hashtable<String, SacTimeSeries> eventSynthetics = null;

            for (File sacFile : sacFiles) {
                logger.info(String.format("Found sacFile=%s [%s]", sacFile, sacFile.getName()) );
                SacTimeSeries sac = new SacTimeSeries();
                try {
                    sac.read(sacFile);
                }
                catch (Exception e) {
                	logger.error("Exception:", e);
                }
                if (eventSynthetics == null) {
                    eventSynthetics = new Hashtable<String, SacTimeSeries>();
                }
                eventSynthetics.put(sacFile.getName(), sac); // e.g., key="HRV.XX.LXZ.modes.sac.proc"
            }

            if (allEventSynthetics == null) {
                allEventSynthetics = new Hashtable<String, Hashtable<String, SacTimeSeries>>();
            }

            if (eventSynthetics != null) {  // Add this event synthetics IF we found the sacFiles
                allEventSynthetics.put(idString, eventSynthetics);
            }
        }
        //return eventSynthetics;
        return allEventSynthetics;
    }


    //public Hashtable<String, EventCMT> getDayEvents(Calendar timestamp) {

    synchronized public Hashtable<String, EventCMT> getDayEvents(Calendar timestamp) {

        final String key = makeKey(timestamp);

        logger.debug("getDayEvents: Request events for key=[{}]", key);

        if (!eventsDirectoryValid) {
            logger.error("getDayEvents: eventsDirectory is NOT valid --> return null");
            return null;
        }

        if (cmtTree != null) {
            if (cmtTree.containsKey(key)) {
                //System.out.format("== EventLoader.getDayEvents: key=[%s] FOUND --> Return the events\n", key);
                return cmtTree.get(key);
            }
        }
        else {
            cmtTree = new Hashtable<String, Hashtable<String, EventCMT>>();
        }

        //System.out.format("== EventLoader.getDayEvents: key=[%s] NOT FOUND --> Try to load it\n", key);
        Hashtable<String, EventCMT> dayCMTs = loadDayCMTs(key);

        if (dayCMTs != null) {
            cmtTree.put( key, dayCMTs);
        }

        return dayCMTs;
    }


    private Hashtable<String, EventCMT> loadDayCMTs(final String yyyymodd){

        Hashtable<String, EventCMT> dayCMTs = null;
            
        File[] events = null;

        String yyyy = yyyymodd.substring(0,4);

        File yearDir  = new File(eventsDirectory + "/" + yyyy); // e.g., ../xs0/events/2012

    //  File filter to catch dir names like "C201204112255A"
        FilenameFilter eventFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                File file = new File(dir + "/" + name);
                if (name.contains(yyyymodd) && file.isDirectory() ) {
                    return true;
                } else {
                    return false;
                }
            }
        };

    // Check that yearDir exists and is a Directory:

        if (!yearDir.exists()) {
            logger.warn(String.format( "loadDayCMTs: eventsDir=%s does NOT exist --> Skip Event Metrics", yearDir) );
            return null;
        }
        else if (!yearDir.isDirectory()) {
            logger.error(String.format( "loadDayCMTs: eventsDir=%s is NOT a Directory --> Skip Event Metrics", yearDir) );
            return null;
        }
        else {  // yearDir was found --> Scan for matching events
            logger.info(String.format( "loadDayCMTs: getEventData: FOUND eventsDir=%s", yearDir) );
            events = yearDir.listFiles(eventFilter);
            if (events == null) {
                logger.warn(String.format( "No Matching events found for [yyyymodd=%s] "
                                           + "in eventsDir=%s", yyyymodd, yearDir) );
                return null;
            }
        // Loop over event dirs for this day and scan in CMT info, etc

            for (File event : events) { // Loop over event "file" (really a directory - e.g., ../2012/C201204122255A/)
                logger.info(String.format( "Found matching event dir=[%s]", event) );
                File cmtFile = new File(event + "/" + "currCMTmineos" ); 
                if (!cmtFile.exists()) {
                    logger.error(String.format( "Did NOT find cmtFile=currCMTmineos in dir=[%s]", event) );
                    continue;
                }
                else {
                    BufferedReader br = null;
                    try { // to read cmtFile

                        br = new BufferedReader(new FileReader(cmtFile));
                        String line = br.readLine();

                        if (line == null) {
                            logger.error(String.format( "cmtFile=currCMTmineos in dir=[%s] is EMPTY", event) );
                            continue;
                        }
                        else {
                            String[] args = line.trim().split("\\s+") ;
                            if (args.length < 9) {
                                logger.error(String.format( "cmtFile=currCMTmineos in dir=[%s] is INVALID", event) );
                                continue;
                            }

//C201204112255A 2012 102 22 55 10.80 18.1500 -102.9600 21.3000 1.0 5.2000 1.204e26 7.9 -7.49 -0.41 7.7 -4.18 2.99 1.0e25 0 0 0 0 0 0

                            try {
                                String idString = args[0];
                                int year        = Integer.valueOf(args[1].trim());
                                int dayOfYear   = Integer.valueOf(args[2].trim());
                                int hh          = Integer.valueOf(args[3].trim());
                                int mm          = Integer.valueOf(args[4].trim());
                                double xsec     = Double.valueOf(args[5].trim());
                                double lat      = Double.valueOf(args[6].trim());
                                double lon      = Double.valueOf(args[7].trim());
                                double dep      = Double.valueOf(args[8].trim());

                                int sec    = (int)xsec;
                                double foo = 1000*(xsec - sec);
                                int msec   = (int)foo;

                                GregorianCalendar gcal =  new GregorianCalendar( TimeZone.getTimeZone("GMT") );
                                gcal.set(Calendar.YEAR, year);
                                gcal.set(Calendar.DAY_OF_YEAR, dayOfYear);
                                gcal.set(Calendar.HOUR_OF_DAY, hh);
                                gcal.set(Calendar.MINUTE, mm);
                                gcal.set(Calendar.SECOND, sec);
                                gcal.set(Calendar.MILLISECOND, msec);

                                EventCMT eventCMT = new EventCMT.Builder(idString).calendar(gcal).latitude(lat).longitude(lon).depth(dep).build();
                                //eventCMT.printCMT();
                            // Add EventCMT to this Day's event CMTs:
                                if (dayCMTs == null) {
                                    dayCMTs = new Hashtable<String, EventCMT>();
                                }
                                dayCMTs.put(idString, eventCMT);
                            }
                            catch (NumberFormatException e) {
                                StringBuilder message = new StringBuilder();
                                message.append(String.format("Caught NumberFormatException while trying to read cmtFile=[%s]\n", cmtFile));
                                logger.error(message.toString(), e);
                            }
                        } // else line != null

                    } // end try to read cmtFile 
                    catch (IOException e) {
                        StringBuilder message = new StringBuilder();
                        message.append(String.format("Caught IOException while trying to read cmtFile=[%s]\n", cmtFile));
                        logger.error(message.toString(), e);
                    }
                    finally {
                        try {
                            if (br != null)br.close();
                        }
                        catch (IOException ex) {
                            logger.error("IOException:", ex);
                        }
                    }

                } // else cmtFile exists

            } // for event

        } // else yearDir was found

    return dayCMTs;

    }

}
