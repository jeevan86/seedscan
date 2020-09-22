package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import asl.seedscan.event.EventLoader;
import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EventCompareStrongMotionTest {

  private EventCompareStrongMotion metric;
  private static MetricData data;
  private static EventLoader eventLoader;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    try {
      data = ResourceManager.loadNWAOMainTestCase();
      eventLoader = new EventLoader(ResourceManager.getDirectoryPath("/event_synthetics"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    data = null;
    eventLoader = null;
  }

  @Test
  public void testProcessDefault() throws Exception {
    metric = new EventCompareStrongMotion();
    metric.setData(data);
    LocalDate date = LocalDate.of(2015, 10, 26);
    metric.setEventTable(eventLoader.getDayEvents(date));
    MetricTestMap expect = new MetricTestMap();
    double error = 1E-5;
    expect.put("00-20,LHZ-LNZ",   0.04060, error); //Was 2.884394926482693
    expect.put("00-20,LHND-LNND", 0.40673, error); //Was 0.6848874383447533
    expect.put("00-20,LHED-LNED", 1.00148, error); //1.0097711288921811
    expect.put("10-20,LHZ-LNZ",   4.0);
    expect.put("10-20,LHND-LNND", 4.0);
    expect.put("10-20,LHED-LNED", 4.0);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcessCustomConfig() throws Exception {

    metric = new EventCompareStrongMotion();

    //Not a strong motion comparison, but that is not what we are testing.
    //Only care if the custom channel is set.
    metric.add("base-channel", "10-LH");
    metric.add("channel-restriction", "LH");

    metric.setData(data);
    LocalDate date = LocalDate.of(2015, 10, 26);
    metric.setEventTable(eventLoader.getDayEvents(date));
    MetricTestMap expect = new MetricTestMap();
    expect.put("00-10,LHZ-LHZ",   -1.134958E-4, 1E-7); // was -3.2751134376322145E-6
    expect.put("00-10,LHND-LHND", -1.149003E-4, 1E-7);
    expect.put("00-10,LHED-LHED", -0.000004285066); // was 1.0633680999724207E-7

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testGetVersion() throws Exception {
    metric = new EventCompareStrongMotion();
    assertEquals(2, metric.getVersion());
  }

  @Test
  public final void testGetName() throws Exception {
    metric = new EventCompareStrongMotion();
    assertEquals("EventCompareStrongMotion", metric.getName());
  }

}
