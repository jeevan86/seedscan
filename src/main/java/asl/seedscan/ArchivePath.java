package asl.seedscan;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import asl.metadata.Station;

/**
 * The Class ArchivePath. This class generates file paths for seed files from a
 * pattern set in the config.xml file.
 */
public class ArchivePath {

	/** The timestamp. */
	private LocalDateTime timestamp = null;

	/** The station. */
	private Station station = null;

	/**
	 * Instantiates a new archive path based on a Station
	 *
	 * @param station
	 *            the station
	 */
	public ArchivePath(Station station) {
		this.station = station;
	}

	/**
	 * Instantiates a new archive path based on a GregorianCalendar and Station
	 *
	 * @param timestamp
	 *            the timestamp
	 * @param station
	 *            the station
	 */
	public ArchivePath(LocalDateTime timestamp, Station station) {
		this.timestamp = timestamp;
		this.station = station;
	}

	/**
	 * Sets the timestamp.
	 *
	 * @param timestamp
	 *            the new timestamp
	 */
	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Sets the station.
	 *
	 * @param station
	 *            the new station
	 */
	public void setStation(Station station) {
		this.station = station;
	}

	/**
	 * Make path from non null components. Null members are ignored.
	 *
	 * @param pattern
	 *            the pattern
	 * @return the pattern after appropriate replacements
	 */
	public String makePath(String pattern) {
		if (station != null) {
			if (station.getNetwork() != null) {
				pattern = pattern.replace("${NETWORK}", station.getNetwork());
			}
			pattern = pattern.replace("${STATION}", station.getStation());
		}

		if (timestamp != null) {
			pattern = pattern.replace("${YEAR}", timestamp.format(DateTimeFormatter.ofPattern("yyyy")));
			pattern = pattern.replace("${MONTH}", timestamp.format(DateTimeFormatter.ofPattern("MM")));
			pattern = pattern.replace("${DAY}", timestamp.format(DateTimeFormatter.ofPattern("dd")));
			pattern = pattern.replace("${JDAY}", timestamp.format(DateTimeFormatter.ofPattern("DDD")));
			pattern = pattern.replace("${HOUR}", timestamp.format(DateTimeFormatter.ofPattern("HH")));
			pattern = pattern.replace("${MINUTE}", timestamp.format(DateTimeFormatter.ofPattern("mm")));
			pattern = pattern.replace("${SECOND}", timestamp.format(DateTimeFormatter.ofPattern("ss")));
		}

		return pattern;
	}
}
