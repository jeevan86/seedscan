package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import asl.testutils.ResourceManager;
import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class StationDeviationMetricTest {

  private StationDeviationMetric metric;
  private static MetricData data1;
  private static MetricData data2;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    data1 = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
    data2 = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/GS.OK029.2015.360.MetricData.ser.gz", false);
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    data1 = null;
    data2 = null;
  }

  @Before
  public void setUp() throws Exception {
    metric = new StationDeviationMetric();
    metric.add("lower-limit", "90");
    metric.add("upper-limit", "110");
    metric.add("modelpath", ResourceManager.getDirectoryPath("/station_noise_models"));
  }

  @Test
  public final void testGetVersion() throws Exception {
    assertEquals("Version #: ", (long) 2, metric.getVersion());
  }

  @Test
  public final void testProcessANMO() throws Exception {
    metric.setData(data1);

    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,LH1", -6.259435685534593);
    expect.put("00,LH2", -3.6049945557454826);
    expect.put("00,LHZ", -4.813953146458895);
    expect.put("10,LH1", -14.172829523795704);
    expect.put("10,LH2", -11.9362081816316);
    expect.put("10,LHZ", -10.650704302448048);

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testProcessGSOK() throws Exception {
    metric.setData(data2);

    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,LH1", 10.55316202766241);
    expect.put("00,LH2", 13.004893924325444);

    expect.put("00,HH1", 79.1343722643019);
    expect.put("00,HH2", 84.66032464538895);
    expect.put("00,HHZ", 69.51285486264509);

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testGetBaseName() throws Exception {
    assertEquals("BaseName", "StationDeviationMetric", metric.getBaseName());
  }

}
