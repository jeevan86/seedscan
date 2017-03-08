package asl.seedscan.scanner;

import static org.junit.Assert.assertTrue;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.MetricDatabaseMock;
import asl.testutils.Dependent;
import asl.testutils.ResourceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ScanManagerTest {

  private static MetaGenerator metaGenerator;
  private MetricDatabaseMock database;
  private ScanManager manager;

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

  @Before
  public void setUp() throws Exception {
    database = new MetricDatabaseMock();
    manager = new ScanManager(database, metaGenerator);
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test(expected = IllegalStateException.class)
  public void scan_ExceptionOnMultipleScanCalls_DatabaseErrorInserted() throws Exception {
    Runnable first = () -> manager.scan();
    try {
      new Thread(first).start();
      Thread.sleep(500);
      manager.scan();
    } finally {
      manager.halt();
      //Should have also inserted an error into the db.
      assertTrue(database.getNumberErrors() > 0);
    }

  }

  @Test
  public void scan_doesEmptyQueueQueryTheDatabase() throws Exception {

  }

  @Ignore
  @Test
  public void scan_doesScanPeriodicallyCheckForNewScans() throws Exception {

  }

  @Ignore
  @Test
  public void addTask_doesTaskExecuteAfterBeingAdded_singleTask() throws Exception {

  }

  @Ignore
  @Test
  public void addTask_doesTaskExecuteAfterBeingAdded_ManyTasks() throws Exception {

  }

}