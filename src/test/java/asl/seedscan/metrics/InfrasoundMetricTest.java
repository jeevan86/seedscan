package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.metadata.meta_new.ChannelMetaException;
import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class InfrasoundMetricTest {

  private InfrasoundMetric metric;
  private static MetricData data;
  private static Station station;
  private static LocalDate dataDate;


  @BeforeClass
  public static void setUpBeforeClass() {
    try {
      String metadataLocation = "/metadata/rdseed/IU-ANMO-ascii.txt";
      String seedDataLocation = "/seed_data/IU_ANMO/2018/121";
      String networkName = "IU";
      station = new Station(networkName, "ANMO");
      dataDate = LocalDate.of(2018, 5, 1);
      data = ResourceManager.getMetricData(seedDataLocation, metadataLocation, dataDate, station, networkName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() {
    data = null;
    station = null;
    dataDate = null;
  }

  @Test
  public void testMetricResultMatchesExpected()
      throws MetricException, MetricPSDException, ChannelMetaException {
    metric = new InfrasoundMetric();
    metric.setData(data);
    Channel channel = data.getMetaData().getChannelArray("BD,HD", false, true).get(0);

    MetricTestMap expect = new MetricTestMap();
    expect.put("32,BDF", 1.1671, 1E-4);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testGetVersion() {
    metric = new InfrasoundMetric();
    assertEquals(1, metric.getVersion());
  }

  @Test
  public final void testGetName() {
    metric = new InfrasoundMetric();
    assertEquals("InfrasoundMetric", metric.getName());
  }
}
