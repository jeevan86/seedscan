// added by HPC to put in a package
//package net.alomax.freq;
// change package
package freq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 
 * This file is part of the Anthony Lomax Java Library.
 *
 * Copyright (C) 1999 Anthony Lomax <lomax@faille.unice.fr>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */





public class GaussianFilter implements FrequencyDomainProcess {
	private static final Logger logger = LoggerFactory.getLogger(freq.GaussianFilter.class);
	private SeisGramText localeText;
	public double centFreq;
	public double alpha;

	public String errorMessage;

	private static final double FREQ_MIN = Double.MIN_VALUE;
	private static final double FREQ_MAX = Double.MAX_VALUE;

	private static final double ALPHA_MIN = Double.MIN_VALUE;
	private static final double ALPHA_MAX = Double.MAX_VALUE;


	/** constructor */

	public GaussianFilter(SeisGramText localeText, double centFreq, 
							double alpha) {
		this.localeText = localeText;
		this.centFreq = centFreq;
		this.alpha = alpha;
		this.errorMessage = " ";
	}


	/** Method to set center frequency */

	public void setCentFreq(double freqValue) 
									throws FilterException {
		if (freqValue < FREQ_MIN || freqValue > FREQ_MAX) {
			//throw new FilterException(localeText.invalid_center_frequency);
			FilterException e = new FilterException(localeText.invalid_center_frequency);
			logger.error("GaussianFilter Exception:", e);
			return;
		}

		centFreq = freqValue;
	}


	/** Method to set center frequency */

	public void setCentFreq(String str)
									throws FilterException {

		double freqValue;

		try {
			freqValue = Double.valueOf(str).doubleValue();
		} catch (NumberFormatException e) {
			//throw new FilterException(localeText.invalid_center_frequency);
			logger.error("GaussianFilter NumberFormatException:", e);
			return;
		}

		setCentFreq(freqValue);
	}


	/** Method to set alpha */

	public void setAlpha(double alphaValue)
									throws FilterException {
		if (alphaValue < ALPHA_MIN || alphaValue > ALPHA_MAX) {
			//throw new FilterException(localeText.invalid_alpha_value);
			FilterException e = new FilterException(localeText.invalid_alpha_value);
			logger.error("GaussianFilter Exception:", e);
			return;
		}

		alpha = alphaValue;
	}


	/** Method to set alpha */

	public void setAlpha(String str)
									throws FilterException {

		double alphaValue;

		try {
			alphaValue = Double.valueOf(str).doubleValue();
		} catch (NumberFormatException e) {
			//throw new FilterException(localeText.invalid_alpha_value);
			logger.error("GaussianFilter NumberFormatException:", e);
			return;
		}

		setAlpha(alphaValue);
	}



	/** Method to check settings */

	public void checkSettings() throws FilterException {

		String errMessage = "";
		int badSettings = 0;

		if (centFreq < FREQ_MIN || centFreq > FREQ_MAX) {
			errMessage += ": " + localeText.invalid_center_frequency;
			badSettings++;
		}

		if (alpha < ALPHA_MIN || alpha > ALPHA_MAX) {
			errMessage += ": " + localeText.invalid_alpha_value;
			badSettings++;
		}

		if (badSettings > 0) {
			//throw new FilterException(errMessage + ".");
			FilterException e = new FilterException(errMessage + ".");
			logger.error("GaussianFilter Exception:", e);
			return;
		}

	}



	/*** function to do gaussian filter in frequency domain */
 
	public final Cmplx[] apply(double dtime, Cmplx[] cz) {

	//void gauss_filt(np, dtime, cz, fcent, alpha)
	//int np;
	//double dtime;
	//fcomplex cz[];
	//double fcent, alpha;


		double wcent = 2.0 * Math.PI * centFreq;
//System.out.println("cz.length "+cz.length+"  ((double) cz.length)) "+((double) cz.length));
		double freq0 = 2.0 * Math.PI / (((double) (cz.length - 1) + 1.0) * dtime);
		// fix bug with (double) operator in JDK 1.1.8 win32 jit compiler !!!
		//double freq0 = 2.0 * Math.PI / ((double) cz.length * dtime);

		int i1, i2;
		int np = cz.length;
		int np2 = cz.length / 2;
		double w;
		double wdiff;
		Cmplx ctf;
		Cmplx chalf = new Cmplx(0.5, 0.);
		for (int i = 0; i < np2; i++) {
			w = freq0 * (double) (i + 1);
			wdiff = (w - wcent) / wcent;
			ctf = new Cmplx(Math.exp(-alpha * wdiff * wdiff), 0.0);
			i1 = i + 1;
			i2 = np - 1 - i;
			if (i != np2 - 1) {
				cz[i1] = Cmplx.mul( cz[i1], ctf );
				cz[i2] = Cmplx.mul( cz[i2], ctf );
			} else {
				cz[i1] = Cmplx.mul( cz[i1], Cmplx.mul( ctf, chalf ) );
			}
		}
		cz[0] = new Cmplx(0., 0.);

		return(cz);

	}


}	// End class GaussianFilter


