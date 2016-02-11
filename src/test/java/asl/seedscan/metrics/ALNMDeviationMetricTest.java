package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import asl.util.ResourceManager;

public class ALNMDeviationMetricTest {

	private ALNMDeviationMetric metric;
	private static MetricData data1;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			data1 = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz");
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
		metric = new ALNMDeviationMetric();
		metric.add("lower-limit", "4");
		metric.add("upper-limit", "8");
		metric.setData(data1);
	}

	@Test
	public final void testGetVersion() throws Exception {
		assertEquals("Metric Version: ", 1, metric.getVersion());
	}

	@Test
	public final void testProcess() throws Exception {
		
		metric.add("channel-restriction", "LN,HN");
		
		/* The String key matches the MetricResult ids */
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("20,LN1", 11.755173416833836);
		expect.put("20,LN2", 12.908588992722896);
		expect.put("20,LNZ", 12.920609888672088);

		metric.process();
		MetricResult result = metric.getMetricResult();
		assertFalse(result.getIdSet().isEmpty());
		for (String id : result.getIdSet()) {
			//Round to 7 places to match the Metric injector
			Double expected = (double)Math.round(expect.get(id)       * 1000000d) / 1000000d;
			Double resulted = (double)Math.round(result.getResult(id) * 1000000d) / 1000000d;
			assertEquals(id + " result: ", expected, resulted);	
		}
	}

	@Test
	public final void testGetBaseName() throws Exception {
		assertEquals("Base name: ", "ALNMDeviationMetric", metric.getBaseName());
	}

	@Test
	public final void testGetName() throws Exception {
		assertEquals("Metric name: ", "ALNMDeviationMetric:4-8", metric.getName());
	}

	@Test
	public final void testALNMDeviationMetric() throws Exception {
		metric = new ALNMDeviationMetric();
		metric.add("lower-limit", "18");
		metric.add("upper-limit", "22");
		/* This has to come after adding the limits */
		metric.setData(data1);
	}

	@Test
	public final void testGetALNM() throws Exception {
		metric.add("alnm-modelfile", "build/resources/main/noiseModels/ALNM.ascii");
		assertTrue(ALNMDeviationMetric.getALNM().isValid());
	}

	@Test
	public final void testGetAHNM() throws Exception {
		//metric.add("nhnm-modelfile", "build/resources/main/noiseModels/NHNM.ascii");
		assertTrue(ALNMDeviationMetric.getAHNM().isValid());
	}

}
