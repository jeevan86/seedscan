package seed;

/**
 * Util.java contains various static methods needed routinely in many places.
 *Its purpose is to hold all of these little helper functions in one place 
 *rather that creating a lot of little classes whose names would have to be 
 *remembered.
 *
 * Created on March 14, 2000, 3:58 PMt
 */

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Util extends Object {
	private static final Logger logger = LoggerFactory
			.getLogger(seed.Util.class);

	private static PrintStream out = System.out;
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
	 * prta adds time stamp to output of prt(). takes the input text and prints
	 * it out. It might go into the MSDOS window or to the SESSION.OUT file
	 * depending on the state of the debug flag. The "main" should decide on
	 * debug or not, set the flag and then all of the output will be available
	 * on the window or in the file. The file is really useful when something
	 * does not work because the user can e-mail it to us and a full debug
	 * listing is available for postmortem
	 * 
	 * @param txt
	 *            The output text
	 */
	static void prta(String txt) {
		Util.prt(Util.asctime() + " " + txt);
	}

	/**
	 * prt takes the input text and prints it out. It might go into the MSDOS
	 * window or to the SESSION.OUT file depending on the state of the debug
	 * flag. The "main" should decide on debug or not, set the flag and then all
	 * of the output will be available on the window or in the file. The file is
	 * really useful when something does not work because the user can e-mail it
	 * to us and a full debug listing is available for postmortem
	 * 
	 * @param txt
	 *            The output text
	 */
	static void prt(String txt) {
		// System.out.println("OS="+OS+" Debug="+debug_flag+" isApplet="+isApplet+" txt="+txt+" out="+out);
		out.println(txt);
		out.flush();
	}

	// This sets the default time zone to GMT so that GregorianCalendar uses GMT
	// as the local time zone!
	public static void setModeGMT() {
		TimeZone tz = TimeZone.getTimeZone("GMT+0");
		TimeZone.setDefault(tz);
	}

	/**
	 * return a time string to the hundredths of second for current time
	 * 
	 * @return the time string hh:mm:ss.hh
	 */
	private static String asctime() {
		return asctime(new GregorianCalendar());
	}

	/**
	 * return a time string to the hundredths of second from a GregorianCalendar
	 * 
	 * @param d
	 *            A gregorian calendar to translate to time hh:mm:ss.hh
	 * @return the time string hh:mm:ss.hh
	 */
	private static String asctime(GregorianCalendar d) {
		if (df == null)
			df = new DecimalFormat("00");
		return df.format(d.get(Calendar.HOUR_OF_DAY)) + ":"
				+ df.format(d.get(Calendar.MINUTE)) + ":"
				+ df.format(d.get(Calendar.SECOND)) + "."
				+ df.format((d.get(Calendar.MILLISECOND) + 5) / 10);
	}

	private static DecimalFormat df;
	private static DecimalFormat df3;

	/**
	 * return a time string to the millisecond from a GregorianCalendar
	 * 
	 * @param d
	 *            A gregorian calendar to translate to time hh:mm:ss.mmm
	 * @return the time string hh:mm:ss.mmm
	 */
	static String asctime2(GregorianCalendar d) {
		if (df == null)
			df = new DecimalFormat("00");
		if (df3 == null)
			df3 = new DecimalFormat("000");
		return df.format(d.get(Calendar.HOUR_OF_DAY)) + ":"
				+ df.format(d.get(Calendar.MINUTE)) + ":"
				+ df.format(d.get(Calendar.SECOND)) + "."
				+ df3.format(d.get(Calendar.MILLISECOND));
	}

	/***
	 * return the given GreogoianCalendar date as yyyy/mm/dd
	 * 
	 * @param d
	 *            A GregorianCalendar to translate
	 * @return The current data
	 */
	static String ascdate(GregorianCalendar d) {
		if (df == null)
			df = new DecimalFormat("00");
		return d.get(Calendar.YEAR) + "/"
				+ df.format(d.get(Calendar.MONTH) + 1) + "/"
				+ df.format(d.get(Calendar.DAY_OF_MONTH));
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
	public static String toHex(byte b) {
		return toHex(((long) b) & 0xFFL);
	}

	/**
	 * convert to hex string
	 * 
	 * @param b
	 *            The item to convert to hex
	 * @return The hex string
	 */
	public static String toHex(short b) {
		return toHex(((long) b) & 0xFFFFL);
	}

	/**
	 * convert to hex string
	 * 
	 * @param b
	 *            The item to convert to hex
	 * @return The hex string
	 */
	public static String toHex(int b) {
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
			// prt(i+" i >> j="+j+" 0xF="+val);
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
