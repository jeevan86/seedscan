package asl.timeseries;

import static org.junit.Assert.assertEquals;

import asl.utils.FFTResult;
import java.util.Arrays;
import org.apache.commons.math3.complex.Complex;
import org.junit.Test;

/**
 * Most of the testing here is very basic. If FFT is actually broken, it should
 * show up in the metric tests.
 *
 * @author James Holland - USGS
 */
public class FFTUtilsTest {


  @Test
  public final void testGetRealArray() throws Exception {
    int size = 5;
    double[] input = new double[size];
    Double[] output = new Double[]{0.875, 0.7499999999999999, 0.875, 0.75, 0.875};
    for (int i = 0; i < size; i++) {
      input[i] = 1;// Math.sin(i);
    }

    Complex[] fft = FFTUtils.singleSidedFFT(input);
    Complex[] inversefft = FFTUtils.inverseFFT(fft);
    double[] reals = FFTUtils.getRealArray(inversefft, size);

    for (int i = 0; i < size; i++) {
      System.out.println(inversefft[i].abs());
      assertEquals(output[i], (Double) reals[i]);
    }

  }

  @Test
  public final void testInverseFFT() throws Exception {
    int size = 8;
    double[] input = new double[size];
    for (int i = 0; i < size; i++) {
      input[i] = 1;// Math.sin(i);
    }

    Complex[] fft = FFTUtils.singleSidedFFT(input);
    double[] inverseReal = FFTResult.singleSidedInverseFFT(fft, input.length);

    // Check inverse
    for (int i = 0; i < inverseReal.length; i++) {
      assertEquals((Double) input[i], (Double) inverseReal[i]);
    }
  }

  @Test
  public final void testSingleSidedFFT() throws Exception {
    int size = 8;
    double[] input = new double[size];
    Complex[] output = new Complex[size];
    for (int i = 0; i < size; i++) {
      input[i] = 1;// Math.sin(i);
      output[i] = Complex.ZERO;
    }
    output[0] = new Complex(size, 0);

    Complex[] fft = FFTUtils.singleSidedFFT(input);
    for (int i = 0; i < fft.length; i++) {
      assertEquals(output[i], fft[i]);
    }
  }

}
