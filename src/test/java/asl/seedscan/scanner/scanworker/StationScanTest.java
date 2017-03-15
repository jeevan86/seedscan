package asl.seedscan.scanner.scanworker;

import static org.junit.Assert.*;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.MetricDatabaseMock;
import asl.seedscan.scanner.ScanManager;
import asl.testutils.Dependent;
import asl.testutils.ResourceManager;
import java.util.ArrayList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class StationScanTest {

  private static MetaGenerator metaGenerator;
  private static ScanManager manager;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    Dependent.assumeRDSeed();
    Dependent.assumeGlobalState();
    try {
      metaGenerator = new MetaGenerator(
          ResourceManager.getDirectoryPath("/dataless"),
          new ArrayList<>());
      manager = new ScanManager(new MetricDatabaseMock(), metaGenerator);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    metaGenerator = null;
    manager.halt();
    manager = null;
  }

  @Ignore
  @Test
  public void run_AreAllMetricsAttempted() throws Exception {

  }

  @Ignore
  @Test
  public void run_DidTheNextDayGetAdded() throws Exception {

  }

  @Ignore
  @Test
  public void run_DoesItStopWhenDaysFinish() throws Exception {

  }

  @Ignore
  @Test
  public void run_NoMetadataForDay() throws Exception {

  }

  @Ignore
  @Test
  public void run_NoDataForDay() throws Exception {

  }

  @Ignore
  @Test
  public void run_NoEventsOnDay() throws Exception {

  }

  @Ignore
  @Test
  public void run_EventsOnDay() throws Exception {

  }

  @Ignore
  @Test
  public void getBasePriority() throws Exception {

  }

  @Ignore
  @Test
  public void getFinePriority() throws Exception {

  }

  @Ignore
  @Test
  public void compareTo() throws Exception {

  }

}