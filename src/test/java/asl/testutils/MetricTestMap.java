package asl.testutils;

import java.util.HashMap;
import java.util.Map;

public class MetricTestMap {

  private Map<String, ResultWithError> resultMap;

  private class ResultWithError {
    double result;
    double error;

    ResultWithError(double result, double error) {
      this.result = result;
      this.error = error;
    }
  }

  public MetricTestMap() {
    resultMap = new HashMap<>();
  }

  public void put(String key, double result, double error) {
    resultMap.put(key, new ResultWithError(result, error));
  }

  public void put(String key, double result) {
    resultMap.put(key, new ResultWithError(result, 1E-7));
  }

  public double getResult(String key) {
    return resultMap.get(key).result;
  }

  public double getError(String key) {
    return resultMap.get(key).error;
  }

  public int size() {
    return resultMap.size();
  }
}
