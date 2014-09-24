
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
	private static final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.BIG_ENDIAN;
	private ByteOrder byteOrder = DEFAULT_BYTE_ORDER;

	protected byte[] buf;
	protected ByteBuffer bb;

	public Blockette() {
		this(4);
	}

	Blockette(int bufferSize) {
		allocateBuffer(bufferSize);
		bb.position(0);
		bb.putShort(blocketteNumber());
	}

	Blockette(byte[] b) {
		reload(b);
	}

	void reload(byte[] b) {
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

	private static short peekBlocketteType(byte[] b) {
		return peekBlocketteType(b, DEFAULT_BYTE_ORDER);
	}

	private static short peekBlocketteType(byte[] b, ByteOrder order) {
		ByteBuffer wrapper = ByteBuffer.wrap(b);
		wrapper.position(0);
		wrapper.order(order);
		return wrapper.getShort();
	}

	private void allocateBuffer(int length) {
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

// TODO: Evaluate if this method is both valid and needed. It has zero references.
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
