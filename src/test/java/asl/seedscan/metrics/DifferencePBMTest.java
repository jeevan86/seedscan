package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import asl.util.ResourceManager;

public class DifferencePBMTest {

	private DifferencePBM metric;
	private static MetricData data;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		data = null;
	}

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void testProcess() throws Exception {
		/* The String key matches the MetricResult ids */
		
		//TEST 4 - 8
		metric = new DifferencePBM();
		metric.add("lower-limit", "4");
		metric.add("upper-limit", "8");
		metric.setData(data);
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 0.0277901829800261);
		expect.put("00-10,LHND-LHND", -0.7957964379263428);
		expect.put("00-10,LHED-LHED", -0.052274342636852655);
		testMetric(metric, expect);

		//TEST 18 - 22
		metric = new DifferencePBM();
		metric.add("lower-limit", "18");
		metric.add("upper-limit", "22");
		metric.setData(data);
		expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", -0.012528556291703559);
		expect.put("00-10,LHND-LHND", -0.7353751350788797);
		expect.put("00-10,LHED-LHED", -0.044778096414228245);
		testMetric(metric, expect);
		
		//TEST 90 - 110
		metric = new DifferencePBM();
		metric.add("lower-limit", "90");
		metric.add("upper-limit", "110");
		metric.setData(data);
		expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", -0.8565980848492304);
		expect.put("00-10,LHND-LHND", -2.523363274250344);
		expect.put("00-10,LHED-LHED", 1.964715975773799);
		testMetric(metric, expect);

		//TEST 200 - 500
		metric = new DifferencePBM();
		metric.add("lower-limit", "200");
		metric.add("upper-limit", "500");
		metric.setData(data);
		expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 2.4820410648465976);
		expect.put("00-10,LHND-LHND", -4.305281836191613);
		expect.put("00-10,LHED-LHED",  6.142238640298331);
		testMetric(metric, expect);
		
		//TEST Change in base
		//Results should be negative of 00-10
		metric = new DifferencePBM();
		metric.add("lower-limit", "200");
		metric.add("upper-limit", "500");
		metric.add("base-channel", "10-LH");
		metric.setData(data);
		expect = new HashMap<String, Double>();
		expect.put("10-00,LHZ-LHZ", -2.4820410648465976);
		expect.put("10-00,LHND-LHND", 4.305281836191613);
		expect.put("10-00,LHED-LHED",  -6.142238640298331);
		testMetric(metric, expect);

	}
	
	public void testMetric(DifferencePBM metric, HashMap<String, Double> expect) throws Exception {
		metric.process();
			MetricResult result = metric.getMetricResult();
			for (String id : result.getIdSet()) {
				/*
				 * If this is too stingy, try rounding 7 places like the metric
				 * injector does
				 */
				//Round to 7 places to match the Metric injector
				Double expected = (double)Math.round(expect.get(id)       * 1000000d) / 1000000d;
				Double resulted = (double)Math.round(result.getResult(id) * 1000000d) / 1000000d;
				assertEquals(id + " result: ", expected, resulted);	
			}
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
