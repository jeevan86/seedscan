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

public class WPhaseQualityMetricTest {

  private WPhaseQualityMetric metric;
  private static MetricData data;
  private static EventLoader eventLoader;
  private static LocalDate dataDate = LocalDate.ofYearDay(2016, 62);
  private static Station station1;

  @BeforeClass
  public static void setUpBeforeClass() {
    String doy = String.format("%03d", dataDate.getDayOfYear());
    String networkName = "IU";
    String stationName = "PMG";
    String metadataLocation = "/metadata/rdseed/" + networkName + "-" + stationName + "-ascii.txt";
    String seedDataLocation = "/seed_data/" + networkName + "_" + stationName + "/" +
        dataDate.getYear() + "/" + doy + "";
    station1 = new Station(networkName, stationName);
    data = ResourceManager.getMetricData(seedDataLocation, metadataLocation, dataDate, station1);
    eventLoader = new EventLoader(ResourceManager.getDirectoryPath("/event_synthetics"));
  }

  @Test
  public void testProcessDefault() throws NoSuchFieldException {
    metric = new WPhaseQualityMetric();
    metric.add("channel-restriction", "LH");
    metric.setData(data);
    metric.setEventTable(eventLoader.getDayEvents(dataDate));
    metric.setEventSynthetics(eventLoader.getDaySynthetics(dataDate, station1));
    MetricTestMap expect = new MetricTestMap();
    expect.put("00,LHND", 1, 1E-3);
    //expect.put("00,LHED", 1, 1E-3);
    //expect.put("00,LHZ", 1, 1E-3);
    expect.put("10,LHND", 1, 1E-3);
    expect.put("10,LHED", 1, 1E-3);
    expect.put("10,LHZ", 1, 1E-3);
    expect.put("60,LHND", 1, 1E-3);
    expect.put("60,LHED", 1, 1E-3);
    //expect.put("60,LHZ", 1, 1E-3);
    TestUtils.testMetric(metric, expect);
  }

  @Test
  public void checkNHNMInterpolation() {
    double[] frequencies = new double[100];
    double startingFrequency = 0.01;
    double delta = 0.005;
    for (int i = 0; i < frequencies.length; ++i) {
      frequencies[i] = startingFrequency + delta * i;
    }
    double[] nhnmData = WPhaseQualityMetric.getNHNMOverFrequencies(frequencies);
    // values derived from python interpolation code
    double[] expected = new double[]{-131.5, -133.26281916094973, -134.51323991547991,
        -135.48331073547976, -136.27611064511396, -136.94558800732372, -137.52650932901653,
        -138.03895080679092, -138.2406633721824, -131.75746356978468, -125.59843893557799,
        -120.1683530237313, -119.2632479684357, -118.59127563533225, -117.962700409375,
        -117.37213899810281, -116.81563350332853, -116.2892571850731, -115.79020319333213,
        -115.31538456291327, -114.86246267827762, -114.42923920179334, -114.01452931844439,
        -113.61747750454013, -112.02975699310085, -109.94546942068384, -107.9358525741661,
        -105.99846228454766, -104.124753242806, -102.31481557937612, -100.90468921011227,
        -100.44400888688106, -100.01679546090476, -99.60209192118009, -99.19899311162482,
        -98.80690019286902, -98.42521529245502, -98.05337789560583, -97.69125726505979,
        -97.33804540345191, -96.99319453651937, -96.65901365810771, -96.590836758796,
        -96.76729928144599, -96.94004206180769, -97.1086889228091, -97.27418781373316,
        -97.43596355365385, -97.59471351847205, -97.75004732674545, -97.90262355878822,
        -98.09950119940233, -98.36328839682191, -98.62256617863584, -98.87691137474619,
        -99.12665974903209, -99.37226919910294, -99.61386738362089, -99.85103733058888,
        -100.08426731540655, -100.31381911263912, -100.53980032069428, -100.76229908248662,
        -100.98129796448644, -101.19679087056426, -101.40896279210729, -101.61814566198026,
        -101.8242450960601, -102.02733641010656, -102.22759373290309, -102.425041746041,
        -102.61984382348938, -102.81194895703837, -103.00145655435395, -103.18848467934777,
        -103.37302789913254, -103.55529643030239, -103.73528893006625, -103.91290766527534,
        -104.08826839301547, -104.26133947741135, -104.43241536877672, -104.60151455856978,
        -104.76862335937153, -104.93380653579274, -105.0971551456985, -105.25810828186832,
        -105.41814428406627, -105.576052, -105.7320509081545, -105.88648202731626,
        -106.03910642817193, -106.1898823075677, -106.33921677162766, -106.48714023866063,
        -106.63355260010202, -106.77810388534631, -106.92134362725099, -107.06333138000909,
        -107.20382983926373};
    assertArrayEquals(expected, nhnmData, 1);
  }

