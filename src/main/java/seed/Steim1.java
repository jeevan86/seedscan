package seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for decoding or encoding Steim1-compressed data blocks to or from an
 * array of integer values.
 * <p>
 * Steim compression scheme Copyrighted by Dr. Joseph Steim.
 * <p>
 * <dl>
 * <dt>Reference material found in:</dt>
 * <dd>
 * Appendix B of SEED Reference Manual, 2nd Ed., pp. 119-125 <i>Federation of
 * Digital Seismic Networks, et al.</i> February, 1993</dd>
 * <dt>Coding concepts gleaned from code written by:</dt>
 * <dd>Guy Stewart, IRIS, 1991</dd>
 * <dd>Tom McSweeney, IRIS, 2000</dd>
 * </dl>
 * 
 * @author Philip Crotwell (U South Carolina)
 * @author Robert Casey (IRIS DMC)
 * @version 10/22/2002
 */

class Steim1 {

	private static final Logger logger = LoggerFactory
			.getLogger(seed.Steim1.class);

	/**
	 * Decode the indicated number of samples from the provided byte array and
	 * return an integer array of the decompressed values. Being differencing
	 * compression, there may be an offset carried over from a previous data
	 * record. This offset value can be placed in <b>bias</b>, otherwise leave
	 * the value as 0.
	 * 
	 * @param b
	 *            input byte array to be decoded
	 * @param numSamples
	 *            the number of samples that can be decoded from array <b>b</b>
	 * @param swapBytes
	 *            if true, swap reverse the endian-ness of the elements of byte
	 *            array <b>b</b>.
	 * @param bias
	 *            the first difference value will be computed from this value.
	 *            If set to 0, the method will attempt to use the X(0) constant
	 *            instead.
	 * @return int array of length <b>numSamples</b>.
	 * @throws SteimException
	 *             - encoded data length is not multiple of 64 bytes.
	 */
	static int[] decode(byte[] b, int numSamples, boolean swapBytes,
			int bias) {
		// Decode Steim1 compression format from the provided byte array, which
		// contains numSamples number
		// of samples. swapBytes is set to true if the value words are to be
		// byte swapped. bias represents
		// a previous value which acts as a starting constant for continuing
		// differences integration. At the
		// very start, bias is set to 0.
		if (b.length % 64 != 0) {
			// throw new
			// SteimException("encoded data length is not multiple of 64 bytes ("
			// + b.length + ")");
			SteimException e = new SteimException(
					"encoded data length is not multiple of 64 bytes ("
							+ b.length + ")");
			logger.error("Steim1 SteimException:", e);
			return null;
		}
		int[] samples = new int[numSamples];
		int[] tempSamples;
		int numFrames = b.length / 64;
		int current = 0;
		int start = 0;
		int firstData = 0;
		int lastValue = 0;

		// System.err.println("DEBUG: number of samples: " + numSamples +
		// ", number of frames: " + numFrames + ", byte array size: " +
		// b.length);
		for (int i = 0; i < numFrames; i++) {
			// System.err.println("DEBUG: start of frame " + i);
			tempSamples = extractSamples(b, i * 64, swapBytes); // returns only
																// differences
																// except for
																// frame 0
			firstData = 0; // d(0) is byte 0 by default
			if (i == 0) { // special case for first frame
				lastValue = bias; // assign our X(-1)
				// x0 and xn are in 1 and 2 spots
				start = tempSamples[1]; // X(0) is byte 1 for frame 0
				 // X(n) is byte 2 for frame 0
				firstData = 3; // d(0) is byte 3 for frame 0
				// System.err.println("DEBUG: frame " + i + ", bias = " + bias +
				// ", x(0) = " + start + ", x(n) = " + end);
				// if bias was zero, then we want the first sample to be X(0)
				// constant
				if (bias == 0)
					lastValue = start - tempSamples[3]; // X(-1) = X(0) - d(0)
			}
			// System.err.print("DEBUG: ");
			for (int j = firstData; j < tempSamples.length
					&& current < numSamples; j++) {
				samples[current] = lastValue + tempSamples[j]; // X(n) = X(n-1)
																// + d(n)
				lastValue = samples[current];
				// System.err.print("d(" + (j-firstData) + ")" + tempSamples[j]
				// + ", x(" + current + ")" + samples[current] + ";"); // DEBUG
				current++;
			}
			// System.out.println("at frame="+i+" tempSampes="+tempSamples.length+" current="+current);
			// System.err.println("DEBUG: end of frame " + i);
		} // end for each frame...
		return samples;
	}

