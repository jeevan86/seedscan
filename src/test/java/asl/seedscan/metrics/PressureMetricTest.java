package asl.seedscan.metrics;

import asl.metadata.Station;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class PressureMetricTest {

  private PressureMetric metric;
  private static MetricData data;
  private static Station station;
  private static LocalDate dataDate;


  @BeforeClass
  public static void setUpBeforeClass() {
    try {
      String metadataLocation = "/metadata/rdseed/IU-ANMO-ascii.txt";
      String seedDataLocation = "/seed_data/IU_ANMO/2018/121";
      station = new Station("IU", "ANMO");
      dataDate = LocalDate.of(2018, 5, 01);
      data = ResourceManager.getMetricData(seedDataLocation, metadataLocation, dataDate, station);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() {
    data = null;
    station = null;
    dataDate = null;
  }

  @Test
  public void testMetricResultNear1() {
    metric = new PressureMetric();
    metric.setData(data);
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("30,LDO", 0.9934842767063014);
    TestUtils.testMetric(metric, expect);
  }
}
