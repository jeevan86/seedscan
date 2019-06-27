package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ALNMDeviationMetricTest {

  private ALNMDeviationMetric metric;
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

  @Before
  public void setUp() throws Exception {
    metric = new ALNMDeviationMetric();
    metric.add("lower-limit", "4");
    metric.add("upper-limit", "8");
    metric.setData(data1);
  }

  @Test
  public final void testGetVersion() throws Exception {
    assertEquals("Metric Version: ", 1, metric.getVersion());
  }

  @Test
  public final void testProcess() throws Exception {

    metric.add("channel-restriction", "LN,HN");

		/* The String key matches the MetricResult ids */
    MetricTestMap expect = new MetricTestMap();
    double error = 1E-5;
    expect.put("20,LN1", 13.87991, error); // was 11.75517 before util repo changes
    expect.put("20,LN2", 14.50298, error); // was 12.90859 before util repo changes
    expect.put("20,LNZ", 15.23679, error); // was 12.92061 before changes

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testGetBaseName() {
    assertEquals("Base name: ", "ALNMDeviationMetric", metric.getBaseName());
  }

  @Test
  public final void testGetName() {
    assertEquals("Metric name: ", "ALNMDeviationMetric:4-8", metric.getName());
  }

  @Test
  public final void testALNMDeviationMetric() throws Exception {
    metric = new ALNMDeviationMetric();
    metric.add("lower-limit", "18");
    metric.add("upper-limit", "22");
    /* This has to come after adding the limits */
    metric.setData(data1);
  }

  @Test
  public final void testGetALNM() throws Exception {
    assertTrue(ALNMDeviationMetric.getALNM().isValid());
  }

  @Test
  public final void testGetAHNM() throws Exception {
    assertTrue(ALNMDeviationMetric.getAHNM().isValid());
  }

}
