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
  private static double ERROR = 1E-5;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    data = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
    maleableData = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.NWAO.2015.299.MetricData.ser.gz", true);
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
    expect.put("00-10,LHZ-LHZ", 0.99997, ERROR);
    expect.put("00-10,LHND-LHND", 0.99973, ERROR);
    expect.put("00-10,LHED-LHED", 0.99763, ERROR);
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
    expect.put("00-10,LHZ-LHZ", 0.10371478170685652);
    expect.put("00-10,LHND-LHND", 0.12759091538485867);
    expect.put("00-10,LHED-LHED", 0.1321379382028378);
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
    expect.put("00-10,LHZ-LHZ", 0.99972, ERROR);
    expect.put("00-10,LHND-LHND", 0.99765, ERROR);
    expect.put("00-10,LHED-LHED", 0.99859, ERROR);
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
    expect.put("00-10,LHZ-LHZ", 0.84999, ERROR);
    expect.put("00-10,LHND-LHND", 0.59943, ERROR);
    expect.put("00-10,LHED-LHED", 0.65845, ERROR);
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
    expect.put("00-10,LHZ-LHZ", 0.2937884614962973);
    expect.put("00-10,LHND-LHND", 0.2116717537635636);
    expect.put("00-10,LHED-LHED", 0.21227322319511213);
    System.out.println(metric);
    System.out.println(expect);
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
    expect.put("10-00,LHZ-LHZ", 0.2937884614962973);
    expect.put("10-00,LHND-LHND", 0.2116717537635636);
    expect.put("10-00,LHED-LHED", 0.21227322319511213);
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
