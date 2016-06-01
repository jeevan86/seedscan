package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import asl.metadata.Station;
import asl.seedscan.event.EventLoader;
import asl.util.ResourceManager;

public class EventCompareSyntheticTest {
	private EventCompareSynthetic metric;
	private static MetricData data;
	private static EventLoader eventLoader;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.NWAO.2015.299.MetricData.ser.gz", false);
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
		metric = new EventCompareSynthetic();
		metric.setData(data);
		Calendar date = new GregorianCalendar(2015,9,26);
		metric.setEventTable(eventLoader.getDayEvents(date));
		metric.setEventSynthetics(eventLoader.getDaySynthetics(date, new Station("IU", "NWAO")));
		HashMap<String, Double> expect = new HashMap<String, Double>();
		expect.put("00,LHZ", 0.7365784417165183);  
		expect.put("00,LHND", 0.8034777781560093); 
		expect.put("00,LHED", 0.7419806164967785);
		expect.put("10,LHZ", 0.0002246588435010572);
		expect.put("10,LHND", 0.00008719589964150271);
		//expect.put("10,LHED-LNED", -4.0); //Nonexistent
		TestUtils.testMetric(metric, expect);

	}

	@Test
	public final void testGetVersion() throws Exception {
		metric = new EventCompareSynthetic();
		assertEquals(2, metric.getVersion());
	}

	@Test
	public final void testGetName() throws Exception {
		metric = new EventCompareSynthetic();
		assertEquals("EventCompareSynthetic", metric.getName());
	}
}
