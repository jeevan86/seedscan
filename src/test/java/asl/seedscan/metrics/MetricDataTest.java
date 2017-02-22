package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;

import org.junit.BeforeClass;
import org.junit.Test;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.database.MetricValueIdentifier;
import asl.seedsplitter.DataSet;
import asl.testutils.ResourceManager;
import seed.Blockette320;
import asl.seedscan.database.MetricDatabaseMock;

public class MetricDataTest {

	private static MetricDatabaseMock database;
	private static MetricData data;
	private static StationMeta metadata;


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		data = (MetricData) ResourceManager
				.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
		metadata = (StationMeta) ResourceManager
				.loadCompressedObject("/metadata/CU.BCIP.2015.228.StationMeta.ser.gz", false);
		database = new MetricDatabaseMock();

		// BCIP - Digest and data
		LocalDate expectDate = LocalDate.parse("2014-07-12");
		Station expectStation = new Station("CU", "BCIP");
		String expectMetricName = "AvailabilityMetric";

		Channel expectChannel = new Channel("00", "LHZ");
		database.insertMockData(new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel), 35.123456, ByteBuffer.wrap("Same".getBytes()));

		expectChannel = new Channel("00", "LH1");
		database.insertMockData(new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel), 99.999, ByteBuffer.wrap("Same".getBytes()));

		expectChannel = new Channel("00", "LH2");
		database.insertMockData(new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel), 0.00, ByteBuffer.wrap("Different".getBytes()));

		// ANMO - Digest only tests
		expectDate = LocalDate.parse("2015-08-16");
		expectStation = new Station("IU", "ANMO");

		expectChannel = new Channel("10", "BH1"); // Precomputed the digest
		database.insertMockDigest(new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
				ByteBuffer.wrap(DatatypeConverter.parseHexBinary("9A4FE3A10FD60F93526F464B0DB9580E")));
		expectChannel = new Channel("00", "LH2");
		database.insertMockDigest(new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
				ByteBuffer.wrap("Different".getBytes()));
	}


	/*
	 * Tests the missing data constructor.
	 */
	@Test
	public final void testMetricDataMetricReaderStationMeta() throws Exception {
		MetricData metricData = new MetricData(new MetricDatabaseMock(), metadata);
		assertNotNull(metricData);
	}

	/*
	 * Basic checks of a loaded metadata to ensure loading correct metadata in
	 * class.
	 */
	@Test
	public final void testGetMetaData() throws Exception {
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
		MetricData metricData = new MetricData(database, metadata);

		LocalDate date = LocalDate.parse("2014-07-12");
		Station station = new Station("CU", "BCIP");
		String metricName = "AvailabilityMetric";
		Channel channel = new Channel("00", "LHZ");
		
		double value = metricData.getMetricValue(date, metricName, station, channel);

		//Round to 7 places to match the Metric injector
		Double expected = (double)Math.round(35.123456 * 1000000d) / 1000000d;
		Double resulted = (double)Math.round(value     * 1000000d) / 1000000d;
		assertEquals(expected, resulted);	
		

		// Check disconnected reader
		database.setConnected(false);
		assertNull(metricData.getMetricValue(date, metricName, station, channel));
	}

	/*
	 * Test if channel data is returned.
	 */
	@Test
	public final void testGetChannelDataChannel() throws Exception {
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
		assertFalse(data.hasCalibrationData());
		MetricData metricData = new MetricData(new MetricDatabaseMock(), metadata);
		assertFalse(metricData.hasCalibrationData());
	}

	/*
	 * TODO: Need a day with a calibration, but won't worry about until
	 * Calibration metric is working.
	 */
	@Test
	public final void testGetChannelCalDataChannel() throws Exception {
		ArrayList<Blockette320> calData;

		calData = data.getChannelCalData(new Channel("00", "LHZ"));
		assertNull(calData);
	}

	@Test
	public final void testGetChannelTimingQualityDataChannel() throws Exception {
		ArrayList<Integer> timingQuality;

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
		MetricData metricData = new MetricData(database, metadata);

		LocalDate date = LocalDate.parse("2014-07-12");
		Station station = new Station("CU", "BCIP");
		String metricName = "AvailabilityMetric";

		// No data, digest in database, no force update
		Channel channel = new Channel("00", "LHZ");
		ByteBuffer digest = metricData.valueDigestChanged(channel,
				new MetricValueIdentifier(date, metricName, station, channel), false);
		assertNull(digest);

		// No data, digest not in database, no force update
		// Should come back with something.
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
		database.setConnected(false);
		digest = metricData.valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
				false);
		assertNotNull(digest);

		database.setConnected(true);

		// DATA
		metricData = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz", true);
		metricData.setMetricReader(database);
		date = LocalDate.parse("2015-08-16");

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
		database.setConnected(false);
		digest = metricData.valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
				false);
		assertNotNull(digest);
		database.setConnected(true);

		// Data, digest mismatch in Reader, no force update
		channel = new Channel("00", "LH2");
		digest = metricData.valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
				false);
		assertNotNull(digest);

	}
}
