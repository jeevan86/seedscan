package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import java.io.ObjectInputStream;
import java.util.HashMap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NLNMDeviationMetricTest {
	
	private NLNMDeviationMetric metric;
	private static MetricData data1;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ObjectInputStream serialIn = new ObjectInputStream(NLNMDeviationMetricTest.class.getResourceAsStream("/data/IU.ANMO.2015.206.MetricData.ser"));
		try{
		data1 = (MetricData)serialIn.readObject();
		}catch(Exception e){
		e.printStackTrace();
		}
	}

	@Before
	public void setUp() throws Exception {
		metric = new NLNMDeviationMetric();
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
		/*The String key matches the MetricResult ids*/
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00,LH1", 6.144156714847165);
		expect.put("00,LH2", 6.94984340838684);
		expect.put("00,LHZ", 9.502837498158087);
		expect.put("10,LH1", 7.126248440694965);
		expect.put("10,LH2", 6.701346629427542);
		expect.put("10,LHZ", 9.473855204418532);
		
		metric.process();
		MetricResult result = metric.getMetricResult();
		for (String id : result.getIdSet()){
			/*If this is too stingy, try rounding 7 places like the metric injector does*/
			assertEquals(id +" result: ", expect.get(id), result.getResult(id));
		}
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
		/*This has to come after adding the limits*/
		metric.setData(data1);
	}

	@Test
	public final void testGetNLNM() throws Exception {
		metric.add("nlnm-modelfile", "build/resources/main/noiseModels/NLNM.ascii");
		org.junit.Assert.assertTrue(NLNMDeviationMetric.getNLNM().isValid());
	}

	@Test
	public final void testGetNHNM() throws Exception {
		metric.add("nhnm-modelfile", "build/resources/main/noiseModels/NHNM.ascii");
		org.junit.Assert.assertTrue(NLNMDeviationMetric.getNHNM().isValid());
	}

}
