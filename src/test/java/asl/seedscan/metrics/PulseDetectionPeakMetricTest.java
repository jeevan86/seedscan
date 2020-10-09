package asl.seedscan.metrics;

import asl.metadata.Station;
import asl.seedscan.event.EventLoader;
import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;

public class PulseDetectionPeakMetricTest {

  private PulseDetectionPeakMetric metric;
  private static MetricData data;
  private static EventLoader eventLoader;
  private static final LocalDate dataDate = LocalDate.ofYearDay(2020, 104);
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
    metric = new PulseDetectionPeakMetric();
    metric.setData(data);
    metric.setEventTable(eventLoader.getDayEvents(dataDate));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station1));
    metric.add("channel-restriction", "VH");
    metric.add("coefficient-threshold", "0.70");
    metric.add("amplitude-threshold", "1.0");
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,VH2", 12.2, 1E-1);
    expect.put("10,VH2", 10.2, 1E-1);
    expect.put("60,VH1", 2.9, 1E-1);
    expect.put("60,VH2", 4.3, 1E-1);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void testProcessDefault() throws NoSuchFieldException {
    LocalDate dataDate2 = LocalDate.of(2020, 9, 7);
    String networkName = "US";
    String stationName = "KSU1";
    String metadataLocation = "/metadata/rdseed/" + networkName + "-" + stationName + "-ascii.txt";

    String doy = String.format("%03d", dataDate2.getDayOfYear());
    String seedDataLocation = "/seed_data/" + networkName + "_" + stationName + "/" +
        dataDate2.getYear() + "/" + doy + "";
    Station station2 = new Station(networkName, stationName);
    MetricData data2 =
        ResourceManager.getMetricData(seedDataLocation, metadataLocation, dataDate2, station2);
    metric = new PulseDetectionPeakMetric();
    metric.setData(data2);
    metric.setEventTable(eventLoader.getDayEvents(dataDate2));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station2));
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,LH1", 80.6, 1E-1);
    expect.put("00,LH2", 108.8, 1E-1);
    TestUtils.testMetric(metric, expect);
  }
}
