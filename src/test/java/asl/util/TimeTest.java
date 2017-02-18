package asl.util;

import static asl.util.Time.btimeToLocalDateTime;
import static asl.util.Time.calculateEpochMicroSeconds;
import static asl.util.Time.calculateEpochMilliSeconds;
import static asl.util.Time.decimillisecondsToNanoSeconds;
import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import org.junit.Test;

public class TimeTest {

  @Test
  public void testCalculateEpochMicroSeconds() throws Exception {
    //2010-02-17 05:58:28 + 101001 nanoseconds (101.001 microseconds
    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(1266386308l, 101001, ZoneOffset.UTC);
    Long expectedMicroSeconds = 1000000 * 1266386308l + 101001 / 1000;

    Long resultMicroSeconds = calculateEpochMicroSeconds(dateTime);
    assertEquals(expectedMicroSeconds, resultMicroSeconds);
  }

  @Test
  public void testCalculateEpochMilliSeconds() throws Exception {
    //2020-07-25 13:23:12 + 123 milliseconds
    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(1595704992l, 123000000, ZoneOffset.UTC);
    Long expectedMilliSeconds = 1595704992123l;

    Long resultMilliSeconds = calculateEpochMilliSeconds(dateTime);
    assertEquals(expectedMilliSeconds, resultMilliSeconds);

  }

  @Test
  public void testDecimillisecondsToNanoSeconds() throws Exception {
    Integer expected = 999900000;
    Integer result = decimillisecondsToNanoSeconds(9999);
    assertEquals(expected, result);

    expected = 0;
    result = decimillisecondsToNanoSeconds(0);
    assertEquals(expected, result);

    expected = 124500000;
    result = decimillisecondsToNanoSeconds(1245);
    assertEquals(expected, result);
  }

  @Test
  public void testBtimeToLocalDateTime_Leap_Second() throws Exception {
    LocalDateTime expected = LocalDateTime.of(2012, Month.JUNE, 30, 23, 59, 59, 999999999);
    LocalDateTime dateTime = btimeToLocalDateTime(2012, 182, 23, 59, 60, 0);
    assertEquals(expected, dateTime);

    expected = LocalDateTime.of(2012, Month.JUNE, 30, 23, 59, 59, 999999999);
    dateTime = btimeToLocalDateTime(2012, 182, 23, 59, 61, 5000);
    assertEquals(expected, dateTime);
  }

  @Test
  public void testBtimeToLocalDateTime_Standard() throws Exception {
    LocalDateTime expected = LocalDateTime.of(2001, Month.JULY, 1, 8, 30, 35, 123400000);
    LocalDateTime dateTime = btimeToLocalDateTime(2001, 182, 8, 30, 35, 1234);
    assertEquals(expected, dateTime);

    expected = LocalDateTime.of(2025, Month.DECEMBER, 21, 11, 10, 01, 652100000);
    dateTime = btimeToLocalDateTime(2025, 355, 11, 10, 01, 6521);
    assertEquals(expected, dateTime);
  }

  @Test
  public void testBtimeToLocalDateTime_Leap_Year() throws Exception {
    LocalDateTime expected = LocalDateTime.of(2000, Month.JULY, 1, 8, 12, 24, 100000000);
    LocalDateTime dateTime = btimeToLocalDateTime(2000, 183, 8, 12, 24, 1000);
    assertEquals(expected, dateTime);

    expected = LocalDateTime.of(2020, Month.DECEMBER, 21, 11, 10, 01, 652100000);
    dateTime = btimeToLocalDateTime(2020, 356, 11, 10, 01, 6521);
    assertEquals(expected, dateTime);
  }

}