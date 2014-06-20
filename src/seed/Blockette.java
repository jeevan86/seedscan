/*
 * Copyright 2012, United States Geological Survey or
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

package seed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class represents the Blockette 1000 from the SEED standard V2.4
 * Blockette.java * All blockettes start with 0 UWORD - blockette type 2 UWORD -
 * offset of next blockette within record (offset to next blockette)
 * 
 * Created on October 3, 2012, 10:38 UTC
 * 
 * 
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 */
public abstract class Blockette {
	public static final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.BIG_ENDIAN;
	private ByteOrder byteOrder = DEFAULT_BYTE_ORDER;

	protected byte[] buf;
	protected ByteBuffer bb;

	public Blockette() {
		this(4);
	}

	public Blockette(int bufferSize) {
		allocateBuffer(bufferSize);
		bb.position(0);
		bb.putShort(blocketteNumber());
	}

	public Blockette(byte[] b) {
		reload(b);
	}

	public void reload(byte[] b) {
		assert blocketteNumber() == peekBlocketteType(b);
		if ((buf == null) || (buf.length != b.length)) {
			allocateBuffer(b.length);
		}
		System.arraycopy(b, 0, buf, 0, b.length);
	}

	protected abstract short blocketteNumber();

	public void setByteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
	}

	public ByteOrder getByteOrder() {
		return byteOrder;
	}

	public static short peekBlocketteType(byte[] b) {
		return peekBlocketteType(b, DEFAULT_BYTE_ORDER);
	}

	public static short peekBlocketteType(byte[] b, ByteOrder order) {
		ByteBuffer wrapper = ByteBuffer.wrap(b);
		wrapper.position(0);
		wrapper.order(order);
		return wrapper.getShort();
	}

	protected void allocateBuffer(int length) {
		buf = new byte[length];
		bb = ByteBuffer.wrap(buf);
	}

	protected void reallocateBuffer(int length) {
		byte[] old = buf;
		allocateBuffer(length);
		if (old != null) {
			System.arraycopy(old, 0, buf, 0, Math.min(length, old.length));
		}
	}

	public void yieldBuffer(Blockette b) {
		b.buf = buf;
		b.bb = bb;
		buf = null;
		bb = null;
	}

	public byte[] getBytes() {
		return buf;
	}

	public short getBlocketteType() {
		bb.position(0);
		return bb.getShort();
	}

	public void setNextOffset(int i) {
		bb.position(2);
		bb.putShort((short) i);
	}

	public short getNextOffset() {
		bb.position(2);
		return bb.getShort();
	}
}
