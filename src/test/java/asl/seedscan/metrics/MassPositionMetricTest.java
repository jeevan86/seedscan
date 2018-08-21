package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import asl.testutils.ResourceManager;
import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MassPositionMetricTest {

  private static MetricData data1;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    try {
      data1 = (MetricData) ResourceManager
          .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    data1 = null;
  }

  @Test
  public final void testGetVersion() throws Exception {
    MassPositionMetric metric = new MassPositionMetric();
    assertEquals("Metric Version: ", 1, metric.getVersion());
  }

  @Test
  public final void testProcessDefault() throws Exception {
    MassPositionMetric metric = new MassPositionMetric();
    metric.setData(data1);
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,VM1", 0.8839489469085381);
    expect.put("00,VM2", 0.06631853547951848);
    expect.put("00,VMZ", 8.000000000000338);
    expect.put("10,VMV", 57.142857142857146);
    expect.put("10,VMU", 57.142857142857146);
    expect.put("10,VMW", 57.142857142857146);

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testProcessChannelRestricted() throws Exception {
    MassPositionMetric metric = new MassPositionMetric();
    metric.setData(data1);

		/* Using the standard VM metric because no other mass position channels currently exist.*/
    metric.add("channel-restriction", "VM");

    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,VM1", 0.8839489469085381);
    expect.put("00,VM2", 0.06631853547951848);
    expect.put("00,VMZ", 8.000000000000338);
    expect.put("10,VMV", 57.142857142857146);
    expect.put("10,VMU", 57.142857142857146);
    expect.put("10,VMW", 57.142857142857146);

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testGetName() throws Exception {
    MassPositionMetric metric = new MassPositionMetric();
    assertEquals("MassPositionMetric", metric.getName());
  }
}
