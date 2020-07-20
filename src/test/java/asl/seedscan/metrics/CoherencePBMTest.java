package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CoherencePBMTest {

  private static MetricData data;
  private static MetricData maleableData;
  private static double ERROR = 1E-4;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    data = ResourceManager.loadANMOMainTestCase();
    maleableData = ResourceManager.loadNWAOMainTestCase();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    data = null;
    maleableData = null;
  }

  @Before
  public void setUp() throws Exception {

  }

  @Test
  public void testProcess4_8() throws Exception {
    //TEST 4 - 8
    CoherencePBM metric = new CoherencePBM();
    metric.add("lower-limit", "4");
    metric.add("upper-limit", "8");
    metric.setData(data);
    MetricTestMap expect = new MetricTestMap();
    expect.put("00-10,LHZ-LHZ",   0.99997, ERROR);
    expect.put("00-10,LHND-LHND", 0.99991, ERROR);
    expect.put("00-10,LHED-LHED", 0.99867, ERROR);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcess4_8_RotationNeeded() throws Exception {
    //TEST 4 - 8
    CoherencePBM metric = new CoherencePBM();
    metric.add("lower-limit", "4");
    metric.add("upper-limit", "8");
    metric.setData(maleableData);
    MetricTestMap expect = new MetricTestMap();
    expect.put("00-10,LHZ-LHZ",   0.46650701372368103);
    expect.put("00-10,LHND-LHND", 0.5182666130353961);
    expect.put("00-10,LHED-LHED", 0.6147684499533778);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcess18_22() throws Exception {
    //TEST 18 - 22
    CoherencePBM metric = new CoherencePBM();
    metric.add("lower-limit", "18");
    metric.add("upper-limit", "22");
    metric.setData(data);
    MetricTestMap expect = new MetricTestMap();
    expect.put("00-10,LHZ-LHZ",   0.99972, ERROR);
    expect.put("00-10,LHND-LHND", 0.99818, ERROR);
    expect.put("00-10,LHED-LHED", 0.99880, ERROR);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcess90_110() throws Exception {
    //TEST 90 - 110
    CoherencePBM metric = new CoherencePBM();
    metric.add("lower-limit", "90");
    metric.add("upper-limit", "110");
    metric.setData(data);
    MetricTestMap expect = new MetricTestMap();
    expect.put("00-10,LHZ-LHZ",   0.85146, ERROR);
    expect.put("00-10,LHND-LHND", 0.59659, ERROR);
    expect.put("00-10,LHED-LHED", 0.66137, ERROR);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcess200_500() throws Exception {
    //TEST 200 - 500
    CoherencePBM metric = new CoherencePBM();
    metric.add("lower-limit", "200");
    metric.add("upper-limit", "500");
    metric.setData(data);
    MetricTestMap expect = new MetricTestMap();
    expect.put("00-10,LHZ-LHZ",   0.4220825); // was 0.2937884614962973
    expect.put("00-10,LHND-LHND", 0.3879783); // was 0.2116717537635636
    expect.put("00-10,LHED-LHED", 0.3641776); // was 0.21227322319511213
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcess200_500_Reverse() throws Exception {
    //TEST Change in base
    //Results should match 00-10
    CoherencePBM metric = new CoherencePBM();
    metric.add("lower-limit", "200");
    metric.add("upper-limit", "500");
    metric.add("base-channel", "10-LH");
    metric.setData(data);
    MetricTestMap expect = new MetricTestMap();
    expect.put("10-00,LHZ-LHZ",   0.4220825);
    expect.put("10-00,LHND-LHND", 0.3879783);
    expect.put("10-00,LHED-LHED", 0.3641776);
    TestUtils.testMetric(metric, expect);

  }

  @Test
  public final void testGetVersion() throws Exception {
    CoherencePBM metric = new CoherencePBM();
    assertEquals(1, metric.getVersion());
  }

  @Test
  public final void testGetBaseName() throws Exception {
    CoherencePBM metric = new CoherencePBM();
    assertEquals("CoherencePBM", metric.getBaseName());
  }
}
