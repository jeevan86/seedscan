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
 * Blockette1000.java
 * * The blockette 1000 has :
 * 0  i*2 blockette type  is 1000
 * 2  i*2 Next Blockette byte number (offset to next blockette)
 * 4  Encoding format - 10 = steimI, 11=steim2, 
 * 5  word order 0 little endian, 1=big endian
 * 6  record length power (2^b(6) = length) 
 * 7 reserved

 * Created on November 21, 2006, 3:30 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package seed;


/**
 * This class represents the Blockette 1000 from the SEED standard V2.4
 * Blockette1000.java * The blockette 1000 has : 0 i*2 blockette type is 1000 2
 * i*2 Next Blockette byte number (offset to next blockette) 4 Encoding format -
 * 10 = steimI, 11=steim2, 5 word order 0 little endian, 1=big endian 6 record
 * length power (2^b(6) = length) 7 reserved
 * 
 * 
 * @author davidketchum
 */
public class Blockette1000 extends Blockette {
	/**
	 * Creates a new instance of Blockette1000
	 * 
	 * @param b
	 *            The block of 8 bytes to build this blockette 1000 from
	 */
	public Blockette1000(byte[] b) {
		super(b);
	}

	@Override
	public short blocketteNumber() {
		return 1000;
	}

	public int getEncoding() {
		return ((int) buf[4]) & 0xff;
	}

	/**
	 * get record length is bytes
	 * 
	 * @return The record length in bytes (512, 4096, etc)
	 */
	public int getRecordLength() {
		return 1 << (int) buf[6];
	}

	/**
	 * get word order
	 * 
	 * @return true if bigEndian, false if little endian
	 */
	public boolean isBigEndian() {
		return (buf[5] != 0);
	}

	/**
	 * set the encoding
	 * 
	 * @param b
	 *            The encoding to use (10-steim1, 11=steim2, 19=steim3
	 */
	public void setEncoding(byte b) {
		buf[4] = b;
	}

	/**
	 * string rep
	 * 
	 * @return A string documenting this b1000 contents
	 */
	@Override
	public String toString() {
		return "(M:e=" + getEncoding() + " l=" + getRecordLength()
				+ (isBigEndian() ? "" : "S") + ")";
	}

	/**
	 * set the record length
	 * 
	 * @param len
	 *            the record length in bytes
	 */
	public void setRecordLength(int len) {
		int i = 0;
		len = len >> 1;
		while (len > 0) {
			i++;
			len = len >> 1;
		}
		buf[6] = (byte) i;
	}

}
