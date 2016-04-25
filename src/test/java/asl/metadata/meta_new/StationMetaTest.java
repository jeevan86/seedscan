package asl.metadata.meta_new;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import asl.metadata.Channel;
import asl.metadata.ChannelKey;
import asl.seedscan.metrics.MetricData;
import asl.util.ResourceManager;

public class StationMetaTest {
	
	private static MetricData data1;
	private static MetricData data2;
	private static MetricData data3;

	private static StationMeta metadata1;
	private static StationMeta metadata2;
	private static StationMeta metadata3;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			data1 = (MetricData) ResourceManager.loadCompressedObject("/data/IU.NWAO.2015.299.MetricData.ser.gz");
			data2 = (MetricData) ResourceManager.loadCompressedObject("/data/GS.OK029.2015.360.MetricData.ser.gz");
			data3 = (MetricData) ResourceManager.loadCompressedObject("/data/IU.ANMO.2015.206.MetricData.ser.gz");
			
			metadata1 = data1.getMetaData();
			metadata2 = data2.getMetaData();
			metadata3 = data3.getMetaData();

		} catch (Exception e) {
			e.printStackTrace();
		}
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

	@Test
	public final void testGetStation() throws Exception {
		assertEquals("NWAO", metadata1.getStation());
		assertEquals("OK029", metadata2.getStation());
	}

	@Test
	public final void testGetNetwork() throws Exception {
		assertEquals("IU", metadata1.getNetwork());
		assertEquals("GS", metadata2.getNetwork());
	}

	@Test
	public final void testGetLatitude() throws Exception {
		assertEquals(new Double(-32.9277), new Double(metadata1.getLatitude()));
		assertEquals(new Double(35.79657), new Double(metadata2.getLatitude()));
	}

	@Test
	public final void testGetLongitude() throws Exception {
		assertEquals(new Double(117.239), new Double(metadata1.getLongitude()));
		assertEquals(new Double(-97.45486), new Double(metadata2.getLongitude()));
	}

	@Test
	public final void testGetElevation() throws Exception {
		assertEquals(new Double(380.0), new Double(metadata1.getElevation()));
		assertEquals(new Double(333.0), new Double(metadata2.getElevation()));
	}

	@Test
	public final void testGetNumberOfChannels() throws Exception {
		assertEquals(new Integer(35), new Integer(metadata1.getNumberOfChannels()));
		assertEquals(new Integer(12), new Integer(metadata2.getNumberOfChannels()));
	}

	@Test
	public final void testGetTimestamp() throws Exception {
		GregorianCalendar cal1 = new GregorianCalendar();
		GregorianCalendar cal2 = new GregorianCalendar();
		cal1.setTimeInMillis(1445817600000l);
		cal2.setTimeInMillis(1451088000000l);
		cal1.setTimeZone(TimeZone.getTimeZone("GMT"));
		cal2.setTimeZone(TimeZone.getTimeZone("GMT"));

		assertEquals(cal1.getTime(), metadata1.getTimestamp().getTime());
		assertEquals(cal2.getTime(), metadata2.getTimestamp().getTime());
	}

	@Test
	public final void testGetDate() throws Exception {
		assertEquals("2015:299", metadata1.getDate());
		assertEquals("2015:360", metadata2.getDate());
	}

	@Test
	public final void testGetChannelMetadataChannel() throws Exception {
		Channel channel = new Channel("00", "LH1");
		ChannelMeta meta = metadata1.getChannelMetadata(channel);
		
		/*
		 * This should only change with different metadata. This isn't an
		 * optimal way of checking that we have the same metadata as it depends
		 * on getting a digest.
		 * 
		 * This could potentially change out from under this class and require
		 * reworking these tests too.
		 */
		ByteBuffer buf = meta.getDigestBytes();
		IntBuffer cbuf =buf.asIntBuffer();

		//We expect 4 integers in the buffer
		assertEquals(new Integer(4), new Integer(cbuf.remaining()));
		
		//Our digest values in testing.
		assertEquals(new Integer(1305518060), new Integer(cbuf.get()));
		assertEquals(new Integer(-354324251), new Integer(cbuf.get()));
		assertEquals(new Integer(-1383591863), new Integer(cbuf.get()));
		assertEquals(new Integer(-1320735307), new Integer(cbuf.get()));
	}

	@Test
	public final void testGetChannelHashTable() throws Exception {
		//10-BH1, 00-VHZ, 00-BH2, 00-BH1, 20-LNZ, 00-VM2, 00-VM1, 20-HNZ, 10-LHZ, 10-VH2, 00-VH2, 00-LHZ, 10-VH1, 00-VH1, 20-LN2, 10-HHZ, 20-LN1
		Hashtable<ChannelKey, ChannelMeta> table = metadata1.getChannelHashTable();
		assertEquals(new Integer(35), new Integer(table.size()));
		
		ChannelKey key = null;
		ChannelMeta meta = null;
		//Check random sample matches our original benchmark.
		key = new ChannelKey("20", "HN2");
		meta = table.get(key);
		assertEquals(new Double(90), new Double(meta.getAzimuth()));
		assertEquals(new Double(100), new Double(meta.getSampleRate()));
		assertEquals(new Integer(3), new Integer(meta.getNumberOfStages()));
		
		key = new ChannelKey("10", "LH2");
		meta = table.get(key);
		assertEquals(new Double(90), new Double(meta.getAzimuth()));
		assertEquals(new Double(1), new Double(meta.getSampleRate()));
		assertEquals(new Integer(3), new Integer(meta.getNumberOfStages()));

		key = new ChannelKey("10", "BHZ");
		meta = table.get(key);
		assertEquals(new Double(0), new Double(meta.getAzimuth()));
		assertEquals(new Double(40), new Double(meta.getSampleRate()));
		assertEquals(new Integer(3), new Integer(meta.getNumberOfStages()));
		
		key = new ChannelKey("31", "LDO");
		meta = table.get(key);
		assertEquals(new Double(0), new Double(meta.getAzimuth()));
		assertEquals(new Double(1), new Double(meta.getSampleRate()));
		assertEquals(new Integer(1), new Integer(meta.getNumberOfStages()));
		
		key = new ChannelKey("00", "LH1");
		meta = table.get(key);
		assertEquals(new Double(79), new Double(meta.getAzimuth()));
		assertEquals(new Double(1), new Double(meta.getSampleRate()));
		assertEquals(new Double(105), new Double(meta.getDepth()));
		assertEquals(new Integer(3), new Integer(meta.getNumberOfStages()));
		
		key = new ChannelKey("10", "HH1");
		meta = table.get(key);
		assertEquals(new Double(0), new Double(meta.getAzimuth()));
		assertEquals(new Double(100), new Double(meta.getSampleRate()));
		assertEquals(new Integer(3), new Integer(meta.getNumberOfStages()));
		
		key = new ChannelKey("10", "VMW");
		meta = table.get(key);
		assertEquals(new Double(0), new Double(meta.getAzimuth()));
		assertEquals(new Double(0.1), new Double(meta.getSampleRate()));
		assertEquals(new Integer(1), new Integer(meta.getNumberOfStages()));
		
		key = new ChannelKey("10", "VHZ");
		meta = table.get(key);
		assertEquals(new Double(0), new Double(meta.getAzimuth()));
		assertEquals(new Double(0.1), new Double(meta.getSampleRate()));
		assertEquals(new Double(-90), new Double(meta.getDip()));
		assertEquals(new Integer(3), new Integer(meta.getNumberOfStages()));
	}

	@Test
	public final void testGetChannelArrayForIgnoringTriggeredChannels() throws Exception {
		List<Channel> channels = metadata1.getChannelArray("LH,BH,HH", true, false);
		assertEquals(new Integer(12), new Integer(channels.size()));

		//Should now get 3 more triggered HH channels
		channels = metadata1.getChannelArray("LH,BH,HH", false, false);
		assertEquals(new Integer(15), new Integer(channels.size()));
	}
	
	@Test
	public final void testGetChannelArrayForIgnoringDerivedChannels() throws Exception {
		List<Channel> channels = metadata3.getChannelArray("LH,BH", false, true);
		assertEquals(new Integer(12), new Integer(channels.size()));
		
		//Should now get 4 (not 6) more derived channels channels
		//*Z channels should be included regardless as they are always vertical.
		channels = metadata3.getChannelArray("LH,BH", false, false);
		assertEquals(new Integer(16), new Integer(channels.size()));
	}
	
	@Test
	public final void testGetChannelArrayForChangingBands() throws Exception {
		List<Channel> channels = metadata3.getChannelArray("LH,BH", false, true);
		assertEquals(new Integer(12), new Integer(channels.size()));
		
		//Should not change from last result
		channels = metadata3.getChannelArray("LH,BH", false, true);
		assertEquals(new Integer(12), new Integer(channels.size()));
		
		//Should now get HH as well
		channels = metadata3.getChannelArray("LH,BH,HH", false, true);
		assertEquals(new Integer(15), new Integer(channels.size()));
		
		//Should only get LH
		channels = metadata3.getChannelArray("LH", false, true);
		assertEquals(new Integer(6), new Integer(channels.size()));
	}

	@Test
	public final void testGetChannelUsingComponent12() throws Exception {
		assertEquals(new Channel("00", "LH1"), metadata1.getChannel("00", "LH", "1"));
		assertEquals(new Channel("00", "HH2"), metadata1.getChannel("00", "HH", "2"));
		
		assertNull(metadata1.getChannel("30", "LH", "1"));
		assertNull(metadata1.getChannel("11", "HH", "2"));

	}

	@Test
	public final void testGetContinuousChannels() throws Exception {
		throw new RuntimeException("not yet implemented");
	}

	@Test
	public final void testGetRotatableChannels() throws Exception {
		throw new RuntimeException("not yet implemented");
	}

	@Test
	public final void testHasChannelChannel() throws Exception {
		throw new RuntimeException("not yet implemented");
	}

	@Test
	public final void testHasChannelsChannelArray() throws Exception {
		throw new RuntimeException("not yet implemented");
	}

	@Test
	public final void testHasChannelsStringString() throws Exception {
		throw new RuntimeException("not yet implemented");
	}

	@Test
	public final void testAddRotatedChannelMeta() throws Exception {
		throw new RuntimeException("not yet implemented");
	}

	@Test
	public final void testPrintStationInfo() throws Exception {
		throw new RuntimeException("not yet implemented");
	}

	@Test
	public final void testToString() throws Exception {
		throw new RuntimeException("not yet implemented");
	}

}
