package asl.seedscan.metrics;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

public class TestUtils {

	/**
	 * TestMetric() takes setup metric, processes it and finally compares it to
	 * an expected result set.
	 * 
	 * Comparisons:
	 * 	Number of results matches expectation.
	 *  Values of results matches corresponding expectation within 7 decimal places.
	 * 
	 * @param metric
	 *            a setup metric
	 * @param expect
	 *            the expected results
	 * @throws Exception
	 *             any exceptions generated during process or comparison.
	 */
	static void testMetric(Metric metric, HashMap<String, Double> expect) throws Exception {
		metric.process();
		MetricResult result = metric.getMetricResult();
		
		assertEquals("Result Size: ",expect.size(), result.getIdSet().size());
		
		for (String id : result.getIdSet()) {
			//System.out.println(id+"   "+result.getResult(id));
			Double expected = (double) Math.round(expect.get(id) * 1000000d) / 1000000d;
			Double resulted = (double) Math.round(result.getResult(id) * 1000000d) / 1000000d;
			assertEquals(id + " result: ", expected, resulted);
		}
	}
}
