package asl.seedscan.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import asl.metadata.Station;
import asl.util.ResourceManager;
import sac.SacTimeSeries;

public class EventLoaderTest {

	private static EventLoader eventLoader;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		eventLoader = new EventLoader(ResourceManager.getDirectoryPath("/events"));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		eventLoader = null;
	}

	@Test
	public final void testEventLoader() throws Exception {
		String correctEventDir = EventLoader.getEventsDirectory();
		// Try to change the directory, shouldn't happen.
		new EventLoader(ResourceManager.getDirectoryPath("/models"));
		assertEquals(EventLoader.getEventsDirectory(), correctEventDir);
	}

	@Test
	public final void testGetDaySynthetics() throws Exception {
		Calendar date = new GregorianCalendar(2015, 9, 26); // GregorianCalendar
															// is 0 based with
															// months
		Station station = new Station("IU", "NWAO");

		eventLoader.getDayEvents(date); // TODO: This shouldn't be required.
		Hashtable<String, Hashtable<String, SacTimeSeries>> synthetics = eventLoader.getDaySynthetics(date, station);
		assertNotNull(synthetics);
		Hashtable<String, SacTimeSeries> eventSynthetics = synthetics.get("C201510260909A");

		SacTimeSeries timeseries = eventSynthetics.get("NWAO.XX.LXZ.modes.sac.proc");
		assertEquals(new Integer(timeseries.getNumPtsRead()), new Integer(3999));
		timeseries = eventSynthetics.get("NWAO.XX.LXZ.modes.sac");
		assertEquals(new Integer(timeseries.getNumPtsRead()), new Integer(8000));

		// Should only return table with synthetics that are actually there.
		station = new Station("IU", "MAKZ");
		synthetics = eventLoader.getDaySynthetics(date, station);
		assertTrue(synthetics.isEmpty());

	}

	@Test
	public final void testGetDayEvents() throws Exception {
		Calendar date = new GregorianCalendar(2015, 9, 26); // GregorianCalendar
															// is 0 based with
															// months
		Hashtable<String, EventCMT> cmts = eventLoader.getDayEvents(date);
		EventCMT cmt = cmts.get("C201510260909A");
		assertEquals(new Double(cmt.getLatitude()), new Double(36.44));
		assertEquals(new Double(cmt.getLongitude()), new Double(70.72));
		assertEquals(new Double(cmt.getDepth()), new Double(212.5));
	}

}
