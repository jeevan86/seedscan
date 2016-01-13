package seed;

/**
 * Generic class providing static methods for converting between integer numbers
 * and byte arrays.
 * 
 * @author Philip Crotwell
 * @author Robert Casey
 * @version 6/6/2002
 */

public class Utility {

	/**
	 * Convert a single byte to a 32-bit int, with sign extension.
	 * 
	 * @param a
	 *            signed byte value
	 * @return 32-bit integer
	 */
	static int bytesToInt(byte a) {
		return (int) a; // whatever the high-order bit is set to is extended
						// into integer 32-bit space
	}

	/**
	 * Concatenate two bytes to a 32-bit int value. <b>a</b> is the high order
	 * byte in the resulting int representation, unless swapBytes is true, in
	 * which <b>b</b> is the high order byte.
	 * 
	 * @param a
	 *            high order byte
	 * @param b
	 *            low order byte
	 * @param swapBytes
	 *            byte order swap flag
	 * @return 32-bit integer
	 */
	static int bytesToInt(byte a, byte b, boolean swapBytes) {
		// again, high order bit is expressed left into 32-bit form
		if (swapBytes) {
			return (a & 0xff) + ((int) b << 8);
		} else {
			return ((int) a << 8) + (b & 0xff);
		}
	}

	/**
	 * Concatenate four bytes to a 32-bit int value. Byte order is
	 * <b>a,b,c,d</b> unless swapBytes is true, in which case the order is
	 * <b>d,c,b,a</b>. <i>Note:</i> This method will accept unsigned and signed
	 * byte representations, since high bit extension is not a concern here.
	 * Java does not support unsigned integers, so the maximum value is not as
	 * high as would be the case with an unsigned integer. To hold an unsigned
	 * 32-bit value, use uBytesToLong().
	 * 
	 * @param a
	 *            highest order byte
	 * @param b
	 *            second-highest order byte
	 * @param c
	 *            second-lowest order byte
	 * @param d
	 *            lowest order byte
	 * @param swapBytes
	 *            byte order swap flag
	 * @return 32-bit integer
	 */
	static int bytesToInt(byte a, byte b, byte c, byte d,
			boolean swapBytes) {
		if (swapBytes) {
			return ((a & 0xff)) + ((b & 0xff) << 8) + ((c & 0xff) << 16)
					+ ((d & 0xff) << 24);
		} else {
			return ((a & 0xff) << 24) + ((b & 0xff) << 16) + ((c & 0xff) << 8)
					+ ((d & 0xff));
		}
	}
}
