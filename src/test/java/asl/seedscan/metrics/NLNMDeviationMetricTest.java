package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.metadata.meta_new.ChannelMeta.ResponseUnits;
import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import asl.timeseries.CrossPower;
import asl.utils.FFTResult;
import asl.utils.TimeSeriesUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Arrays;
import org.apache.commons.math3.complex.Complex;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NLNMDeviationMetricTest {

  private NLNMDeviationMetric metric;
  private static MetricData data1;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    try {
      data1 = (MetricData) ResourceManager
          .loadCompressedObject("/java_serials/data/IU.ANMO.2015.206.MetricData.ser.gz", false);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    data1 = null;
  }

  @Before
  public void setUp() throws Exception {
    metric = new NLNMDeviationMetric();
    metric.add("lower-limit", "4");
    metric.add("upper-limit", "8");
  }

  @Test
  public final void testGetVersion() throws Exception {
    assertEquals("Metric Version: ", 1, metric.getVersion());
  }

  @Test
  public final void testProcess() throws Exception {

    metric.setData(data1);
    metric.add("channel-restriction", "LH");

    String chlName = "LH1";
    Channel channelToCheck = new Channel("00", chlName);

    StringBuilder sb;
    double sampleRate = data1.getChannelData(channelToCheck).get(0).getSampleRate();
    long samplingInterval = (long) (TimeSeriesUtils.ONE_HZ_INTERVAL / sampleRate);
    double[] detrendedData = data1.getDetrendedPaddedDayData(channelToCheck);

    FFTResult rawSpectrum = FFTResult.spectralCalc(detrendedData, detrendedData, samplingInterval);
    double[] rawSpectrumFreqs = rawSpectrum.getFreqs();
    Complex[] basicPSD = rawSpectrum.getFFT();

    CrossPower crossPower = metric.getCrossPower(channelToCheck, channelToCheck);

    /*
    // TODO: double-check that this value is what we expect it to be
    Complex[] respCurve = data1.getMetaData().getChannelMetadata(channelToCheck)
        .getResponse(freqs, ResponseUnits.ACCELERATION);
    */

    sb = new StringBuilder();
    //double[] spectrum = crossPower.getSpectrum();
    //double[] frequencies = crossPower.getSpectrumFrequencies();
    for (int i = 0; i < rawSpectrumFreqs.length; ++i) {
      sb.append(rawSpectrumFreqs[i]).append(", ").append(basicPSD[i].getReal())
          .append(", ").append(basicPSD[i].getImaginary()).append("\n");
    }

    PrintWriter out = new PrintWriter(new File("ANMO-00-" + chlName + ".2015.206.basic-PSD.csv"));
    out.write(sb.toString());
    out.close();


		/* The String key matches the MetricResult ids */
    MetricTestMap expect = new MetricTestMap();
    double error = 1E-5;
    expect.put("00,LH1",  7.917047, error); // TODO: figure out why this seems too low
    expect.put("00,LH2",  8.459049, error);
    expect.put("00,LHZ", 10.898116, error);
    expect.put("10,LH1",  8.528913, error);
    expect.put("10,LH2",  8.537544, error);
    expect.put("10,LHZ", 10.869350, error);

    TestUtils.testMetric(metric, expect);

    // Consider adding this once memozing computePSD is done. Currently it
    // pretends to, but doesn't really.

    // metric = new NLNMDeviationMetric();
    // metric.add("lower-limit", "4");
    // metric.add("upper-limit", "8");
    // metric.setData(data1);
    // //metric.add("channel-restriction", "LH");
    //
    // /* The String key matches the MetricResult ids */
    // expect = new HashMap<String, Double>();
    // expect.put("00,LH1", 6.144156714847165);
    // expect.put("00,LH2", 6.94984340838684);
    // expect.put("00,LHZ", 9.502837498158087);
    // expect.put("10,LH1", 7.126248440694965);
    // expect.put("10,LH2", 6.701346629427542);
    // expect.put("10,LHZ", 9.473855204418532);
    // expect.put("00,BH1", 7.190265405958664);
    // expect.put("00,BH2", 8.080463692084166);
    // expect.put("00,BHZ", 10.582575310053912);
    // expect.put("10,BH1", 8.132530485497114);
    // expect.put("10,BH2", 7.876391947428445);
    // expect.put("10,BHZ", 10.551351503338514);
    //
    // metric.process();
    // result = metric.getMetricResult();
    // for (String id : result.getIdSet()) {
    // //Round to 7 places to match the Metric injector
    // Double expected = (double)Math.round(expect.get(id) * 1000000d) /
    // 1000000d;
    // Double resulted = (double)Math.round(result.getResult(id) * 1000000d)
    // / 1000000d;
    // System.out.println(id + " " +result.getResult(id));
    // assertEquals(id + " result: ", expected, resulted);
    // }

    // Test High frequency cases
    // LH should be ignored by this metric since it is below our nyquist
    // frequency.

    metric = new NLNMDeviationMetric();
    metric.add("lower-limit", "0.125");
    metric.add("upper-limit", "0.25");
    /*
     * This should cause it to print a log message for failing to makePlots
		 */
    metric.add("makeplots", "true");

    metric.setData(data1);

		/* The String key matches the MetricResult ids */
    expect = new MetricTestMap();

    expect.put("00,BH1", 9.23291, error);
    expect.put("00,BH2", 10.19558, error);
    expect.put("00,BHZ", 14.13442, error);
    expect.put("10,BH1", 11.99870, error);
    expect.put("10,BH2", 11.41340, error);
    expect.put("10,BHZ", 12.49639, error);

    TestUtils.testMetric(metric, expect);
  }

  @Test
  public final void testProcess_PMSA() {
    String seedDataLocation = "/seed_data/IU_PMSA/2019/062";
    String metadataLocation = "";
    Station pmsa = new Station("IU", "PMSA");
    LocalDate dataDate = LocalDate.of(2019, 1, 10);

  }

  @Test
  public final void testGetBaseName() throws Exception {
    assertEquals("Base name: ", "NLNMDeviationMetric", metric.getBaseName());
  }

  @Test
  public final void testGetName() throws Exception {
    assertEquals("Metric name: ", "NLNMDeviationMetric:4-8", metric.getName());
  }

  @Test
  public final void testNLNMDeviationMetric() throws Exception {
    metric = new NLNMDeviationMetric();
    metric.add("lower-limit", "90");
    metric.add("upper-limit", "110");
    /* This has to come after adding the limits */
    metric.setData(data1);
  }

  @Test
  public final void testGetNLNM() throws Exception {
    assertTrue(NLNMDeviationMetric.getNLNM().isValid());
  }

  @Test
  public final void testGetNHNM() throws Exception {
    assertTrue(NLNMDeviationMetric.getNHNM().isValid());
  }

}
