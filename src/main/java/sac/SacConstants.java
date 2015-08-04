//package edu.sc.seis.seisFile.sac;
package sac;

/**
 * IF THERE ARE ANY PROBLEMS IN ANY OF THE SAC CLASSES, CONSIDER SCRAPPING OUR
 * VERSION AND LINKING TO A JAR LIKE A NORMAL LIBRARY
 */

public class SacConstants {

	static boolean isUndef(float f) {
		return f == FLOAT_UNDEF;
	}

	// undef values for sac
	public static final float FLOAT_UNDEF = -12345.0f;

	public static final int INT_UNDEF = -12345;

	public static final int FALSE = 0;

	/** Time series file */
	public static final int ITIME = 1;

	/** Spectral file-real/imag */
	public static final int IRLIM = 2;

	/** Spectral file-ampl/phase */
	public static final int IAMPH = 3;

	public static final int data_offset = 632;

	public static final int NVHDR_OFFSET = 76 * 4;

	public static final boolean SunByteOrder = true;

	public static final boolean IntelByteOrder = false;

	private SacConstants() {
	}
}
