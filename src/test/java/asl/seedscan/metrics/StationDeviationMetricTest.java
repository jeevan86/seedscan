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

    double err = 1E-4;
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,LH1", -1.3673368, err);
    expect.put("00,LH2", -0.9144481, err);
    expect.put("00,LHZ", -3.1754843, err);

    expect.put("10,LH1", -6.9036831, err);
    expect.put("10,LH2", -1.3517243, err);
    expect.put("10,LHZ", -4.3853272, err);

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testProcessGSOK() throws Exception {
    metric.setData(data2);

    double err = 1E-5;
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,LH1",  0.233198, err);
    expect.put("00,LH2", 14.616353, err);
    expect.put("00,LHZ", 22.065026, err);
    expect.put("00,HH1",  2.803599, err);
    expect.put("00,HH2", 13.600242, err);
    expect.put("00,HHZ", 21.547157, err);

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testGetBaseName() throws Exception {
    assertEquals("BaseName", "StationDeviationMetric", metric.getBaseName());
  }

}
