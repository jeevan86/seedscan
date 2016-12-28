package asl.seedscan;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.MetaServer;
import asl.metadata.Station;
import asl.seedscan.config.ArgumentT;
import asl.seedscan.config.MetricT;
import asl.seedscan.config.ScanT;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.metrics.MetricWrapper;

/**
 * The Class SeedScan.
 * @author James Holland - USGS
 * @author Joel Edwards - USGS
 * @author Mike Hagerty
 * @author Alejandro Gonzales - Honeywell
 * @author Nick Falco - Honeywell
 * 
 * The parsing of the config file has been moved from the Seedscan's main method 
 * to the Global class's static initializer. 
 */
public class SeedScan {
	
	/** The logger. */
	private static Logger logger = LoggerFactory.getLogger(asl.seedscan.SeedScan.class);

	/**
	 * The main method for seedscan.
	 *
	 * @param args command line arguments. 
	 */
	public static void main(String args[]) {
		//Some components like JFreeChart try to behave like a GUI, this should fix that
		System.setProperty("java.awt.headless", "true");
		Global.args = args; //Pass command line arguments to the Global class. 

		// ===== CONFIG: SCANS =====
		Hashtable<String, Scan> scans = new Hashtable<String, Scan>();
		if (Global.CONFIG.getScans().getScan() == null) {
			logger.error("No scans in configuration.");
			System.exit(1);
		} 
		else {
			for (ScanT scanCfg : Global.CONFIG.getScans().getScan()) {
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
					ScanFilter filter = new ScanFilter(false);
					for (String network : scanCfg.getNetworkSubset().split(",")) {
						logger.debug("Network =[{}]", network);
						filter.addFilter(network);
					}
					scan.setNetworks(filter);
				}
				if (scanCfg.getStationSubset() != null) {
					logger.debug("Filter on Station Subset=[{}]",
							scanCfg.getStationSubset());
					ScanFilter filter = new ScanFilter(false);
					for (String station : scanCfg.getStationSubset().split(",")) {
						logger.debug("Station =[{}]", station);
						filter.addFilter(station);
					}
					scan.setStations(filter);
				}
				if (scanCfg.getLocationSubset() != null) {
					logger.debug("Filter on Location Subset=[{}]",
							scanCfg.getLocationSubset());
					ScanFilter filter = new ScanFilter(false);
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
					ScanFilter filter = new ScanFilter(false);
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
		ScanFilter networks = null;
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
		MetaServer metaServer = null;
		if (Global.CONFIG.getMetaserver() != null) {
			if (Global.CONFIG.getMetaserver().getUseRemote().equals("yes")
					|| Global.CONFIG.getMetaserver().getUseRemote().equals("true")) {
				String remoteServer = Global.CONFIG.getMetaserver().getRemoteUri();
				try {
					metaServer = new MetaServer(new URI(remoteServer));
				} catch (Exception e) {
					logger.error("Caught URI exception:", e);
				}
			} else {
				metaServer = new MetaServer(scan.getDatalessDir(), netKeys);
			}
		} else { // Use local MetaServer
			metaServer = new MetaServer(scan.getDatalessDir(), netKeys);
		}

		List<Station> stations = null;
		if (Global.CONFIG.getStationList() == null) { // get StationList from
												// MetaServer
			logger.info("Get StationList from MetaServer");
			stations = metaServer.getStationList();
		} else { // read StationList from config.xml
			logger.info("Read StationList from config.xml");
			List<String> stationList = Global.CONFIG.getStationList().getStation();
			if (stationList.size() > 0) {
				stations = new ArrayList<Station>();
				for (String station : stationList) {
					String[] words = station.split("_");
					if (words.length != 2) {
						logger.warn(String
								.format("stationList: station=[%s] is NOT a valid station --> Skip",
										station));
					} else {
						stations.add(new Station(words[0], words[1]));
						logger.info("config.xml: Read station:" + station);
					}
				}
			} else {
				logger.error("No valid stations read from config.xml");
			}
		}

		if (stations == null) {
			logger.error("NO stations to scan --> EXITING SeedScan");
			System.exit(1);
		}
		
		// ===== CONFIG: DATABASE =====
		MetricDatabase database = null;
		try {
			database = new MetricDatabase(Global.CONFIG.getDatabase());
		} catch (SQLException e1) {
			logger.error("Unable to communicate with Database");
			System.exit(1);
		}

		// Loop over scans and hand each one to a ScanManager
		logger.info("Hand scan to ScanManager");		

		for (String key : scans.keySet()) {
			scan = scans.get(key);
			logger.info(String.format(
					"Scan=[%s] startDay=%d startDate=%d daysToScan=%d\n", key,
					scan.getStartDay(), scan.getStartDate(),
					scan.getDaysToScan()));
			@SuppressWarnings("unused")
			ScanManager scanManager = new ScanManager(database,
					stations, scan, metaServer);
		}

		logger.info("ScanManager is [ FINISHED ] --> stop the injector and reader threads");

		database.close();
		try {
			Global.lock.release();
		} catch (IOException e) {
			logger.error("IOException:", e);
		} finally {
			logger.info("Release seedscan lock and quit metaServer");
			Global.lock = null;
			metaServer.quit();
			System.exit(0);	
		}
	} // main()

} // class SeedScan
