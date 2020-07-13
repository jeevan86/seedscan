package asl.seedscan.metrics;

import static asl.utils.ResponseUnits.ResolutionType.HIGH;
import static asl.utils.ResponseUnits.SensorType.STS2gen3;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.seedscan.event.EventLoader;
import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import asl.utils.input.InstrumentResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.stream.DoubleStream;
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
  public void testWindowWithinDay(){
    // 2019-01-20 0800
    long start = 1547971200000L;
    // 2019-01-20 1500
    long end = 1547996400000L;
    double[] results = data.getWindowedData(new Channel("00", "LHZ"), start, end );
    double resultMin = DoubleStream.of(results).min().getAsDouble();
    double resultMax = DoubleStream.of(results).max().getAsDouble();
    double resultSum = DoubleStream.of(results).sum();
    int resultCount = results.length;
    assertEquals(25200, resultCount);
    assertEquals(1165370757, (int)resultSum);
    assertEquals(42808, (int)resultMin);
    assertEquals(49598, (int)resultMax);
  }
  @Test
  public void testWindowOverlapPrevious() {
    // 2019-01-19 1500
    long start = 1547910000000L;
    // 2019-01-20 1500
    long end = 1547996400000L;
    double[] results = data.getWindowedData(new Channel("00", "LHZ"), start, end );
    double resultMin = DoubleStream.of(results).min().getAsDouble();
    double resultMax = DoubleStream.of(results).max().getAsDouble();
    double resultSum = DoubleStream.of(results).sum();
    int resultCount = results.length;
    assertEquals(86400, resultCount);
    assertEquals(3908047188L, (long)resultSum);
    assertEquals(14629, (int)resultMin);
    assertEquals(71711, (int)resultMax);
  }

  @Test
  public void testWindowOverlapNext() {
    // 2019-01-20 0800
    long start = 1547971200000L;
    // 2019-01-21 1500
    long end = 1548082800000L;
    double[] results = data.getWindowedData(new Channel("00", "LHZ"), start, end );
    double resultMin = DoubleStream.of(results).min().getAsDouble();
    double resultMax = DoubleStream.of(results).max().getAsDouble();
    double resultSum = DoubleStream.of(results).sum();
    int resultCount = results.length;
    assertEquals(111600, resultCount);
    assertEquals(5073549637L, (long)resultSum);
    assertEquals(36847, (int)resultMin);
    assertEquals(55294, (int)resultMax);
  }

  @Test
  public void testWindowOverlapBoth() {
    // 2019-01-19 1500
    long start = 1547910000000L;
    // 2019-01-21 1500
    long end = 1548082800000L;
    double[] results = data.getWindowedData(new Channel("00", "LHZ"), start, end );
    double resultMin = DoubleStream.of(results).min().getAsDouble();
    double resultMax = DoubleStream.of(results).max().getAsDouble();
    double resultSum = DoubleStream.of(results).sum();
    int resultCount = results.length;
    assertEquals(172800, resultCount);
    assertEquals(7816226068L, (long)resultSum);
    assertEquals(14629, (int)resultMin);
    assertEquals(71711, (int)resultMax);  }

}
