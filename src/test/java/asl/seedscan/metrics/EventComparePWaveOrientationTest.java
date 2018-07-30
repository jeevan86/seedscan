package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;
import java.util.HashMap;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import asl.metadata.Station;
import asl.seedscan.event.EventLoader;
import asl.testutils.ResourceManager;

public class EventComparePWaveOrientationTest {


  private EventComparePWaveOrientation metric;
  private static MetricData data, data2;
  private static EventLoader eventLoader;
  private static Station station;
  private static LocalDate dataDate;


  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    try {
      String metadataLocation = "/metadata/rdseed/IU-ANMO-ascii.txt";
      String seedDataLocation = "/seed_data/IU_ANMO/2018/010";
      station = new Station("IU", "ANMO");
      dataDate = LocalDate.of(2018, 1, 10);
      data = ResourceManager.getMetricData(seedDataLocation, metadataLocation, dataDate, station);
      data2 = (MetricData) ResourceManager
          .loadCompressedObject("/java_serials/data/IU.TUC.2018.023.ser.gz", false);
      eventLoader = new EventLoader(ResourceManager.getDirectoryPath("/event_synthetics"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    data = null;
    data2 = null;
    eventLoader = null;
  }

  @Test
  public void testProcessDefault() throws Exception {
    metric = new EventComparePWaveOrientation();
    metric.setData(data);
    metric.setEventTable(eventLoader.getDayEvents(dataDate));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station));
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,LHND", -3.3598227091923576);
    expect.put("10,LHND", -0.43859398446427633);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcessDefaultData2() throws Exception {
    metric = new EventComparePWaveOrientation();
    metric.setData(data2);
    LocalDate date = LocalDate.of(2018, 1, 23);
    metric.setEventTable(eventLoader.getDayEvents(date));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(date, new Station("IU", "TUC")));
    HashMap<String, Double> expect = new HashMap<>();
    // expect.put("00,LHND", -6.2504661649869036);
    expect.put("10,LHND", -0.15767736353728878);
    // expect.put("60,LHND", -0.5888912051460125);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcessCustomConfig() throws Exception {
    metric = new EventComparePWaveOrientation();

    //Not a strong motion comparison, but that is not what we are testing.
    //Only care if the custom channel is set.
    metric.add("base-channel", "XX-LX");
    metric.add("channel-restriction", "LH");

    metric.setData(data);
    metric.setEventTable(eventLoader.getDayEvents(dataDate));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station));
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,LHND", -3.3598227091923576);
    expect.put("10,LHND", -0.43859398446427633);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcessCustomConfigMissingSynthetic() throws Exception {
    metric = new EventComparePWaveOrientation();

    //Not a strong motion comparison, but that is not what we are testing.
    //Only care if the custom channel is set.
    metric.add("base-channel", "XX-BH");
    metric.add("channel-restriction", "BH");

    metric.setData(data);
    metric.setEventTable(eventLoader.getDayEvents(dataDate));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station));
    HashMap<String, Double> expect = new HashMap<>();
    // expect should be empty because no BH synthetic data exists!
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testGetVersion() throws Exception {
    metric = new EventComparePWaveOrientation();
    assertEquals(1, metric.getVersion());
  }

  @Test
  public final void testGetName() throws Exception {
    metric = new EventComparePWaveOrientation();
    assertEquals("EventComparePWaveOrientation", metric.getName());
  }

  @Test
  public final void testCalculateCorrectSampleCount() {
    int secs = 20;
    double sampleRate = 15;
    int samples = EventComparePWaveOrientation.getSamplesInTimePeriod(secs, sampleRate);
    assertEquals(300, samples);
  }

  @Test
  public final void testGetCorrectPairedChannelNameNorthDerived() throws MetricException {
    String init = "10-LHND";
    String result = EventComparePWaveOrientation.getPairedChannelNameString(init);
    assertTrue(result.equals("10-LHED"));
  }

  @Test
  public final void testGetCorrectPairedChannelNameEastDerived() throws MetricException {
    String init = "10-LHED";
    String result = EventComparePWaveOrientation.getPairedChannelNameString(init);
    assertTrue(result.equals("10-LHND"));
  }

  @Test
  public final void testGetCorrectPairedChannelNameNorth() throws MetricException {
    String init = "10-LHN";
    String result = EventComparePWaveOrientation.getPairedChannelNameString(init);
    assertTrue(result.equals("10-LHE"));
  }

  @Test
  public final void testGetCorrectPairedChannelNameEast() throws MetricException {
    String init = "00-LHE";
    String result = EventComparePWaveOrientation.getPairedChannelNameString(init);
    assertTrue(result.equals("00-LHN"));
  }

  @Test(expected = MetricException.class)
  public final void testGetCorrectPairedChannelNameException() throws MetricException {
    String init = "10-LH1";
    EventComparePWaveOrientation.getPairedChannelNameString(init);
  }

}
