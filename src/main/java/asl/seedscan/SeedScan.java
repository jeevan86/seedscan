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
        throw new IOException("Unable to access data path [" + basePath + "]: check path exists.");
      }
      // path might exist, but what if it has no contents?
      // we'll check for subdirectories if wildcards are part of the data path
      // (e.g., there are folders like ${NEWORK}_${STATION} holding data)
      // and because only some of them may be pointed to via symlinks we'll try a
      if (path.length() > firstWildcard) {
        for (String network : Global.getNetworkRestrictions()) {
          int validSubdirectories = 0;
          // this probably won't work unless networks have hierarchy over station in path
          // (i.e., only works if paths are like /msd/IU/ANMO or /msd/IU_ANMO)
          String pathFilter = path.replace("${NETWORK}", network);
          final String finalPathFilter;
          finalPathFilter = pathFilter.contains("$") ?
              pathFilter.substring(0, pathFilter.indexOf('$')) : pathFilter;
          FileFilter filter = (name) -> name.getPath().startsWith(finalPathFilter);
          File[] children = checkExistence.listFiles(filter);
          System.out.println(Arrays.toString(children));
          if (children[0].exists() && children[0].isDirectory()) {
              ++validSubdirectories;
          }
          if (validSubdirectories == 0) {
            throw new IOException(
                "No valid data for " + network + " within [" + basePath +
                    "] -- check proper mounting.");
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
