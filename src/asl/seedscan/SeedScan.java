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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.security.*;
import asl.seedscan.config.*;
import asl.seedscan.database.*;
import asl.seedscan.metrics.*;
import asl.util.*;

/**
 * 
 */
public class SeedScan
{
    private static Logger logger = LoggerFactory.getLogger(asl.seedscan.SeedScan.class);

    public static void main(String args[])
    {
     // Default locations of config and schema files
        File configFile = new File("config.xml");
        File schemaFile = new File("schemas/SeedScanConfig.xsd");
        boolean parseConfig = true;

        ArrayList<File> schemaFiles = new ArrayList<File>();
        schemaFiles.add(schemaFile);

// ==== Command Line Parsing ====
        Options options = new Options();
        Option opConfigFile = new Option("c", "config-file", true, 
                            "The config file to use for seedscan. XML format according to SeedScanConfig.xsd.");
        Option opSchemaFile = new Option("s", "schema-file", true, 
                            "The xsd schema file which should be used to verify the config file format. ");  

        OptionGroup ogConfig = new OptionGroup();
        ogConfig.addOption(opConfigFile);

        OptionGroup ogSchema = new OptionGroup();
        ogConfig.addOption(opSchemaFile);

        options.addOptionGroup(ogConfig);
        options.addOptionGroup(ogSchema);

        PosixParser optParser = new PosixParser();
        CommandLine cmdLine = null;
        try {
            cmdLine = optParser.parse(options, args, true);
        } catch (org.apache.commons.cli.ParseException e) {
            logger.error("Error while parsing command-line arguments.");
            System.exit(1);
        }

        Option opt;
        Iterator<?> iter = cmdLine.iterator();
        while (iter.hasNext()) {
            opt = (Option)iter.next();
            if (opt.getOpt().equals("c")) {
                configFile = new File(opt.getValue());
            }
            else if (opt.getOpt().equals("s")) {
                schemaFile = new File(opt.getValue());
            }
        }

// ==== Configuration Read and Parse Actions ====
        ConfigParser parser = new ConfigParser(schemaFiles);
        ConfigT config = parser.parseConfig(configFile);

     // Print out configuration file contents
        Formatter formatter = new Formatter(new StringBuilder(), Locale.US);

     // ===== CONFIG: LOCK FILE =====
        File lockFile = new File(config.getLockfile());
        logger.info("SeedScan lock file is '" +lockFile+ "'");
        LockFile lock = new LockFile(lockFile);
        if (!lock.acquire()) {
            logger.error("Could not acquire lock.");
            System.exit(1);
        }
        
     // ===== CONFIG: LOGGING =====
     // MTH: This is now done in log4j.properties file

     // ===== CONFIG: DATABASE =====
        MetricDatabase readDB   = new MetricDatabase(config.getDatabase());
        MetricDatabase writeDB  = new MetricDatabase(config.getDatabase());
    	MetricReader   reader 	= new MetricReader(readDB); 
    	MetricInjector injector = new MetricInjector(writeDB);

     // ===== CONFIG: SCANS =====
        Hashtable<String, Scan> scans = new Hashtable<String, Scan>();
        if (config.getScans().getScan() == null) {
            logger.error("No scans in configuration.");
            System.exit(1);
        }
        else {
            for (ScanT scanCfg: config.getScans().getScan()) {
                String name = scanCfg.getName();
                if (scans.containsKey(name)) {
                    logger.error("Duplicate scan name '" +name+ "' encountered.");
                    System.exit(1);
                }

            // This should really be handled by jaxb by setting it up in schemas/SeedScanConfig.xsd
                if(scanCfg.getStartDay() == null && scanCfg.getStartDate() == null) {
                    logger.error("== SeedScan Error: Must set EITHER cfg:start_day -OR- cfg:start_date in config.xml to start Scan!");
                    System.exit(1);
                }

        // Configure this Scan
                Scan scan = new Scan(scanCfg.getName());
                scan.setPathPattern(scanCfg.getPath());
                scan.setDatalessDir(scanCfg.getDatalessDir());
                scan.setEventsDir(scanCfg.getEventsDir());
                scan.setPlotsDir(scanCfg.getPlotsDir());
                scan.setDaysToScan(scanCfg.getDaysToScan().intValue());
                if (scanCfg.getStartDay() != null) {
                    scan.setStartDay(scanCfg.getStartDay().intValue());
                }
                if (scanCfg.getStartDate() != null) {
                    scan.setStartDate(scanCfg.getStartDate().intValue());
                }

                if (scanCfg.getNetworkSubset() != null) {
                    logger.debug("Filter on Network Subset=[{}]", scanCfg.getNetworkSubset());
                    Filter filter = new Filter(false);
                    for (String network : scanCfg.getNetworkSubset().split(",") ) {
                        logger.debug("Network =[{}]", network);
                        filter.addFilter(network);
                    }
                    scan.setNetworks(filter);
                }
                if (scanCfg.getStationSubset() != null) {
                    logger.debug("Filter on Station Subset=[{}]", scanCfg.getStationSubset());
                    Filter filter = new Filter(false);
                    for (String station : scanCfg.getStationSubset().split(",") ) {
                        logger.debug("Station =[{}]", station);
                        filter.addFilter(station);
                    }
                    scan.setStations(filter);
                }
                if (scanCfg.getLocationSubset() != null) {
                    logger.debug("Filter on Location Subset=[{}]", scanCfg.getLocationSubset());
                    Filter filter = new Filter(false);
                    for (String location : scanCfg.getLocationSubset().split(",") ) {
                        logger.debug("Location =[{}]", location);
                        filter.addFilter(location);
                    }
                    scan.setLocations(filter);
                }
                if (scanCfg.getChannelSubset() != null) {
                    logger.debug("Filter on Channel Subset=[{}]", scanCfg.getChannelSubset());
                    Filter filter = new Filter(false);
                    for (String channel : scanCfg.getChannelSubset().split(",") ) {
                        logger.debug("Channel =[{}]", channel);
                        filter.addFilter(channel);
                    }
                    scan.setChannels(filter);
                }

                for (MetricT met: scanCfg.getMetrics().getMetric()) {
                    try {
                        Class<?> metricClass = Class.forName(met.getClassName());
                        MetricWrapper wrapper = new MetricWrapper(metricClass);
                        for (ArgumentT arg: met.getArgument()) {
                            wrapper.add(arg.getName(), arg.getValue());
                        }
                        scan.addMetric(wrapper);
                    } catch (ClassNotFoundException ex) {
                        logger.error("No such metric class '" +met.getClassName()+ "'");
                        System.exit(1);
                    } catch (InstantiationException ex) {
                        logger.error("Could not dynamically instantiate class '" +met.getClassName()+ "'");
                        System.exit(1);
                    } catch (IllegalAccessException ex) {
                        logger.error("Illegal access while loading class '" +met.getClassName()+ "'");
                        System.exit(1);
                    } catch (NoSuchFieldException ex) {
                        logger.error("Invalid dynamic argument to Metric subclass '" +met.getClassName()+ "'");
                        System.exit(1);
                    }

                }
                scans.put(name, scan);
            }
        }

// ==== Establish Database Connection ====
        // TODO: State Tracking in the Database
        // - Record scan started in database.
        // - Track our progress as we go so a new process can pick up where
        //   we left off if our process dies.
        // - Mark when each date-station-channel-operation is complete
        //LogDatabaseHandler logDB = new LogDatabaseHandler(configuration.get

        // For each day ((yesterday - scanDepth) to yesterday)
        // scan for these channel files, only process them if
        // they have not yet been scanned, or if changes have
        // occurred to the file since its last scan. Do this for
        // each scan type. Do not re-scan data for each type,
        // launch processes for each scan and use the same data set
        // for each. If we can pipe the data as it is read, do so.
        // If we need to push all of it at once, do these in sequence
        // in order to preserve overall system memory resources.

        Scan scan = null;
        Filter networks = null;
        Set<String> netKeys = null;
        
// ==== Perform Scans ====

        scan = scans.get("daily");
        networks = scan.getNetworks();
        if(networks == null)
        	netKeys = null;
        else
        	netKeys = networks.getKeys();
        		
        
//MTH: This part could/should be moved up higher except that we need to know datalessDir, which,
//     at this point, is configured on a per scan basis ... so we need to know what scan we're doing
        MetaServer metaServer = null;
        if (config.getMetaserver() != null) {
            if (config.getMetaserver().getUseRemote().equals("yes") || config.getMetaserver().getUseRemote().equals("true")) {
                String remoteServer = config.getMetaserver().getRemoteUri();
                try {
                    metaServer = new MetaServer( new URI(remoteServer) );
                }
                catch (Exception e) {
                    logger.error("caught URI exception:" + e.getMessage() );
                }
            }
            else {
            	metaServer = new MetaServer(scan.getDatalessDir(), netKeys);
            }
        }
        else { // Use local MetaServer
        	metaServer = new MetaServer(scan.getDatalessDir(), netKeys);
        }

        List<Station> stations = null;

        if (config.getStationList() == null){       // get StationList from MetaServer
            logger.info("Get StationList from MetaServer");
            stations = metaServer.getStationList();
        }
        else {                                      // read StationList from config.xml
            logger.info("Read StationList from config.xml");
            List<String> stationList = config.getStationList().getStation();
            if (stationList.size() > 0) {
                stations = new ArrayList<Station>();
                for (String station : stationList) {
                    String[] words = station.split("_");
                    if (words.length != 2) {
                        logger.warn(String.format("stationList: station=[%s] is NOT a valid station --> Skip", station) );
                    }
                    else {
                        stations.add( new Station(words[0],words[1]) );
                        logger.info("config.xml: Read station:" + station);
                    }
                }
            }
            else {
                logger.error("Error: No valid stations read from config.xml");
            }
        }

        if (stations == null) {
            logger.error("Found NO stations to scan --> EXITTING SeedScan");
            System.exit(1);
        }

        Thread readerThread = new Thread(reader);
        readerThread.start();
        logger.info("Reader thread started.");
        
        Thread injectorThread = new Thread(injector);
        injectorThread.start();
        logger.info("Injector thread started.");

    // Loop over scans and hand each one to a ScanManager
        logger.info("Hand scan to ScanManager");
        for (String key : scans.keySet() ) {
            scan = scans.get(key);
            logger.info(String.format("Scan=[%s] startDay=%d startDate=%d daysToScan=%d\n", key, scan.getStartDay(), 
                                       scan.getStartDate(), scan.getDaysToScan() ));
            ScanManager scanManager = new ScanManager(reader, injector, stations, scan, metaServer);
        }

        logger.info("ScanManager is [ FINISHED ] --> stop the injector and reader threads");

        try {
	        injector.halt();
	        logger.info("All stations processed. Waiting for injector thread to finish...");
            synchronized(injectorThread) {
	            //injectorThread.wait();
	            injectorThread.interrupt();
            }
	        logger.info("Injector thread halted.");
        } catch (InterruptedException ex) {
        	logger.warn("The injector thread was interrupted while attempting to complete requests.");
        }
        
        try {
	        reader.halt();
	        logger.info("All stations processed. Waiting for reader thread to finish...");
            synchronized(readerThread) {
	            //readerThread.wait();
	            readerThread.interrupt();
            }
	        logger.info("Reader thread halted.");
        } catch (InterruptedException ex) {
        	logger.warn("The reader thread was interrupted while attempting to complete requests.");
        }

        try {
            lock.release();
        } catch (IOException e) {
            ;
        } finally {
logger.info("Release seedscan lock and quit metaServer");
            lock = null;
            metaServer.quit();
        }
    } // main()


} // class SeedScan
