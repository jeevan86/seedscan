package asl.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

	/**
	 * This method converts from the seed Convention that uses 0.0001 seconds (decimilliseconds)
	 * to nanoseconds for use with more standard applications.
	 * @param dmSeconds 0.0001 seconds
	 * @return the nanosecond conversion
	 */
	public static int decimillisecondsToNanoSeconds(int dmSeconds){
		return dmSeconds * 100000;
	}

	/**
	 * Creates a LocalDateTime object from the values in a BTIME field in the SEED format.
	 * See Chapter 3 in Seed Manual
	 * @param year Year (e.g., 1987)
	 * @param dayOfYear Day of Year (Jan 1 is 1)
	 * @param hour Hours of day (0-23)
	 * @param minute Minutes of day (0-59)
	 * @param second Seconds of day (0-59, 60 for leap seconds)
	 * @param dmSecond 0.0001 seconds (0-9999)
	 * @return the LocalDateTime version of provided data
	 */
	public static LocalDateTime btimeToLocalDateTime(int year, int dayOfYear, int hour, int minute, int second, int dmSecond){
		LocalDate date = LocalDate.ofYearDay(year, dayOfYear);
		LocalTime time = LocalTime.of(hour, minute, second, Time.decimillisecondsToNanoSeconds(dmSecond));

		return LocalDateTime.of(date, time);

	}
}
