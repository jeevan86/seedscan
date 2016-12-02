//package edu.sc.seis.seisFile.sac;
package sac;

import static sac.SacConstants.FLOAT_UNDEF;
import static sac.SacConstants.INT_UNDEF;
import static sac.SacConstants.IntelByteOrder;
import static sac.SacConstants.NVHDR_OFFSET;
import static sac.SacConstants.SunByteOrder;
import static sac.SacConstants.data_offset;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * 
 * IF THERE ARE ANY PROBLEMS IN ANY OF THE SAC CLASSES, CONSIDER SCRAPPING OUR
 * VERSION AND LINKING TO A JAR LIKE A NORMAL LIBRARY
 * <a href="https://github.com/crotwell/seisFile"></a>
 * 
 * Class that represents a sac file heder. All headers are have the same names
 * as within the Sac program.
 * 
 * This reflects the sac header as of version 101.4 in utils/sac.h
 * 
 * Notes: Key to comment flags describing each field: Column 1: R required by
 * SAC (blank) optional Column 2: A = settable from a priori knowledge D =
 * available in data F = available in or derivable from SEED fixed data header T
 * = available in SEED header tables (blank) = not directly available from SEED
 * data, header tables, or elsewhere
 * 
 * 
 * @author H. Philip Crotwell
 */
public class SacHeader {
	public SacHeader() {
	};

	SacHeader(DataInput indis) throws IOException {
		readHeader(indis);
	}

	/**
	 * reads the header from the given stream. The NVHDR value (should be 6) is
	 * checked to see if byte swapping is needed. If so, all header values are
	 * byte swapped and the byteOrder is set to IntelByteOrder (false) so that
	 * the data section will also be byte swapped on read. Extra care is taken
	 * to do all byte swapping before the byte values are transformed into
	 * floats as java can do very funny things if the byte-swapped float happens
	 * to be a NaN.
	 */
	private void readHeader(DataInput indis) throws IOException {
		byte[] headerBuf = new byte[data_offset];
		indis.readFully(headerBuf);
		if (headerBuf[NVHDR_OFFSET] == 6 && headerBuf[NVHDR_OFFSET + 1] == 0
				&& headerBuf[NVHDR_OFFSET + 2] == 0
				&& headerBuf[NVHDR_OFFSET + 3] == 0) {
			byteOrder = IntelByteOrder;
			// little endian byte order, swap bytes on first 110 4-byte values
			// in header, rest are text
			for (int i = 0; i < 110 * 4; i += 4) {
				byte tmp = headerBuf[i];
				headerBuf[i] = headerBuf[i + 3];
				headerBuf[i + 3] = tmp;
				tmp = headerBuf[i + 1];
				headerBuf[i + 1] = headerBuf[i + 2];
				headerBuf[i + 2] = tmp;
			}
		} else if (!(headerBuf[NVHDR_OFFSET] == 0
				&& headerBuf[NVHDR_OFFSET + 1] == 0
				&& headerBuf[NVHDR_OFFSET + 2] == 0 && headerBuf[NVHDR_OFFSET + 3] == 6)) {
			throw new IOException(
					"Does not appear to be a SAC file, NVHDR header bytes should be (int) 6 but found "
							+ headerBuf[NVHDR_OFFSET]
							+ " "
							+ headerBuf[NVHDR_OFFSET + 1]
							+ " "
							+ headerBuf[NVHDR_OFFSET + 2]
							+ " "
							+ headerBuf[NVHDR_OFFSET + 3]);
		}
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(
				headerBuf));
		delta = dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		b = dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		dis.readFloat();
		nzyear = dis.readInt();
		nzjday = dis.readInt();
		nzhour = dis.readInt();
		nzmin = dis.readInt();
		nzsec = dis.readInt();
		nzmsec = dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		npts = dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		iftype = dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		leven = dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		dis.readInt();
		byte[] eightBytes = new byte[8];
		byte[] sixteenBytes = new byte[16];
		dis.readFully(eightBytes);
		dis.readFully(sixteenBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
		dis.readFully(eightBytes);
	}

	final static int swapBytes(int val) {
		return ((val & 0xff000000) >>> 24) + ((val & 0x00ff0000) >> 8)
				+ ((val & 0x0000ff00) << 8) + ((val & 0x000000ff) << 24);
	}

	private boolean byteOrder = SunByteOrder;

	public boolean getByteOrder() {
		return byteOrder;
	}

	/** RF time increment, sec */
	private float delta = FLOAT_UNDEF;

	/** RD initial time - wrt nz* */
	private float b = FLOAT_UNDEF;

	/** F zero time of file, yr */
	private int nzyear = INT_UNDEF;

	/** F zero time of file, day */
	private int nzjday = INT_UNDEF;

	/** F zero time of file, hr */
	private int nzhour = INT_UNDEF;

	/** F zero time of file, min */
	private int nzmin = INT_UNDEF;

	/** F zero time of file, sec */
	private int nzsec = INT_UNDEF;

	/** F zero time of file, msec */
	private int nzmsec = INT_UNDEF;

	/** RF number of samples */
	private int npts = INT_UNDEF;

	/** RA type of file */
	private int iftype = INT_UNDEF;

	/** RA data-evenly-spaced flag */
	private int leven = INT_UNDEF;

	public float getDelta() {
		return delta;
	}

	public float getB() {
		return b;
	}

	public void setE(float e) {
	}

	public int getNzyear() {
		return nzyear;
	}

	public int getNzjday() {
		return nzjday;
	}

	public int getNzhour() {
		return nzhour;
	}

	public int getNzmin() {
		return nzmin;
	}

	public int getNzsec() {
		return nzsec;
	}

	public int getNzmsec() {
		return nzmsec;
	}

	public void setNzmsec(int nzmsec) {
		this.nzmsec = nzmsec;
	}

	public int getNpts() {
		return npts;
	}

	public void setNpts(int npts) {
		this.npts = npts;
	}

	public int getIftype() {
		return iftype;
	}

	public int getLeven() {
		return leven;
	}
}
