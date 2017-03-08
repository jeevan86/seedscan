package asl.seedscan.scanner;

import static org.junit.Assert.assertTrue;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.MetricDatabaseMock;
import asl.seedscan.scanner.scanworker.ScanWorker;
import asl.testutils.Dependent;
import asl.testutils.ResourceManager;
import asl.testutils.ThreadUtils.MutableFlag;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    metaGenerator = null;
  }

  @Before
  public void setUp() throws Exception {
    database = new MetricDatabaseMock();
    manager = new ScanManager(database, metaGenerator);
  }

  @After
  public void tearDown() throws Exception {
    manager.halt();
  }

  @Test(expected = IllegalStateException.class, timeout = 20000)
  public void scan_ExceptionOnMultipleScanCalls_DatabaseErrorInserted() throws Exception {
    Runnable first = () -> manager.scan();
    try {
      new Thread(first).start();
      Thread.sleep(500);
      manager.scan();
    } finally {
      //Should have also inserted an error into the db.
      assertTrue(database.getNumberErrors() > 0);
    }

  }

  @Test(timeout = 20000)
  public void scan_doesEmptyQueueQueryTheDatabase_doesScanPeriodicallyCheckForNewScans()
      throws Exception {
    Runnable scanner = () -> manager.scan();
    manager.setQueryTime(500);
    new Thread(scanner).start();
    //Sleep first to allow default starts to clear up.
    Thread.sleep(500);
    int count = database.getNumberOfScanRequests();
    Thread.sleep(500);

    assertTrue(count < database.getNumberOfScanRequests());
  }

  /**
   * @throws Exception if a timeout occurs, meaning the task did not run.
   */
  @Test(timeout = 20000)
  public void addTask_doesTaskExecuteAfterBeingAdded_singleTask() throws Exception {
    MutableFlag flag = new MutableFlag();

    ScanWorker worker = new ScanWorker(manager) {
      @Override
      protected Integer getBasePriority() {
        return 1;
      }

      @Override
      protected Long getFinePriority() {
        return 1L;
      }

      @Override
      public void run() {
        flag.set(true);
      }
    };

    manager.addTask(worker);
    while (!flag.get()) {
      Thread.sleep(5);
    }
  }

  @Test(timeout = 20000)
  public void addTask_doesTaskExecuteAfterBeingAdded_ManyTasks() throws Exception {
    Queue<MutableFlag> flags = new LinkedBlockingQueue<>();

    for (int i = 0; i < 100; i++) {
      MutableFlag flag = new MutableFlag();
      flags.offer(flag);
      ScanWorker worker = new ScanWorker(manager) {
        @Override
        protected Integer getBasePriority() {
          return 1;
        }

        @Override
        protected Long getFinePriority() {
          return 1L;
        }

        @Override
        public void run() {
          flag.set(true);
        }
      };

      manager.addTask(worker);
    }

    while (!flags.isEmpty()) {
      MutableFlag flag = flags.poll();
      while (!flag.get()) {
        Thread.sleep(5);
      }
    }
  }

}