package asl.seedscan.scanner.scanworker;

import java.time.temporal.ChronoUnit;
import java.util.List;

import asl.metadata.Station;
import asl.seedscan.database.DatabaseScan;
import asl.seedscan.scanner.ScanManager;

/**]
 * This worker retrieves a Scan from the database.
 * Determines if it is a Station and splits it into station scans if it is not.
 * @author jholland - USGS
 *
 */
public class RetrieveScan extends ScanWorker {

	RetrieveScan(ScanManager manager) {
		super(manager);
	}

	@Override
	public void run() {
		//Get Scan from database
		DatabaseScan newScan = manager.database.takeNextScan();
		
		long dayLength = ChronoUnit.DAYS.between(newScan.startDate, newScan.endDate);
		String[] networks = newScan.network.split(",");
		String[] stations = newScan.station.split(",");
		
		//Check if it is a Station Scan.
		if(stations.length == 1 && networks.length == 1 && dayLength <=30){
			manager.threadPool.submit(new StationScan(manager, newScan));
		}
		//Split the non Station Scan into Station Scans
		else{
			//Get possible stations list
			List<Station> possibleStations = manager.metaGenerator.getStationList();
		}
		
		//Add new Retriever to queue
		manager.threadPool.submit(new RetrieveScan(manager));
	}

	@Override
	Integer getBasePriority() {
		//This should always run after everything else.
		return Integer.MAX_VALUE;
	}

	@Override
	Long getFinePriority() {
		//This should always run after everything else.
		return Long.MAX_VALUE;
	}

}
