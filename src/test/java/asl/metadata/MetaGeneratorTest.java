package asl.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import asl.testutils.ResourceManager;
import java.io.File;
import java.util.List;
import org.junit.Test;

public class MetaGeneratorTest {

  @Test
  public void testGetDatalessFilesForNetwork_findsMultipleFiles() {
    File dir = new File(ResourceManager.getDirectoryPath("/metadata/station_dataless"));
    String filePattern = "${NETWORK}_${STATION}.dataless";
    String networkName = "IU";
    List<String> files = MetaGenerator.getDatalessFilesForNetwork(dir, filePattern, networkName);

    assertEquals(2, files.size());
    assertTrue(files.get(0).contains("ANMO") || files.get(1).contains("ANMO"));
    assertTrue(files.get(0).contains("ANTO") || files.get(1).contains("ANTO"));
  }

  @Test
  public void testGetDatalessFilesForNetwork_findsNone_MismatchFileName() {
    File dir = new File(ResourceManager.getDirectoryPath("/metadata/station_dataless"));
    String filePattern = "${NETWORK}.${STATION}.dataless";
    String networkName = "IU";
    List<String> files = MetaGenerator.getDatalessFilesForNetwork(dir, filePattern, networkName);

    assertEquals(0, files.size());
  }

  @Test
  public void testGetStationNameFromPath_matchesMultiplePatterns() {
    String filePattern = "${NETWORK}.${STATION}.dataless";
    String networkName = "IU";
    String path = "/data/directory/somewhere/IU.ANMB.dataless";

    String output = MetaGenerator.getStationNameFromPath(path, networkName, filePattern);
    assertEquals("ANMB", output);

    filePattern = "${STATION}_${NETWORK}.dataless";
    networkName = "CU";
    path = "/data/directory/somewhere/BOA_CU.dataless";

    output = MetaGenerator.getStationNameFromPath(path, networkName, filePattern);
    assertEquals("BOA", output);
  }

  @Test(expected = IllegalStateException.class)
  public void testGetStationNameFromPath_doesNotMatch() {
    String filePattern = "${NETWORK}_${STATION}.dataless";
    String networkName = "IU";
    String path = "/data/directory/somewhere/IU.ANMB.dataless";

    String output = MetaGenerator.getStationNameFromPath(path, networkName, filePattern);
    assertEquals("ANMB", output);

    filePattern = "${STATION}.${NETWORK}.dataless";
    networkName = "CU";
    path = "/data/directory/somewhere/BOA_CU.dataless";

    output = MetaGenerator.getStationNameFromPath(path, networkName, filePattern);
    assertEquals("BOA", output);
  }
}