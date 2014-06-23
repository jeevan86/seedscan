package timeutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freq.Cmplx;

/**
 * @author Mike Hagerty <hagertmb@bc.edu>
 */
public class PSD {
	private static final Logger logger = LoggerFactory
			.getLogger(timeutils.PSD.class);
	Cmplx[] psd = null;
	double[] freq = null;
	double[] dataX = null;
	double[] dataY = null;
	double df;
	double dt;
	int ndata;

	// constructor(s)
	public PSD(double[] dataX, double[] dataY, double dt)
			throws RuntimeException {
		if (dataX.length != dataY.length) {
			throw new RuntimeException(
					"== ndataX != ndataY --> Can't create new PSD");
		}
		if (dt <= 0.) {
			throw new RuntimeException("== Invalid dt --> Can't create new PSD");
		}
		this.dataX = dataX;
		this.dataY = dataY;
		this.ndata = dataX.length;
		this.dt = dt;
		computePSD();
	}

	public final Cmplx[] getSpectrum() {
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
			specMag[k] = psd[k].mag();
		}
		return specMag;
	}

	/**
	 * computePSD
	 * 
	 * Use Peterson's algorithm (24 hrs = 13 segments with 75% overlap, etc.)
	 * 
	 * From Bendat & Piersol p.328: time segment averaging --> reduces the
	 * normalized standard error by sqrt (1 / nsegs) and increases the
	 * resolution bandwidth to nsegs * df frequency smoothing --> has same
	 * effect with nsegs replaced by nfrequencies to smooth The combination of
	 * both will reduce error by sqrt(1 / nfreqs * nsegs)
	 * 
	 * @psd[f] - Contains smoothed crosspower-spectral density computed for nf =
	 *         nfft/2 + 1 frequencies (+ve freqs + DC + Nyq)
	 * 
	 * @author Mike Hagerty
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
		// ** Still need to handle the case of multiple datasets with gaps!

		int nseg_pnts = ndata / 4;
		int noff = nseg_pnts / 4;

		// Find smallest power of 2 >= nseg_pnts:
		int nfft = 1;
		while (nfft < nseg_pnts)
			nfft = (nfft << 1);

		// We are going to do an nfft point FFT which will return
		// nfft/2+1 +ve frequencies (including DC + Nyq)
		int nf = nfft / 2 + 1;
		df = 1. / (nfft * dt);

		double[] xseg = new double[nseg_pnts];
		double[] yseg = new double[nseg_pnts];

		Cmplx[] xfft = null;
		Cmplx[] yfft = null;
		psd = new Cmplx[nf];
		double wss = 0.;

		int iwin = 0;
		int ifst = 0;
		int ilst = nseg_pnts - 1;
		int offset = 0;

		// Initialize the Cmplx PSD
		for (int k = 0; k < nf; k++) {
			psd[k] = new Cmplx(0., 0.);
		}

		while (ilst < ndata) // ndata needs to come from largest dataset
		{
			for (int k = 0; k < nseg_pnts; k++) { // Load current window
				xseg[k] = dataX[k + offset];
				yseg[k] = dataY[k + offset];
			}
			Timeseries.detrend(xseg);
			Timeseries.detrend(yseg);
			Timeseries.debias(xseg);
			Timeseries.debias(yseg);
			wss = Timeseries.costaper(xseg, .10);
			wss = Timeseries.costaper(yseg, .10);
			// MTH: Maybe want to assert here that wss > 0 to avoid
			// divide-by-zero below ??

			// fft2 returns just the (nf = nfft/2 + 1) positive frequencies
			xfft = Cmplx.fft2(xseg);
			yfft = Cmplx.fft2(yseg);

			// Load up the 1-sided PSD:
			for (int k = 0; k < nf; k++) {
				psd[k] = Cmplx.add(psd[k], Cmplx.mul(xfft[k], yfft[k].conjg()));
			}

			iwin++;
			offset += noff;
			ilst += noff;
			ifst += noff;
		} // end while
		int nwin = iwin; // Should have nwin = 13

		// Divide the summed psd[]'s by the number of windows (=13) AND
		// Normalize the PSD ala Bendat & Piersol, to units of (time series)^2 /
		// Hz AND
		// At same time, correct for loss of power in window due to 10% cosine
		// taper

		double psdNormalization = 2.0 * dt / (double) nfft;
		double windowCorrection = wss / (double) nseg_pnts; // =.875 for 10%
															// cosine taper
		psdNormalization = psdNormalization / windowCorrection;
		psdNormalization = psdNormalization / (double) nwin;

		freq = new double[nf];

		for (int k = 0; k < nf; k++) {
			psd[k] = Cmplx.mul(psd[k], psdNormalization);
			freq[k] = (double) k * df;
		}

		// We have psdC[f] so this is a good point to do any smoothing over
		// neighboring frequencies:
		int nsmooth = 11;
		int nhalf = 5;
		int nw = nf - nsmooth;
		Cmplx[] psdCFsmooth = new Cmplx[nf];

		int iw = 0;

		for (iw = 0; iw < nhalf; iw++) {
			psdCFsmooth[iw] = psd[iw];
		}

		// iw is really icenter of nsmooth point window
		for (; iw < nf - nhalf; iw++) {
			int k1 = iw - nhalf;
			int k2 = iw + nhalf;

			Cmplx sumC = new Cmplx(0., 0.);
			for (int k = k1; k < k2; k++) {
				sumC = Cmplx.add(sumC, psd[k]);
			}
			psdCFsmooth[iw] = Cmplx.div(sumC, (double) nsmooth);
		}

		// Copy the remaining point into the smoothed array
		for (; iw < nf; iw++) {
			psdCFsmooth[iw] = psd[iw];
		}

		// Copy Frequency smoothed spectrum back into psd[f] and proceed as
		// before
		for (int k = 0; k < nf; k++) {
			// psd[k] = psdCFsmooth[k].mag();
			psd[k] = psdCFsmooth[k];
		}
		// psd[0]=0; // Reset DC

	} // end computePSD

} // end class

