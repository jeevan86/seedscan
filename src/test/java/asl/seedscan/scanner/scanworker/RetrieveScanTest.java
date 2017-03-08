package asl.seedscan.scanner.scanworker;

import static org.junit.Assert.*;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.MetricDatabaseMock;
import asl.seedscan.scanner.ScanManager;
import asl.testutils.Dependent;
import asl.testutils.ResourceManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


public class RetrieveScanTest {

  private static MetaGenerator metaGenerator;
  private MetricDatabaseMock database;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    Dependent.assumeRDSeed();

    try {
      metaGenerator = new MetaGenerator(
          ResourceManager.getDirectoryPath("/dataless"),
          null);
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

  }

  @After
  public void tearDown() throws Exception {

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