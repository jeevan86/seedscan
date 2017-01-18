package asl.seedscan.scanner.scanworker;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Station;
import asl.seedscan.database.DatabaseScan;
import asl.seedscan.scanner.ScanManager;
import asl.util.Logging;

/**
 * This worker retrieves a Scan from the database. Determines if it is a
 * Station and splits it into station scans if it is not.
 * 
 * @author jholland - USGS
 *
 */
public class RetrieveScan extends ScanWorker {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.scanner.scanworker.RetrieveScan.class);

	public RetrieveScan(ScanManager manager) {
		super(manager);
	}

	@Override
	public void run() {
		//Runtime Exceptions thrown here are not caught anywhere else.
		try{
		// Get Scan from database
		logger.info("Taking scan from database");
		DatabaseScan newScan = manager.database.takeNextScan();

		if (newScan != null) {
			logger.info("Scan loaded: {}", newScan);
			long dayLength = ChronoUnit.DAYS.between(newScan.startDate, newScan.endDate);
			String[] networks = null;
			if(newScan.network != null) {
				networks = newScan.network.split(",");
			}
			String[] stations = null;
			if(newScan.station != null){
				stations = newScan.station.split(",");
			}

			// Check if it is a Station Scan.
			if (networks != null && stations != null && stations.length == 1 && networks.length == 1 && dayLength <= 30) {
				logger.info("Station Scan loaded adding to local queue");
				manager.addTask(new StationScan(manager, newScan));
			}
			// Split the non Station Scan into Station Scans
			else {
				logger.info("Splitting Scan {}", newScan.scanID);
				// Get possible stations list
				List<Station> possibleStations = manager.metaGenerator.getStationList(networks, stations);
				LocalDate start = newScan.startDate;
				LocalDate end;
				do {
					end = start.plusDays(30);
					if (end.compareTo(newScan.endDate) > 0) {
						end = newScan.endDate;
					}
					for (Station station : possibleStations) {
						//@formatter:off
						manager.database.insertChildScan(
								newScan.scanID,
								station.getNetwork(),
								station.getStation(),
								newScan.location,
								newScan.channel,
								newScan.metricName,
								start, end,
								newScan.priority,
								newScan.deleteExisting);
						//@formatter:on
					}
					
					start = end;
				} while (!end.equals(newScan.endDate));
			}
			// Add new Retriever to queue since we know more probably exist.
			manager.addTask(new RetrieveScan(manager));
		}
		else{
			logger.info("Database has no Scans left!");
		}
			/*
			 * Don't bother adding a new Retrieving Task since DB is empty. A
			 * different process will handle this.
			 */

		}catch(Exception e){
			String message = Logging.exceptionToString(e);
			logger.error(message);
			manager.database.insertError(message);
		}
	}

	@Override
	Integer getBasePriority() {
		// This should always run after everything else.
		return Integer.MAX_VALUE;
	}

	@Override
	Long getFinePriority() {
		// This should always run after everything else.
		return Long.MAX_VALUE;
	}

}
