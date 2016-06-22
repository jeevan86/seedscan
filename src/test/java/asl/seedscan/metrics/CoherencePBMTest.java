package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import asl.testutils.ResourceManager;

public class CoherencePBMTest {
	private static MetricData data;
	private static MetricData maleableData;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
			maleableData = (MetricData) ResourceManager.loadCompressedObject("/data/IU.NWAO.2015.299.MetricData.ser.gz", true);

		} catch (Exception e) {
			e.printStackTrace();
		}
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
	public void testProcess48() throws Exception {
		//TEST 4 - 8
		CoherencePBM metric = new CoherencePBM();
		metric.add("lower-limit", "4");
		metric.add("upper-limit", "8");
		metric.setData(data);
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 0.99997471849571);
		expect.put("00-10,LHND-LHND", 0.9997343078305163);
		expect.put("00-10,LHED-LHED", 0.9976266947968053);
		TestUtils.testMetric(metric, expect);
	}
	@Test
	public void testProcess48RotationNeeded() throws Exception {
		//TEST 4 - 8
		CoherencePBM metric = new CoherencePBM();
		metric.add("lower-limit", "4");
		metric.add("upper-limit", "8");
		metric.setData(maleableData);
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 0.10350950393220554);
		expect.put("00-10,LHND-LHND", 0.12234638248634985);
		expect.put("00-10,LHED-LHED", 0.1273019962691998);
		TestUtils.testMetric(metric, expect);
	}
	@Test
	public void testProcess1822() throws Exception {
		//TEST 18 - 22
		CoherencePBM metric = new CoherencePBM();
		metric.add("lower-limit", "18");
		metric.add("upper-limit", "22");
		metric.setData(data);
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 0.9996693481353688);
		expect.put("00-10,LHND-LHND", 0.997686279191404);
		expect.put("00-10,LHED-LHED", 0.9983404620924748);
		TestUtils.testMetric(metric, expect);
	}
	@Test
	public void testProcess90110() throws Exception {
		//TEST 90 - 110
		CoherencePBM metric = new CoherencePBM();
		metric.add("lower-limit", "90");
		metric.add("upper-limit", "110");
		metric.setData(data);
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 0.8466037396390191);
		expect.put("00-10,LHND-LHND", 0.5581889516577663);
		expect.put("00-10,LHED-LHED", 0.659323426099024);
		TestUtils.testMetric(metric, expect);
	}
	@Test
	public void testProcess200500() throws Exception {
		//TEST 200 - 500
		CoherencePBM metric = new CoherencePBM();
		metric.add("lower-limit", "200");
		metric.add("upper-limit", "500");
		metric.setData(data);
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00-10,LHZ-LHZ", 0.2937884614962967);
		expect.put("00-10,LHND-LHND", 0.21167174950454593);
		expect.put("00-10,LHED-LHED",  0.21227611120383297);
		TestUtils.testMetric(metric, expect);
	}
	@Test
	public void testProcess200500Reverse() throws Exception {
		//TEST Change in base
		//Results should match 00-10
		CoherencePBM metric = new CoherencePBM();
		metric.add("lower-limit", "200");
		metric.add("upper-limit", "500");
		metric.add("base-channel", "10-LH");
		metric.setData(data);
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("10-00,LHZ-LHZ", 0.2937884614962967);
		expect.put("10-00,LHND-LHND", 0.21167174950454593);
		expect.put("10-00,LHED-LHED",  0.21227611120383297);
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
