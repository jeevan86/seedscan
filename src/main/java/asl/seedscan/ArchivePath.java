package asl.seedscan;

import java.util.GregorianCalendar;

import asl.metadata.Station;

/**
 * The Class ArchivePath.
 * This class generates file paths for seed files from a pattern set in the config.xml file.
 */
public class ArchivePath {

	/** The timestamp. */
	private GregorianCalendar timestamp;
	
	/** The station. */
	private Station station = null;

	/**
	 * Instantiates a new archive path based on a Station
	 *
	 * @param station the station
	 */
	public ArchivePath(Station station) {
		this.station = station;
	}

	/**
	 * Instantiates a new archive path based on a GregorianCalendar and Station
	 *
	 * @param timestamp the timestamp
	 * @param station the station
	 */
	ArchivePath(GregorianCalendar timestamp, Station station) {
		this.timestamp = timestamp;
		this.station = station;
	}

	/**
	 * Sets the timestamp.
	 *
	 * @param timestamp the new timestamp
	 */
	public void setTimestamp(GregorianCalendar timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Sets the station.
	 *
	 * @param station the new station
	 */
	public void setStation(Station station) {
		this.station = station;
	}

	/**
	 * Make path.
	 *
	 * @param pattern the pattern
	 * @return the pattern after appropriate replacements
	 */
	public String makePath(String pattern) {
		if (station != null) {
			if (station.getNetwork() != null) {
				pattern = pattern.replace("${NETWORK}", station.getNetwork());
			}
			pattern = pattern.replace("${STATION}", station.getStation());
		}

		pattern = pattern.replace("${YEAR}", String.format("%1$tY", timestamp));
		pattern = pattern
				.replace("${MONTH}", String.format("%1$tm", timestamp));
		pattern = pattern.replace("${DAY}", String.format("%1$td", timestamp));
		pattern = pattern.replace("${JDAY}", String.format("%1$tj", timestamp));
		pattern = pattern.replace("${HOUR}", String.format("%1$tH", timestamp));
		pattern = pattern.replace("${MINUTE}",
				String.format("%1$tM", timestamp));
		pattern = pattern.replace("${SECOND}",
				String.format("%1$tS", timestamp));

		return pattern;
	}
}
