package asl.seedscan;

import asl.seedscan.config.ArgumentT;
import asl.seedscan.config.ConfigT;
import asl.seedscan.config.DatabaseT;
import asl.seedscan.config.MetricT;
import asl.seedscan.metrics.MetricException;
import asl.seedscan.metrics.MetricWrapper;
import asl.util.LockFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class parses the configuration file (config.xml) when the application is
 * started, and makes the parsed data "globally" accessible throughout the
 * application.
 *
 * @author James Holland - USGS
 * @author Nicholas Falco - Honeywell
 */

public abstract class Global {

  /**
   * Contains the getters and setters for xml structures defined as children
   * to the ConfigT complex type in SeedScanConfig.xsd
   **/
  private static ConfigT CONFIG;

  /**
   * Object that prevents two Seedscan processes from running at the same time
   **/
  public static LockFile lock;

  /**
   * An unmodifiable list of Metrics from the config file.
   */
  protected static List<MetricWrapper> metrics;
  protected static List<String> networkRestrictions;

  protected static final Logger logger = LoggerFactory.getLogger(asl.seedscan.Global.class);
  protected static String datalessDir;
  protected static DatabaseT database;
  protected static String plotsDir;
  protected static String dataDir;
  protected static String eventsDir;
  protected static String qualityflags;
  protected static String lockfile;


  /**
   * Load a configuration file into the global state.
   *
   * @param configPath dataDir to the configuration file.
   * @throws MetricException if an exception is encountered generated the available metric list
   * @throws FileNotFoundException if the file is not found or readable
   * @throws JAXBException if the configuration cannot be marshalled
   */
  static void loadConfig(String configPath)
      throws MetricException, FileNotFoundException, JAXBException {

    // Default locations of config and schema files
    File configFile = new File(configPath);
    URL schemaFile = Global.class.getResource("/schemas/SeedScanConfig.xsd");
    ArrayList<URL> schemaFiles = new ArrayList<>();
    schemaFiles.add(schemaFile);

    // ==== Configuration Read and Parse Actions ====
    ConfigParser parser = new ConfigParser(schemaFiles);
    CONFIG = parser.parseConfig(configFile);

    // ===== CONFIG: QUALITY FLAGS =====

    ArrayList<MetricWrapper> metrics = new ArrayList<>();
    for (MetricT met : Global.CONFIG.getMetrics().getMetric()) {
      try {
        Class<?> metricClass = Class.forName(met.getClassName());
        MetricWrapper wrapper = new MetricWrapper(metricClass);
        for (ArgumentT arg : met.getArgument()) {
          wrapper.add(arg.getName(), arg.getValue());
        }
        metrics.add(wrapper);
      } catch (ClassNotFoundException ex) {
        throw new MetricException("No such metric class '" + met.getClassName() + "'", ex);
      } catch (InstantiationException ex) {
        throw new MetricException(
            "Could not dynamically instantiate class '" + met.getClassName() + "'", ex);
      } catch (IllegalAccessException ex) {
        throw new MetricException("Illegal access while loading class '" + met.getClassName() + "'",
            ex);
      } catch (NoSuchFieldException ex) {
        throw new MetricException(
            "Invalid dynamic argument to Metric subclass '" + met.getClassName() + "'", ex);
      }
    }

    Global.metrics = Collections.unmodifiableList(metrics);

    List<String> networks = new ArrayList<>();
    if (Global.CONFIG.getNetworkSubset() != null) {
      logger.debug("Filter on Network Subset=[{}]", Global.CONFIG.getNetworkSubset());
      Collections.addAll(networks, Global.CONFIG.getNetworkSubset().split(","));
    }

    networkRestrictions = Collections.unmodifiableList(networks);

    datalessDir = CONFIG.getDatalessDir();
    database = CONFIG.getDatabase();

    lockfile = CONFIG.getLockfile();

    qualityflags = CONFIG.getQualityflags();
    if (qualityflags == null) {
      logger.error("No data quality flags in configuration: Using default \"All\"");
      qualityflags = "All";
    }

    plotsDir = CONFIG.getPlotsDir();

    dataDir = CONFIG.getPath();

    eventsDir = CONFIG.getEventsDir();
  }

  public static List<String> getNetworkRestrictions() {
    return networkRestrictions;
  }

  public static String getDatalessDir() {
    return datalessDir;
  }

  public static DatabaseT getDatabase() {
    return database;
  }

  public static List<MetricWrapper> getMetrics() {
    return metrics;
  }

  public static String getPlotsDir() {
    return plotsDir;
  }

  public static String getDataDir() {
    return dataDir;
  }

  public static String getEventsDir() {
    return eventsDir;
  }

  public static String getQualityflags() {
    return qualityflags;
  }

  public static String getLockfile() {
    return lockfile;
  }
}
