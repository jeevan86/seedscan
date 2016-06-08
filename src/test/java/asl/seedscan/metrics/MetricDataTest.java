package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import asl.concurrent.Task;
import asl.metadata.Channel;
import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;
import asl.seedscan.database.MetricReader;
import asl.seedscan.database.MetricValueIdentifier;
import asl.seedscan.database.QueryContext;
import asl.seedsplitter.DataSet;
import asl.testutils.ResourceManager;
import seed.Blockette320;

public class MetricDataTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/*
	 * Tests the missing data constructor.
	 */
	@Test
	public final void testMetricDataMetricReaderStationMeta() throws Exception {
		StationMeta metadata = (StationMeta) ResourceManager
				.loadCompressedObject("/metadata/CU.BCIP.2015.228.StationMeta.ser.gz", false);
		MetricData metricData = new MetricData(new MockReader(), metadata);
		assertNotNull(metricData);
	}

	/*
	 * Basic checks of a loaded metadata to ensure loading correct metadata in
	 * class.
	 */
	@Test
	public final void testGetMetaData() throws Exception {
		MetricData data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
		StationMeta metadata = data.getMetaData();
		assertNotNull(metadata);
		assertEquals("2015:206", metadata.getDate());
		assertEquals((Double) 1820.0, (Double) metadata.getElevation());
		assertEquals("IU_ANMO", metadata.toString());
	}

	/*
	 * Checks various types of channels that should exist or do not exist in
	 * this data.
	 */
	@Test
	public final void testHasChannels() throws Exception {
		MetricData data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
		// Should exist
		assertTrue(data.hasChannels("00", "LH"));
		assertTrue(data.hasChannels("10", "BH"));
		// Double Check
		assertTrue(data.hasChannels("10", "BH"));

		// Should not exist
		assertFalse(data.hasChannels("70", "BH"));
		// 20-HNZ closed before this date
		assertFalse(data.hasChannels("20", "HN"));
		// LDO exists, but is out of the scope of this method
		assertFalse(data.hasChannels("35", "LD"));
		// Flipped parameters
		assertFalse(data.hasChannels("BH", "00"));

	}

	/*
	 * Basic metric reader return value test and check if reader disconnected.
	 */
	@Test
	public final void testGetMetricValue() throws Exception {
		StationMeta metadata = (StationMeta) ResourceManager
				.loadCompressedObject("/metadata/CU.BCIP.2015.228.StationMeta.ser.gz", false);
		MockReader reader = new MockReader();
		MetricData metricData = new MetricData(reader, metadata);

		Calendar date = new GregorianCalendar(2014, 7, 12);
		Station station = new Station("CU", "BCIP");
		String metricName = "AvailabilityMetric";
		Channel channel = new Channel("00", "LHZ");
		
		double value = metricData.getMetricValue(date, metricName, station, channel);

		//Round to 7 places to match the Metric injector
		Double expected = (double)Math.round(35.123456 * 1000000d) / 1000000d;
		Double resulted = (double)Math.round(value     * 1000000d) / 1000000d;
		assertEquals(expected, resulted);	
		

		// Check disconnected reader
		reader.setConnected(false);
		assertNull(metricData.getMetricValue(date, metricName, station, channel));

	}

	/*
	 * Test if channel data is returned.
	 */
	@Test
	public final void testGetChannelDataChannel() throws Exception {
		MetricData data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
		ArrayList<DataSet> channelData = data.getChannelData(new Channel("00", "LHZ"));

		assertNotNull(channelData);
		assertEquals((Integer) 1, (Integer) channelData.size());

		// Make sure we got the correct Channel back
		DataSet dataSet = channelData.get(0);
		assertEquals("IU", dataSet.getNetwork());
		assertEquals("ANMO", dataSet.getStation());
		assertEquals("00", dataSet.getLocation());
		assertEquals("LHZ", dataSet.getChannel());

	}

	/*
	 * TODO: Need a day with a calibration, but won't worry about until
	 * Calibration metric is working.
	 */
	@Test
	public final void testHasCalibrationData() throws Exception {
		MetricData data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
		assertFalse(data.hasCalibrationData());

		StationMeta metadata = (StationMeta) ResourceManager
				.loadCompressedObject("/metadata/CU.BCIP.2015.228.StationMeta.ser.gz", false);
		MetricData metricData = new MetricData(new MockReader(), metadata);
		assertFalse(metricData.hasCalibrationData());

	}

	/*
	 * TODO: Need a day with a calibration, but won't worry about until
	 * Calibration metric is working.
	 */
	@Test
	public final void testGetChannelCalDataChannel() throws Exception {
		ArrayList<Blockette320> calData;

		MetricData data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
		calData = data.getChannelCalData(new Channel("00", "LHZ"));
		assertNull(calData);
	}

	@Test
	public final void testGetChannelTimingQualityDataChannel() throws Exception {
		ArrayList<Integer> timingQuality;

		MetricData data = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);

		timingQuality = data.getChannelTimingQualityData(new Channel("10", "BH1"));

		int i;
		for (i = 0; i < 100; i++) {
			if (timingQuality.get(i) != 100) {
				fail("Timing Quality doesn't match expected 100%");
			}
		}

		timingQuality = data.getChannelTimingQualityData(new Channel("45", "BH1"));
		assertNull(timingQuality);

	}

	@Test
	public final void testValueDigestChanged() throws Exception {
		// NO DATA
		StationMeta metadata = (StationMeta) ResourceManager
				.loadCompressedObject("/metadata/CU.BCIP.2015.228.StationMeta.ser.gz", false);
		MockReader reader = new MockReader();
		MetricData metricData = new MetricData(reader, metadata);

		Calendar date = new GregorianCalendar(2014, 7, 12);
		Station station = new Station("CU", "BCIP");
		String metricName = "AvailabilityMetric";

		// No data, digest in Reader, no force update
		Channel channel = new Channel("00", "LHZ");
		ByteBuffer digest = metricData.valueDigestChanged(channel,
				new MetricValueIdentifier(date, metricName, station, channel), false);
		assertNull(digest);

		// No data, digest not in Reader, no force update
		channel = new Channel("00", "BH1"); // Not set in reader
		digest = metricData.valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
				false);
		assertNotNull(digest);

		// No data, digest in Reader, force update
		channel = new Channel("00", "LHZ");
		digest = metricData.valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
				true);
		assertNotNull(digest);

		// No data, digest in Reader, reader disconnected
		channel = new Channel("00", "LHZ");
		reader.setConnected(false);
		digest = metricData.valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
				false);
		assertNotNull(digest);

		reader.setConnected(true);

		// DATA
		metricData = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
		metricData.setMetricReader(reader);
		date = new GregorianCalendar(2015, 8, 16);
		station = new Station("IU", "ANMO");
		metricName = "AvailabilityMetric";

		// Data, no digest in Reader, no force update
		channel = new Channel("10", "BH2");
		digest = metricData.valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
				false);
		assertNotNull(digest);

		// Data, digest match in Reader, no force update
		channel = new Channel("10", "BH1");
		digest = metricData.valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
				false);
		assertNull(digest);

		// Data, digest match in Reader, force update
		digest = metricData.valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
				true);
		assertNotNull(digest);

		// Data, digest match in Reader, reader disconnected
		reader.setConnected(false);
		digest = metricData.valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
				false);
		assertNotNull(digest);
		reader.setConnected(true);

		// Data, digest mismatch in Reader, no force update
		channel = new Channel("00", "LH2");
		digest = metricData.valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
				false);
		assertNotNull(digest);

	}

	private class MockReader extends MetricReader {

		private boolean mockConnected;

		public MockReader() {
			super(null);
			this.mockConnected = true;
		}

		@Override
		public boolean isConnected() {
			return mockConnected;
		}

		public void setConnected(boolean isConnected) {
			this.mockConnected = isConnected;
		}

		@Override
		protected void setup() {
			// Don't need this
		}

		@Override
		protected void performTask(Task<QueryContext<? extends Object>> task) {
			// Don't need this
		}

		@Override
		protected void cleanup() {
			/// Don't need this
		}

		/*
		 * Reserved channels that must be missing include 00-BH*
		 */
		public Double getMetricValue(MetricValueIdentifier id) {
			HashMap<MetricValueIdentifier, Double> expect = new HashMap<MetricValueIdentifier, Double>();
			Calendar date = new GregorianCalendar(2014, 7, 12);
			Station station = new Station("CU", "BCIP");
			String metricName = "AvailabilityMetric";

			Channel channel = new Channel("00", "LHZ");
			expect.put(new MetricValueIdentifier(date, metricName, station, channel), 35.123456);

			channel = new Channel("00", "LH1");
			expect.put(new MetricValueIdentifier(date, metricName, station, channel), 99.999);

			channel = new Channel("00", "LH2");
			expect.put(new MetricValueIdentifier(date, metricName, station, channel), 0.00);

			return expect.get(id);
		}

		/**
		 * Currently getMetricValueDigest() is the only method called (from the
		 * MetricData class)
		 */
		public ByteBuffer getMetricValueDigest(MetricValueIdentifier id) {
			HashMap<MetricValueIdentifier, ByteBuffer> expect = new HashMap<MetricValueIdentifier, ByteBuffer>();
			Calendar date = new GregorianCalendar(2014, 7, 12);
			Station station = new Station("CU", "BCIP");
			String metricName = "AvailabilityMetric";

			Channel channel = new Channel("00", "LHZ");
			expect.put(new MetricValueIdentifier(date, metricName, station, channel),
					ByteBuffer.wrap("Same".getBytes()));

			channel = new Channel("00", "LH1");
			expect.put(new MetricValueIdentifier(date, metricName, station, channel),
					ByteBuffer.wrap("Same".getBytes()));

			channel = new Channel("00", "LH2");
			expect.put(new MetricValueIdentifier(date, metricName, station, channel),
					ByteBuffer.wrap("Different".getBytes()));

			// ANMO
			date = new GregorianCalendar(2015, 8, 16);
			station = new Station("IU", "ANMO");
			metricName = "AvailabilityMetric";

			channel = new Channel("10", "BH1"); // Precomputed the digest
			expect.put(new MetricValueIdentifier(date, metricName, station, channel),
					ByteBuffer.wrap(DatatypeConverter.parseHexBinary("9A4FE3A10FD60F93526F464B0DB9580E")));
			channel = new Channel("00", "LH2");
			expect.put(new MetricValueIdentifier(date, metricName, station, channel),
					ByteBuffer.wrap("Different".getBytes()));

			System.out.println(DatatypeConverter
					.printHexBinary(expect.get(new MetricValueIdentifier(date, metricName, station, channel)).array()));

			return expect.get(id);
		}
	}
}
