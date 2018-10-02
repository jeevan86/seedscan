package asl.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import asl.seedscan.metrics.MetricException;
import org.junit.Test;

public class LoggingTest {

  @Test
  public void testExceptionToString() {
    NullPointerException exception = new NullPointerException("Exception 1");
    String message = Logging.exceptionToString(exception);
    // Verify that the stacktrace was included by checking for "at asl.util..."
    assertTrue(message.startsWith("java.lang.NullPointerException: Exception 1\n"
        + "\tat asl.util.LoggingTest.testExceptionToString(LoggingTest.java:"));
  }

  @Test
  public void testPrettyException() {
    NullPointerException exception = new NullPointerException("Exception 1");
    String message = Logging.prettyException(exception);
    assertTrue(message.startsWith("Exception 1\n"
        + "java.lang.NullPointerException: Exception 1"));
  }

  @Test
  public void testPrettyExceptionWithCause_threeParentCauses() {
    NullPointerException exception = new NullPointerException("Exception 1");
    MetricException exception2 = new MetricException("Exception 2", exception);
    MetricException exception3 = new MetricException("Exception 3", exception2);
    String message = Logging.prettyExceptionWithCause(exception3);
    assertTrue(message.startsWith("Exception 3\n"
        + "asl.seedscan.metrics.MetricException: Exception 3"));
    assertTrue(message.contains("Exception 2\n"
        + "asl.seedscan.metrics.MetricException: Exception 2"));
    assertTrue(message.contains("Exception 1\n"
        + "java.lang.NullPointerException: Exception 1"));
    assertTrue(message.endsWith("\n"));
  }

  @Test
  public void testPrettyExceptionWithCause_oneParentCauses() {
    NullPointerException exception = new NullPointerException("Exception 1");
    MetricException exception2 = new MetricException("Exception 2", exception);
    String message = Logging.prettyExceptionWithCause(exception2);
    assertTrue(message.startsWith("Exception 2\n"
        + "asl.seedscan.metrics.MetricException: Exception 2"));
    assertTrue(message.contains("Exception 1\n"
        + "java.lang.NullPointerException: Exception 1"));
    assertTrue(message.endsWith("\n"));
  }

  @Test
  public void testPrettyExceptionWithCause_noParentCauses() {
    NullPointerException exception = new NullPointerException("Exception 1");
    String message = Logging.prettyExceptionWithCause(exception);
    assertTrue(message.startsWith("Exception 1\n"
        + "java.lang.NullPointerException: Exception 1"));
    assertTrue(message.endsWith("\n"));
  }

  @Test
  public void testPrettyExceptionWithCause_nullException() {
    String message = Logging.prettyExceptionWithCause(null);
    assertEquals("", message);
  }
}