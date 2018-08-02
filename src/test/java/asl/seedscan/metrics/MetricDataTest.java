package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;
import asl.seedscan.database.MetricDatabaseMock;
import asl.seedscan.database.MetricValueIdentifier;
import asl.seedsplitter.DataSet;
import asl.testutils.ResourceManager;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.xml.bind.DatatypeConverter;
import org.junit.BeforeClass;
import org.junit.Test;
import seed.Blockette320;

public class MetricDataTest {

  private static MetricDatabaseMock database;
  private static MetricData data;
  private static StationMeta metadata;


  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    data = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
    metadata = (StationMeta) ResourceManager
        .loadCompressedObject("/java_serials/metadata/CU.BCIP.2015.228.StationMeta.ser.gz", false);
    database = new MetricDatabaseMock();

    // BCIP - Digest and data
    LocalDate expectDate = LocalDate.parse("2015-08-16");
    Station expectStation = new Station("CU", "BCIP");
    String expectMetricName = "AvailabilityMetric";

    Channel expectChannel = new Channel("00", "LHZ");
    database.insertMockData(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
        35.123456, ByteBuffer.wrap("Same".getBytes()));

    expectChannel = new Channel("00", "LH1");
    database.insertMockData(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
        99.999, ByteBuffer.wrap("Same".getBytes()));

