/*
 * The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 * Copyright (C) 1998-2000 University of South Carolina This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA. The current version can be found at
 * <A HREF="www.seis.sc.edu">http://www.seis.sc.edu </A> Bug reports and
 * comments should be directed to H. Philip Crotwell, crotwell@seis.sc.edu or
 * Tom Owens, owens@seis.sc.edu
 */
// package edu.sc.seis.TauP;
//package edu.sc.seis.seisFile.sac;
package sac;

import static sac.SacConstants.FALSE;
import static sac.SacConstants.IAMPH;
import static sac.SacConstants.IRLIM;
import static sac.SacConstants.ITIME;
import static sac.SacConstants.IntelByteOrder;
import static sac.SacConstants.data_offset;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 
 * IF THERE ARE ANY PROBLEMS IN ANY OF THE SAC CLASSES, CONSIDER SCRAPPING OUR
 * VERSION AND LINKING TO A JAR LIKE A NORMAL LIBRARY
 * 
 * Class that represents a sac file. All headers are have the same names as
 * within the Sac program. Can read the whole file or just the header as well as
 * write a file.
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
 * @version 1.1 Wed Feb 2 20:40:49 GMT 2000
 * @author H. Philip Crotwell
 */
public class SacTimeSeries {

	public SacTimeSeries() {
	}

	public float[] getY() {
		return y;
	}

	public void setY(float[] y) {
		this.y = y;
		getHeader().setNpts(y.length);
		if (!SacConstants.isUndef(getHeader().getDelta())
				&& !SacConstants.isUndef(getHeader().getB())) {
			getHeader().setE(
					getHeader().getB() + (y.length - 1)
					* getHeader().getDelta());
		}
	}

	public float[] getX() {
		return x;
	}

	public void setX(float[] x) {
		this.x = x;
	}

	public float[] getReal() {
		return real;
	}

	public void setReal(float[] real) {
		this.real = real;
	}

	public float[] getImaginary() {
		return imaginary;
	}

	public void setImaginary(float[] imaginary) {
		this.imaginary = imaginary;
	}

	public float[] getAmp() {
		return amp;
	}

	public void setAmp(float[] amp) {
		this.amp = amp;
	}

	public float[] getPhase() {
		return phase;
	}

	public void setPhase(float[] phase) {
		this.phase = phase;
	}

	public SacHeader getHeader() {
		return header;
	}

	public int getNumPtsRead() {
		return numPtsRead;
	}

	private SacHeader header;

	private float[] y;

	private float[] x;

	private float[] real;

	private float[] imaginary;

	private float[] amp;

	private float[] phase;

	private int numPtsRead = 0;

	public void read(File sacFile) throws IOException {
		if (sacFile.length() < data_offset) {
			throw new IOException(sacFile.getName()
					+ " does not appear to be a sac file! File size ("
					+ sacFile.length() + " is less than sac's header size ("
					+ data_offset + ")");
		}
		DataInputStream dis = new DataInputStream(new BufferedInputStream(
				new FileInputStream(sacFile)));
		try {
			header = new SacHeader(dis);
			if (header.getLeven() == 1 && header.getIftype() == ITIME) {
				if (sacFile.length() != header.getNpts() * 4 + data_offset) {
					throw new IOException(sacFile.getName()
							+ " does not appear to be a sac file! npts("
							+ header.getNpts() + ") * 4 + header("
							+ data_offset + ") !=  file length="
							+ sacFile.length() + "\n  as linux: npts("
							+ SacHeader.swapBytes(header.getNpts())
							+ ")*4 + header(" + data_offset
							+ ") !=  file length=" + sacFile.length());
				}
			} else if (header.getLeven() == 1
					|| (header.getIftype() == IAMPH || header.getIftype() == IRLIM)) {
				if (sacFile.length() != header.getNpts() * 4 * 2 + data_offset) {
					throw new IOException(
							sacFile.getName()
							+ " does not appear to be a amph or rlim sac file! npts("
							+ header.getNpts() + ") * 4 *2 + header("
							+ data_offset + ") !=  file length="
							+ sacFile.length() + "\n  as linux: npts("
							+ SacHeader.swapBytes(header.getNpts())
							+ ")*4*2 + header(" + data_offset
							+ ") !=  file length=" + sacFile.length());
				}
			} else if (header.getLeven() == 0
					&& sacFile.length() != header.getNpts() * 4 * 2
					+ data_offset) {
				throw new IOException(sacFile.getName()
						+ " does not appear to be a uneven sac file! npts("
						+ header.getNpts() + ") * 4 *2 + header(" + data_offset
						+ ") !=  file length=" + sacFile.length()
						+ "\n  as linux: npts("
						+ SacHeader.swapBytes(header.getNpts())
						+ ")*4*2 + header(" + data_offset
						+ ") !=  file length=" + sacFile.length());
			}
			readData(dis);
		} finally {
			dis.close();
		}
	}

	/** read the data portion of the given File */
	private void readData(DataInput fis) throws IOException {
		y = new float[header.getNpts()];
		readDataArray(fis, y, header.getByteOrder());
		if (header.getLeven() == FALSE || header.getIftype() == IRLIM
				|| header.getIftype() == IAMPH) {
			x = new float[header.getNpts()];
			readDataArray(fis, x, header.getByteOrder());
			if (header.getIftype() == IRLIM) {
				real = y;
				imaginary = x;
			}
			if (header.getIftype() == IAMPH) {
				amp = y;
				phase = x;
			}
		}
		numPtsRead = header.getNpts();
	}

	private static void readDataArray(DataInput fis, float[] d,
			boolean byteOrder) throws IOException {
		byte[] dataBytes = new byte[d.length * 4];
		int numAdded = 0;
		int i = 0;
		fis.readFully(dataBytes);
		while (numAdded < d.length) {
			if (byteOrder == IntelByteOrder) {
				d[numAdded++] = Float
						.intBitsToFloat(((dataBytes[i++] & 0xff))
								+ ((dataBytes[i++] & 0xff) << 8)
								+ ((dataBytes[i++] & 0xff) << 16)
								+ ((dataBytes[i++] & 0xff) << 24));
			} else {
				d[numAdded++] = Float
						.intBitsToFloat(((dataBytes[i++] & 0xff) << 24)
								+ ((dataBytes[i++] & 0xff) << 16)
								+ ((dataBytes[i++] & 0xff) << 8)
								+ ((dataBytes[i++] & 0xff)));
			}
		}
	}

}
