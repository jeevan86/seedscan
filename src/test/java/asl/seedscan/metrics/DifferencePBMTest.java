package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import asl.testutils.ResourceManager;
import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DifferencePBMTest {

  private DifferencePBM metric;
  private static MetricData data;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    data = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    data = null;
  }

  @Test
  public void testProcess4_8() throws Exception {
    /* The String key matches the MetricResult ids */

    //TEST 4 - 8
    metric = new DifferencePBM();
    metric.add("lower-limit", "4");
    metric.add("upper-limit", "8");
    metric.setData(data);
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00-10,LHZ-LHZ", 0.02779018298002567);
    expect.put("00-10,LHND-LHND", -0.7957964379446976);
    expect.put("00-10,LHED-LHED", -0.05227434264917066);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcess18_22() throws Exception {
    //TEST 18 - 22
    metric = new DifferencePBM();
    metric.add("lower-limit", "18");
    metric.add("upper-limit", "22");
    metric.setData(data);
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00-10,LHZ-LHZ", -0.012528556291717641);
    expect.put("00-10,LHND-LHND", -0.7353751399284663);
    expect.put("00-10,LHED-LHED", -0.04477810128884697);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcess90_110() throws Exception {
    //TEST 90 - 110
    metric = new DifferencePBM();
    metric.add("lower-limit", "90");
    metric.add("upper-limit", "110");
    metric.setData(data);
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00-10,LHZ-LHZ", -0.8565980848491274);
    expect.put("00-10,LHND-LHND", -2.5233649990080536);
    expect.put("00-10,LHED-LHED", 1.9647142330285077);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcess200_500() throws Exception {
    //TEST 200 - 500
    metric = new DifferencePBM();
    metric.add("lower-limit", "200");
    metric.add("upper-limit", "500");
    metric.setData(data);
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00-10,LHZ-LHZ", 2.482041064846602);
    expect.put("00-10,LHND-LHND", -4.305257164778122);
    expect.put("00-10,LHED-LHED", 6.142284343045829);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcess200_500_Reverse() throws Exception {
    //TEST Change in base
    //Results should be negative of 00-10
    metric = new DifferencePBM();
    metric.add("lower-limit", "200");
    metric.add("upper-limit", "500");
    metric.add("base-channel", "10-LH");
    metric.setData(data);
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("10-00,LHZ-LHZ", -2.482041064846602);
    expect.put("10-00,LHND-LHND", 4.305257164778122);
    expect.put("10-00,LHED-LHED", -6.142284343045829);
    TestUtils.testMetric(metric, expect);

  }

  @Test
  public final void testGetVersion() throws Exception {
    metric = new DifferencePBM();
    assertEquals(2, metric.getVersion());
  }

  @Test
  public final void testGetBaseName() throws Exception {
    metric = new DifferencePBM();
    assertEquals("DifferencePBM", metric.getBaseName());
  }

}
