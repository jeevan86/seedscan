package asl.seedscan;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.MetaServer;
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
 * 
 */
public class SeedScan {

	/** The logger. */
	private static Logger logger = LoggerFactory.getLogger(asl.seedscan.SeedScan.class);

	/**
	 * The main method for seedscan.
	 *
	 * @param args
	 *            command line arguments.
	 */
	public static void main(String args[]) {
		// Some components like JFreeChart try to behave like a GUI, this fixes
		// that
		System.setProperty("java.awt.headless", "true");

		ScanManager scanManager = null;
		MetaServer metaServer = null;
		MetricDatabase database = null;

		try {
			//Change of scope for Garbage collection since setup objects may exist for life of program.
			{
				List<String> networks = new ArrayList<String>();
				if (Global.CONFIG.getNetworkSubset() != null) {
					logger.debug("Filter on Network Subset=[{}]", Global.CONFIG.getNetworkSubset());
					for (String network : Global.CONFIG.getNetworkSubset().split(",")) {
						networks.add(network);
					}
				}

				metaServer = new MetaServer(Global.CONFIG.getDatalessDir(), networks);
				database = new MetricDatabase(Global.CONFIG.getDatabase());
				scanManager = new ScanManager(database, metaServer);
			}

			logger.info("Handing control to ScanManager");
			// Blocking call to begin scanning.
			scanManager.scan();

			// Will likely never get here.
			logger.info("ScanManager is [ FINISHED ] --> stop the injector and reader threads");

			Global.lock.release();

		} catch (IOException e) {
			logger.error("IOException:", e);
		} catch (SQLException e) {
			logger.error("Unable to communicate with Database");
			logger.error(e.getLocalizedMessage());
		} finally {
			logger.info("Release seedscan lock and quit metaServer");
			Global.lock = null;
			if (database != null){
				database.close();
			}
			if (metaServer != null){
				metaServer.quit();
			}
			System.exit(0);
		}
	} // main()

} // class SeedScan
