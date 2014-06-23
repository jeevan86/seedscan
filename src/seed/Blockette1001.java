/*
 * Copyright 2011, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */

/*
 * Blockette1001.java
 *
 * Created on November 21, 2006, 3:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package seed;


/**
 * The blockette 1001 has : 0 i*2 blockette type is 1001 2 i*2 Next Blockette
 * byte number (offset to next blockette) 4 Timing quality (0-100) 5 usecs - in
 * addition to the other time 6 reserved 7 frame count (the number of frames in
 * compression section that are occupied)
 * 
 * @author davidketchum
 */
public class Blockette1001 extends Blockette {
	/**
	 * Creates a new instance of Blockette1001
	 * 
	 * @param b
	 *            a buffer with 8 raw bytes for blockette 1001
	 */
	public Blockette1001(byte[] b) {
		super(b);
	}

	@Override
	public short blocketteNumber() {
		return 1001;
	}

	/**
	 * get timingQuality
	 * 
	 * @return The timing quality (normally 0-101)
	 */
	public int getTimingQuality() {
		return ((int) buf[4]) & 0xff;
	}

	/**
	 * get used frame count
	 * 
	 * @return The number of frames marked used in this B1001
	 */
	public int getFrameCount() {
		return (int) buf[7];
	}

	/**
	 * get microseconds correction
	 * 
	 * @return the microsecs correction from b1001
	 */
	public int getUSecs() {
		return (int) buf[5];
	}

	/**
	 * set timing Quality
	 * 
	 * @param b
	 *            A timing quality byte normally (0-101)
	 */
	public void setTimingQuality(byte b) {
		buf[4] = b;
	}

	/**
	 * set the frame count used
	 * 
	 * @param len
	 *            The # of frames used in this b1001's data record
	 */
	public void setFrameCount(int len) {
		buf[7] = (byte) len;
	}

	/**
	 * set the microseconds
	 * 
	 * @param usecs
	 *            Set the microseconds part of the B1001
	 */
	public void setUSecs(int usecs) {
		buf[5] = (byte) usecs;
	}

	/**
	 * string rep
	 * 
	 * @return A string representing the data in this B1001.
	 */
	public String toString() {
		return "(MI:tQ=" + Util.leftPad("" + getTimingQuality(), 3) + " #fr="
				+ getFrameCount() + " u=" + getUSecs() + ")";
	}
}
