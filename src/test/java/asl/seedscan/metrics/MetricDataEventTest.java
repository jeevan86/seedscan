package asl.seedscan.metrics;

import static asl.utils.ResponseUnits.ResolutionType.HIGH;
import static asl.utils.ResponseUnits.SensorType.STS2gen3;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import asl.metadata.Station;
import asl.seedscan.event.EventLoader;
import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import asl.utils.input.InstrumentResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import org.apache.commons.math3.complex.Complex;
import org.junit.BeforeClass;
import org.junit.Test;

public class MetricDataEventTest {

  private WPhaseQualityMetric metric;
  private static MetricData data;
  private static EventLoader eventLoader;
  private static LocalDate dataDate = LocalDate.ofYearDay(2019, 20);
  private static Station station1;

  @BeforeClass
  public static void setUpBeforeClass() {
    String doy = String.format("%03d", dataDate.getDayOfYear());
    String networkName = "IU";
    String stationName = "RSSD";
    String metadataLocation = "/metadata/rdseed/" + networkName + "-" + stationName + "-ascii.txt";
    String seedDataLocation = "/seed_data/" + networkName + "_" + stationName + "/" +
        dataDate.getYear() + "/" + doy + "";
    station1 = new Station(networkName, stationName);
    data = ResourceManager.getMetricData(seedDataLocation, metadataLocation, dataDate, station1);

    doy = String.format("%03d", dataDate.minusDays(1).getDayOfYear());
    seedDataLocation = "/seed_data/" + networkName + "_" + stationName + "/" +
        dataDate.getYear() + "/" + doy + "";
    data.setPreviousMetricData(ResourceManager.getMetricData(seedDataLocation, metadataLocation, dataDate.minusDays(1), station1));

    doy = String.format("%03d", dataDate.plusDays(1).getDayOfYear());
    seedDataLocation = "/seed_data/" + networkName + "_" + stationName + "/" +
        dataDate.getYear() + "/" + doy + "";
    data.setNextMetricData(ResourceManager.getMetricData(seedDataLocation, metadataLocation, dataDate.plusDays(1), station1));

    eventLoader = new EventLoader(ResourceManager.getDirectoryPath("/event_synthetics"));
  }

  @Test
  public void testWindowWithinDay() {
   assertTrue(false);
  }
  @Test
  public void testWindowOverlapPrevious() {
    assertTrue(false);
  }

  @Test
  public void testWindowOverlapNext() {
    assertTrue(false);
  }

  @Test
  public void testWindowOverlapBoth() {
    assertTrue(false);
  }

}
