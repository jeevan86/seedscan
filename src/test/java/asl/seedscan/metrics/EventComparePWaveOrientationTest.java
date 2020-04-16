package asl.seedscan.metrics;

import static asl.seedscan.metrics.EventComparePWaveOrientation.correctBackAzimuthQuadrant;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.seedscan.event.EventLoader;
import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.cglib.core.Local;

public class EventComparePWaveOrientationTest {


  private EventComparePWaveOrientation metric;
  private static MetricData data, data2, data3, data4;
  private static EventLoader eventLoader;
  private static LocalDate dataDate = LocalDate.ofYearDay(2018, 10);
  private static LocalDate tucDate =  LocalDate.ofYearDay(2018, 23);
  private static Station station1, station2, station3, station4;


  @BeforeClass
  public static void setUpBeforeClass() {
    try {
      String metadataLocation = "/metadata/rdseed/IU-ANMO-ascii.txt";
      String seedDataLocation = "/seed_data/IU_ANMO/2018/010";

      String metadataLocation2 = "/metadata/rdseed/IU-TUC-LH-ascii.txt";
      String seedDataLocation2 = "/seed_data/IU_TUC/2018/023";

      String metadataLocation3 = "/metadata/rdseed/IU-SSPA-ascii.txt";
      String seedDataLocation3 = "/seed_data/IU_SSPA/2018/010";

      String metadataLocation4 = "/metadata/rdseed/IU-RAR-ascii-LH.txt";
      String seedDataLocation4 = "/seed_data/IU_RAR/2018/010";

      String networkName = "IU";

      station1 = new Station(networkName, "ANMO");
      station2 = new Station(networkName, "TUC");
      station3 = new Station(networkName, "SSPA");
      station4 = new Station(networkName, "RAR");
      data = ResourceManager.getMetricData(seedDataLocation, metadataLocation, dataDate, station1);
      data2 = ResourceManager.getMetricData(seedDataLocation2, metadataLocation2, tucDate, station2);
      data3 = ResourceManager.getMetricData(seedDataLocation3, metadataLocation3, dataDate, station3);
      data4 = ResourceManager.getMetricData(seedDataLocation4, metadataLocation4, dataDate, station4);
      eventLoader = new EventLoader(ResourceManager.getDirectoryPath("/event_synthetics"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() {
    data = null;
    data2 = null;
    data3 = null;
    data4 = null;
    station1 = null;
    station2 = null;
    station3 = null;
    station4 = null;
    eventLoader = null;
    dataDate = null;
    tucDate = null;
  }

  @Test
  public void testProcessDefault() {
    metric = new EventComparePWaveOrientation();
    metric.setData(data);
    metric.setEventTable(eventLoader.getDayEvents(dataDate));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station1));
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,LHND", -2.387, 1E-3);
    expect.put("10,LHND", 0.429, 1E-3);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcess90DegreesOut() throws Exception {
    metric = new EventComparePWaveOrientation();
    metric.setData(data3);
    metric.setEventTable(eventLoader.getDayEvents(dataDate));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station3));
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,LHND", -1.197, 1E-3);
    expect.put("10,LHND", -0.189, 1E-3);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcessDefaultData2() {
    metric = new EventComparePWaveOrientation();
    metric.setData(data2);
    metric.setEventTable(eventLoader.getDayEvents(tucDate));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(tucDate, station2));
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,LHND", -6.299, 1E-3);
    expect.put("10,LHND", -0.238, 1E-3);
    expect.put("60,LHND", -0.662, 1E-3);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcessDefaultData4() {
    metric = new EventComparePWaveOrientation();
    metric.setData(data4);
    metric.setEventTable(eventLoader.getDayEvents(dataDate));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station4));
    MetricTestMap expect = new MetricTestMap();
    expect.put("10,LHND", -2.260, 1E-3);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcessCustomConfig() throws Exception {
    metric = new EventComparePWaveOrientation();

    //Not a strong motion comparison, but that is not what we are testing.
    //Only care if the custom channel is set.
    metric.add("channel-restriction", "LH");

    metric.setData(data);
    metric.setEventTable(eventLoader.getDayEvents(dataDate));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station1));
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,LHND", -2.387, 1E-3);
    expect.put("10,LHND", 0.429, 1E-3);
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
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station1));
    // we can actually get results since we only need to know where event happened
    // and don't actually use the raw synthetic data for anything in this test
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,BHND", -2.318, 1E-3);
    expect.put("10,BHND", 0.495, 1E-3);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void getNorthOutOfPhaseCorrectQuadrant() {
    int north = 1;
    int east = -1;
    int vert = -1;
    double angle = 46;
    double correctedAngle = correctBackAzimuthQuadrant(angle, north, east, vert);
    assertEquals(270 + angle, correctedAngle, 1E-5);
  }

  @Test
  public final void getEastOutOfPhaseCorrectQuadrant() {
    int north = 1;
    int east = -1;
    int vert = 1;
    double angle = 46;
    double correctedAngle = correctBackAzimuthQuadrant(angle, north, east, vert);
    assertEquals(90 + angle, correctedAngle, 1E-5);
  }

  @Test
  public final void getVertOutOfPhaseCorrectQuadrant() {
    int north = -1;
    int east = -1;
    int vert = 1;
    double angle = 46;
    double correctedAngle = correctBackAzimuthQuadrant(angle, north, east, vert);
    assertEquals(angle, correctedAngle, 1E-5);
  }

  @Test
  public final void getAllInPhaseCorrectQuadrant() {
    int north = 1;
    int east = 1;
    int vert = 1;
    double angle = 46;
    double correctedAngle = correctBackAzimuthQuadrant(angle, north, east, vert);
    assertEquals(180 + angle, correctedAngle, 1E-5);
  }

  @Test
  public final void testGetVersion() {
    metric = new EventComparePWaveOrientation();
    assertEquals(1, metric.getVersion());
  }

  @Test
  public final void testGetName() {
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
  public final void testGetVerticalName() {
    String starting = "LHN";
    String derived = "LHND";
    assertEquals("LHZ", EventComparePWaveOrientation.getVerticalChannelNameString(starting));
    assertEquals("LHZ", EventComparePWaveOrientation.getVerticalChannelNameString(derived));
  }

  @Test
  public final void testFilterChannel_SingleBandPass() {
    List<String> allowedBands = new LinkedList<>();
    allowedBands.add("LH");
    Channel channel = new Channel("00", "LHND");
    assertFalse(EventComparePWaveOrientation.filterChannel(channel, allowedBands));
  }

  @Test
  public final void testFilterChannel_MultiBandPass() {
    List<String> allowedBands = new LinkedList<>();
    allowedBands.add("LH");
    allowedBands.add("BH");
    Channel channel = new Channel("10", "LHND");
    assertFalse(EventComparePWaveOrientation.filterChannel(channel, allowedBands));
    channel = new Channel("00", "BHND");
    assertFalse(EventComparePWaveOrientation.filterChannel(channel, allowedBands));
  }

  @Test
  public final void testFilterChannel_MultiBandDifferentOrderingPass() {
    List<String> allowedBands = new LinkedList<>();
    allowedBands.add("LH");
    allowedBands.add("BH");
    Channel channel = new Channel("10", "BHND");
    assertFalse(EventComparePWaveOrientation.filterChannel(channel, allowedBands));
    channel = new Channel("00", "LHND");
    assertFalse(EventComparePWaveOrientation.filterChannel(channel, allowedBands));
  }

  @Test
  public final void testFilterChannel_MultiBandReject() {
    List<String> allowedBands = new LinkedList<>();
    allowedBands.add("LH");
    allowedBands.add("BH");
    Channel channel = new Channel("10", "HHND");
    assertTrue(EventComparePWaveOrientation.filterChannel(channel, allowedBands));
    channel = new Channel("00", "LNND");
    assertTrue(EventComparePWaveOrientation.filterChannel(channel, allowedBands));
  }

  @Test
  public final void testFilterChannel_SingleBandReject() {
    List<String> allowedBands = new LinkedList<>();
    allowedBands.add("BH");
    Channel channel = new Channel("10", "LHND");
    assertTrue(EventComparePWaveOrientation.filterChannel(channel, allowedBands));
  }

  @Test
  public final void testFilterChannel_EastDerivedReject() {
    List<String> allowedBands = new LinkedList<>();
    allowedBands.add("LH");
    Channel channel = new Channel("10", "LHED");
    assertTrue(EventComparePWaveOrientation.filterChannel(channel, allowedBands));
  }

  @Test
  public final void testFilterChannel_NorthDerivedPass() {
    List<String> allowedBands = new LinkedList<>();
    allowedBands.add("LH");
    Channel channel = new Channel("10", "LHND");
    assertFalse(EventComparePWaveOrientation.filterChannel(channel, allowedBands));
  }

  @Test
  public final void testFilterChannel_ZReject() {
    List<String> allowedBands = new LinkedList<>();
    allowedBands.add("LH");
    Channel channel = new Channel("10", "LHZ");
    assertTrue(EventComparePWaveOrientation.filterChannel(channel, allowedBands));
  }

  @Test
  public final void testnormalize_PositiveOnly_Data2Max() {
    double[] data1 = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    double[] data2 = {1, 1, 1, 1, 1, 1, 1, 1, 1, 2};
    double[] data1Expected = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
    double[] data2Expected = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 1};
    EventComparePWaveOrientation.normalize(data1, data2);

    assertArrayEquals(data1Expected, data1, 1e-7);
    assertArrayEquals(data2Expected, data2, 1e-7);
  }

  @Test
  public final void testnormalize_PositiveOnly_onlyOneInput() {
    double[] data1 = {1, 1, 1, 1, 1, 1, 1, 1, 1, 2};
    double[] data1Expected = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 1};
    EventComparePWaveOrientation.normalize(data1);

    assertArrayEquals(data1Expected, data1, 1e-7);
  }

  @Test
  public final void testnormalize_PositiveOnly_Data1Max() {
    double[] data1 = {1, 1, 1, 1, 0, 1, 1, 1, 1, 4};
    double[] data2 = {1, 1.5, 1, 1, 1, 1, 1, 1, 1, 2};
    double[] data1Expected = {0.25, 0.25, 0.25, 0.25, 0, 0.25, 0.25, 0.25, 0.25, 1};
    double[] data2Expected = {0.25, 0.375, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.5};
    EventComparePWaveOrientation.normalize(data1, data2);

    assertArrayEquals(data1Expected, data1, 1e-7);
    assertArrayEquals(data2Expected, data2, 1e-7);
  }

  @Test
  public final void testnormalize_NegativeMax() {
    double[] data1 = {-1, 1, 1, 1, 0, 1, 1, 1, 1, -4};
    double[] data2 = {1, 1.5, 1, -1, 1, 1, 1, 1, 1, -2};
    double[] data1Expected = {-0.25, 0.25, 0.25, 0.25, 0, 0.25, 0.25, 0.25, 0.25, -1};
    double[] data2Expected = {0.25, 0.375, 0.25, -0.25, 0.25, 0.25, 0.25, 0.25, 0.25, -0.5};
    EventComparePWaveOrientation.normalize(data1, data2);

    assertArrayEquals(data1Expected, data1, 1e-7);
    assertArrayEquals(data2Expected, data2, 1e-7);
  }

  @Test
  public final void testnormalize_NegativeMax_OnlyOneInput() {
    double[] data1 = {-1, 1, 1, 1, 0, 1, 1, 1, 1, -4};
    double[] data1Expected = {-0.25, 0.25, 0.25, 0.25, 0, 0.25, 0.25, 0.25, 0.25, -1};
    EventComparePWaveOrientation.normalize(data1);

    assertArrayEquals(data1Expected, data1, 1e-7);
  }

  @Test
  public final void testnormalize_0s() {
    double[] data1 = {0,0,0,0,0,0,0};
    double[] data2 = {0,0,0,0,0,0,0};
    double[] data1Expected = {0,0,0,0,0,0,0};
    double[] data2Expected = {0,0,0,0,0,0,0};
    EventComparePWaveOrientation.normalize(data1, data2);

    assertArrayEquals(data1Expected, data1, 1e-7);
    assertArrayEquals(data2Expected, data2, 1e-7);
  }

  @Test
  public final void testnormalize_0s_OnlyOneInput() {
    double[] data1 = {0,0,0,0,0,0,0};
    double[] data1Expected = {0,0,0,0,0,0,0};
    EventComparePWaveOrientation.normalize(data1);

    assertArrayEquals(data1Expected, data1, 1e-7);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void testnormalize_MismatchedLengths_ExpectException() {
    double[] data1 = {0,0,0,0,0,0,0};
    double[] data2 = {0,0,0,0,0,0};

    EventComparePWaveOrientation.normalize(data1, data2);
  }

}
