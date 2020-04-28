package asl.timeseries;

public class InterpolatedNHNM {

  private final static int PERIOD = 0;
  private final static int A = 1;
  private final static int B = 2;
  private final static double[][] NHNM_DATA = new double[][]{
      {0.1, -108.73, -17.23},
      {0.22, -150.34, -80.50},
      {0.32, -122.31, -23.87},
      {0.80, -116.85, 32.51},
      {3.80, -108.48, 18.08},
      {4.60, -74.66, -32.95},
      {6.30, 0.66, -127.18},
      {7.90, -93.37, -22.42},
      {15.40, 73.54, -162.98},
      {20.00, -151.52, 10.01},
      {354.80, -206.66, 31.63},
      {10000, -206.66, 31.63}};

  /**
   * Evaluation of high noise model for a given period output in Acceleration
   *
   * @param p
   *            period
   * @return new high noise model value
   */
  public static double fnhnm(double p) {
    return fnnm(NHNM_DATA, p);
  }

  /**
   * Evaluation of noise model for a given model (low or high) and period
   *
   * @param data
   *            noise model data
   * @param p
   *            period
   * @return noise value
   */
  private static double fnnm(double[][] data, double p) {
    final double nnm;
    final int lastIndex = data.length - 1;

    if (p < data[0][PERIOD]) { // if value is less than minimum
        nnm = 0.0;
    } else if (p > data[lastIndex][PERIOD]) { // if value is greater than maximum
      // New model undefined
      nnm = 0.0;
    } else {
      int k;
      for (k = 0; k < lastIndex; k++)
        if (p < data[k + 1][PERIOD])
          break;
      nnm = data[k][A] + data[k][B] * Math.log10(p);
    }

    return nnm;
  }



}
