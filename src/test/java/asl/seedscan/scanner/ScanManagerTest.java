package asl.seedscan.scanner;

import static org.junit.Assert.*;

import asl.metadata.MetaGenerator;
import asl.seedscan.metrics.MetricData;
import asl.testutils.ResourceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ScanManagerTest {

  private ScanManager manager;
  private static MetaGenerator metaGenerator;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    try {
     // metaGenerator = new MetaGenerator()
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Before
  public void setUp() throws Exception {
    //manager = new ScanManager()

  }

  @After
  public void tearDown() throws Exception {

  }

  @Ignore
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