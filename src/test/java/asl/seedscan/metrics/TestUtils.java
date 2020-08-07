package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import asl.metadata.MetaGeneratorMock;
import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;
import asl.testutils.MetricTestMap;
import asl.testutils.ResourceManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class TestUtils {

  /**
   * TestMetric() takes setup metric, processes it and finally compares it to
   * an expected result set.
   *
   * Comparisons:
   * Number of results matches expectation.
   * Values of results matches corresponding expectation within 7 decimal places.
   *
   * @param metric a setup metric
   * @param expect the expected results
   */
  static void testMetric(Metric metric, HashMap<String, Double> expect) {
    metric.process();
    MetricResult result = metric.getMetricResult();

    // assertEquals("Result Size: ", expect.size(), result.getIdSet().size());

    for (String id : expect.keySet()) {
      //System.out.println(id+"   "+result.getResult(id));

      //System.out.println("expect.put(\""+id+"\", "+ result.getResult(id) +");");

      /*System.out.println("database.insertMockData(\n"
          + "        new MetricValueIdentifier(expectDate, metricName, station, new Channel(\""
					+id.split(",")[0]+"\",\""+ id.split(",")[1]+"\")),\n"
					+ "        "+result.getResult(id)+", ByteBuffer.wrap(DatatypeConverter.parseHexBinary(\""+printHexBinary(result.getDigest(id).array())+"\")));");
      */

      Double expected = expect.get(id);
      Double resulted = result.getResult(id);
      assertEquals(id + " result: ", expected, resulted, 1e-7);
    }
  }

  /**
   * TestMetric() takes setup metric, processes it and finally compares it to
   * an expected result set.
   *
   * Comparisons:
   * Number of results matches expectation.
   * Values of results matches corresponding expectation within 7 decimal places.
   *
   * @param metric a setup metric
   * @param expect the expected results
   */
  static void testMetric(Metric metric, MetricTestMap expect) {
    metric.process();
    MetricResult result = metric.getMetricResult();

    if (!expect.keySet().equals(result.getIdSet())) {
      String errorMessage = "Result has these traces: " + result.getIdSet()
          + "\nbut input has these traces: " + expect.keySet();
      fail(errorMessage);
    }
    ArrayList<String> channels = new ArrayList<>(expect.keySet());
    Collections.sort(channels);
    for (String id : channels) {
      //System.out.println(id+"   "+result.getResult(id));

      //System.out.println("expect.put(\""+id+"\", "+ result.getResult(id) +");");

      /*System.out.println("database.insertMockData(\n"
          + "        new MetricValueIdentifier(expectDate, metricName, station, new Channel(\""
					+id.split(",")[0]+"\",\""+ id.split(",")[1]+"\")),\n"
					+ "        "+result.getResult(id)+", ByteBuffer.wrap(DatatypeConverter.parseHexBinary(\""+printHexBinary(result.getDigest(id).array())+"\")));");
      */

      double expected = expect.getResult(id);
      double error = expect.getError(id);
      Double resulted = result.getResult(id);
      assertEquals(id + " result: ", expected, resulted, error);
    }
  }

}