  @Test
  public void verifyNHNMNoiseComparison_noisyDataFails() {
    double[] psd = new double[10000];
    // we'll just use an obviously, egregiously bad data case here
    Arrays.fill(psd, -75);
    double df = 0.0005;
    double resultNHNMScreen = WPhaseQualityMetric.passesPSDNoiseScreening(psd, df);
    assertTrue(resultNHNMScreen > 0);
  }

  @Test
  public void verifyNHNMNoiseComparisonPassesGoodData() {
    double[] psd = new double[10000];
    // presumably a reasonable PSD estimation would be somewhere around here for most of its values
    Arrays.fill(psd, -150);
    double df = 0.0005;
    double resultNHNMScreen = WPhaseQualityMetric.passesPSDNoiseScreening(psd, df);
    assertTrue(resultNHNMScreen < 0);
  }

  @Test
  public void misfitCheckPassesAllZero() {
    double[] sensorData = new double[100];
    float[] synthData = new float[100];
    assertTrue(WPhaseQualityMetric.passesMisfitScreening(synthData, sensorData));
  }

  @Test
  public void misfitCheckPassesPointsNearEqual() {
    float[] synthData = new float[100];
    double[] sensorData = new double[synthData.length];

    for (int i = 0; i < synthData.length; ++i) {
      synthData[i] = (float) Math.sin(i * Math.PI / 100.);
      // add a random perturbation not larger than the amplitude of the synthetic data
      sensorData[i] = synthData[i] + Math.random();
    }
    assertTrue(WPhaseQualityMetric.passesMisfitScreening(synthData, sensorData));
  }

  @Test
  public void misfitCheckFailsSensorDataNotClose() {
    float[] synthData = new float[100];
    double[] sensorData = new double[synthData.length];

    for (int i = 0; i < synthData.length; ++i) {
      synthData[i] = (float) Math.sin(i * Math.PI / 100.);
      // add a constant offset much greater than the synthetic data's amplitude
      sensorData[i] = synthData[i] + 10;
    }
    assertFalse(WPhaseQualityMetric.passesMisfitScreening(synthData, sensorData));
  }

  @Test
  public void verifyIntegrationByTrapezoid() {
    int len = 100;
    double[] linearFunction = new double[len];
    double[] squaredFunction = new double[len];
    // can't evaluate directly this way so instead let's compare with the scipy result for
    // doing the trapezoid integration twice
    double[] cubicFunction = new double[]{0., 1.25, 4.5, 10.75, 21., 36.25, 57.5, 85.75, 122.};
    for (int i = 0; i < len; ++i) {
      linearFunction[i] = i;
      squaredFunction[i] = linearFunction[i] * i / 2.;
    }
    double[] evaluatedSquared =
        WPhaseQualityMetric.performIntegrationByTrapezoid(linearFunction, 1);
    assertEquals(4.9005e3, evaluatedSquared[len-1], 0.);
    double[] squaredInput =
        Arrays.copyOfRange(evaluatedSquared, 1, cubicFunction.length + 1);
    double[] evaluatedCubic =
        WPhaseQualityMetric.performIntegrationByTrapezoid(squaredInput, 1);
    assertArrayEquals(squaredFunction, evaluatedSquared, 1E-10);
    assertArrayEquals(cubicFunction, evaluatedCubic, 1E-10);
  }

  @Test
  public void responseCheckPasses() throws IOException {
    InstrumentResponse resp = InstrumentResponse.loadEmbeddedResponse(STS2gen3, HIGH);
    Complex pole = resp.getPoles().get(0);
    double w = pole.abs(); // corner frequency
    double h = Math.abs(pole.getReal() / pole.abs()); // damping
    assertTrue(WPhaseQualityMetric.passesResponseCheck(w, h));
    // not necessarily great form for unit tests but method is basic enough we'll just feed in
    // some bad data we know will cause it to fail and make sure it does
    w = 3;
    assertFalse(WPhaseQualityMetric.passesResponseCheck(w, h));
    w = pole.abs();
    h = 1;
    assertFalse(WPhaseQualityMetric.passesResponseCheck(w, h));
  }

  @Test
  public final void testGetVersion() {
    metric = new WPhaseQualityMetric();
    assertEquals(1, metric.getVersion());
  }

  @Test
  public final void testGetName() {
    metric = new WPhaseQualityMetric();
    assertEquals("WPhaseQualityMetric", metric.getName());
  }
}
