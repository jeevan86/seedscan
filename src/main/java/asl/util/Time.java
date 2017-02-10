package asl.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * This class contains basic utility functions when using Java 8 Time classes.
 * 
 * @author jholland - USGS
 *
 */
public class Time {

	/**
	 * Converts LocalDateTime to epoch microseconds, assuming it is UTC
	 * 
	 * @param timestamp date time for conversion, Assumed UTC
	 * @return microseconds since 1970-01-01:00:00:00.0
	 */
	public static long calculateEpochMicroSeconds(LocalDateTime timestamp) {
		Instant startInstant = timestamp.atZone(ZoneId.ofOffset("", ZoneOffset.UTC)).toInstant();
		// Convert to microseconds
		return startInstant.getEpochSecond() * 1000000 + startInstant.getNano() / 1000;
	}

	/**
	 * Converts LocalDateTime to epoch milliseconds, assuming it is UTC
	 *
	 * @param timestamp date time for conversion, Assumed UTC
	 * @return milliseconds since 1970-01-01:00:00:00.0
	 */
	public static long calculateEpochMilliSeconds(LocalDateTime timestamp) {
		Instant startInstant = timestamp.atZone(ZoneId.ofOffset("", ZoneOffset.UTC)).toInstant();
		return startInstant.toEpochMilli();
	}
}
