package timeutils;

import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;

import asl.util.FFTUtils;

/**
 * @author Mike Hagerty  - hagertmb@bc.edu
 * @author James Holland - USGS
 */
public class PSD {
	private Complex[] psd = null;
	private double[] freq = null;
	private double[] dataX = null;
	private double[] dataY = null;
	private double df;
	private double dt;
	private int dataSize;

	// constructor(s)
	public PSD(double[] dataX, double[] dataY, double dt) throws RuntimeException {
		if (dataX.length != dataY.length) {
			throw new RuntimeException("== ndataX != ndataY --> Can't create new PSD");
		}
		if (dt <= 0.) {
			throw new RuntimeException("== Invalid dt --> Can't create new PSD");
		}
		this.dataX = dataX;
		//Check if duplicate data was given.
		if(Arrays.equals(dataX, dataY)){
			this.dataY = null;
		}
		else{
			this.dataY = dataY;
		}
		this.dataSize = dataX.length;
		this.dt = dt;
		computePSD();
	}

	public final Complex[] getSpectrum() {
		return psd;
	}

	public final double[] getFreq() {
		return freq;
	}

	public final double getDeltaF() {
		return df;
	}

	public double[] getMagnitude() {
		double[] specMag = new double[freq.length];
		for (int k = 0; k < freq.length; k++) {
			specMag[k] = psd[k].abs();
		}
		return specMag;
	}

	/**
	 * computePSD
	 * 
	 * Use Peterson's algorithm (24 hrs = 13 segments with 75% overlap, etc.)
	 * 
	 * From Bendat &amp; Piersol p.328: time segment averaging reduces the
	 * normalized standard error by sqrt (1 / nsegs) and increases the
	 * resolution bandwidth to nsegs * df frequency smoothing has same effect
	 * with nsegs replaced by nfrequencies to smooth The combination of both
	 * will reduce error by sqrt(1 / nfreqs * nsegs)
	 * 
	 * psd[f] - Contains smoothed crosspower-spectral density computed for nf =
	 * nfft/2 + 1 frequencies (+ve freqs + DC + Nyq)
	 * 
	 */
	private void computePSD() {

		// Compute PSD using the following algorithm:
		// Break up the data (one day) into 13 overlapping segments of 75%
		// Remove the trend and mean
		// Apply a taper (cosine)
		// Zero pad to a power of 2
		// Compute FFT
		// Average all 13 FFTs
		// Remove response (not done in this routine)

		// For 13 windows with 75% overlap, each window will contain ndata/4
		// points
		// TODO: Still need to handle the case of multiple datasets with gaps!

		int segmentSize = dataSize / 4;
		int segmentOffsetSize = segmentSize / 4;

		// Find smallest power of 2 >= segmentSize:
		int paddedSegmentSize = FFTUtils.getPaddedSize(segmentSize);

		int singleSideSize = paddedSegmentSize / 2 + 1;
		df = 1. / (paddedSegmentSize * dt);

		//double[] xseg = new double[segmentSize];
		//double[] yseg = new double[segmentSize];

		psd = new Complex[singleSideSize];
		double wss = 0.;

		int numberSegmentsProcessed = 0;
		int segmentLastIndex = segmentSize;
		int offset = 0;

		for (int k = 0; k < singleSideSize; k++) {
			psd[k] = Complex.ZERO;
		}

		while (segmentLastIndex <= dataSize)
		{
			Complex[] xfft = null;
			Complex[] yfft = null;
			
			double[] xseg = Arrays.copyOfRange(dataX, offset, segmentLastIndex);
			Timeseries.detrend(xseg);
			Timeseries.debias(xseg);
			wss = Timeseries.costaper(xseg, .10);
			xfft = FFTUtils.singleSidedFFT(xseg);
			
			//Only use yseg if dataY actually exists.
			if(dataY != null){
				double[] yseg = Arrays.copyOfRange(dataY, offset, segmentLastIndex);
				Timeseries.detrend(yseg);
				Timeseries.debias(yseg);
				wss = Timeseries.costaper(yseg, .10);
				yfft = FFTUtils.singleSidedFFT(yseg);
			}
			else{
				//Since dataY is null, dataY must have been equal to dataX
				yfft = xfft;
			}

			// Load up the 1-sided PSD:
			for (int k = 0; k < singleSideSize; k++) {
				psd[k] = psd[k].add(xfft[k].multiply(yfft[k].conjugate()));
			}

			numberSegmentsProcessed++;
			offset += segmentOffsetSize;
			segmentLastIndex += segmentOffsetSize;
		}

		// Divide the summed psd[]'s by the number of windows (=13) AND
		// Normalize the PSD ala Bendat & Piersol, to units of (time series)^2 /
		// Hz AND
		// At same time, correct for loss of power in window due to 10% cosine
		// taper

		double psdNormalization = 2.0 * dt / (double) paddedSegmentSize;
		double windowCorrection = wss / (double) segmentSize; // =.875 for 10%
															// cosine taper
		psdNormalization = psdNormalization / windowCorrection;
		psdNormalization = psdNormalization / (double) numberSegmentsProcessed;

		freq = new double[singleSideSize];

		for (int k = 0; k < singleSideSize; k++) {
			psd[k] = psd[k].multiply(psdNormalization);
			freq[k] = (double) k * df;
		}

		// We have psdC[f] so this is a good point to do any smoothing over
		// neighboring frequencies:
		int nsmooth = 11;
		int nhalf = 5;
		Complex[] psdCFsmooth = new Complex[singleSideSize];

		int iw = 0;

		for (iw = 0; iw < nhalf; iw++) {
			psdCFsmooth[iw] = psd[iw];
		}

		// iw is really icenter of nsmooth point window
		for (; iw < singleSideSize - nhalf; iw++) {
			int k1 = iw - nhalf;
			int k2 = iw + nhalf;

			Complex sumC = Complex.ZERO;
			for (int k = k1; k < k2; k++) {
				sumC = sumC.add(psd[k]);
			}
			psdCFsmooth[iw] = sumC.divide((double) nsmooth);
		}

		// Copy the remaining point into the smoothed array
		for (; iw < singleSideSize; iw++) {
			psdCFsmooth[iw] = psd[iw];
		}

		// Copy Frequency smoothed spectrum back into psd[f] and proceed as
		// before
		for (int k = 0; k < singleSideSize; k++) {
			// psd[k] = psdCFsmooth[k].mag();
			psd[k] = psdCFsmooth[k];
		}
	}
}
