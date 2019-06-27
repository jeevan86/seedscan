package asl.timeseries;

import asl.utils.FFTResult;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * FFTUtils is a wrapper for the Apache FFT library.
 * 
 * @author James Holland - USGS
 *
 */
public class FFTUtils {

	/**
	 * Returns the first half + 1 of the FFT.
	 *
	 * @param fullFFT
	 *            the full fft
	 * @return first half of the FFT
	 */
	private static Complex[] getFirstSide(Complex[] fullFFT) {

		Complex[] halfFFT = new Complex[fullFFT.length / 2 + 1];

		System.arraycopy(fullFFT, 0, halfFFT, 0, halfFFT.length);

		return halfFFT;
	}

	/**
	 * Zero pads an array of doubles to a length that is a power of 2.
	 * 
	 * @param data
	 *            Complex[] to pad
	 * @return the new 0 padded Complex[]
	 */
	private static Complex[] getPaddedData(Complex[] data) {
		Complex[] paddedData = new Complex[FFTResult.findFFTPaddingLength(data.length)];
		System.arraycopy(data, 0, paddedData, 0, data.length);

		for (int i = data.length; i < paddedData.length; i++) {
			paddedData[i] = Complex.ZERO;
		}

		return paddedData;
	}

	/**
	 * Zero pads an array of doubles to a length that is a power of 2.
	 * 
	 * @param data
	 *            double[] to pad
	 * @return the new 0 padded double[]
	 */
	private static double[] getPaddedData(double[] data) {
		double[] paddedData = new double[FFTResult.findFFTPaddingLength(data.length)];
		// Default value of double is 0 in java.
		System.arraycopy(data, 0, paddedData, 0, data.length);

		return paddedData;
	}
	/**
	 * Find only the real component of the Complex[]. Truncates according to
	 * length
	 * 
	 * @param data
	 *            Complex[]
	 * @param length
	 *            truncation size
	 * @return double[] of real components
	 */
	public static double[] getRealArray(Complex[] data, int length) {
		double[] realData = new double[length];
		for (int i = 0; i < length; i++) {
			realData[i] = data[i].getReal();
		}
		return realData;
	}

	/**
	 * Pads and returns a full inverse FFT.
	 * 
	 * @param data
	 *            accepts non-padded data
	 * @return padded inverse FFT
	 */
	public static Complex[] inverseFFT(Complex[] data) {
		Complex[] paddedData = getPaddedData(data);
		FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
		return fft.transform(paddedData, TransformType.INVERSE);
	}

	/**
	 * Pads and performs a forward FFT, returns paddedsize/2 + 1.
	 * 
	 * @param data
	 *            accepts non-padded data
	 * @return one side of a padded FFT.
	 */
	public static Complex[] singleSidedFFT(double[] data) {
		double[] paddedData = getPaddedData(data);

		FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
		Complex[] fullFFT = fft.transform(paddedData, TransformType.FORWARD);

		return getFirstSide(fullFFT);
	}

}
