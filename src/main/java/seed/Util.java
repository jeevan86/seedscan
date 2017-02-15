package seed;


/**
 * Util.java contains various static methods needed routinely in many places.
 *Its purpose is to hold all of these little helper functions in one place
 *rather that creating a lot of little classes whose names would have to be
 *remembered.
 *
 * Created on March 14, 2000, 3:58 PMt
 */
class Util {
	/**
	 * Print a string in all printable characers, take non-printable to their
	 * hex vales
	 * 
	 * @param s
	 *            The string to print after conversion
	 * @return The String with non-printables converted
	 */
	static String toAllPrintable(String s) {
		byte[] b = s.getBytes();
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < b.length; i++)
			if (b[i] < 32 || b[i] == 127)
				sb.append(Util.toHex(b[i]));
			else
				sb.append(s.charAt(i));
		return sb.toString();
	}

	/**
	 * Left pad a string s to Width.
	 * 
	 * @param s
	 *            The string to pad
	 * @param width
	 *            The desired width
	 * @return The padded string to width
	 */
	static String leftPad(String s, int width) {
		String tmp = "";
		int npad = width - s.length();
		if (npad < 0)
			tmp = s.substring(0, width);
		else if (npad == 0)
			tmp = s;
		else {
			for (int i = 0; i < npad; i++)
				tmp += " ";
			tmp += s;
		}
		return tmp;
	}

	/**
	 * convert to hex string
	 * 
	 * @param b
	 *            The item to convert to hex
	 * @return The hex string
	 */
	static String toHex(byte b) {
		return toHex(((long) b) & 0xFFL);
	}

	/**
	 * convert to hex string
	 * 
	 * @param b
	 *            The item to convert to hex
	 * @return The hex string
	 */
	static String toHex(short b) {
		return toHex(((long) b) & 0xFFFFL);
	}

	/**
	 * convert to hex string
	 * 
	 * @param b
	 *            The item to convert to hex
	 * @return The hex string
	 */
	static String toHex(int b) {
		return toHex(((long) b) & 0xFFFFFFFFL);
	}

	/**
	 * convert to hex string
	 * 
	 * @param i
	 *            The item to convert to hex
	 * @return The hex string
	 */
	private static String toHex(long i) {
		StringBuilder s = new StringBuilder(16);
		int j = 60;
		int k;
		long val;
		char c;
		boolean flag = false;
		s.append("0x");

		for (k = 0; k < 16; k++) {
			val = (i >> j) & 0xf;
			if (val < 10)
				c = (char) (val + '0');
			else
				c = (char) (val - 10 + 'a');
			if (c != '0')
				flag = true;
			if (flag)
				s.append(c);
			j = j - 4;
		}
		if (!flag)
			s.append("0");
		return s.toString();
	}

}
