package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;
import java.util.HashMap;
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
  public static void setUpBeforeClass() throws Exception {
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
  public static void tearDownAfterClass() throws Exception {
    data = null;
    data2 = null;
    eventLoader = null;
  }

  @Test
  public void testProcessDefault() throws Exception {
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
  public void testProcessDefaultData2() throws Exception {
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
