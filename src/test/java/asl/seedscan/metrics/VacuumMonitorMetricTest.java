package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import asl.testutils.ResourceManager;
import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class VacuumMonitorMetricTest {

  private VacuumMonitorMetric metric;
  private static MetricData data1;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    try {
      data1 = (MetricData) ResourceManager
          .loadCompressedObject("/java_serials/data/IU.ADK.2015.40.MetricData.ser.gz", false);
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
    metric = new VacuumMonitorMetric();
    metric.setData(data1);
  }

  @Test
  public final void testGetVersion() throws Exception {
    assertEquals("Metric Version: ", 1, metric.getVersion());
  }

  @Test
  public final void testProcessDefault() throws Exception {
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,VY1", 39.2043354373);
    expect.put("00,VY2", 39.4955981431);
    expect.put("00,VYZ", 45.5289258911);

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testProcessChannelRestricted() throws Exception {
    /* Using the standard VM metric because no other mass position channels currently exist.*/
    metric.add("channel-restriction", "VY");

    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,VY1", 39.2043354373);
    expect.put("00,VY2", 39.4955981431);
    expect.put("00,VYZ", 45.5289258911);

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testGetName() throws Exception {
    assertEquals("VacuumMonitorMetric", metric.getName());
  }
}
