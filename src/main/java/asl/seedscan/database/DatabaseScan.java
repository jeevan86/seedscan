package asl.seedscan.database;

import java.time.LocalDate;
import java.util.UUID;


public class DatabaseScan {
	
	public final UUID scanID;
	
	public final UUID parentScanID;


	/** The start date. */
	public final LocalDate startDate;
	
	/** The end date. */
	public final LocalDate endDate;
	
	/** True if the scan requires deletion of existing data. */
	public final boolean deleteExisting;

	/* Scope of Scan */
	/** Restrict to this network */
	public final String network;
	
	/** Restrict to this station */
	public final String station;
	
	/** Used for deleting existing data. */
	public final String location;
	
	/** Used for deleting existing data. */
	public final String channel;
	
	/** Only recompute this metric */
	public final String metricName;

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
	public DatabaseScan(
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
}
