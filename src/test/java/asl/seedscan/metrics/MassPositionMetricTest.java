package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import asl.testutils.ResourceManager;

public class MassPositionMetricTest {
	private MassPositionMetric metric;
	private static MetricData data1;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			data1 = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
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
		metric = new MassPositionMetric();
		metric.setData(data1);
	}

	@Test
	public final void testGetVersion() throws Exception {
		assertEquals("Metric Version: ", 1, metric.getVersion());
	}

	@Test
	public final void testProcess() throws Exception {
		
		//metric.add("channel-restriction", "VM");
		
		/* The String key matches the MetricResult ids */
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00,VM1", 0.8839489469085381);
		expect.put("00,VM2", 0.06631853547951848);
		expect.put("00,VMZ", 8.000000000000338);
		expect.put("10,VMV", 57.142857142857146);
		expect.put("10,VMU", 57.142857142857146);
		expect.put("10,VMW", 57.142857142857146);

		TestUtils.testMetric(metric, expect);
	}

	@Test
	public final void testGetName() throws Exception {
		assertEquals("Base name: ", "MassPositionMetric", metric.getName());
	}
}
