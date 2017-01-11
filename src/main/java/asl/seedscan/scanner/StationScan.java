package asl.seedscan.scanner;

import java.time.LocalDate;
import java.util.UUID;

// TODO: Auto-generated Javadoc
/**
 * The Class Scan.
 */
public class StationScan extends Worker {
	
	protected final UUID scanID;
	
	protected final UUID parentScanID;


	/** The start date. */
	protected final LocalDate startDate;
	
	/** The end date. */
	protected final LocalDate endDate;
	
	/** True if the scan requires deletion of existing data. */
	protected final boolean deleteExisting;

	/* Scope of Scan */
	/** Restrict to this network */
	protected final String network;
	
	/** Restrict to this station */
	protected final String station;
	
	/** Used for deleting existing data. */
	protected final String location;
	
	/** Used for deleting existing data. */
	protected final String channel;
	
	/** Only recompute this metric */
	protected final String metricName;

	/**
	 * Instantiates a new scan.
	 *
	 * @param startDate the start date
	 * @param endDate the end date
	 * @param deleteExisting the delete existing
	 * @param network the network
	 * @param station the station
	 * @param location the location
	 * @param channel the channel
	 * @param metricName the metric name
	 */
	//@formatter:off
	public StationScan(
		UUID scanID,
		UUID parentScanID,
		String metricName,
		String network,
		String station,
		String location,
		String channel,
		LocalDate startDate,
		LocalDate endDate,
		boolean deleteExisting
	) {
		this.scanID = scanID;
		this.parentScanID = parentScanID;
		this.startDate = startDate;
		this.endDate = endDate;
		this.deleteExisting = deleteExisting;

		this.network = network;
		this.station = station;
		this.location = location;
		this.channel = channel;
		this.metricName = metricName;
	}
	//@formatter:on

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Worker o) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see asl.seedscan.worker.Worker#getPriorityBase()
	 */
	@Override
	public int getPriorityBase() {
		return 1;
	}

}
