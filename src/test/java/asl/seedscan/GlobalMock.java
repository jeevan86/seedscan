package asl.seedscan;

/**
 * Allows custom setup of Global state.
 *
 * @author James Holland - usgs
 */
public class GlobalMock extends Global{

  public static void setQualityFlags(String flag){
    qualityflags = flag;
  }

  public static void setDataDir(String directory){
    dataDir = directory;
  }

}
