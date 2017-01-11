package asl.seedscan;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.seedscan.config.ArgumentT;
import asl.seedscan.config.ConfigT;
import asl.seedscan.config.MetricT;
import asl.seedscan.metrics.MetricWrapper;

/**
 * 
 * This class parses the configuration file (config.xml) when the application is
 * started, and makes the parsed data "globally" accessible throughout the
 * application.
 */

public abstract class Global {

	/**
	 * Contains the getters and setters for xml structures defined as children
	 * to the ConfigT complex type in SeedScanConfig.xsd
	 **/
	public static final ConfigT CONFIG;

	/**
	 * Object that prevents two Seedscan processes from running at the same time
	 **/
	public static LockFile lock;
	
	/**
	 * An unmodifiable list of Metrics from the config file.
	 */
	public static List<MetricWrapper> METRICS;

	private static Logger logger = LoggerFactory.getLogger(asl.seedscan.Global.class);


	static {
		
		// Default locations of config and schema files
		File configFile = new File("config.xml");
		URL schemaFile = Global.class.getResource("/schemas/SeedScanConfig.xsd");
		ArrayList<URL> schemaFiles = new ArrayList<URL>();
		schemaFiles.add(schemaFile);

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
		if (CONFIG.getQualityflags() == null) {
			logger.error("No data quality flags in configuration.");
			System.exit(1);
		}

		ArrayList<MetricWrapper> metrics = new ArrayList<MetricWrapper>();
		for (MetricT met : Global.CONFIG.getMetrics().getMetric()) {
			try {
				Class<?> metricClass = Class.forName(met.getClassName());
				MetricWrapper wrapper = new MetricWrapper(metricClass);
				for (ArgumentT arg : met.getArgument()) {
					wrapper.add(arg.getName(), arg.getValue());
				}
				metrics.add(wrapper);
			} catch (ClassNotFoundException ex) {
				String message = "No such metric class '" + met.getClassName() + "'";
				logger.error(message, ex);
				System.exit(1);
			} catch (InstantiationException ex) {
				String message = "Could not dynamically instantiate class '" + met.getClassName() + "'";
				logger.error(message, ex);
				System.exit(1);
			} catch (IllegalAccessException ex) {
				String message = "Illegal access while loading class '" + met.getClassName() + "'";
				logger.error(message, ex);
				System.exit(1);
			} catch (NoSuchFieldException ex) {
				String message = "Invalid dynamic argument to Metric subclass '" + met.getClassName() + "'";
				logger.error(message, ex);
				System.exit(1);
			}
		}

		METRICS = Collections.unmodifiableList(metrics);
	}

}
