package asl.seedscan.scanner.scanworker;

import static org.junit.Assert.assertEquals;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.MetricDatabaseMock;
import asl.seedscan.scanner.ScanManagerMock;
import asl.testutils.Dependent;
import asl.testutils.ResourceManager;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


public class RetrieveScanTest {

  private static MetaGenerator metaGenerator;
  private ScanManagerMock manager;
  private MetricDatabaseMock database;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    Dependent.assumeRDSeed();
    Dependent.assumeGlobalState();
    try {
      metaGenerator = new MetaGenerator(
          ResourceManager.getDirectoryPath("/dataless"),
          new ArrayList<>());

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    metaGenerator = null;
  }

  @Before
  public void setUp() throws Exception {
    database = new MetricDatabaseMock();
    manager = new ScanManagerMock(database, metaGenerator);
  }

  @After
  public void tearDown() throws Exception {
    manager.halt();
  }

  @Ignore
  @Test
  public void run_StationScan() throws Exception {

  }

  @Ignore
  @Test
  public void run_SplitScan_NoNetwork() throws Exception {

  }

  @Ignore
  @Test
  public void run_SplitScan_NoNetwork_NoStation() throws Exception {

  }

  @Ignore
  @Test
  public void run_SplitScan_NoNetwork_NoStation_Over30days() throws Exception {

  }

  @Ignore
  @Test
  public void run_SplitScan_Network_Station_Over30days() throws Exception {

  }

  @Ignore
  @Test
  public void run_SplitScan_NoNetwork_Station() throws Exception {

  }

  @Ignore
  @Test
  public void run_SplitScan_Network_NoStation() throws Exception {

  }

  @Ignore
  @Test
  public void run_SplitScan_AddRetrieveScanToQueueAfterSplit() throws Exception {

  }

  @Ignore
  @Test
  public void run_SplitScan_StationsSeparatedByCommas() throws Exception {

  }

  @Ignore
  @Test
  public void run_SplitScan_NetworksSeparatedByCommas() throws Exception {

  }

  @Ignore
  @Test
  public void run_NullScanFromDatabase_DoNotAddRetrieveScanToQueue() throws Exception {

  }

  @Test
  public void getBasePriority() throws Exception {
    //This task should always run after every other type.
    assertEquals(Integer.MAX_VALUE, new RetrieveScan(manager).getBasePriority().intValue());
  }

  @Test
  public void getFinePriority() throws Exception {
    //This task should always run after every other type.
    assertEquals(Long.MAX_VALUE, new RetrieveScan(manager).getFinePriority().longValue());
  }

}