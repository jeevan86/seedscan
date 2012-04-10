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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import asl.seedscan.config.Configuration;
import asl.seedscan.config.ConfigReader;

/**
 * 
 */
public class SeedScan
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.SeedScan");

    private static final String allchanURLstr = "http://wwwasl/uptime/honeywell/gsn_allchan.txt";
    private static URL allchanURL;
    private static Handler consoleHandler;

    public static void findConsoleHandler()
    {
     // Locate the global logger's ConsoleHandler if it exists
        Logger globalLogger = Logger.getLogger("");
        for (Handler handler: globalLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                consoleHandler = handler;
                break;
            }
        }

     // Ensure the global logger has an attached ConsoleHandler
     // creating one for it if necessary
        if (consoleHandler == null) {
            consoleHandler = new ConsoleHandler();
            globalLogger.addHandler(consoleHandler);
        }
    }

    public static void main(String args[])
    {
        findConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        Logger.getLogger("").setLevel(Level.ALL);
        Logger.getLogger("asl.seedscan").setLevel(Level.FINEST);
        Logger.getLogger("asl.seedscan.config").setLevel(Level.FINEST);

     // Default locations of config and schema files
        File configFile = new File("config.xml");
        File schemaFile = new File("schemas/SeedScanConfig.xsd");
        boolean parseConfig = true;

// ==== Command Line Parsing ====
        Options options = new Options();
        Option opConfigFile = new Option("c", "config-file", true, 
                            "The config file to use for seedscan. XML format according to SeedScanConfig.xsd.");
        Option opSchemaFile = new Option("s", "schema-file", true, 
                            "The schame file which should be used to verify the config file format. ");  

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
            logger.severe("Error while parsing command-line arguments.");
            System.exit(1);
        }

        Option opt;
        Iterator iter = cmdLine.iterator();
        while (iter.hasNext()) {
            opt = (Option)iter.next();
            if (opt.getOpt().equals("c")) {
                configFile = new File(opt.getValue());
            }
            else if (opt.getOpt().equals("s")) {
                schemaFile = new File(opt.getValue());
            }
        }

// ==== Default Patterns ====
        String tr1PathPattern = "/tr1/telemetry_days/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}";
        //String xs0PathPattern = "/xs0/seed/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}_${NETWORK}_${STATION}";
        //String xs1PathPattern = "/xs1/seed/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}_${NETWORK}_${STATION}";
        //String lockFilePath = "seedscan.lock";
        //String url  = "jdbc:mysql://136.177.121.210:54321/seedscan";
        //String user = "seedscan_write";
        //String pass = "";

        //String url  = "jdbc:oracle://<server>:<port>/database";
        //Console cons = System.console();
        //char[] pass = cons.readPassword("SeedScan Unlock Password: ");

        ConfigReader configReader = new ConfigReader(schemaFile);
        configReader.loadConfiguration(configFile);
        Configuration configuration = configReader.getConfiguration();

// ==== Configuration Read and Parse Actions ====
     // Print out configuration file contents
        Formatter formatter = new Formatter(new StringBuilder(), Locale.US);

        // TODO: print out the configuration
        //String format = formatter.format(" %%1$%ds : %%2$s\n", maxKey).toString();
        //System.out.printf("");

     // Set the lock file from the configuration
        LockFile lock = new LockFile(configuration.getLockFile());
        if (!lock.acquire()) {
            logger.severe("Could not acquire lock.");
            System.exit(1);
        }
        
     // Set the log file from the configuration
        LogFileHandler logFile = new LogFileHandler(configuration.getLogConfig());
        logFile.setLevel(Level.ALL);
        Logger.getLogger("").addHandler(logFile);

     // Set logger levels from configuration
        //Logger.getLogger("asl.seedscan").setLevel(Level.FINEST);
        //Logger.getLogger("asl.seedscan.Scanner").setLevel(Level.INFO);
        //Logger.getLogger("asl.seedsplitter").setLevel(Level.INFO);


        // TODO: Locate scans in config file




        int startDepth = 1; // Start this many days back.
        int scanDepth  = 2; // Number of days to evaluate.
        boolean scanXS0 = false;

// ==== Establish Database Connection ====
        // TODO: State Tracking in the Database
        // - Record scan started in database.
        // - Track our progress as we go so a new process can pick up where
        //   we left off if our process dies.
        // - Mark when each date-station-channel-operation is complete

        //StationDatabase database = new StationDatabase(url, user, pass);
        StationDatabase database = null;
        //Station[] stations = null;

        // Get the list of active channels from a web page where they are currently maintained.
        try 
        {
            allchanURL = new URL(allchanURLstr);
        } catch (MalformedURLException e1) {
            logger.warning("Exception opening URL %s.");
            e1.printStackTrace();
            allchanURL = null;
        }

        if (allchanURL == null)
        {
            logger.severe("Unable to open resource " +allchanURLstr+ ", aborting!\n");
            System.exit(1);
        }

        logger.info("We seem to have found " +allchanURLstr+ "\n");
        logger.fine("Testing Logger Level FINE");
        logger.finer("Testing Logger Level FINER");
        logger.finest("Testing Logger Level FINEST");

        // TEST LIST (TODO: Remove once configurations are working)
        Station[] stations = {
            new Station("IU", "ANMO"),
            /*
            new Station("IU", "CCM"),
            new Station("IU", "COR"),
            new Station("IU", "HKT"),
            new Station("IU", "MA2"),
            new Station("IU", "MAJO"),
            new Station("IU", "SJG"),
            new Station("IU", "YSS"),
            */
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

// ==== Perform Scans ====
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
