package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import asl.testutils.ResourceManager;
import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NLNMDeviationMetricTest {

  private NLNMDeviationMetric metric;
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
    metric = new NLNMDeviationMetric();
    metric.add("lower-limit", "4");
    metric.add("upper-limit", "8");
  }

  @Test
  public final void testGetVersion() throws Exception {
    assertEquals("Metric Version: ", 1, metric.getVersion());
  }

  @Test
  public final void testProcess() throws Exception {

    metric.setData(data1);
    metric.add("channel-restriction", "LH");

		/* The String key matches the MetricResult ids */
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,LH1", 6.144156714847165);
    expect.put("00,LH2", 6.94984340838684);
    expect.put("00,LHZ", 9.502837498158087);
    expect.put("10,LH1", 7.126248440694965);
    expect.put("10,LH2", 6.701346629427542);
    expect.put("10,LHZ", 9.473855204418532);

    TestUtils.testMetric(metric, expect);

    // Consider adding this once memozing computePSD is done. Currently it
    // pretends to, but doesn't really.

    // metric = new NLNMDeviationMetric();
    // metric.add("lower-limit", "4");
    // metric.add("upper-limit", "8");
    // metric.setData(data1);
    // //metric.add("channel-restriction", "LH");
    //
    // /* The String key matches the MetricResult ids */
    // expect = new HashMap<String, Double>();
    // expect.put("00,LH1", 6.144156714847165);
    // expect.put("00,LH2", 6.94984340838684);
    // expect.put("00,LHZ", 9.502837498158087);
    // expect.put("10,LH1", 7.126248440694965);
    // expect.put("10,LH2", 6.701346629427542);
    // expect.put("10,LHZ", 9.473855204418532);
    // expect.put("00,BH1", 7.190265405958664);
    // expect.put("00,BH2", 8.080463692084166);
    // expect.put("00,BHZ", 10.582575310053912);
    // expect.put("10,BH1", 8.132530485497114);
    // expect.put("10,BH2", 7.876391947428445);
    // expect.put("10,BHZ", 10.551351503338514);
    //
    // metric.process();
    // result = metric.getMetricResult();
    // for (String id : result.getIdSet()) {
    // //Round to 7 places to match the Metric injector
    // Double expected = (double)Math.round(expect.get(id) * 1000000d) /
    // 1000000d;
    // Double resulted = (double)Math.round(result.getResult(id) * 1000000d)
    // / 1000000d;
    // System.out.println(id + " " +result.getResult(id));
    // assertEquals(id + " result: ", expected, resulted);
    // }

    // Test High frequency cases
    // LH should be ignored by this metric since it is below our nyquist
    // frequency.

    metric = new NLNMDeviationMetric();
    metric.add("lower-limit", "0.125");
    metric.add("upper-limit", "0.25");
    /*
     * This should cause it to print a log message for failing to makePlots
		 */
    metric.add("makeplots", "true");

    metric.setData(data1);

		/* The String key matches the MetricResult ids */
    expect = new HashMap<>();

    expect.put("00,BH1", 9.232908381769587);
    expect.put("00,BH2", 10.195579936296998);
    expect.put("00,BHZ", 14.13441804592783);
    expect.put("10,BH1", 11.99869763918782);
    expect.put("10,BH2", 11.413398919704086);
    expect.put("10,BHZ", 12.496385325897268);

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testGetBaseName() throws Exception {
    assertEquals("Base name: ", "NLNMDeviationMetric", metric.getBaseName());
  }

  @Test
  public final void testGetName() throws Exception {
    assertEquals("Metric name: ", "NLNMDeviationMetric:4-8", metric.getName());
  }

  @Test
  public final void testNLNMDeviationMetric() throws Exception {
    metric = new NLNMDeviationMetric();
    metric.add("lower-limit", "90");
    metric.add("upper-limit", "110");
    /* This has to come after adding the limits */
    metric.setData(data1);
  }

  @Test
  public final void testGetNLNM() throws Exception {
    metric.add("nlnm-modelfile", "build/resources/main/noiseModels/NLNM.ascii");
    assertTrue(NLNMDeviationMetric.getNLNM().isValid());
  }

  @Test
  public final void testGetNHNM() throws Exception {
    metric.add("nhnm-modelfile", "build/resources/main/noiseModels/NHNM.ascii");
    assertTrue(NLNMDeviationMetric.getNHNM().isValid());
  }

}
