package asl.seedscan;

import static asl.util.Logging.exceptionToString;
import static asl.util.Logging.prettyException;

import asl.seedscan.metrics.MetricException;
import asl.util.LockFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

import javax.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.scanner.ScanManager;

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

      lock = new LockFile(Global.getLockfile());
      if (!lock.acquire()) {
        throw new IOException("Unable to acquire lock.");
      }

      metaGenerator = new MetaGenerator(Global.getDatalessDir(), Global.getNetworkRestrictions());
      database = new MetricDatabase(Global.getDatabase());
      scanManager = new ScanManager(database, metaGenerator);

      logger.info("Handing control to ScanManager");
      // Blocking call to begin scanning.
      scanManager.scan();

      // Will likely never get here.
      logger.info("ScanManager is [ FINISHED ] --> stop the injector and reader threads");


    } catch(FileNotFoundException e) {
      logger.error("FileNotFoundException: Could not locate config file:");
      logger.error(prettyException(e));
    } catch(JAXBException e){
      logger.error("JAXBException: Could not unmarshal config file:");
      logger.error(prettyException(e));
  }catch (IOException | MetricException e) {
      logger.error(prettyException(e));
    } catch (SQLException e) {
      logger.error("Unable to communicate with Database");
      logger.error(exceptionToString(e));
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
