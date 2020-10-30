package asl.seedscan;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.metrics.MetricException;
import asl.seedscan.scanner.ScanManager;
import asl.util.LockFile;
import asl.util.Logging;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import javax.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class SeedScan.
 *
 * @author James Holland - USGS
 * @author Joel Edwards - USGS
 * @author Mike Hagerty
 * @author Alejandro Gonzales - Honeywell
 * @author Nick Falco - Honeywell
 */
public class SeedScan {

  /**
   * The logger.
   */
  private static Logger logger = LoggerFactory.getLogger(asl.seedscan.SeedScan.class);

  /**
   * The main method for seedscan.
   *
   * @param args command line arguments.
   */
  public static void main(String args[]) {
    /* Some components like JFreeChart try to behave like a GUI, this fixes
     that*/
    System.setProperty("java.awt.headless", "true");

    ScanManager scanManager;
    MetaGenerator metaGenerator;
    MetricDatabase database = null;
    LockFile lock = null;

    try {
      Global.loadConfig("config.xml");

      { // This whole block is to handle the case where our data directory may not be mounted.
        // Data availability is a station-level defect. We don't want to check everything
        // against, say, stations listed in the metadata, but we do want to make sure a network
        // has some station data available, at which point it is a network-level defect.
        // We expect that there's a top-level 'network' structure we can resolve before station
        // wildcarding (i.e., "/mount/data/${NETWORK}/${STATION}" or
        // "/mount/data/${NETWORK}_${STATION}"). Otherwise this may produce false positive results.
        String path = Global.getDataDir();
        int firstWildcard = path.indexOf('$');
        String basePath = path;
        if (firstWildcard > 0) {
          basePath = basePath.substring(0, firstWildcard);
        }
        File checkExistence = new File(basePath);
        // getCanonicalPath should resolve symlink allowing us to check where it points to
        checkExistence = new File(checkExistence.getCanonicalPath());
        if (!checkExistence.exists()) {
          throw new IOException(
              "Unable to access data path [" + basePath + "]: check path exists.");
        }
        // path might exist, but what if it has no contents?
        // we'll check for subdirectories if wildcards are part of the data path
        // we look at each network independently because they may come from different mount points
        // (i.e., II might be local while IU is not)
        if (path.length() > firstWildcard) {
          for (String network : Global.getNetworkRestrictions()) {
            boolean hasValidSubdirectories = false;
            String pathFilter = path.replace("${NETWORK}", network);
            final String finalPathFilter;
            finalPathFilter = pathFilter.contains("$") ?
                pathFilter.substring(0, pathFilter.indexOf('$')) : pathFilter;
            FileFilter filter = (name) -> name.getPath().startsWith(finalPathFilter);
            File[] children = checkExistence.listFiles(filter);
            if (children.length != 0) {
              // this loop is to ensure that there's at least one subdirectory for the network
              // with valid data -- so that if we pick up files that aren't directories we don't
              // close prematurely; we'd like to have limited assumptions about the contents of
              // the file structure that eventually leads to the seed data we're scanning
              // and we'd also like to have this check be as efficient as possible
              for (File child : children) {
                if (child.exists() && child.isDirectory()) {
                  hasValidSubdirectories = true;
                  break;
                }
              }
            }
            if (!hasValidSubdirectories) {
              throw new IOException(
                  "No valid data for " + network + " within [" + basePath +
                      "] -- check proper mounting.");
            }
          }
        }
      }

      lock = new LockFile(Global.getLockfile());
      if (!lock.acquire()) {
        throw new IOException("Unable to acquire lock.");
      }

      metaGenerator = new MetaGenerator(Global.getDatalessDir(), Global.getDatalessFile(),
          Global.getNetworkRestrictions());
      database = new MetricDatabase(Global.getDatabase());
      scanManager = new ScanManager(database, metaGenerator);

      logger.info("Handing control to ScanManager");
      // Blocking call to begin scanning.
      scanManager.scan();

      // Will likely never get here.
      logger.info("ScanManager is [ FINISHED ] --> stop the injector and reader threads");


    } catch (FileNotFoundException e) {
      logger.error("FileNotFoundException: Could not locate config file:");
      logger.error(Logging.prettyExceptionWithCause(e));
    } catch (JAXBException e) {
      logger.error("JAXBException: Could not unmarshal config file:");
      logger.error(Logging.prettyExceptionWithCause(e));
    } catch (IOException | MetricException e) {
      logger.error(Logging.prettyExceptionWithCause(e));
    } catch (SQLException e) {
      logger.error("Unable to communicate with Database");
      logger.error(Logging.prettyExceptionWithCause(e));
    } finally {
      logger.info("Release seedscan lock and quit metaServer");
      try {
        if (lock != null) {
          lock.release();
        }
      } catch (IOException ignored) {
      }
      if (database != null) {
        database.close();
      }
    }
  }

}
