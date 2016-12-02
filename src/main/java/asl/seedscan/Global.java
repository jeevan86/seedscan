package asl.seedscan;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.seedscan.config.ConfigT;
/**
 * 
 * This class parses the configuration file (config.xml) when the application is started, and makes the parsed data "globally"
 * accessible throughout the application. 
 */

public abstract class Global {
	
	/**
	 * args command line arguments that are passed from the asl.seedscan.Seedscan.java main method 
	 **/
	public static String[] args;
	
	/**Contains the getters and setters for xml structures defined as children to the ConfigT complex type in SeedScanConfig.xsd**/
	public static final ConfigT CONFIG; 
	
	/** Object that prevents two Seedscan processes from running at the same time **/
	public static LockFile lock;  
	
	/** The logger. */
	private static Logger logger = LoggerFactory.getLogger(asl.seedscan.Global.class);
	
	//Static Initializer 
	static
	{
		// Default locations of config and schema files
				File configFile = new File("config.xml");
				URL schemaFile = Global.class.getResource("/schemas/SeedScanConfig.xsd");
				ArrayList<URL> schemaFiles = new ArrayList<URL>();
				schemaFiles.add(schemaFile);

				// ==== Command Line Parsing ====
				Options options = new Options();
				Option opConfigFile = new Option(
						"c",
						"config-file",
						true,
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
					logger.error("Error while parsing command-line arguments:", e);
					System.exit(1);
				}

				Option opt;
				Iterator<?> iter = cmdLine.iterator();
				while (iter.hasNext()) {
					opt = (Option) iter.next();
					if (opt.getOpt().equals("c")) {
						configFile = new File(opt.getValue());
					} else if (opt.getOpt().equals("s")) {
						try {
							schemaFile = new File(opt.getValue()).toURI().toURL();
						} catch (MalformedURLException e) {
							logger.error("Invalid schema file.");
						}
					}
				}

				// ==== Configuration Read and Parse Actions ====
				ConfigParser parser = new ConfigParser(schemaFiles);
				CONFIG = parser.parseConfig(configFile);

				// ===== CONFIG: LOCK FILE =====
				File lockFile = new File(CONFIG.getLockfile());
				logger.info("SeedScan lock file is '" + lockFile + "'");
				lock = new LockFile(lockFile);
				if (!lock.acquire()) {
					logger.error("Could not acquire lock.");
					System.exit(1);
				}
				
				// ===== CONFIG: QUALITY FLAGS =====
				if(CONFIG.getQualityflags() == null){
					logger.error("No quality flags in configuration.");
					System.exit(1);
				}
	}
	
}
