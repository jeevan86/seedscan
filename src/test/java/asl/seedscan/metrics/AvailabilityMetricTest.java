package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import asl.testutils.ResourceManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AvailabilityMetricTest {

  private AvailabilityMetric metric;
  private static MetricData data;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    data = (MetricData) ResourceManager
        .loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    data = null;
  }

  @Test
  public final void testGetName() throws Exception {
    Metric metric = new AvailabilityMetric();
    assertEquals("AvailabilityMetric", metric.getName());
  }

  @Test
  public final void testGetVersion() throws Exception {
    Metric metric = new AvailabilityMetric();
    assertEquals(1, metric.getVersion());
  }

  @Test
  public final void testProcess_HasData_NoDB() throws Exception {
    throw new RuntimeException("not yet implemented");
  }

  @Test
  public final void testProcess_HasData_HasDB() throws Exception {
    throw new RuntimeException("not yet implemented");
  }

  @Test
  public final void testProcess_NoData_NoDB() throws Exception {
    throw new RuntimeException("not yet implemented");
  }

  @Test
  public final void testProcess_NoData_HasDB() throws Exception {
    throw new RuntimeException("not yet implemented");
  }
}