	/**
	 * Abbreviated, zero-bias version of decode().
	 * Used in test case to test steim1 decoding.
	 */
	static int[] decode(byte[] b, int numSamples, boolean swapBytes) // NO_UCD (test only)
			throws SteimException {
		// zero-bias version of decode
		return decode(b, numSamples, swapBytes, 0);
	}

	/**
	 * Extracts differences from the next 64 byte frame of the given compressed
	 * byte array (starting at offset) and returns those differences in an int
	 * array. An offset of 0 means that we are at the first frame, so include
	 * the header bytes in the returned int array...else, do not include the
	 * header bytes in the returned array.
	 * 
	 * @param bytes
	 *            byte array of compressed data differences
	 * @param offset
	 *            index to begin reading compressed bytes for decoding
	 * @param swapBytes
	 *            reverse the endian-ness of the compressed bytes being read
	 * @return integer array of difference (and constant) values
	 */
	private static int[] extractSamples(byte[] bytes, int offset,
			boolean swapBytes) {
		/* get nibbles */
		int nibbles = Utility.bytesToInt(bytes[offset], bytes[offset + 1],
				bytes[offset + 2], bytes[offset + 3], swapBytes);
		int currNibble = 0;
		int[] temp = new int[64]; // 4 samples * 16 longwords, can't be more
		int currNum = 0;
		// System.err.print ("DEBUG: ");
		for (int i = 0; i < 16; i++) { // i is the word number of the frame
										// starting at 0
			// currNibble = (nibbles >>> (30 - i*2 ) ) & 0x03; // count from top
			// to bottom each nibble in W(0)
			currNibble = (nibbles >> (30 - i * 2)) & 0x03; // count from top to
															// bottom each
															// nibble in W(0)
			// System.err.print("c(" + i + ")" + currNibble + ","); // DEBUG
			// Rule appears to be:
			// only check for byte-swap on actual value-atoms, so a 32-bit word
			// in of itself
			// is not swapped, but two 16-bit short *values* are or a single
			// 32-bit int *value* is, if the flag is set to TRUE. 8-bit values
			// are naturally not swapped.
			// It would seem that the W(0) word is swap-checked, though, which
			// is confusing...
			// maybe it has to do with the reference to high-order bits for c(0)
			switch (currNibble) {
			case 0:
				// System.out.println("0 means header info");
				// only include header info if offset is 0
				if (offset == 0) {
					temp[currNum++] = Utility.bytesToInt(
							bytes[offset + (i * 4)],
							bytes[offset + (i * 4) + 1], bytes[offset + (i * 4)
									+ 2], bytes[offset + (i * 4) + 3],
							swapBytes);
				}
				break;
			case 1:
				// System.out.println("1 means 4 one byte differences");
				for (int n = 0; n < 4; n++) {
					temp[currNum] = Utility.bytesToInt(bytes[offset + (i * 4)
							+ n]);
					currNum++;
				}
				break;
			case 2:
				// System.out.println("2 means 2 two byte differences");
				for (int n = 0; n < 4; n += 2) {
					temp[currNum] = Utility.bytesToInt(bytes[offset + (i * 4)
							+ n], bytes[offset + (i * 4) + n + 1], swapBytes);
					currNum++;
				}
				break;
			case 3:
				// System.out.println("3 means 1 four byte difference");
				temp[currNum++] = Utility.bytesToInt(bytes[offset + (i * 4)],
						bytes[offset + (i * 4) + 1],
						bytes[offset + (i * 4) + 2],
						bytes[offset + (i * 4) + 3], swapBytes);
				break;
			default:
				// System.out.println("default");
			}
		}
		// System.err.println("."); // DEBUG
		int[] out = new int[currNum];
		System.arraycopy(temp, 0, out, 0, currNum); // trim array to number of
													// values
		return out;
	}
}
