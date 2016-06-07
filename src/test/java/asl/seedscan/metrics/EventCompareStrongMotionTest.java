package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import asl.seedscan.event.EventLoader;
import asl.util.ResourceManager;

public class EventCompareStrongMotionTest {

	private EventCompareStrongMotion metric;
	private static MetricData data;
	private static EventLoader eventLoader;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.NWAO.2015.299.MetricData.ser.gz", false);
			//data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz");
			eventLoader = new EventLoader(ResourceManager.getDirectoryPath("/events"));
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
		metric = new EventCompareStrongMotion();
		metric.setData(data);
		Calendar date = new GregorianCalendar(2015,9,26);
		metric.setEventTable(eventLoader.getDayEvents(date));
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00-20,LHZ-LNZ", 2.884394926482693);  //Was 2.884394926482693 before removing hard coding
		expect.put("00-20,LHND-LNND", 0.6842270334452618); //Was 0.6848874383447533
		expect.put("00-20,LHED-LNED", 1.0140182353130993); //1.0097711288921811
		expect.put("10-20,LHZ-LNZ", 4.0); //4.0
		expect.put("10-20,LHND-LNND", 4.0); //4.0
		expect.put("10-20,LHED-LNED", -4.0); //Nonexistent
		TestUtils.testMetric(metric, expect);
	}

	@Test
	public final void testGetVersion() throws Exception {
		metric = new EventCompareStrongMotion();
		assertEquals(2, metric.getVersion());
	}

	@Test
	public final void testGetName() throws Exception {
		metric = new EventCompareStrongMotion();
		assertEquals("EventCompareStrongMotion", metric.getName());
	}

}
