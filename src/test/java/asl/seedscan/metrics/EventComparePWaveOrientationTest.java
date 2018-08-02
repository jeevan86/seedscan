package asl.seedscan.metrics;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import asl.metadata.Channel;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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

  @BeforeClass
  public static void setUpBeforeClass() {
    try {
      data = (MetricData) ResourceManager
          .loadCompressedObject("/data/IU.NWAO.2015.299.MetricData.ser.gz", false);
      data2 = (MetricData) ResourceManager
          .loadCompressedObject("/data/IU.TUC.2018.023.ser.gz", false);
      eventLoader = new EventLoader(ResourceManager.getDirectoryPath("/events"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() {
    data = null;
    data2 = null;
    eventLoader = null;
  }

  @Test
  public void testProcessDefault() {
    metric = new EventComparePWaveOrientation();
    metric.setData(data);
    LocalDate date = LocalDate.of(2015, 10, 26);
    metric.setEventTable(eventLoader.getDayEvents(date));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(date, new Station("IU", "NWAO")));
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,LHND", -2.540952254110209);
    // expect.put("10,LHND", 0.0); // skipped because linearity not good enough
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcessDefaultData2() {
    metric = new EventComparePWaveOrientation();
    metric.setData(data2);
    LocalDate date = LocalDate.of(2018, 01, 23);
    metric.setEventTable(eventLoader.getDayEvents(date));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(date, new Station("IU", "TUC")));
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,LHND", -6.2504661649869036);
    //expect.put("10,LHND", 0.5574496184388522);
    expect.put("60,LHND", -0.5888912051460125);
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
    LocalDate date = LocalDate.of(2015, 10, 26);
    metric.setEventTable(eventLoader.getDayEvents(date));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(date, new Station("IU", "NWAO")));
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00,LHND", -2.540952254110209);
    // expect.put("10,LHND", 0.0);
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
    LocalDate date = LocalDate.of(2015, 10, 26);
    metric.setEventTable(eventLoader.getDayEvents(date));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(date, new Station("IU", "NWAO")));
    HashMap<String, Double> expect = new HashMap<>();
    // expect should be empty because no BH synthetic data exists!
    TestUtils.testMetric(metric, expect);
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
  public final void testScaleByMax_PositiveOnly_Data2Max() {
    double[] data1 = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    double[] data2 = {1, 1, 1, 1, 1, 1, 1, 1, 1, 2};
    double[] data1Expected = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
    double[] data2Expected = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 1};
    EventComparePWaveOrientation.scaleByMax(data1, data2);

    assertArrayEquals(data1Expected, data1, 1e-7);
    assertArrayEquals(data2Expected, data2, 1e-7);
  }

  @Test
  public final void testScaleByMax_PositiveOnly_Data1Max() {
    double[] data1 = {1, 1, 1, 1, 0, 1, 1, 1, 1, 4};
    double[] data2 = {1, 1.5, 1, 1, 1, 1, 1, 1, 1, 2};
    double[] data1Expected = {0.25, 0.25, 0.25, 0.25, 0, 0.25, 0.25, 0.25, 0.25, 1};
    double[] data2Expected = {0.25, 0.375, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.5};
    EventComparePWaveOrientation.scaleByMax(data1, data2);

    assertArrayEquals(data1Expected, data1, 1e-7);
    assertArrayEquals(data2Expected, data2, 1e-7);
  }

  @Test
  public final void testScaleByMax_NegativeMax() {
    double[] data1 = {-1, 1, 1, 1, 0, 1, 1, 1, 1, -4};
    double[] data2 = {1, 1.5, 1, -1, 1, 1, 1, 1, 1, -2};
    double[] data1Expected = {-0.25, 0.25, 0.25, 0.25, 0, 0.25, 0.25, 0.25, 0.25, -1};
    double[] data2Expected = {0.25, 0.375, 0.25, -0.25, 0.25, 0.25, 0.25, 0.25, 0.25, -0.5};
    EventComparePWaveOrientation.scaleByMax(data1, data2);

    assertArrayEquals(data1Expected, data1, 1e-7);
    assertArrayEquals(data2Expected, data2, 1e-7);
  }

  @Test
  public final void testScaleByMax_0s() {
    double[] data1 = {0,0,0,0,0,0,0};
    double[] data2 = {0,0,0,0,0,0,0};
    double[] data1Expected = {0,0,0,0,0,0,0};
    double[] data2Expected = {0,0,0,0,0,0,0};
    EventComparePWaveOrientation.scaleByMax(data1, data2);

    assertArrayEquals(data1Expected, data1, 1e-7);
    assertArrayEquals(data2Expected, data2, 1e-7);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void testScaleByMax_MismatchedLengths_ExpectException() {
    double[] data1 = {0,0,0,0,0,0,0};
    double[] data2 = {0,0,0,0,0,0};

    EventComparePWaveOrientation.scaleByMax(data1, data2);
  }

}
