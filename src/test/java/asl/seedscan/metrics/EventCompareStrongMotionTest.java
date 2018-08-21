package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import asl.seedscan.event.EventLoader;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import java.util.HashMap;
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
      data = (MetricData) ResourceManager
          .loadCompressedObject("/java_serials/data/IU.NWAO.2015.299.MetricData.ser.gz", false);
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
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00-20,LHZ-LNZ",
        2.884394926482693);  //Was 2.884394926482693 before removing hard coding
    expect.put("00-20,LHND-LNND", 0.6842270334452618); //Was 0.6848874383447533
    expect.put("00-20,LHED-LNED", 1.0140182353130993); //1.0097711288921811
    expect.put("10-20,LHZ-LNZ", 4.0); //4.0
    expect.put("10-20,LHND-LNND", 4.0); //4.0
    expect.put("10-20,LHED-LNED", -4.0); //Nonexistent
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
    HashMap<String, Double> expect = new HashMap<>();
    expect.put("00-10,LHZ-LHZ", -0.0000032751134376322145);
    expect.put("00-10,LHND-LHND", 0.0000023421505281695907);
    expect.put("00-10,LHED-LHED", 0.00000010633680999724207);

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
