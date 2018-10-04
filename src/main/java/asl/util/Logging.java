package asl.util;

import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * Basic Logging methods for pretty logging.
 */
public class Logging {

  /**
   * Create a pretty string version of an exception's stack trace.
   *
   * @param e exception to prettify
   * @return string copy of stacktrace
   */
  public static String exceptionToString(Throwable e) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    e.printStackTrace(printWriter);
    return stringWriter.toString();
  }

  /**
   * Create a pretty string with exception message and stack trace
   *
   * @param e exception to prettify
   * @return string with message and stacktrace
   */
  public static String prettyException(Throwable e) {
    return e.getLocalizedMessage() + "\n" + exceptionToString(e);
  }

  /**
   * Recursively stringify an exception and all parent causes of the exception
   *
   * @param e Exception to log
   */
  public static String prettyExceptionWithCause(Throwable e) {
    if (e == null) {
      return "";
    }
    Throwable cause = e.getCause();
    return prettyException(e) + "\n" + prettyExceptionWithCause(cause);
  }

}
