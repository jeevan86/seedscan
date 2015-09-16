package asl.seedscan;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.MetaServer;
import asl.metadata.Station;
import asl.seedscan.config.ArgumentT;
import asl.seedscan.config.ConfigT;
import asl.seedscan.config.MetricT;
import asl.seedscan.config.ScanT;
import asl.seedscan.metrics.MetricWrapper;
import asl.util.Filter;

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
	public static final ConfigT config; 
	
	/** Hashtable of scan objects **/
	public static final Hashtable<String, Scan> scans; 
	
	/** List of stations to be scanned **/
	public static final List<Station> stations; 
	
	/** Object used to access metadata **/
	public static final MetaServer metaServer; 
	
	/** Object that prevents two Seedscan processes from running at the same time **/
	public static LockFile lock;  
	
	/** The logger. */
	private static Logger logger = LoggerFactory.getLogger(asl.seedscan.SeedScan.class);
	
	//Static Initializer 
	static
	{
		// Default locations of config and schema files
				File configFile = new File("config.xml");
				URL schemaFile = SeedScan.class.getResource("/schemas/SeedScanConfig.xsd");
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
				config = parser.parseConfig(configFile);

				// ===== CONFIG: LOCK FILE =====
				File lockFile = new File(config.getLockfile());
				logger.info("SeedScan lock file is '" + lockFile + "'");
				lock = new LockFile(lockFile);
				if (!lock.acquire()) {
					logger.error("Could not acquire lock.");
					System.exit(1);
				}
				
				// ===== CONFIG: QUALITY FLAGS =====
				if(config.getQualityflags() == null){
					logger.error("No quality flags in configuration.");
					System.exit(1);
				}
				else{
					config.setQualityflags(config.getQualityflags());
				}

				// ===== CONFIG: SCANS =====
				scans = new Hashtable<String, Scan>();
				if (config.getScans().getScan() == null) {
					logger.error("No scans in configuration.");
					System.exit(1);
				} 
				else {
					for (ScanT scanCfg : config.getScans().getScan()) {
						String name = scanCfg.getName();
						if (scans.containsKey(name)) {
							logger.error("Duplicate scan name '" + name
									+ "' encountered.");
							System.exit(1);
						}

						// This should really be handled by jaxb by setting it up in
						// schemas/SeedScanConfig.xsd
						if (scanCfg.getStartDay() == null
								&& scanCfg.getStartDate() == null) {
							logger.error("== Must set EITHER cfg:start_day -OR- cfg:start_date in config.xml to start Scan!");
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
							logger.debug("Filter on Network Subset=[{}]",
									scanCfg.getNetworkSubset());
							Filter filter = new Filter(false);
							for (String network : scanCfg.getNetworkSubset().split(",")) {
								logger.debug("Network =[{}]", network);
								filter.addFilter(network);
							}
							scan.setNetworks(filter);
						}
						if (scanCfg.getStationSubset() != null) {
							logger.debug("Filter on Station Subset=[{}]",
									scanCfg.getStationSubset());
							Filter filter = new Filter(false);
							for (String station : scanCfg.getStationSubset().split(",")) {
								logger.debug("Station =[{}]", station);
								filter.addFilter(station);
							}
							scan.setStations(filter);
						}
						if (scanCfg.getLocationSubset() != null) {
							logger.debug("Filter on Location Subset=[{}]",
									scanCfg.getLocationSubset());
							Filter filter = new Filter(false);
							for (String location : scanCfg.getLocationSubset().split(
									",")) {
								logger.debug("Location =[{}]", location);
								filter.addFilter(location);
							}
							scan.setLocations(filter);
						}
						if (scanCfg.getChannelSubset() != null) {
							logger.debug("Filter on Channel Subset=[{}]",
									scanCfg.getChannelSubset());
							Filter filter = new Filter(false);
							for (String channel : scanCfg.getChannelSubset().split(",")) {
								logger.debug("Channel =[{}]", channel);
								filter.addFilter(channel);
							}
							scan.setChannels(filter);
						}

						for (MetricT met : scanCfg.getMetrics().getMetric()) {
							try {
								Class<?> metricClass = Class
										.forName(met.getClassName());
								MetricWrapper wrapper = new MetricWrapper(metricClass);
								for (ArgumentT arg : met.getArgument()) {
									wrapper.add(arg.getName(), arg.getValue());
								}
								scan.addMetric(wrapper);
							} catch (ClassNotFoundException ex) {
								String message = "No such metric class '"
										+ met.getClassName() + "'";
								logger.error(message, ex);
								System.exit(1);
							} catch (InstantiationException ex) {
								String message = "Could not dynamically instantiate class '"
										+ met.getClassName() + "'";
								logger.error(message, ex);
								System.exit(1);
							} catch (IllegalAccessException ex) {
								String message = "Illegal access while loading class '"
										+ met.getClassName() + "'";
								logger.error(message, ex);
								System.exit(1);
							} catch (NoSuchFieldException ex) {
								String message = "Invalid dynamic argument to Metric subclass '"
										+ met.getClassName() + "'";
								logger.error(message, ex);
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
				// we left off if our process dies.
				// - Mark when each date-station-channel-operation is complete
				// LogDatabaseHandler logDB = new LogDatabaseHandler(configuration.get

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
				if (networks == null)
					netKeys = null;
				else
					netKeys = networks.getKeys();

				// MTH: This part could/should be moved up higher except that we need to
				// know datalessDir, which,
				// at this point, is configured on a per scan basis ... so we need to
				// know what scan we're doing
				MetaServer temp_metaServer = null;
				if (config.getMetaserver() != null) {
					if (config.getMetaserver().getUseRemote().equals("yes")
							|| config.getMetaserver().getUseRemote().equals("true")) {
						String remoteServer = config.getMetaserver().getRemoteUri();
						try {
							temp_metaServer = new MetaServer(new URI(remoteServer));
						} catch (Exception e) {
							logger.error("Caught URI exception:", e);
						}
					} else {
						temp_metaServer = new MetaServer(scan.getDatalessDir(), netKeys);
					}
				} else { // Use local MetaServer
					temp_metaServer = new MetaServer(scan.getDatalessDir(), netKeys);
				}
				metaServer = temp_metaServer; 

				List<Station >temp_stations = null;
				if (config.getStationList() == null) { // get StationList from
														// MetaServer
					logger.info("Get StationList from MetaServer");
					temp_stations = metaServer.getStationList();
				} else { // read StationList from config.xml
					logger.info("Read StationList from config.xml");
					List<String> stationList = config.getStationList().getStation();
					if (stationList.size() > 0) {
						temp_stations = new ArrayList<Station>();
						for (String station : stationList) {
							String[] words = station.split("_");
							if (words.length != 2) {
								logger.warn(String
										.format("stationList: station=[%s] is NOT a valid station --> Skip",
												station));
							} else {
								temp_stations.add(new Station(words[0], words[1]));
								logger.info("config.xml: Read station:" + station);
							}
						}
					} else {
						logger.error("No valid stations read from config.xml");
					}
				}

				if (temp_stations == null) {
					logger.error("NO stations to scan --> EXITING SeedScan");
					System.exit(1);
				}
				stations = temp_stations;
	}
	
}
