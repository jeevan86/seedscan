package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		metric = new CoherencePBM();
		metric.add("lower-limit", "4");
		metric.add("upper-limit", "8");
		metric.setData(data);
	}

	@Test
	public void testGetVersion() throws Exception {
		assertEquals("Metric Version: ", 1, metric.getVersion());	
	}

	@Test
	public void testProcess() throws Exception {
		/* The String key matches the MetricResult ids */
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 0.99997471849571);
		expect.put("00-10,LHND-LHND", 0.9997343078305163);
		expect.put("00-10,LHED-LHED", 0.9976266947968053);
		expect.put("00-10,LH1-LH1", 0.5667547040224983);
		expect.put("00-10,LH2-LH2", 0.557422800339938);
		//expect.put("00-10,BH1-BH1", 0.99997471849571);

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

	@Test
	public void testGetBaseName() throws Exception {
		assertEquals("Base name: ", "CoherencePBM", metric.getBaseName());	}

	@Test
	public void testGetName() throws Exception {
		assertEquals("Metric name: ", "CoherencePBM:4-8", metric.getName());
	}

}
