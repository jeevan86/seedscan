package asl.testutils;

/**
 * A location for various tools that are useful for testing threaded code.
 *
 * @author James Holland - USGS
 */
public class ThreadUtils {

  public static class MutableFlag {

    private Boolean flag = false;

    public synchronized void set(Boolean newValue) {
      flag = newValue;
    }

    public synchronized Boolean get() {
      return flag;
    }
  }

}
