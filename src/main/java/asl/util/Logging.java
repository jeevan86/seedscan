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
  public static String exceptionToString(Exception e) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    e.printStackTrace(printWriter);
    return stringWriter.toString();
  }

}
