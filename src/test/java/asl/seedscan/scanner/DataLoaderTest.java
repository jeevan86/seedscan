package asl.seedscan.scanner;

import asl.metadata.MetaGenerator;
import asl.metadata.Station;
import asl.seedscan.database.MetricDatabaseMock;
import asl.seedscan.metrics.MetricData;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class DataLoaderTest {

  private static MetaGenerator metaGenerator;
  private static ScanManager manager;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
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
    manager = null;
  }

  @Before
  public void setUp() throws Exception {

  }

  @After
  public void tearDown() throws Exception {

  }

  @Ignore
  @Test
  public void getMetricData_NullMetaData() throws Exception {
    MetricData data = DataLoader.getMetricData(LocalDate.of(2016,6,30), new Station("IC", "BJT"), manager);

        System.out.println(data.hashCode());
  }

}