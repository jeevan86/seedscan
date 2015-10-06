package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

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
		expect.put("00-10,LH1-LH1", 0.5667547040224983);
		expect.put("00-10,LH2-LH2", 0.557422800339938);
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
		expect.put("00-10,LH1-LH1", 0.5925009698602728);
		expect.put("00-10,LH2-LH2", 0.7084211389935788);
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
		expect.put("00-10,LH1-LH1", 0.3831008902272484);
		expect.put("00-10,LH2-LH2", 0.4012376564953167);
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
		expect.put("00-10,LH1-LH1", 0.22145105082997765);
		expect.put("00-10,LH2-LH2", 0.2187485908321105);
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
				System.out.println(id + " result: " + expect.get(id) + " " + result.getResult(id));
				assertEquals(id + " result: ", expect.get(id), result.getResult(id));
			}
	}

}
