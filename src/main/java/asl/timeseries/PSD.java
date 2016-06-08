package asl.timeseries;

import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;

/**
 * The Class PSD.
 * This computes PSDs and stores their outputs.
 *
 * @author Mike Hagerty  - hagertmb@bc.edu
 * @author James Holland - USGS
 */
public class PSD {
	
	/** The powers of the PSD. */
	private Complex[] psd = null;
	
	/** The frequencies that correspond with psd. */
	private double[] frequencies = null;
	
	/** The data x. */
	private double[] dataX = null;
	
	/** The data y. This is null if dataX and dataY were identical */
	private double[] dataY = null;
	
	/** The delta frequency. Each value in frequencies increments by this number. */
	private double deltaFrequency;
	
	/** The period. */
	private double period;
	
	/** The size of the data passed in. */
	private int dataSize;

	/**
	 * Instantiates a new PSD.
	 * Also computes the PSD based on the passed data.
	 *
	 * @param dataX the x data array
	 * @param dataY the y data array, if this is identical to dataX it is not stored. The PSD is computed using dataX only.
	 * @param period the period, must be greater than 0.
	 * @throws RuntimeException if dataX.length != dataY.length or period <= 0
	 */
	public PSD(double[] dataX, double[] dataY, double period) throws RuntimeException {
		if (dataX.length != dataY.length) {
			throw new RuntimeException("== ndataX != ndataY --> Can't create new PSD");
		}
		if (period <= 0.) {
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
		this.period = period;
		computePSD();
	}

	/**
	 * Gets the spectrum.
	 *
	 * @return the spectrum
	 */
	public final Complex[] getSpectrum() {
		return psd;
	}

	/**
	 * Gets the frequencies for the spectrum.
	 * This array increments each step by deltaF.
	 *
	 * @return the frequency array
	 */
	public final double[] getFreq() {
		return frequencies;
	}

	/**
	 * Gets the frequency delta.
	 *
	 * @return the change in frequency between spectral densities.
	 */
	public final double getDeltaF() {
		return deltaFrequency;
	}

	/**
	 * Gets the magnitudes (or absolute values) of the spectrum.
	 *
	 * @return the magnitude
	 */
	public double[] getMagnitude() {
		double[] specMag = new double[frequencies.length];
		for (int k = 0; k < frequencies.length; k++) {
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

		// For 13 windows with 75% overlap, each window will contain dataSize/4
		// points
		// TODO: Still need to handle the case of multiple datasets with gaps!

		int segmentSize = dataSize / 4;
		int segmentOffsetSize = segmentSize / 4;

		// Find smallest power of 2 >= segmentSize:
		int paddedSegmentSize = FFTUtils.getPaddedSize(segmentSize);

		int singleSideSize = paddedSegmentSize / 2 + 1;
		deltaFrequency = 1. / (paddedSegmentSize * period);

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
			
			//Using Arrays.copyOfRange() is better than a loop when dealing with HH data.
			double[] xseg = Arrays.copyOfRange(dataX, offset, segmentLastIndex);
			Timeseries.detrend(xseg);
			Timeseries.demean(xseg);
			wss = Timeseries.costaper(xseg, .10);
			xfft = FFTUtils.singleSidedFFT(xseg);
			
			//Only use yseg if dataY actually exists. Cuts computation time in half.
			if(dataY != null){
				double[] yseg = Arrays.copyOfRange(dataY, offset, segmentLastIndex);
				Timeseries.detrend(yseg);
				Timeseries.demean(yseg);
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

		double psdNormalization = 2.0 * period / (double) paddedSegmentSize;
		double windowCorrection = wss / (double) segmentSize; // =.875 for 10%
															// cosine taper
		psdNormalization = psdNormalization / windowCorrection;
		psdNormalization = psdNormalization / (double) numberSegmentsProcessed;

		frequencies = new double[singleSideSize];

		for (int k = 0; k < singleSideSize; k++) {
			psd[k] = psd[k].multiply(psdNormalization);
			frequencies[k] = (double) k * deltaFrequency;
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