    expectChannel = new Channel("00", "LH2");
    database.insertMockData(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel), 0.00,
        ByteBuffer.wrap("Different".getBytes()));

    expectMetricName = "AnyMetric";
    expectChannel = new Channel("00", "LHZ");
    database.insertMockData(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
        35.123456, ByteBuffer.wrap("Same1".getBytes()));

    expectChannel = new Channel("00", "LH1");
    database.insertMockData(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
        99.999, ByteBuffer.wrap("Same1".getBytes()));

    expectChannel = new Channel("00", "LH2");
    database.insertMockData(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel), 0.00,
        ByteBuffer.wrap("Different1".getBytes()));

    // ANMO - Digest only tests
    expectMetricName = "AvailabilityMetric";
    expectDate = LocalDate.parse("2015-07-25");
    expectStation = new Station("IU", "ANMO");

    expectChannel = new Channel("10", "BH1"); // Precomputed the digest
    database.insertMockDigest(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("9A4FE3A10FD60F93526F464B0DB9580E")));
    expectChannel = new Channel("00", "LH2");
    database.insertMockDigest(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
        ByteBuffer.wrap("Different".getBytes()));

    expectMetricName = "AnyMetric";
    expectChannel = new Channel("10", "BH1"); // Precomputed the digest
    database.insertMockDigest(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("9A4FE3A10FD60F93526F464B0DB9580E")));
    expectChannel = new Channel("00", "LH2");
    database.insertMockDigest(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
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
  public final void testGetMetaData_BasicInformationCorrect() throws Exception {
    StationMeta metadata = data.getMetaData();
    assertNotNull(metadata);
    assertEquals("2015:206", metadata.getDate());
    assertEquals((Double) 1820.0, (Double) metadata.getElevation());
    assertEquals("IU_ANMO", metadata.toString());
  }

  @Test
  public final void testHasChannels_Exist() throws Exception {
    // Should exist
    assertTrue(data.hasChannels("00", "LH"));
    assertTrue(data.hasChannels("10", "BH"));
    // Double Check
    assertTrue(data.hasChannels("10", "BH"));
  }

  @Test
  public final void testHasChannels_Nonexistent() throws Exception {
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
  public final void testGetMetricValue_KnownValues() throws Exception {
    MetricData metricData = new MetricData(database, metadata);

    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "AvailabilityMetric";
    Channel channel = new Channel("00", "LHZ");

    double value = metricData.getMetricValue(date, metricName, station, channel);

    //Round to 7 places to match the Metric injector
    Double expected = (double) Math.round(35.123456 * 1000000d) / 1000000d;
    Double resulted = (double) Math.round(value * 1000000d) / 1000000d;
    assertEquals(expected, resulted);
  }

  @Test
  public final void testGetMetricValue_Disconnected() throws Exception {
    //Mock Data
    MetricDatabaseMock tempDatabase = new MetricDatabaseMock();
    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "RandomMetric";

    Channel channel = new Channel("00", "LHZ");
    tempDatabase.insertMockData(
        new MetricValueIdentifier(date, metricName, station, channel),
        35.123456, ByteBuffer.wrap("OtherBytes".getBytes()));
    // Check disconnected reader

    MetricData metricData = new MetricData(tempDatabase, metadata);
    //Should be there
    assertNotNull(metricData.getMetricValue(date, metricName, station, channel));

    //Disconnect
    tempDatabase.setConnected(false);
    //Now it shouldn't
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

  /*
   * The tests for valueDigestChanged follow this pattern:
   * Data |DigestInDB |ForceUpdate|AvailabilityMetric |Expect Null response
   * 0    |0          |0          |0                  |1
   * 0    |0          |0          |1                  |0
   * 0    |0          |1          |0                  |1
   * 0    |0          |1          |1                  |0
   * 0    |1          |0          |0                  |1
   * 0    |1          |0          |1                  |1
   * 0    |1          |1          |0                  |1
   * 0    |1          |1          |1                  |0
   */
  @Test
  public final void testValueDigestChanged_NoData_NoDigestDatabase_NoForceUpdate_NotAvailability()
      throws Exception {
    MetricData metricData = new MetricData(database, metadata);

    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "NotAvailMetric";
    Channel channel = new Channel("00", "BH1"); // Not set in reader
    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            false);
    //Nothing to compute so null
    assertNull(digest);
  }

  @Test
  public final void testValueDigestChanged_NoData_NoDigestDatabase_NoForceUpdate_Availability()
      throws Exception {
    MetricData metricData = new MetricData(database, metadata);

    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "AvailabilityMetric";
    Channel channel = new Channel("00", "BH1"); // Not set in reader
    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            false);
    //Availability computes regardless of No Data so not null
    assertNotNull(digest);
  }

  @Test
  public final void testValueDigestChanged_NoData_NoDigestDatabase_ForceUpdate_NotAvailability()
      throws Exception {
    MetricData metricData = new MetricData(database, metadata);

    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "AnyMetric";

    Channel channel = new Channel("00", "BH1"); // Not set in reader
    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            true);
    //No data so nothing to compute
    assertNull(digest);
  }

  @Test
  public final void testValueDigestChanged_NoData_NoDigestDatabase_ForceUpdate_Availability()
      throws Exception {
    MetricData metricData = new MetricData(database, metadata);

    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "AvailabilityMetric";

    Channel channel = new Channel("00", "BH1"); // Not set in reader
    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            true);
    //Availability with forced update, should compute
    assertNotNull(digest);
  }


  @Test
  public final void testValueDigestChanged_NoData_DigestDatabase_NoForceUpdate_NotAvailability()
      throws Exception {
    MetricData metricData = new MetricData(database, metadata);

    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "AnyMetric";

    Channel channel = new Channel("00", "LHZ");
    ByteBuffer digest = metricData.valueDigestChanged(channel,
        new MetricValueIdentifier(date, metricName, station, channel), false);
    //No data and not availability don't care if already computed, don't recompute.
    assertNull(digest);
  }

  @Test
  public final void testValueDigestChanged_NoData_DigestDatabase_NoForceUpdate_Availability()
      throws Exception {
    MetricData metricData = new MetricData(database, metadata);

    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "AvailabilityMetric";

    Channel channel = new Channel("00", "LHZ");
    ByteBuffer digest = metricData.valueDigestChanged(channel,
        new MetricValueIdentifier(date, metricName, station, channel), false);
    //Should be null, since database has availability, it might be temporary data.
    assertNull(digest);
  }

  @Test
  public final void testValueDigestChanged_NoData_DigestDatabase_ForceUpdate_NotAvailability()
      throws Exception {
    MetricData metricData = new MetricData(database, metadata);

    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "AnyMetric";

    Channel channel = new Channel("00", "LHZ");
    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            true);
    //No data, can't compute
    assertNull(digest);
  }

  @Test
  public final void testValueDigestChanged_NoData_DigestDatabase_ForceUpdate_Availability()
      throws Exception {
    MetricData metricData = new MetricData(database, metadata);

    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "AvailabilityMetric";

    Channel channel = new Channel("00", "LHZ");
    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            true);
    //Force Update overrides digest in database should recompute.
    assertNotNull(digest);
  }

  /*
   * Disconnected Database test without data.
   */
  @Test
  public final void testValueDigestChanged_NoData_DatabaseDisconnected_NoForceUpdate_NotAvailability()
      throws Exception {
    //Mock Data
    MetricDatabaseMock tempDatabase = new MetricDatabaseMock();
    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    /*Metric name should match exact this should not count as AvailabilityMetric*/
    String metricName = "NotAvailabilityMetric";

    Channel channel = new Channel("00", "LHZ");
    tempDatabase.insertMockData(
        new MetricValueIdentifier(date, metricName, station, channel),
        35.123456, ByteBuffer.wrap("OtherBytes".getBytes()));
    MetricData metricData = new MetricData(tempDatabase, metadata);

    tempDatabase.setConnected(false);

    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            false);
    //No data, not Availability, can't compute
    assertNull(digest);
  }

  @Test
  public final void testValueDigestChanged_NoData_DatabaseDisconnected_NoForceUpdate_Availability()
      throws Exception {
    //Mock Data
    MetricDatabaseMock tempDatabase = new MetricDatabaseMock();
    LocalDate date = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "AvailabilityMetric";

    Channel channel = new Channel("00", "LHZ");
    tempDatabase.insertMockData(
        new MetricValueIdentifier(date, metricName, station, channel),
        35.123456, ByteBuffer.wrap("OtherBytes".getBytes()));
    MetricData metricData = new MetricData(tempDatabase, metadata);
    // No data, digest in Reader, reader disconnected Non Availability Metric
    tempDatabase.setConnected(false);
    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            false);
    //This should exist because it is availability
    assertNotNull(digest);
  }


  @Test
  public final void testValueDigestChanged_Data_NoDigestDatabase_NoForceUpdate() throws Exception {
    MetricData metricData = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", true);
    metricData.setMetricReader(database);
    LocalDate date = LocalDate.parse("2015-08-16");

    Station station = new Station("IU", "ANMO");
    String metricName = "AnyMetric";
    Channel channel = new Channel("10", "BH2");
    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            false);
    assertNotNull(digest);
  }

  @Test
  public final void testValueDigestChanged_Data_MatchDigestDatabase_NoForceUpdate()
      throws Exception {
    MetricData metricData = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", true);
    metricData.setMetricReader(database);
    LocalDate date = LocalDate.parse("2015-07-25");

    Station station = new Station("IU", "ANMO");
    String metricName = "AnyMetric";
    Channel channel = new Channel("10", "BH1");
    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            false);
    //Digest match don't recompute
    assertNull(digest);
  }

  @Test
  public final void testValueDigestChanged_Data_MatchDigestDatabase_ForceUpdate() throws Exception {
    MetricData metricData = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", true);
    metricData.setMetricReader(database);
    LocalDate date = LocalDate.parse("2015-07-25");

    Station station = new Station("IU", "ANMO");
    String metricName = "AnyMetric";
    Channel channel = new Channel("10", "BH1");

    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            true);
    //Force update, so recompute
    assertNotNull(digest);
  }

  @Test
  public final void testValueDigestChanged_Data_MismatchDigestDatabase_NoForceUpdate()
      throws Exception {
    MetricData metricData = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", true);
    metricData.setMetricReader(database);
    LocalDate date = LocalDate.parse("2015-08-16");

    Station station = new Station("IU", "ANMO");
    String metricName = "AnyMetric";

    Channel channel = new Channel("00", "LH2");
    ByteBuffer digest = metricData
        .valueDigestChanged(channel, new MetricValueIdentifier(date, metricName, station, channel),
            false);
    //Digest Mismatch recompute
    assertNotNull(digest);
  }
}
