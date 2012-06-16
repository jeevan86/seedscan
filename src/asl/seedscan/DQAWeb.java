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

import java.io.*;
import java.io.BufferedInputStream;
import java.io.Console;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Hashtable;
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

import asl.logging.*;
import asl.security.*;
import asl.seedscan.config.*;
import asl.seedscan.scan.*;

/**
 * 
 */
public class DQAWeb
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.DQAWeb");

    private static Handler consoleHandler;
    private static Handler logDatabaseHandler;
    private static Handler logFileHandler;

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

    private static String getALLMetrics(String startDate, String endDate)
    {
        String result = "Gottem";
        return result;
    }

    public static void main(String args[])
    {
        findConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        Logger.getLogger("").setLevel(Level.CONFIG);

     // Default locations of config and schema files
        File configFile = new File("dqaweb-config.xml");
        File schemaFile = new File("schemas/DQAWebConfig.xsd");
        boolean parseConfig = true;
        boolean testMode = false;

        ArrayList<File> schemaFiles = new ArrayList<File>();
        schemaFiles.add(schemaFile);
// ==== Command Line Parsing ====
        Options options = new Options();
        Option opConfigFile = new Option("c", "config-file", true, 
                            "The config file to use for seedscan. XML format according to SeedScanConfig.xsd.");
        Option opSchemaFile = new Option("s", "schema-file", true, 
                            "The schame file which should be used to verify the config file format. ");  
        Option opTest = new Option("t", "test", false, 
                                   "Run in test console mode rather than as a servlet.");

        OptionGroup ogConfig = new OptionGroup();
        ogConfig.addOption(opConfigFile);

        OptionGroup ogSchema = new OptionGroup();
        ogConfig.addOption(opSchemaFile);

        OptionGroup ogTest = new OptionGroup();
        ogTest.addOption(opTest);

        options.addOptionGroup(ogConfig);
        options.addOptionGroup(ogSchema);
        options.addOptionGroup(ogTest);

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
            else if (opt.getOpt().equals("t")) {
                testMode = true;
            }
        }
        while (testMode == true ){
            InputStreamReader input = new InputStreamReader(System.in);
            BufferedReader reader = new BufferedReader(input);
            try{
                String query = "";
                System.err.println("Entering Test Mode");
                System.err.println("Enter a query string to view results or type \"help\" for example query strings");
                System.err.printf("Query: ");
                query = reader.readLine();
                if (query.equals("exit")){
                    testMode = false;
                }
                if (query.equals("ALL")){
                    String testStr = getALLMetrics("03-01-2012", "03-31-2012");
                    System.err.println(testStr);
                }
            }
            catch (IOException err) {
                System.out.println("Error reading line");
            }
        }
        System.err.printf("DONE.\n");
    } // main()
} // class DQAWeb
