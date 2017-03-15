package asl.seedscan;

import asl.seedscan.config.DatabaseT;
import asl.util.LockFile;
import java.io.File;
import java.io.FileNotFoundException;
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
 * This class parses the configuration file (config.xml) when the application is
 * started, and makes the parsed data "globally" accessible throughout the
 * application.
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
  protected static String path;
  protected static String eventsDir;
  protected static String qualityflags;
  protected static String lockfile;


  static void loadConfig(String configPath) throws FileNotFoundException {

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

    path = CONFIG.getPath();

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

  public static String getPath() {
    return path;
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
