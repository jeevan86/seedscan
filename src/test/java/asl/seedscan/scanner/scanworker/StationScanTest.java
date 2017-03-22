package asl.seedscan.scanner.scanworker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import asl.metadata.MetaGenerator;
import asl.metadata.Station;
import asl.seedscan.database.DatabaseScan;
import asl.seedscan.database.MetricDatabaseMock;
import asl.seedscan.scanner.ScanManagerMock;
import asl.testutils.Dependent;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Queue;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class StationScanTest {

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
  public void run_AreAllMetricsAttempted() throws Exception {
    /**
     * This test needs to be written when splitting the metrics into their own tasks.
     */

  }

  @Test(timeout = 20000)
  public void run_AreSpawnedScansCorrectlyConstructed() throws Exception {
    //Null metadata
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        new UUID(10, 10),
        null,
        "IU", "KIP", null, null,
        LocalDate.of(2010, 11, 15), LocalDate.of(2010, 11, 16),
        1, false);
    StationScan scan = new StationScan(manager, dbScan);

    //Should do basically nothing and add only one more scan to manager queue
    scan.run();

    assertEquals("Number of station Scans added: ", 1, manager.getNumberTasksAdded());

    Queue<ScanWorker> workQueue = manager.getWorkQueue();
    StationScan nextScan = (StationScan) workQueue.peek();

    //Should be same object
    assertEquals(dbScan, nextScan.databaseScan);

    assertEquals(LocalDate.of(2010, 11, 16), nextScan.currentDate);
    assertEquals(new Station("IU", "KIP"), nextScan.station);

  }

  @Test(timeout = 20000)
  public void run_DidTheNextDayGetAdded_FirstDay() throws Exception {
    //Null metadata
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        new UUID(10, 10),
        null,
        "IU", "KIP", null, null,
        LocalDate.of(2012, 1, 15), LocalDate.of(2012, 1, 16),
        1, false);
    StationScan scan = new StationScan(manager, dbScan);

    //Should do basically nothing and add only one more scan to manager queue
    scan.run();

    assertEquals("Number of station Scans added: ", 1, manager.getNumberTasksAdded());
  }


  @Test(timeout = 20000)
  public void run_DidTheNextDayGetAdded_LaterDays() throws Exception {
    //Null metadata
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        new UUID(10, 10),
        null,
        "IU", "KIP", null, null,
        LocalDate.of(2012, 1, 15), LocalDate.of(2012, 1, 20),
        1, false);
    StationScan scan = new StationScan(manager, dbScan, LocalDate.of(2012, 1, 17), null);

    //Should do basically nothing and add only one more scan to manager queue
    scan.run();

    assertEquals("Number of station Scans added: ", 1, manager.getNumberTasksAdded());
  }

  @Test(timeout = 20000)
  public void run_DoesItStopWhenDaysFinish() throws Exception {
//Null metadata
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        new UUID(10, 10),
        null,
        "IU", "KIP", null, null,
        LocalDate.of(2012, 1, 15), LocalDate.of(2012, 1, 20),
        1, false);
    StationScan scan = new StationScan(manager, dbScan, LocalDate.of(2012, 1, 20), null);

    //Should do basically nothing
    scan.run();

    assertEquals("Number of station Scans added: ", 0, manager.getNumberTasksAdded());
  }

  @Ignore
  @Test
  public void run_NoMetadataForDay_NoMetricsShouldBeRun() throws Exception {

    //Add checks after metrics split to own tasks.

  }

  @Ignore
  @Test
  public void run_NoDataForDay_AllMetricsShouldTryToRun() throws Exception {

  }

  @Ignore
  @Test
  public void run_NoEventsOnDay_EventsNotSetInMetric() throws Exception {

  }

  @Ignore
  @Test
  public void run_EventsOnDay_EventsSetInMetric() throws Exception {

  }

  @Ignore
  @Test
  public void run_CrossPowerTransferredToNextScan() throws Exception {

  }
  @Test
  public void getBasePriority() throws Exception {
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        new UUID(10, 10),
        null,
        "IU", "KIP", null, null,
        LocalDate.of(2008, 1, 15), LocalDate.of(2012, 1, 20),
        1, false);
    StationScan scan = new StationScan(manager, dbScan, LocalDate.of(2008, 1, 18), null);

    assertEquals(45L, scan.getBasePriority().longValue());
  }

  @Test
  public void getFinePriority_VerifyComparable() throws Exception {
    DatabaseScan dbScanA = new DatabaseScan(
        new UUID(100, 100),
        new UUID(10, 10),
        null,
        "IU", "KIP", null, null,
        LocalDate.of(2008, 1, 15), LocalDate.of(2012, 1, 20),
        1, false);
    StationScan scanA = new StationScan(manager, dbScanA, LocalDate.of(2008, 1, 18), null);

    DatabaseScan dbScanB = new DatabaseScan(
        new UUID(100, 100),
        new UUID(10, 10),
        null,
        "IU", "KIP", null, null,
        LocalDate.of(2015, 1, 15), LocalDate.of(2012, 1, 20),
        1, false);
    StationScan scanB = new StationScan(manager, dbScanB, LocalDate.of(2012, 1, 18), null);

    assertTrue(scanA.getFinePriority() < scanB.getFinePriority());

    DatabaseScan dbScanC = new DatabaseScan(
        new UUID(100, 100),
        new UUID(10, 10),
        null,
        "IU", "KIP", null, null,
        LocalDate.of(2015, 1, 15), LocalDate.of(2012, 1, 20),
        1, false);
    StationScan scanC = new StationScan(manager, dbScanC, LocalDate.of(2012, 1, 19), null);

    assertTrue(scanB.getFinePriority() < scanC.getFinePriority());
  }
}