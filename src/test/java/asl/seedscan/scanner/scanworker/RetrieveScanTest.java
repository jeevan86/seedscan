package asl.seedscan.scanner.scanworker;

import static org.junit.Assert.assertEquals;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.DatabaseScan;
import asl.seedscan.database.MetricDatabaseMock;
import asl.seedscan.scanner.ScanManagerMock;
import asl.testutils.Dependent;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class RetrieveScanTest {

  private static MetaGenerator metaGenerator;
  private ScanManagerMock manager;
  private MetricDatabaseMock database;
  private RetrieveScan scan;

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
    scan = new RetrieveScan(manager);
  }

  @After
  public void tearDown() throws Exception {
    manager.halt();
  }

  @Test
  public void parseScan_StationScan() throws Exception {
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        null,
        null,
        "IC", "XAN", null, null,
        LocalDate.of(2013, 1, 1), LocalDate.of(2013, 1, 30),
        1, false);

    scan.parseScan(dbScan);
    assertEquals(1, manager.getNumberTasksAdded());
    //Only 24 will produce data if processed fully.
    assertEquals(0, database.getNumberOfInsertedChildScans());
    assertEquals(0, database.getNumberErrors());
  }

  @Test
  public void parseScan_NoNetwork_NoStation() throws Exception {
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        null,
        null,
        null, null, null, null,
        LocalDate.of(2013, 1, 1), LocalDate.of(2013, 1, 30),
        1, false);

    scan.parseScan(dbScan);
    assertEquals(0, manager.getNumberTasksAdded());
    assertEquals(157, database.getNumberOfInsertedChildScans());
    assertEquals(0, database.getNumberErrors());
  }

  @Test
  public void parseScan_NoNetwork_NoStation_Over30days() throws Exception {
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        null,
        null,
        null, null, null, null,
        LocalDate.of(2010, 1, 1), LocalDate.of(2013, 1, 1),
        1, false);

    scan.parseScan(dbScan);
    assertEquals(0, manager.getNumberTasksAdded());
    //157*36=5652 30 day scans
    assertEquals(5652, database.getNumberOfInsertedChildScans());
    assertEquals(0, database.getNumberErrors());
  }

  @Test
  public void parseScan_Network_Station_Over30days() throws Exception {
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        null,
        null,
        "CU", "SDDR", null, null,
        LocalDate.of(2010, 1, 1), LocalDate.of(2013, 1, 1),
        1, false);

    scan.parseScan(dbScan);
    assertEquals(0, manager.getNumberTasksAdded());
    //36 months of scanning
    assertEquals(36, database.getNumberOfInsertedChildScans());
    assertEquals(0, database.getNumberErrors());
  }

  @Test
  public void parseScan_NoNetwork_Station() throws Exception {
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        null,
        null,
        null, "TGUH", null, null,
        LocalDate.of(2013, 1, 15), LocalDate.of(2013, 1, 20),
        1, false);

    scan.parseScan(dbScan);
    assertEquals(0, manager.getNumberTasksAdded());
    //Only 24 will produce data if processed fully.
    assertEquals(1, database.getNumberOfInsertedChildScans());
    assertEquals(0, database.getNumberErrors());
  }

  @Test
  public void parseScan_Network_NoStation() throws Exception {
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        null,
        null,
        "IC", null, null, null,
        LocalDate.of(2013, 1, 15), LocalDate.of(2013, 1, 20),
        1, false);

    scan.parseScan(dbScan);
    assertEquals(0, manager.getNumberTasksAdded());
    assertEquals(10, database.getNumberOfInsertedChildScans());
    assertEquals(0, database.getNumberErrors());
  }

  @Test
  public void parseScan_NoNetwork_Stations() throws Exception {
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        null,
        null,
        null, "QIZ,XAN,ANWB", null, null,
        LocalDate.of(2013, 1, 15), LocalDate.of(2013, 1, 20),
        1, false);

    scan.parseScan(dbScan);
    assertEquals(0, manager.getNumberTasksAdded());
    assertEquals(3, database.getNumberOfInsertedChildScans());
    assertEquals(0, database.getNumberErrors());
  }

  @Test
  public void parseScan_Networks_NoStation() throws Exception {
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        null,
        null,
        "IC,IW", null, null, null,
        LocalDate.of(2013, 1, 15), LocalDate.of(2013, 1, 20),
        1, false);

    scan.parseScan(dbScan);
    assertEquals(0, manager.getNumberTasksAdded());
    //Only 24 will produce data if processed fully.
    assertEquals(27, database.getNumberOfInsertedChildScans());
    assertEquals(0, database.getNumberErrors());
  }

  @Test
  public void parseScan_Networks_Stations() throws Exception {
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        null,
        null,
        "IC,CU", "XAN,TGUH", null, null,
        LocalDate.of(2013, 1, 15), LocalDate.of(2013, 1, 20),
        1, false);

    scan.parseScan(dbScan);
    assertEquals(0, manager.getNumberTasksAdded());
    assertEquals(2, database.getNumberOfInsertedChildScans());
    assertEquals(0, database.getNumberErrors());
  }

  /**
   * After a split, a new RetrieveScan should be added to the task queue.
   *
   * @throws Exception shouldn't throw anything
   */
  @Test
  public void run_AddRetrieveScanToQueueAfterSplit() throws Exception {
    DatabaseScan dbScan = new DatabaseScan(
        new UUID(100, 100),
        null,
        null,
        null, null, null, null,
        LocalDate.of(2013, 1, 15), LocalDate.of(2013, 1, 20),
        1, false);

    database.offerNewScan(dbScan);
    scan.run();
    assertEquals(1, manager.getNumberTasksAdded());
    assertEquals(0, database.getNumberErrors());
  }

  @Test
  public void run_NullScanFromDatabase_DoNotAddRetrieveScanToQueue() throws Exception {
    //Database has no addedTasks, so it returns null
    scan.run();
    assertEquals(0, manager.getNumberTasksAdded());
    assertEquals(0, database.getNumberOfInsertedChildScans());
    assertEquals(0, database.getNumberErrors());
  }

  @Test
  public void getBasePriority() throws Exception {
    //This task should always run after every other type.
    assertEquals(Integer.MAX_VALUE, scan.getBasePriority().intValue());
  }

  @Test
  public void getFinePriority() throws Exception {
    //This task should always run after every other type.
    assertEquals(Long.MAX_VALUE, scan.getFinePriority().longValue());
  }

}