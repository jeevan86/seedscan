package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import asl.util.ResourceManager;

public class StationDeviationMetricTest {

	private StationDeviationMetric metric;
	private static MetricData data1;
	private static MetricData data2;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			data1 = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz");
			data2 = (MetricData) ResourceManager.loadCompressedObject("/data/GS.OK029.2015.360.MetricData.ser.gz");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		data1 = null;
		data2 = null;
	}

	@Before
	public void setUp() throws Exception {
		metric = new StationDeviationMetric();
		metric.add("lower-limit", "90");
		metric.add("upper-limit", "110");
		metric.add("modelpath", ResourceManager.getDirectoryPath("/models"));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testGetVersion() throws Exception {
		assertEquals("Version #: ", (long) 2, metric.getVersion());
	}

	@Test
	public final void testProcessANMO() throws Exception {
		metric.setData(data1);
		
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00,LH1", -6.259435685534593);
		expect.put("00,LH2", -3.6049945557454826);
		expect.put("00,LHZ", -4.813953146458895);
		expect.put("10,LH1", -14.172829523795704);
		expect.put("10,LH2", -11.9362081816316);
		expect.put("10,LHZ", -10.650704302448048);

		metric.process();
		MetricResult result = metric.getMetricResult();
		for (String id : result.getIdSet()) {
			/*
			 * If this is too stingy, try rounding 7 places like the metric
			 * injector does
			 */
			assertEquals(id + " result: ", expect.get(id), result.getResult(id));
		}		
	}
	
	@Test
	public final void testProcessGSOK() throws Exception {
		metric.setData(data2);
		
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00,LH1", 16.486482398122135);
		expect.put("00,LH2", 9.20595648333834);
		
		expect.put("00,HH1", 16.80422454042565);
		expect.put("00,HH2", 8.336695304459255);
		expect.put("00,HHZ", 3.496288934866328);

		metric.process();
		MetricResult result = metric.getMetricResult();
		for (String id : result.getIdSet()) {
			assertEquals(id + " result: ", expect.get(id), result.getResult(id));
		}	
	}

	@Test
	public final void testGetBaseName() throws Exception {
		assertEquals("BaseName", "StationDeviationMetric", metric.getBaseName());
	}

}
