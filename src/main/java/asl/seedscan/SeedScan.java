package asl.seedscan;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.metrics.MetricException;
import asl.seedscan.scanner.ScanManager;
import asl.util.LockFile;
import asl.util.Logging;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
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
      int pathLength = path.length();
      if (firstWildcard > 0) {
        path = path.substring(0, firstWildcard);
      }
      File checkExistence = new File(path);
      // getCanonicalPath should resolve symlink allowing us to check where it points to
      checkExistence = new File(checkExistence.getCanonicalPath());
      if (!checkExistence.exists()) {
        throw new IOException("Unable to access data path [" + path + "] -- check path exists.");
      }
      // path might exist, but what if it has no contents?
      int validSubdirectories = 0;
      // we'll check for subdirectories if wildcards are part of the data path
      // (e.g., there are folders like ${NEWORK}_${STATION} holding data)
      if (pathLength > firstWildcard) {
        System.out.println(checkExistence.listFiles().length);
        for (File file : checkExistence.listFiles()) {
          if (file.isDirectory()) {
            ++validSubdirectories;
          }
        }
        if (validSubdirectories == 0) {
          throw new IOException("No valid data within [" + path + "] -- check proper mounting.");
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
