package asl.seedscan.metrics;

import asl.metadata.Station;
import asl.seedscan.event.EventLoader;
import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;

public class PulseDetectionCountMetricTest {

  private PulseDetectionCountMetric metric;
  private static MetricData data;
  private static EventLoader eventLoader;
  private static LocalDate dataDate = LocalDate.ofYearDay(2020, 104);
  private static Station station1;

  @BeforeClass
  public static void setUpBeforeClass() {
    String doy = String.format("%03d", dataDate.getDayOfYear());
    String networkName = "IU";
    String stationName = "HRV";
    String metadataLocation = "/metadata/rdseed/" + networkName + "-" + stationName + "-ascii.txt";
    String seedDataLocation = "/seed_data/" + networkName + "_" + stationName + "/" +
        dataDate.getYear() + "/" + doy + "";
    station1 = new Station(networkName, stationName);
    data = ResourceManager.getMetricData(seedDataLocation, metadataLocation, dataDate, station1);
    eventLoader = new EventLoader(ResourceManager.getDirectoryPath("/event_synthetics"));
  }

  @Test
  public void testProcessCustom() throws NoSuchFieldException {
    metric = new PulseDetectionCountMetric();
    metric.setData(data);
    metric.setEventTable(eventLoader.getDayEvents(dataDate));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station1));
    metric.add("channel-restriction", "VH");
    metric.add("amplitude-threshold", "1");
    metric.add("coefficient-threshold", "0.7");
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,VH1", 0, 1E-10);
    expect.put("00,VH2", 3, 1E-10);
    expect.put("00,VHZ", 0, 1E-10);
    expect.put("10,VH1", 0, 1E-10);
    expect.put("10,VH2", 2, 1E-10);
    expect.put("10,VHZ", 0, 1E-10);
    expect.put("60,VH1", 1, 1E-10);
    expect.put("60,VH2", 1, 1E-10);
    expect.put("60,VHZ", 0, 1E-10);
    TestUtils.testMetric(metric, expect);
  }

}
