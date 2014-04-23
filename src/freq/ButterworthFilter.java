
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





public class ButterworthFilter implements FrequencyDomainProcess {
	private static final Logger logger = LoggerFactory.getLogger(freq.ButterworthFilter.class);
	private SeisGramText localeText;
	public double highFreqCorner;
	public double lowFreqCorner;
	public int numPoles;
	public int filterType;

	public String errorMessage;

	private static final double FREQ_MIN = 1.0e-5;
	private static final double FREQ_MAX = 1.0e5;

	private static final double NUM_POLES_MIN = 2;
	private static final double NUM_POLES_MAX = 20;

	private static final double PI = Math.PI;
	private static final double TWOPI = 2.0 * Math.PI;

        public static final int CAUSAL = 0;
        public static final int NONCAUSAL = 1;
        public static final int TWOPASS = 1;

	/** constructor */

    public ButterworthFilter(SeisGramText localeText, 
                             double lowFreqCorner, 
                             double highFreqCorner, 
                             int numPoles) 
    {
           this(localeText, lowFreqCorner, highFreqCorner, numPoles, CAUSAL);
    }

    public ButterworthFilter(SeisGramText localeText, 
                             double lowFreqCorner, 
                             double highFreqCorner, 
			     int numPoles,
			     int filterType) {
		this.localeText = localeText;
		this.highFreqCorner = highFreqCorner;
		this.lowFreqCorner = lowFreqCorner;
		this.numPoles = numPoles;
		this.filterType = filterType;
		this.errorMessage = " ";
	}


	/** Method to set high frequency corner */

	public void setHighFreqCorner(double freqValue) 
	throws FilterException {
		if (freqValue < FREQ_MIN || freqValue > FREQ_MAX) {
			throw new FilterException(localeText.invalid_high_frequency_corner);
		}

		highFreqCorner = freqValue;
	}


	/** Method to set high frequency corner */

	public void setHighFreqCorner(String str)
	throws FilterException,
		   NumberFormatException
	{
		double freqValue;

		try {
			freqValue = Double.valueOf(str).doubleValue();
		} catch (NumberFormatException e) {
			//throw new FilterException(localeText.invalid_high_frequency_corner);
			throw e;
		}
		
		try {
			setHighFreqCorner(freqValue);
		} catch (FilterException e) {
			throw e;
		}
	}


	/** Method to set low frequency corner */

	public void setLowFreqCorner(double freqValue)
	throws FilterException
	{
		if (freqValue < FREQ_MIN || freqValue > FREQ_MAX) {
			throw new FilterException(localeText.invalid_low_frequency_corner);
		}

		lowFreqCorner = freqValue;
	}


	/** Method to set low frequency corner */

	public void setLowFreqCorner(String str)
	throws FilterException,
		   NumberFormatException
	{
		double freqValue;

		try {
			freqValue = Double.valueOf(str).doubleValue();
		} catch (NumberFormatException e) {
			throw e;
		}

		try {
			setLowFreqCorner(freqValue);
		} catch (FilterException e) {
			throw e;
		}
	}


	/** Method to set number of poles */

	public void setNumPoles(int nPoles)
	throws FilterException 
	{
		if (nPoles < NUM_POLES_MIN || nPoles > NUM_POLES_MAX
				|| nPoles % 2 != 0) {
			throw new FilterException(localeText.invalid_number_of_poles);
		}

		numPoles = nPoles;
	}


	/** Method to set number of poles */

	public void setNumPoles(String str)
	throws FilterException,
		   NumberFormatException
	{
		int nPoles;

		try {
			nPoles = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			throw e;
		}

		try {
			setNumPoles(nPoles);
		} catch (FilterException e) {
			throw e;
		}
	}



	/** Method to check settings */

	void checkSettings() 
	throws FilterException 
	{
		String errMessage = "";
		int badSettings = 0;

		if (highFreqCorner < FREQ_MIN || highFreqCorner > FREQ_MAX) {
			errMessage += ": " + localeText.invalid_high_frequency_corner;
			badSettings++;
		}

		if (lowFreqCorner < FREQ_MIN || lowFreqCorner > FREQ_MAX) {
			errMessage += ": " + localeText.invalid_low_frequency_corner;
			badSettings++;
		}

		if (lowFreqCorner >= highFreqCorner) {
			errMessage += 
				": " + localeText.low_corner_greater_than_high_corner;
			badSettings++;
		}

		if (numPoles < NUM_POLES_MIN || numPoles > NUM_POLES_MAX
				|| numPoles % 2 != 0) {
			errMessage += ": " + localeText.invalid_number_of_poles;
			badSettings++;
		}

		if (badSettings > 0) {
			throw new FilterException(errMessage + ".");
		}
	}


	/***  method to do Butterworth band-pass filter in freq domain ***/
	/*
		bandpass filter  (nPBP Butterworth Filter)
 
		convolve with nPole Butterworth Bandpass filter
 
		where -
		fl    - low frequency corner in Hz
		fh    - high frequency corner in Hz
		npole - number of poles in filter at each corner
                   (not more than 20)
		npts  - number of complex fourier spectral coefficients
		dt    - sampling interval in seconds
		cx    - complex fourier spectral coefficients
	*/
 
	


}	// End class ButterworthFilter


