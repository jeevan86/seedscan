package asl.timeseries;

import asl.utils.FFTResult;
import asl.utils.FilterUtils;
import asl.utils.NumericUtils;
import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sac.SacHeader;
import sac.SacTimeSeries;

public class MyFilter {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.timeseries.MyFilter.class);

	public static void bandpass(SacTimeSeries sacSeries, double f1, double f2,
			double f3, double f4) {
		SacHeader hdr = sacSeries.getHeader();
		double delta = (double) hdr.getDelta();
		float[] fdata = sacSeries.getY();
		double[] data = convertFloatsToDoubles(fdata);
		bandpass(data, delta, f1, f2, f3, f4);
		fdata = convertDoublesToFloats(data);
		sacSeries.setY(fdata);
	}

	private static void bandpass(double[] timeseries, double delta, double f1,
			double f2, double f3, double f4) {

		if (!(f1 < f2 && f2 < f3 && f3 < f4)) {
			String msg = String.format(
					"bandpass: invalid freq: range: [%f-%f ----- %f-%f]", f1,
					f2, f3, f4);
			// System.out.println(msg);
			logger.error(msg);
			return;
		}
		if (delta <= 0) {
			String msg = String.format("bandpass: invalid delta dt: [%f]",
					delta);
			// System.out.println(msg);
			logger.error(msg);
			return;
		}

		int ndata = timeseries.length;

		// Find smallest power of 2 >= ndata:
		int nfft = FFTResult.findFFTPaddingLength(ndata);

		double df = 1. / (nfft * delta);

		// We are going to do an nfft point FFT which will return
		// nfft/2+1 +ve frequencies (including DC + Nyq)
		int nf = nfft / 2 + 1;

		double[] data = new double[ndata];
		System.arraycopy(timeseries, 0, data, 0, ndata);
		data = NumericUtils.detrend(data);
		NumericUtils.demean(data);
		FFTResult.cosineTaper(data, .01);

    double fNyq = (double) (nf - 1) * df; // half of sample rate

    // second parameter is sample rate -- inverse of the period, which presumably delta represents
		data = FilterUtils.bandFilter(data, 1. / delta, f2, f3, 2);

		// fft2 returns just the (nf = nfft/2 + 1) positive frequencies
		Complex[] xfft = FFTUtils.singleSidedFFT(data);



		if (f4 > fNyq) {
			f4 = fNyq;
		}
		int k1 = (int) (f1 / df);
		int k2 = (int) (f2 / df);
		int k3 = (int) (f3 / df);
		int k4 = (int) (f4 / df);



		Complex[] cfft = new Complex[nfft];
		cfft[0] = Complex.ZERO; // DC
		cfft[nf - 1] = xfft[nf - 1]; // Nyq
		for (int k = 1; k < nf - 1; k++) { // Reflect spec about the Nyquist to
											// get -ve freqs
			cfft[k] = xfft[k];
			cfft[2 * nf - 2 - k] = xfft[k].conjugate();
		}
		Complex[] inverted = FFTUtils.inverseFFT(cfft);
		//Needed to do this outside of FFTUtils because it modifies the array in place.
		for (int i = 0; i < ndata; i++) {
			timeseries[i] = inverted[i].getReal();
		}

	}

	/**
	 * Return val of cos taper at point n where taper is flat between n2 --- n3
	 * and applies cos between n1-n2 and n3-n4
	 * {@literal (i.e., it is zero for n<=n1 and n>= n4)}
	 */
	 public static double bpass(int n, int n1, int n2, int n3, int n4) {
		if (n <= n1 || n >= n4) // outside of band-pass region
			return (0.);
		else if (n >= n2 && n <= n3) // inside of band-pass region
			return (1.);
		else if (n < n2) // inside low-frequency rolloff edge
			return (.5 * (1 - Math.cos(Math.PI * (n - n1) / (n2 - n1))));
		else // inside high-frequency rolloff edge
      return (.5 * (1 - Math.cos(Math.PI * (n4 - n) / (n4 - n3))));
	}

	public static double[] convertFloatsToDoubles(float[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = (double) input[i];
		}
		return output;
	}

	public static float[] convertDoublesToFloats(double[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		float[] output = new float[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = (float) input[i];
		}
		return output;
	}
}
