package asl.seedscan.scanner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import asl.metadata.MetaGenerator;
import asl.metadata.Station;
import asl.seedscan.database.MetricDatabaseMock;
import asl.seedscan.metrics.MetricData;
import asl.testutils.Dependent;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class DataLoaderTest {

  private static MetaGenerator metaGenerator;
  private static ScanManager manager;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    Dependent.assumeGlobalState();
    Dependent.assumeRDSeed();
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

  @Test
  public void getMetricData_NullMetaData() throws Exception {
    MetricData data = DataLoader
        .getMetricData(LocalDate.of(2016, 6, 30), new Station("IU", "ANMO"), manager);

    assertNull(data);

  }

  @Test
  public void getMetricData_MetaData_NoData() throws Exception {
    MetricData data = DataLoader
        .getMetricData(LocalDate.of(2016, 1, 30), new Station("IC", "BJT"), manager);
    assertNull(data);
  }

  @Test
  public void getMetricData_MetaData_Data() throws Exception {
    MetricData data = DataLoader
        .getMetricData(LocalDate.of(2016, 6, 30), new Station("IC", "BJT"), manager);
    assertNotNull(data);
  }

}