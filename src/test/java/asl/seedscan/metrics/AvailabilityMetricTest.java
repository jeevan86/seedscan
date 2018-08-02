package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;
import asl.seedscan.database.MetricDatabaseMock;
import asl.seedscan.database.MetricValueIdentifier;
import asl.testutils.ResourceManager;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.HashMap;
import javax.xml.bind.DatatypeConverter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AvailabilityMetricTest {

  private static MetricData maleableData;
  private static MetricData data;
  private static StationMeta metadata;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    data = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
    maleableData = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", true);

    metadata = (StationMeta) ResourceManager
        .loadCompressedObject("/java_serials/metadata/CU.BCIP.2015.228.StationMeta.ser.gz", false);

    MetricDatabaseMock mockdatabase = new MetricDatabaseMock();

    String expectMetricName = "AvailabilityMetric";
    LocalDate expectDate = LocalDate.parse("2015-07-25");
    Station expectStation = new Station("IU", "ANMO");

    Channel expectChannel = new Channel("10", "BH1"); // Precomputed the digest
    mockdatabase.insertMockDigest(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("9A4FE3A10FD60F93526F464B0DB9580E")));
    expectChannel = new Channel("00", "LH2");
    mockdatabase.insertMockDigest(
        new MetricValueIdentifier(expectDate, expectMetricName, expectStation, expectChannel),
        ByteBuffer.wrap("Different".getBytes()));

    maleableData.setMetricReader(mockdatabase);
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    maleableData = null;
    data = null;
  }

  @Test
  public final void testGetName() throws Exception {
    Metric metric = new AvailabilityMetric();
    assertEquals("AvailabilityMetric", metric.getName());
  }

  @Test
  public final void testGetVersion() throws Exception {
    Metric metric = new AvailabilityMetric();
    assertEquals(1, metric.getVersion());
  }

  @Test
  public final void testProcess_HasData_NoDB() throws Exception {
    Metric metric = new AvailabilityMetric();
    metric.setData(data);

    /* Should be no HN data in here as it is Triggered*/
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,BH2", 99.99994212966313);
    expect.put("00,BH1", 99.99994212966313);
    expect.put("35,LDO", 99.99884260598836);
    expect.put("00,VM2", 99.9884272653628);
    expect.put("00,VM1", 99.9884272653628);
    expect.put("10,BHZ", 99.99997106482319);
    expect.put("31,LDO", 99.99884260598836);
    expect.put("50,LKO", 99.99884260598836);
    expect.put("40,LFZ", 99.99884260598836);
    expect.put("50,LWS", 99.99884260598836);
    expect.put("10,VMW", 99.9884272653628);
    expect.put("10,VMV", 99.9884272653628);
    expect.put("10,VH2", 99.9884272653628);
    expect.put("10,VMU", 99.9884272653628);
    expect.put("10,VH1", 99.9884272653628);
    expect.put("00,VHZ", 99.9884272653628);
    expect.put("50,LWD", 99.99884260598836);
    expect.put("10,LH2", 99.99884260598836);
    expect.put("10,LH1", 99.99884260598836);
    expect.put("30,LDO", 99.99884260598836);
    expect.put("00,LHZ", 99.99884260598836);
    expect.put("50,LDO", 99.99884260598836);
    expect.put("60,HDF", 99.99998842592727);
    expect.put("20,LNZ", 99.99884260598836);
    expect.put("10,BH2", 99.99997106482319);
    expect.put("10,BH1", 99.99997106482319);
    expect.put("40,LF2", 99.99884260598836);
    expect.put("40,LF1", 99.99884260598836);
    expect.put("00,BHZ", 99.99994212966313);
    expect.put("00,VMZ", 99.9884272653628);
    expect.put("00,VH2", 99.9884272653628);
    expect.put("00,VH1", 99.9884272653628);
    expect.put("10,VHZ", 99.9884272653628);
    expect.put("50,LIO", 99.99884260598836);
    expect.put("00,LH2", 99.99884260598836);
    expect.put("00,LH1", 99.99884260598836);
    expect.put("50,LRI", 99.99884260598836);
    expect.put("20,LN2", 99.99884260598836);
    expect.put("50,LRH", 99.99884260598836);
    expect.put("20,LN1", 99.99884260598836);
    expect.put("10,LHZ", 99.99884260598836);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testProcess_HasData_HasDB() throws Exception {
    MetricDatabaseMock database = new MetricDatabaseMock();

    String metricName = "AvailabilityMetric";
    LocalDate expectDate = LocalDate.parse("2015-07-25");
    Station station = new Station("IU", "ANMO");
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "BH2")),
        99.99994212966313,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("D687B51EE28BC813C3C6E53B979C216B")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "BH1")),
        99.99994212966313,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("58C35785DA33A8635340823CCA925311")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("35", "LDO")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("91693B82D42C10253509442F7F582A6A")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "VM2")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("22FE293A395EF3A98167DFBCD77DA3EA")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "VM1")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("5CC3259EB1D31B7775C1FA6270820A00")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "BHZ")),
        99.99997106482319,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("1A68AF2C463A120AAB9C50C0AACBE96F")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("31", "LDO")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("706C7EF211DC9FDB8BBCDBAF8A7C8A08")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("50", "LKO")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("1DF16F0E944203BF98B67225AC1B1197")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("40", "LFZ")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("F2B4B80E3B4265916797F13C484F38B5")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("50", "LWS")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("624297B93BB8A73DCB30D47A65B46FEB")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "VMW")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("59FA283BCB795EC19D3C97164D714CA1")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "VMV")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("59FA283BCB795EC19D3C97164D714CA1")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "VH2")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("874B8C3995D3C2E5DDE2B0624A88BEA1")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "VMU")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("59FA283BCB795EC19D3C97164D714CA1")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "VH1")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("AB78D49E0A7B2ED7B429722A59B967B5")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "VHZ")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("D29C08638F03DB65EC0D3474B11BFFBF")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("50", "LWD")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("B2C4D340C8BD97C1A71EF5F8976DB826")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "LH2")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("8559C2C0678FF357C1A59D59090F0275")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "LH1")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("EEA71987E29DC2CCF4353D7E0F84B779")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("30", "LDO")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("850A70BFA1D6E335526BE90DAD8CF143")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "LHZ")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("A973C1906ECEBAE34032D65862A6D512")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("50", "LDO")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("ECC3468EADF4B7CCF988A351B503DC9F")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("60", "HDF")),
        99.99998842592727,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("F888F0E30CD74769266224887861CA4F")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("20", "LNZ")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("F4A5A40968D72ED72A6766456DE7BF98")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "BH2")),
        99.99997106482319,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("EF4E2DC8E560F6C49BE602C5E388311D")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "BH1")),
        99.99997106482319,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("9A4FE3A10FD60F93526F464B0DB9580E")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("40", "LF2")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("E5C1E03F1FFCEAD3DE2D2AAE6E01AED5")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("40", "LF1")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("14940D9DE7129CF90F0A8772A9CA2A83")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "BHZ")),
        99.99994212966313,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("91EB44261E0999FB195A174CE22A9F9C")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "VMZ")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("D26C89E8FA1BCFFB182C1FED512C4051")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "VH2")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("AC22DB436084BE7A3631AE21E6870797")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "VH1")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("C2C62588C120D719B2A6020E675FF0CC")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "VHZ")),
        99.9884272653628,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("21ACD71487DFEE6E2E88578340B682F0")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("50", "LIO")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("62ECE2B352AB4C0B016349F5AC6A462F")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "LH2")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("2F9623A85AD7D9F50CD73336102C8E42")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "LH1")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("18DB0882F04A980F0D3519D54F2A2A9D")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("50", "LRI")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("632CAC2ACB4DEECCB2695D03D9363A36")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("20", "LN2")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("59FCCB8C565869B98C84B7FD1EAC7814")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("50", "LRH")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("632CAC2ACB4DEECCB2695D03D9363A36")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("20", "LN1")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("1F7E79381FADB2B54A9B1BE73470A76F")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("10", "LHZ")),
        99.99884260598836,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("EECF1753EBA6A435D851081EE39B372E")));

    Metric metric = new AvailabilityMetric();
    MetricData metricData = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", true);
    metricData.setMetricReader(database);
    metric.setData(metricData);

    HashMap<String, Double> expect = new HashMap<>();
    //All Digests should match so no new results.
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testProcess_NoData_NoDB() throws Exception {
    Metric metric = new AvailabilityMetric();
    MetricData metricData = new MetricData(new MetricDatabaseMock(), metadata);
    metric.setData(metricData);
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("20,LNZ", 0.0);
    expect.put("00,VMW", 0.0);
    expect.put("00,BH2", 0.0);
    expect.put("00,VMV", 0.0);
    expect.put("00,BH1", 0.0);
    expect.put("00,VMU", 0.0);
    expect.put("00,LH2", 0.0);
    expect.put("00,LH1", 0.0);
    expect.put("00,BHZ", 0.0);
    expect.put("00,LHZ", 0.0);
    expect.put("20,LN2", 0.0);
    expect.put("20,LN1", 0.0);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testProcess_NoData_HasDB() throws Exception {
    MetricDatabaseMock database = new MetricDatabaseMock();
    LocalDate expectDate = LocalDate.parse("2015-08-16");
    Station station = new Station("CU", "BCIP");
    String metricName = "AvailabilityMetric";
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("20", "LNZ")),
        100.0,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("6E04CC6659D82C1DC784A36371BF0335")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "VMW")),
        99.9,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("C8280EC927F64731B993EFA825D52929")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "BH2")),
        98.0,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("828D4BDF7B8194E4F721B6D701C6ED44")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "VMV")),
        40.5,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("C8280EC927F64731B993EFA825D52929")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "BH1")),
        35.5,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("828D4BDF7B8194E4F721B6D701C6ED44")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "VMU")),
        10.23,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("C8280EC927F64731B993EFA825D52929")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "LH2")),
        0.0, ByteBuffer.wrap(DatatypeConverter.parseHexBinary("346999B1DBB719CCD2E117DC95832B83")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "LH1")),
        78.9,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("346999B1DBB719CCD2E117DC95832B83")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "BHZ")),
        5.0, ByteBuffer.wrap(DatatypeConverter.parseHexBinary("828D4BDF7B8194E4F721B6D701C6ED44")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("00", "LHZ")),
        10.0,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("346999B1DBB719CCD2E117DC95832B83")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("20", "LN2")),
        100.0,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("6E04CC6659D82C1DC784A36371BF0335")));
    database.insertMockData(
        new MetricValueIdentifier(expectDate, metricName, station, new Channel("20", "LN1")),
        100.0,
        ByteBuffer.wrap(DatatypeConverter.parseHexBinary("6E04CC6659D82C1DC784A36371BF0335")));

    Metric metric = new AvailabilityMetric();
    MetricData metricData = new MetricData(database, metadata);
    metric.setData(metricData);

    HashMap<String, Double> expect = new HashMap<>();
    /* Should have no new results. Availability doesn't replace existing values if it has no data.*/
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testProcess_NoDerivedChannels_NoTriggered() throws Exception {
    Metric metric = new AvailabilityMetric();
    MetricData metricData = (MetricData) ResourceManager
        .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", true);

    //Rotate and add Derived channels to metricData
    metricData.checkForRotatedChannels(new ChannelArray("00", "LHND", "LHED", "LHZ"));
    metric.setData(metricData);

    /* Should be no Triggered HN or HH channels*/
    /* Should be no Derived channels*/
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,BH2", 99.99994212966313);
    expect.put("00,BH1", 99.99994212966313);
    expect.put("35,LDO", 99.99884260598836);
    expect.put("00,VM2", 99.9884272653628);
    expect.put("00,VM1", 99.9884272653628);
    expect.put("10,BHZ", 99.99997106482319);
    expect.put("31,LDO", 99.99884260598836);
    expect.put("50,LKO", 99.99884260598836);
    expect.put("40,LFZ", 99.99884260598836);
    expect.put("50,LWS", 99.99884260598836);
    expect.put("10,VMW", 99.9884272653628);
    expect.put("10,VMV", 99.9884272653628);
    expect.put("10,VH2", 99.9884272653628);
    expect.put("10,VMU", 99.9884272653628);
    expect.put("10,VH1", 99.9884272653628);
    expect.put("00,VHZ", 99.9884272653628);
    expect.put("50,LWD", 99.99884260598836);
    expect.put("10,LH2", 99.99884260598836);
    expect.put("10,LH1", 99.99884260598836);
    expect.put("30,LDO", 99.99884260598836);
    expect.put("00,LHZ", 99.99884260598836);
    expect.put("50,LDO", 99.99884260598836);
    expect.put("60,HDF", 99.99998842592727);
    expect.put("20,LNZ", 99.99884260598836);
    expect.put("10,BH2", 99.99997106482319);
    expect.put("10,BH1", 99.99997106482319);
    expect.put("40,LF2", 99.99884260598836);
    expect.put("40,LF1", 99.99884260598836);
    expect.put("00,BHZ", 99.99994212966313);
    expect.put("00,VMZ", 99.9884272653628);
    expect.put("00,VH2", 99.9884272653628);
    expect.put("00,VH1", 99.9884272653628);
    expect.put("10,VHZ", 99.9884272653628);
    expect.put("50,LIO", 99.99884260598836);
    expect.put("00,LH2", 99.99884260598836);
    expect.put("00,LH1", 99.99884260598836);
    expect.put("50,LRI", 99.99884260598836);
    expect.put("20,LN2", 99.99884260598836);
    expect.put("50,LRH", 99.99884260598836);
    expect.put("20,LN1", 99.99884260598836);
    expect.put("10,LHZ", 99.99884260598836);
    TestUtils.testMetric(metric, expect);
  }
}
