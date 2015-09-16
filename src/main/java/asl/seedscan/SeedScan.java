package asl.seedscan;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.database.MetricInjector;
import asl.seedscan.database.MetricReader;

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
		
		Global.args = args; //Pass command line arguments to the Global class. 

		// ===== CONFIG: DATABASE =====
		MetricDatabase readDB = new MetricDatabase(Global.config.getDatabase());
		MetricDatabase writeDB = new MetricDatabase(Global.config.getDatabase());
		MetricReader reader = new MetricReader(readDB);
		MetricInjector injector = new MetricInjector(writeDB);

		Thread readerThread = new Thread(reader);
		readerThread.start();
		logger.info("Reader thread started.");

		Thread injectorThread = new Thread(injector);
		injectorThread.start();
		logger.info("Injector thread started.");

		// Loop over scans and hand each one to a ScanManager
		logger.info("Hand scan to ScanManager");		

		for (String key : Global.scans.keySet()) {
			Scan scan = Global.scans.get(key);
			logger.info(String.format(
					"Scan=[%s] startDay=%d startDate=%d daysToScan=%d\n", key,
					scan.getStartDay(), scan.getStartDate(),
					scan.getDaysToScan()));
			@SuppressWarnings("unused")
			ScanManager scanManager = new ScanManager(reader, injector,
					Global.stations, scan, Global.metaServer);
		}

		logger.info("ScanManager is [ FINISHED ] --> stop the injector and reader threads");

		try {
			injector.halt();
			logger.info("All stations processed. Waiting for injector thread to finish...");
			synchronized (injectorThread) {
				// injectorThread.wait();
				injectorThread.interrupt();
			}
			logger.info("Injector thread halted.");
		} catch (InterruptedException ex) {
			String message = "The injector thread was interrupted while attempting to complete requests.";
			logger.warn(message, ex);
		}

		try {
			reader.halt();
			logger.info("All stations processed. Waiting for reader thread to finish...");
			synchronized (readerThread) {
				// readerThread.wait();
				readerThread.interrupt();
			}
			logger.info("Reader thread halted.");
		} catch (InterruptedException ex) {
			String message = "The reader thread was interrupted while attempting to complete requests.";
			logger.warn(message, ex);
		}

		try {
			Global.lock.release();
		} catch (IOException e) {
			logger.error("IOException:", e);
		} finally {
			logger.info("Release seedscan lock and quit metaServer");
			Global.lock = null;
			Global.metaServer.quit();
			System.exit(0);	
		}
	} // main()

} // class SeedScan
