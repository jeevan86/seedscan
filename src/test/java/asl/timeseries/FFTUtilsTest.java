package asl.timeseries;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.complex.Complex;
import org.junit.Test;

import asl.timeseries.FFTUtils;

/**
 * Most of the testing here is very basic. If FFT is actually broken, it should
 * show up in the metric tests.
 * 
 * @author James Holland - USGS
 *
 */
public class FFTUtilsTest {

	@Test
	public final void testGetPaddedSize() throws Exception {
		Integer result = FFTUtils.getPaddedSize(0);
		Integer expected = 1;
		assertEquals(expected, result);

		result = FFTUtils.getPaddedSize(1023);
		expected = 1024;
		assertEquals(expected, result);

		result = FFTUtils.getPaddedSize(512);
		expected = 512;
		assertEquals(expected, result);

		result = FFTUtils.getPaddedSize(513);
		expected = 1024;
		assertEquals(expected, result);

		result = FFTUtils.getPaddedSize(2500000);
		expected = 4194304;
		assertEquals(expected, result);
	}

	@Test
	public final void testGetRealArray() throws Exception {
		int size = 5;
		double[] input = new double[size];
		Double[] output = new Double[] { 0.875, 0.7499999999999999, 0.875, 0.75, 0.875 };
		for (int i = 0; i < size; i++) {
			input[i] = 1;// Math.sin(i);
		}

		Complex[] fft = FFTUtils.singleSidedFFT(input);
		Complex[] inversefft = FFTUtils.inverseFFT(fft);
		double[] reals = FFTUtils.getRealArray(inversefft, size);

		for (int i = 0; i < size; i++) {
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
		Complex[] inversefft = FFTUtils.inverseFFT(fft);

		// Check inverse
		for (int i = 0; i < inversefft.length; i++) {
			assertEquals((Double) input[i], (Double) inversefft[i].getReal());
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
