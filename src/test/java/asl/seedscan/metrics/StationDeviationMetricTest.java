package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import asl.metadata.Station;
import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class StationDeviationMetricTest {

  private StationDeviationMetric metric;
  private static MetricData data1;
  private static MetricData data2;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    data1 = ResourceManager.loadANMOMainTestCase();
    String dataPath = "/seed_data/GS_OK029/2015/360";
    String metaPath = "/metadata/rdseed/GS-OK029-00-ascii.txt";
    LocalDate date = LocalDate.ofYearDay(2015, 360);
    Station station = new Station("GS", "OK029");
    data2 = (MetricData) ResourceManager.getMetricData(dataPath, metaPath, date, station);
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    data1 = null;
    data2 = null;
  }

  @Before
  public void setUp() throws Exception {
    metric = new StationDeviationMetric();
    metric.add("lower-limit", "90");
    metric.add("upper-limit", "110");
    metric.add("modelpath", ResourceManager.getDirectoryPath("/station_noise_models"));
  }

  @Test
  public final void testGetVersion() throws Exception {
    assertEquals("Version #: ", (long) 2, metric.getVersion());
  }

  @Test
  public final void testProcessANMO() throws Exception {
    metric.setData(data1);

    double err = 1E-5;
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,LH1", -3.3940738, err); // was -6.259435685534593
    expect.put("00,LH2", -2.3420939, err); // was -3.6049945557454826
    expect.put("00,LHZ", -3.4845588, err); // was -4.813953146458895

    expect.put("10,LH1", -12.6668404, err);
    expect.put("10,LH2",  -9.1613657, err);
    expect.put("10,LHZ", -11.0915996, err);

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testProcessGSOK() throws Exception {
    metric.setData(data2);

    double err = 1E-5;
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,LH1", 16.185851, err); // was 10.55316202766241
    expect.put("00,LH2", 27.280398, err); // was 13.004893924325444
    //expect.put("00,LHZ", 0, err);
    expect.put("00,HH1", 15.869745, err); // was 79.1343722643019
    expect.put("00,HH2", 28.995552, err); // was 84.66032464538895
    expect.put("00,HHZ", 28.992649, err); // was 69.51285486264509

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testGetBaseName() throws Exception {
    assertEquals("BaseName", "StationDeviationMetric", metric.getBaseName());
  }

}
