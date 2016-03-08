package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import asl.util.ResourceManager;

public class CoherencePBMTest {
	

	private CoherencePBM metric;
	private static MetricData data;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz");
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
		metric = new CoherencePBM();
		metric.add("lower-limit", "4");
		metric.add("upper-limit", "8");
		metric.setData(data);
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 0.99997471849571);
		expect.put("00-10,LHND-LHND", 0.9997343078305163);
		expect.put("00-10,LHED-LHED", 0.9976266947968053);
		testMetric(metric, expect);

		//TEST 18 - 22
		metric = new CoherencePBM();
		metric.add("lower-limit", "18");
		metric.add("upper-limit", "22");
		metric.setData(data);
		expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 0.9996693481353688);
		expect.put("00-10,LHND-LHND", 0.997686279191404);
		expect.put("00-10,LHED-LHED", 0.9983404620924748);
		testMetric(metric, expect);
		
		//TEST 90 - 110
		metric = new CoherencePBM();
		metric.add("lower-limit", "90");
		metric.add("upper-limit", "110");
		metric.setData(data);
		expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 0.8466037396390191);
		expect.put("00-10,LHND-LHND", 0.5581889516577663);
		expect.put("00-10,LHED-LHED", 0.659323426099024);
		testMetric(metric, expect);

		//TEST 200 - 500
		metric = new CoherencePBM();
		metric.add("lower-limit", "200");
		metric.add("upper-limit", "500");
		metric.setData(data);
		expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 0.2937884614962967);
		expect.put("00-10,LHND-LHND", 0.21167174950454593);
		expect.put("00-10,LHED-LHED",  0.21227611120383297);
		testMetric(metric, expect);
		
		//TEST Change in base
		//Results should match 00-10
		metric = new CoherencePBM();
		metric.add("lower-limit", "200");
		metric.add("upper-limit", "500");
		metric.add("base-channel", "10-LH");
		metric.setData(data);
		expect = new HashMap<String, Double>();
		expect.put("10-00,LHZ-LHZ", 0.2937884614962967);
		expect.put("10-00,LHND-LHND", 0.21167174950454593);
		expect.put("10-00,LHED-LHED",  0.21227611120383297);
		testMetric(metric, expect);

	}
	
	public void testMetric(CoherencePBM metric, HashMap<String, Double> expect) throws Exception {
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

}
