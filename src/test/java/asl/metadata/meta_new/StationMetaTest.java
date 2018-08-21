package asl.metadata.meta_new;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.ChannelKey;
import asl.metadata.Station;
import asl.seedscan.metrics.MetricData;
import asl.testutils.ResourceManager;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Hashtable;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class StationMetaTest {

  private static MetricData data1;
  private static MetricData maleabledata1;
  private static MetricData data2;
  private static MetricData maleabledata3;

  private static StationMeta metadata1;
  private static StationMeta maleablemetadata1;
  private static StationMeta metadata2;
  private static StationMeta maleablemetadata3;

  //This is not static and can change underneath the tests.
  private static StationMeta maleabledata4;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    data1 = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.NWAO.2015.299.MetricData.ser.gz", false);
    maleabledata1 = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.NWAO.2015.299.MetricData.ser.gz", true);
    data2 = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/GS.OK029.2015.360.MetricData.ser.gz", false);
    maleabledata3 = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", true);
    maleabledata4 = ((MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/GS.OK029.2015.360.MetricData.ser.gz", true)).getMetaData();

    ChannelMeta channelMeta = new ChannelMeta(new ChannelKey("51", "HHE"),
        maleabledata4.getTimestamp(), new Station("GS", "OK029"));
    maleabledata4.addChannel(new ChannelKey("51", "HHE"), channelMeta);

    metadata1 = data1.getMetaData();
    maleablemetadata1 = maleabledata1.getMetaData();
    metadata2 = data2.getMetaData();
    maleablemetadata3 = maleabledata3.getMetaData();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    data1 = null;
    maleabledata1 = null;
    data2 = null;
    maleabledata3 = null;
    maleabledata4 = null;

    metadata1 = null;
    maleablemetadata1 = null;
    metadata2 = null;
    maleablemetadata3 = null;
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
    assertEquals(new Integer(35), new Integer(maleablemetadata1.getNumberOfChannels()));
    assertEquals(new Integer(12), new Integer(metadata2.getNumberOfChannels()));
  }

  @Test
  public final void testGetTimestamp() throws Exception {
    LocalDateTime cal1 = LocalDate.of(2015, 10, 26).atStartOfDay();
    LocalDateTime cal2 = LocalDate.of(2015, 12, 26).atStartOfDay();

    assertEquals(cal1, metadata1.getTimestamp());
    assertEquals(cal2, metadata2.getTimestamp());
  }

  @Test
  public final void testGetDate() throws Exception {
    assertEquals("2015:299", metadata1.getDate());
    assertEquals("2015:360", metadata2.getDate());
  }

  @Test
  public final void testFindChannelMetadataChannel() throws Exception {
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
    IntBuffer cbuf = buf.asIntBuffer();

    //We expect 4 integers in the buffer
    assertEquals(new Integer(4), new Integer(cbuf.remaining()));

    //Our digest values in testing.
    assertEquals(new Integer(1305518060), new Integer(cbuf.get()));
    assertEquals(new Integer(-354324251), new Integer(cbuf.get()));
    assertEquals(new Integer(-1383591863), new Integer(cbuf.get()));
    assertEquals(new Integer(-1320735307), new Integer(cbuf.get()));
  }

  @Test
  public final void testFindChannelHashTable() throws Exception {
    //10-BH1, 00-VHZ, 00-BH2, 00-BH1, 20-LNZ, 00-VM2, 00-VM1, 20-HNZ, 10-LHZ, 10-VH2, 00-VH2, 00-LHZ, 10-VH1, 00-VH1, 20-LN2, 10-HHZ, 20-LN1
    Hashtable<ChannelKey, ChannelMeta> table = maleablemetadata1.getChannelHashTable();
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
  public final void testFindChannelArrayForIgnoringTriggeredChannels() throws Exception {
    List<Channel> channels = maleablemetadata1.getChannelArray("LH,BH,HH", true, false);
    assertEquals(new Integer(12), new Integer(channels.size()));

    //Should now get 3 more triggered HH channels
    channels = maleablemetadata1.getChannelArray("LH,BH,HH", false, false);
    assertEquals(new Integer(15), new Integer(channels.size()));
  }

  @Test
  public final void testFindChannelArrayForIgnoringDerivedChannels() throws Exception {
    maleablemetadata3.addRotatedChannelMeta("00", "LH");
    maleablemetadata3.addRotatedChannelMeta("10", "LH");

    List<Channel> channels = maleablemetadata3.getChannelArray("LH,BH", false, true);
    assertEquals(new Integer(12), new Integer(channels.size()));

    //Should now get 4 (not 6) more derived channels channels
    //*Z channels should be included regardless as they are always vertical.
    channels = maleablemetadata3.getChannelArray("LH,BH", false, false);
    assertEquals(new Integer(16), new Integer(channels.size()));
  }

  @Test
  public final void testFindChannelArrayForChangingBands() throws Exception {
    List<Channel> channels = maleablemetadata3.getChannelArray("LH,BH", false, true);
    assertEquals(new Integer(12), new Integer(channels.size()));

    //Should not change from last result
    channels = maleablemetadata3.getChannelArray("LH,BH", false, true);
    assertEquals(new Integer(12), new Integer(channels.size()));

    //Should now get HH as well
    channels = maleablemetadata3.getChannelArray("LH,BH,HH", false, true);
    assertEquals(new Integer(15), new Integer(channels.size()));

    //Should only get LH
    channels = maleablemetadata3.getChannelArray("LH", false, true);
    assertEquals(new Integer(6), new Integer(channels.size()));
  }

  @Test
  public final void testFindChannelUsingComponent12() throws Exception {
    assertEquals(new Channel("00", "LH1"), maleablemetadata1.findChannel("00", "LH", "1"));
    assertEquals(new Channel("10", "HH2"), maleablemetadata3.findChannel("10", "HH", "2"));

    //Test that still returns N/E if those exist and not 1/2
    //This channel was added during setup.
    assertEquals(new Channel("51", "HHE"), maleabledata4.findChannel("51", "HH", "2"));

    assertNull(maleablemetadata1.findChannel("30", "LH", "1"));
    assertNull(maleablemetadata1.findChannel("00", "HH", "2"));
  }

  @Test
  public final void testGetContinuousChannels() throws Exception {
    List<Channel> channels = maleablemetadata1.getContinuousChannels();
    // The returned list should be sorted, so having the list be exactly
    // this is valid.
    String expected = "[00-BH1, 00-BH2, 00-BHZ, 00-LH1, 00-LH2, 00-LHZ, 00-VH1, 00-VH2, 00-VHZ, 00-VM1, 00-VM2, 00-VMZ, 10-BH1, 10-BH2, 10-BHZ, 10-LH1, 10-LH2, 10-LHZ, 10-VH1, 10-VH2, 10-VHZ, 10-VMU, 10-VMV, 10-VMW, 20-LN1, 20-LN2, 20-LNZ, 30-LDO, 31-LDO]";

    assertEquals(expected, channels.toString());
  }

  @Test
  public final void testGetRotatableChannels() throws Exception {
    List<Channel> channels = maleablemetadata1.getRotatableChannels();

    assertTrue(channels.contains(new Channel("00", "LHZ")));
    assertTrue(channels.contains(new Channel("00", "LHED")));
    assertTrue(channels.contains(new Channel("00", "LHND")));
    assertTrue(channels.contains(new Channel("00", "BHED")));
    assertTrue(channels.contains(new Channel("00", "BHND")));
    assertTrue(channels.contains(new Channel("00", "BHZ")));
    assertTrue(channels.contains(new Channel("20", "LNZ")));
    assertTrue(channels.contains(new Channel("00", "VHND")));
    assertTrue(channels.contains(new Channel("10", "LHED")));
    assertTrue(channels.contains(new Channel("10", "LHND")));

    assertFalse(channels.contains(new Channel("00", "LH1")));
    assertFalse(channels.contains(new Channel("00", "LH1")));
  }

  @Test
  public final void testHasChannelChannel() throws Exception {
    assertTrue(maleablemetadata1.hasChannel(new Channel("00", "LHZ")));
    assertTrue(maleablemetadata1.hasChannel(new Channel("00", "LH1")));
    assertTrue(maleablemetadata1.hasChannel(new Channel("00", "LH2")));
    assertTrue(maleablemetadata1.hasChannel(new Channel("10", "LH1")));
    assertTrue(maleablemetadata1.hasChannel(new Channel("10", "LH2")));
    assertTrue(maleablemetadata1.hasChannel(new Channel("10", "LHZ")));
    assertTrue(maleablemetadata1.hasChannel(new Channel("00", "VM1")));
    assertTrue(maleablemetadata1.hasChannel(new Channel("00", "VM2")));
    assertTrue(maleablemetadata1.hasChannel(new Channel("00", "VMZ")));
    assertTrue(maleablemetadata1.hasChannel(new Channel("10", "VMU")));

    assertFalse(maleablemetadata1.hasChannel(new Channel("51", "HHE")));
  }

  @Test
  public final void testHasChannelsChannelArray() throws Exception {
    assertTrue(maleablemetadata1.hasChannels(new ChannelArray("10", "VMU", "VMV", "VMW")));
    assertTrue(maleablemetadata1.hasChannels(new ChannelArray("10", "LHZ", "LH1", "LH2")));

    assertFalse(maleablemetadata1.hasChannels(new ChannelArray("10", "LHZ", "LHE", "LHN")));
  }

  @Test
  public final void testHasChannelsStringString() throws Exception {
    assertTrue(maleablemetadata1.hasChannels("00", "BH"));
    assertTrue(maleablemetadata1.hasChannels("00", "LH"));
    assertTrue(maleablemetadata1.hasChannels("20", "HN"));
    assertTrue(maleablemetadata1.hasChannels("00", "VH"));

    assertFalse(maleablemetadata1.hasChannels("30", "LD"));
  }

  @Test
  public final void testAddRotatedChannelMeta() throws Exception {
    assertFalse(maleabledata4.hasChannel(new Channel("00", "LHED")));
    assertFalse(maleabledata4.hasChannel(new Channel("00", "LHND")));

    maleabledata4.addRotatedChannelMeta("00", "LH");

    assertTrue(maleabledata4.hasChannel(new Channel("00", "LHED")));
    assertTrue(maleabledata4.hasChannel(new Channel("00", "LHND")));
    assertTrue(maleabledata4.hasChannel(new Channel("00", "LHZ")));
  }

  @Test
  public final void testToString() throws Exception {
    assertEquals("IU_NWAO", maleablemetadata1.toString());
    assertEquals("GS_OK029", metadata2.toString());
    assertEquals("IU_ANMO", maleablemetadata3.toString());
  }

}
