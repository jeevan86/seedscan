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

package seed;

/**
 * Util.java contains various static methods needed routinely in many places.
 *Its purpose is to hold all of these little helper functions in one place 
 *rather that creating a lot of little classes whose names would have to be 
 *remembered.
 *
 * Created on March 14, 2000, 3:58 PMt
 */

import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("cast")
public class Util extends Object {
	private static final Logger logger = LoggerFactory
			.getLogger(seed.Util.class);
	static String process = "UNSET";
	static PrintStream stdout = System.out;
	static PrintStream stderr = System.err;
	// private static final String TRUSTSTORE_FILENAME =
	// new File(System.getProperty("user.home"), ".keystore").getPath();

	private static boolean debug_flag = false;
	private static boolean traceOn = false;
	static PrintStream out = System.out;
	static PrintStream lpt = null;
	static String OS = "";
	static String userdir;
	static String userhome;
	static String printerFile;
	static String device;
	static String node;
	static String username;

	// private static boolean isApplet = true;
	public static void setOutput(PrintStream o) {
		if (out != null)
			out.close();
		out = o;
	}

	public static PrintStream getOutput() {
		return out;
	}

	public static void setProcess(String s) {
		process = s;
	}

	public static String getProcess() {
		return process;
	}

	/**
	 * Print a string in all printable characers, take non-printable to their
	 * hex vales
	 * 
	 * @param s
	 *            The string to print after conversion
	 * @return The String with non-printables converted
	 */
	public static String toAllPrintable(String s) {
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
	 * return a line of user input (from System.in)
	 * 
	 * @return A String with input without the newline
	 */
	public static String getLine() {
		StringBuilder sb = new StringBuilder(200);
		byte[] b = new byte[50];
		boolean done = false;
		while (!done) {
			try {
				int len = System.in.read(b);
				for (int i = 0; i < len; i++) {
					if (b[i] == '\n') {
						done = true;
						break;
					}
					sb.append((char) b[i]);
				}
			} catch (IOException e) {
				// Util.IOErrorPrint(e,"Getting console input");
				logger.error("IOException:", e);
			}
		}

		return sb.toString();
	}

	/**
	 * turn trace output on or off
	 * 
	 * @param t
	 *            If true, start printing trace output
	 */
	public static void setTrace(boolean t) {
		traceOn = t;
	}

	/**
	 * if Trace is on, print out information on object and the given string
	 * 
	 * @param obj
	 *            A object to print its class name
	 * @param txt
	 *            Some text to add to class information
	 */
	public static void trace(Object obj, String txt) {
		if (traceOn)
			Util.prt("TR:" + obj.getClass().getName() + ":" + txt);
	}

	/**
	 * Use this to override the target of the prt and prta() methods from the
	 * console or session.out
	 */
	// public static void setOut(PrintStream o) { out = o;}
	/**
	 * prt takes the input text and prints it out. It might go into the MSDOS
	 * window or to the SESSION.OUT file depending on the state of the debug
	 * flag. The "main" should decide on debug or not, set the flag and then all
	 * of the output will be available on the window or in the file. The file is
	 * really useful when something does not work because the user can e-mail it
	 * to us and a full debug listing is available for postmortem
	 * 
	 * @param out
	 *            The output PrintStream to send output,
	 * @param txt
	 *            The output text
	 */
	public static void prt(PrintStream out, String txt) {
		out.println(txt);
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
	 * @param out
	 *            The output PrintStream to send output,
	 * @param txt
	 *            The output text
	 */
	public static void prta(PrintStream out, String txt) {
		out.println(Util.asctime() + " " + txt);
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
	public static void prta(String txt) {
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
	public static void prt(String txt) {
		// System.out.println("OS="+OS+" Debug="+debug_flag+" isApplet="+isApplet+" txt="+txt+" out="+out);
		out.println(txt);
		out.flush();
	}

	/**
	 * dump a buch of config info
	 */
	public static void prtinfo() {
		Util.prt("Environment : OS=" + OS + " Arch="
				+ System.getProperty("os.arch") + " version="
				+ System.getProperty("os.version") + " user name="
				+ System.getProperty("user.name") + " hm="
				+ System.getProperty("user.home") + " current dir="
				+ System.getProperty("user.dir") + "Separators file="
				+ System.getProperty("file.separator") + " path="
				+ System.getProperty("path.separator"));
		Util.prt("Java compiler=" + System.getProperty("java.compiler")
				+ " JRE version=" + System.getProperty("java.version")
				+ " JRE Manuf=" + System.getProperty("java.vendor")
				+ " Install directory=" + System.getProperty("java.home")
				+ " JRE URL=" + System.getProperty("java.url"));
		Util.prt("VM implementation version="
				+ System.getProperty("java.vm.version") + " vendor="
				+ System.getProperty("java.vm.vendor") + " name="
				+ System.getProperty("java.vm.name"));
		Util.prt("VM Specification version="
				+ System.getProperty("java.vm.specification.version")
				+ " vendor="
				+ System.getProperty("java.vm.specification.vendor") + " name="
				+ System.getProperty("java.vm.specification.name"));
		Util.prt("Class version=" + System.getProperty("java.class.version")
				+ "\nclass path=" + System.getProperty("java.class.path")
				+ "\nlibrary path=" + System.getProperty("java.library.path"));
	}

	/**
	 * set value of debug flag and hence whether Util.prt() generates output to
	 * string. If false, output will go to SESSION.OUT unless an applet
	 * 
	 * @param in
	 *            if true, set debug flag on
	 */
	public static void debug(boolean in) {
		debug_flag = in;
		if (debug_flag)
			prtinfo();
		return;
	}

	/**
	 * get state of debug flag
	 * 
	 * @return Current setting of debug flag
	 */
	public static boolean isDebug() {
		return debug_flag;
	}

	/**
	 * This routine dumps the meta data and the current values from a ResultSet.
	 * Note: the values will always return NULL if the RS is on the insertRow,
	 * even if the insertRow columns have been updated
	 * 
	 * @param rs
	 *            The resultset to print
	 */
	public static void printResultSetMetaData(ResultSet rs) {
		try {
			ResultSetMetaData md = rs.getMetaData();
			Util.prt("Insert row columns= " + md.getColumnCount());
			for (int i = 1; i <= md.getColumnCount(); i++) {
				String column = md.getColumnName(i);
				String type = md.getColumnTypeName(i);
				String txt = "" + i + " " + type + " nm: " + column
						+ " NullOK :" + md.isNullable(i) + "value=";
				if (type.equals("CHAR"))
					txt = txt + rs.getString(column);
				if (type.equals("LONG"))
					txt = txt + rs.getInt(column);
				Util.prt(txt);
			}
		} catch (SQLException e) {
			// Util.SQLErrorPrint(e,"MetaData access failed");
			logger.error("SQLException:", e);
		}
	}

	/**
	 * Clear all of the fields in a ResultSet. Used the by the New record
	 * objects to insure everything is cleared for an InsertRow so the dumb
	 * thing will actually insert something! This uses the Result set meta data
	 * to get the desciptions and types of the columns
	 * 
	 * @param rs
	 *            The resultset to clear.
	 */
	public static void clearAllColumns(ResultSet rs) {
		try {
			ResultSetMetaData md = rs.getMetaData();
			// Util.prt("ClearAllColumns= " + md.getColumnCount());
			for (int i = 1; i <= md.getColumnCount(); i++) {
				String column = md.getColumnName(i);
				String type = md.getColumnTypeName(i);
				// Util.prt("" + i + " " + type + " nm: " + column + " NullOK :"
				// +md.isNullable(i));
				// String txt = "" + i + " " + type + " nm: " + column +
				// " NullOK :" +
				// md.isNullable(i) ;
				// For each data type add an ELSE here
				int j = type.indexOf(" UNSIGNED");
				if (j > 0) {
					type = type.substring(0, j);
					// Util.prta("handle unsigend="+type);
				}
				if (type.equals("CHAR"))
					rs.updateString(column, "");
				else if (type.equals("FLOAT"))
					rs.updateFloat(column, (float) 0.);
				else if (type.equals("DOUBLE"))
					rs.updateDouble(column, (double) 0.);
				else if (type.equals("LONGLONG"))
					rs.updateLong(column, (long) 0);
				else if (type.equals("INTEGER"))
					rs.updateInt(column, (int) 0);
				else if (type.equals("BIGINT"))
					rs.updateInt(column, (int) 0);
				else if (type.equals("LONG"))
					rs.updateInt(column, 0);
				else if (type.equals("SHORT"))
					rs.updateShort(column, (short) 0);
				else if (type.equals("SMALLINT"))
					rs.updateShort(column, (short) 0);
				else if (type.equals("TINY"))
					rs.updateByte(column, (byte) 0);
				else if (type.equals("TINYINT"))
					rs.updateByte(column, (byte) 0);
				else if (type.equals("BYTE"))
					rs.updateByte(column, (byte) 0);
				else if (type.equals("VARCHAR"))
					rs.updateString(column, "");
				else if (type.equals("DATE"))
					rs.updateDate(column, new java.sql.Date((long) 0));
				else if (type.equals("DATETIME"))
					rs.updateDate(column, new java.sql.Date((long) 0));
				else if (type.equals("TIME"))
					rs.updateTime(column, new Time((long) 0));
				else if (type.equals("TINYBLOB"))
					rs.updateString(column, "");
				else if (type.equals("BLOB"))
					rs.updateString(column, "");
				else if (type.equals("MEDIUMBLOB"))
					rs.updateString(column, "");
				else if (type.equals("LONGBLOB"))
					rs.updateString(column, "");
				else if (type.equals("TINYTEXT"))
					rs.updateString(column, "");
				else if (type.equals("TEXT"))
					rs.updateString(column, "");
				else if (type.equals("MEDIUMTEXT"))
					rs.updateString(column, "");
				else if (type.equals("LONGTEXT"))
					rs.updateString(column, "");
				else if (type.equals("TIMESTAMP")) {
					java.util.Date now = new java.util.Date();
					rs.updateTimestamp(column, new Timestamp(now.getTime()));
				} else {
					System.err.println("clearAllColumn type not handled!="
							+ type + " Column=" + column);
					System.exit(0);
				}
				// Util.prt(txt);
			}
		} catch (SQLException e) {
			// Util.SQLErrorPrint(e,"MetaData access failed");
			logger.error("SQLException:", e);
		}
	}

	/**
	 * given and SQLException and local message string, dump the exception and
	 * system state at the time of the exception. This routine should be called
	 * by all "catch(SQLException) clauses to implement standard reporting.
	 * 
	 * @param E
	 *            The esction
	 * @param msg
	 *            The user supplied text to add
	 */
	public static void SQLErrorPrint(SQLException E, String msg) {
		System.err.println(asctime() + " " + msg);
		System.err.println("SQLException : " + E.getMessage());
		System.err.println("SQLState     : " + E.getSQLState());
		System.err.println("SQLVendorErr : " + E.getErrorCode());
		return;
	}

	/**
	 * given and SQLException and local message string, dump the exception and
	 * system state at the time of the exception. This routine should be called
	 * by all "catch(SQLException) clauses to implement standard reporting.
	 * 
	 * @param E
	 *            The esction
	 * @param msg
	 *            The user supplied text to add
	 * @param out
	 *            The printstream to use for outputing this exception
	 */
	public static void SQLErrorPrint(SQLException E, String msg, PrintStream out) {
		if (out == null) {
			SQLErrorPrint(E, msg);
			return;
		}
		out.println(asctime() + " " + msg);
		out.println("SQLException : " + E.getMessage());
		out.println("SQLState     : " + E.getSQLState());
		out.println("SQLVendorErr : " + E.getErrorCode());
		return;
	}

	/**
	 * given and IOException from a Socket IO and local message string, dump the
	 * exception and system state at the time of the exception. This routine
	 * should be called by all "catch(SocketException) clauses to implement
	 * standard reporting.
	 * 
	 * @param e
	 *            The esction
	 * @param msg
	 *            The user supplied text to add
	 */
	public static void SocketIOErrorPrint(IOException e, String msg) {
		SocketIOErrorPrint(e, msg, null);
		return;
	}

	/**
	 * given and IOException from a Socket IO and local message string, dump the
	 * exception and system state at the time of the exception. This routine
	 * should be called by all "catch(SocketException) clauses to implement
	 * standard reporting.
	 * 
	 * @param e
	 *            The esction
	 * @param msg
	 *            The user supplied text to add
	 * @param ps
	 *            The PrintStream to use to output this exception
	 */
	public static void SocketIOErrorPrint(IOException e, String msg,
			PrintStream ps) {
		if (ps == null)
			ps = System.out;
		if (e != null) {
			if (e.getMessage() != null) {
				if (e.getMessage().indexOf("Broken pipe") >= 0)
					ps.println("Broken pipe " + msg);
				else if (e.getMessage().indexOf("Connection reset") >= 0)
					ps.println("Connection reset " + msg);
				else if (e.getMessage().indexOf("Connection timed") >= 0)
					ps.println("Connection timed " + msg);
				else if (e.getMessage().indexOf("Socket closed") >= 0)
					ps.println("Socket closed " + msg);
				else if (e.getMessage().indexOf("Stream closed") >= 0)
					ps.println("Socket Stream closed " + msg);
				else if (e.getMessage().indexOf("Operation interrupt") >= 0)
					ps.println("Socket interrupted " + msg);
				else
					Util.IOErrorPrint(e, msg, ps);
			}
		} else
			Util.IOErrorPrint(e, msg, ps);
		return;
	}

	/**
	 * given and SocketException and local message string, dump the exception
	 * and system state at the time of the exception. This routine should be
	 * called by all "catch(SocketException) clauses to implement standard
	 * reporting.
	 * 
	 * @param E
	 *            The esction
	 * @param msg
	 *            The user supplied text to add
	 */
	public static void SocketErrorPrint(SocketException E, String msg) {
		System.err.println(asctime() + " " + msg);
		System.err.println("SocketException : " + E.getMessage());
		System.err.println("SocketCause     : " + E.getCause());
		E.printStackTrace();
		return;
	}

	/**
	 * given and IOException and local message string, dump the exception and
	 * system state at the time of the exception. This routine should be called
	 * by all "catch(IOException) clauses to implement standard reporting.
	 * 
	 * @param E
	 *            The esction
	 * @param msg
	 *            The user supplied text to add
	 */
	public static void IOErrorPrint(IOException E, String msg) {
		System.err.println(asctime() + " " + msg);
		System.err.println("SocketException : " + E.getMessage());
		System.err.println("SocketCause     : " + E.getCause());
		E.printStackTrace();
		return;
	}

	/**
	 * given and IOException and local message string, dump the exception and
	 * system state at the time of the exception. This routine should be called
	 * by all "catch(IOException) clauses to implement standard reporting.
	 * 
	 * @param E
	 *            The esction
	 * @param msg
	 *            The user supplied text to add
	 * @param out
	 *            The PrintStream to use to output this exception
	 */
	public static void IOErrorPrint(IOException E, String msg, PrintStream out) {
		if (out == null) {
			IOErrorPrint(E, msg);
			return;
		}
		out.println(asctime() + " " + msg);
		out.println("SocketException : " + E.getMessage());
		out.println("SocketCause     : " + E.getCause());
		E.printStackTrace(out);
		return;
	}

	/**
	 * given and UnknownHostException and local message string, dump the
	 * exception and system state at the time of the exception. This routine
	 * should be called by all "catch(UnknownHostException) clauses to implement
	 * standard reporting.
	 * 
	 * @param E
	 *            The esction
	 * @param msg
	 *            The user supplied text to add
	 */
	public static void UnknownHostErrorPrint(UnknownHostException E, String msg) {
		System.err.println(asctime() + " " + msg);
		System.err.println("SocketException : " + E.getMessage());
		System.err.println("SocketCause     : " + E.getCause());
		E.printStackTrace();
		return;
	}

	/**
	 * sleep the give number of milliseconds
	 * 
	 * @param ms
	 *            THe number of millis to sleep
	 */
	public static void sleep(int ms) {
		try {
			Thread.sleep(Math.max(1, ms));
		} catch (InterruptedException e) {
			logger.error("InterruptedException:", e);
		}
	}

	/**
	 * Escape a string for use in an SQL query. The string is returned enclosed
	 * in single quotes with any dangerous characters escaped.
	 * 
	 * This is modeled after escape_string_for_mysql from the MySQL API. Beware
	 * that the characters '%' and '_' are not escaped. They do not have special
	 * meaning except in LIKE clauses.
	 * 
	 * @param s
	 *            The string to be escaped
	 * @return The escaped string
	 */
	@SuppressWarnings("fallthrough") //We want the fall through in the switch.
	public static String sqlEscape(String s) {
		StringBuilder result;
		int i;
		char c;

		result = new StringBuilder();
		result.append('\'');
		for (i = 0; i < s.length(); i++) {
			c = s.charAt(i);
			switch (c) {
			case '\0':
				result.append("\\0");
				break;
			case '\n':
				result.append("\\n");
				break;
			case '\r':
				result.append("\\r");
				break;
			case '\032':
				result.append("\\Z");
			case '\\':
			case '\'':
			case '"':
				result.append("\\" + c);
				break;
			default:
				result.append(c);
				break;
			}
		}
		result.append('\'');

		return result.toString();
	}

	/**
	 * Escape an int for use in an SQL query.
	 * 
	 * @param i
	 *            The int to be escaped
	 * @return The escaped string
	 */
	public static String sqlEscape(int i) {
		return Integer.toString(i);
	}

	/**
	 * Escape a long for use in an SQL query.
	 * 
	 * @param l
	 *            The long to be escaped
	 * @return The escaped string
	 */
	public static String sqlEscape(long l) {
		return Long.toString(l);
	}

	/**
	 * Escape a Date for use in an SQL query. The string returned is in the form
	 * "{d 'yyyy-MM-dd'}".
	 * 
	 * @param d
	 *            The Date to be escaped
	 * @return The escaped string
	 * @see <a
	 *      href="http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html">http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html</a>
	 */
	public static String sqlEscape(java.sql.Date d) {
		return "{d " + sqlEscape(d.toString()) + "}";
	}

	/**
	 * Escape a Time for use in an SQL query. The string returned is in the form
	 * "{t 'hh:mm:ss'}".
	 * 
	 * @param t
	 *            The Time to be escaped
	 * @return The escaped string
	 * @see <a
	 *      href="http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html">http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html</a>
	 */
	public static String sqlEscape(Time t) {
		return "{t " + sqlEscape(t.toString()) + "}";
	}

	/**
	 * Escape a Timestamp for use in an SQL query. The string returned is in the
	 * form "{ts 'yyyy-MM-dd hh:mm:ss.ffffffff'}".
	 * 
	 * @param ts
	 *            The Timestamp to be escaped
	 * @return The escaped string
	 * @see <a
	 *      href="http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html">http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html</a>
	 */
	public static String sqlEscape(Timestamp ts) {
		return "{ts " + sqlEscape(ts.toString()) + "}";
	}

	/**
	 * ResultSets often come with "NULL" and return nulls, we preferr actual
	 * objects with appropriate values. Return a "" if result is null.
	 * 
	 * @param rs
	 *            The ResultSet to get this String column from
	 * @param column
	 *            The name of the column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQLExceptions
	 */
	public static String getString(ResultSet rs, String column)
			throws SQLException {
		// try {
		String t = rs.getString(column);
		// Util.prt("Util.getString for " + column + "=" +t + "| wasnull="
		// +rs.wasNull());
		if (rs.wasNull())
			t = "";
		// } catch (SQLException e) {throw e};
		return t;
	}

	/**
	 * ResultSets often come with "NULL" and return nulls, we preferr actual
	 * objects with appropriate values. Return a "" if result is null.
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQLExceptions
	 */
	public static String getString(ResultSet rs, int column)
			throws SQLException {
		// try {
		String t = rs.getString(column);
		// Util.prt("Util.getString for " + column + "=" +t + "| wasnull="
		// +rs.wasNull());
		if (rs.wasNull())
			t = "";
		// } catch (SQLException e) {throw e};
		return t;
	}

	/**
	 * get and integer from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQLExceptions
	 */
	public static int getInt(ResultSet rs, String column) throws SQLException {
		// try {
		int i = rs.getInt(column);
		if (rs.wasNull())
			i = 0;
		// } catch (SQLException e) { throw e}
		return i;
	}

	/**
	 * get and integer from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQLExceptions
	 */
	public static int getInt(ResultSet rs, int column) throws SQLException {
		// try {
		int i = rs.getInt(column);
		if (rs.wasNull())
			i = 0;
		// } catch (SQLException e) { throw e}
		return i;
	}

	/**
	 * get a long from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQLExceptions
	 */
	public static long getLong(ResultSet rs, String column) throws SQLException {
		// try {
		long i = rs.getLong(column);
		if (rs.wasNull())
			i = 0;
		// } catch (SQLException e) { throw e}
		return i;
	}

	/**
	 * get a long from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQLExceptions
	 */
	public static long getLong(ResultSet rs, int column) throws SQLException {
		// try {
		long i = rs.getLong(column);
		if (rs.wasNull())
			i = 0;
		// } catch (SQLException e) { throw e}
		return i;
	}

	/**
	 * get a short from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQLExceptions
	 */
	public static short getShort(ResultSet rs, String column)
			throws SQLException {
		// try {
		short i = rs.getShort(column);
		if (rs.wasNull())
			i = 0;
		// } catch (SQLException e) { throw e}
		return i;
	}

	/**
	 * get a short from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQLExceptions
	 */
	public static short getShort(ResultSet rs, int column) throws SQLException {
		// try {
		short i = rs.getShort(column);
		if (rs.wasNull())
			i = 0;
		// } catch (SQLException e) { throw e}
		return i;
	}

	/**
	 * get a byte from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQLExceptions
	 */
	public static byte getByte(ResultSet rs, int column) throws SQLException {
		// try {
		byte i = rs.getByte(column);
		if (rs.wasNull())
			i = 0;
		// } catch (SQLException e) { throw e}
		return i;
	}

	/**
	 * get a double from ResultSet rs with name 'column' /** get a short from
	 * ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQL error
	 */
	public static double getDouble(ResultSet rs, String column)
			throws SQLException {
		// try {
		double i = rs.getDouble(column);
		if (rs.wasNull())
			i = 0;
		// } catch (SQLException e) { throw e}
		return i;
	}

	/**
	 * get a double from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQL error
	 */
	public static double getDouble(ResultSet rs, int column)
			throws SQLException {
		// try {
		double i = rs.getDouble(column);
		if (rs.wasNull())
			i = 0;
		// } catch (SQLException e) { throw e}
		return i;
	}

	/**
	 * get a float from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQL error
	 */
	public static float getFloat(ResultSet rs, String column)
			throws SQLException {
		// try {
		float i = rs.getFloat(column);
		if (rs.wasNull())
			i = 0;
		// } catch (SQLException e) { throw e}
		return i;
	}

	/**
	 * get a float from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQL error
	 */
	public static float getFloat(ResultSet rs, int column) throws SQLException {
		// try {
		float i = rs.getFloat(column);
		if (rs.wasNull())
			i = 0;
		// } catch (SQLException e) { throw e}
		return i;
	}

	/**
	 * get a Timestamp from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQL error
	 */
	public static Timestamp getTimestamp(ResultSet rs, String column)
			throws SQLException {
		Timestamp i = rs.getTimestamp(column);
		if (rs.wasNull())
			i = new Timestamp(0);
		return i;
	}

	/**
	 * get a Timestamp from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQL error
	 */
	public static Timestamp getTimestamp(ResultSet rs, int column)
			throws SQLException {
		Timestamp i = rs.getTimestamp(column);
		if (rs.wasNull())
			i = new Timestamp(0);
		return i;
	}

	/**
	 * get a date from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQL error
	 */
	public static java.sql.Date getDate(ResultSet rs, String column)
			throws SQLException {
		java.sql.Date i = rs.getDate(column);
		if (rs.wasNull())
			i = new java.sql.Date((long) 0);
		return i;
	}

	/**
	 * get a date from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQL error
	 */
	public static java.sql.Date getDate(ResultSet rs, int column)
			throws SQLException {
		java.sql.Date i = rs.getDate(column);
		if (rs.wasNull())
			i = new java.sql.Date((long) 0);
		return i;
	}

	/**
	 * get a Time from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQL error
	 */
	public static Time getTime(ResultSet rs, String column) throws SQLException {
		Time i = rs.getTime(column);
		if (rs.wasNull())
			i = new Time(0);
		return i;
	}

	/**
	 * get a Time from ResultSet rs with name 'column'
	 * 
	 * @param rs
	 *            The ResultSet to get this column from
	 * @param column
	 *            The column to get
	 * @return The target column value
	 * @throws SQLException
	 *             if column does not exist or other SQL error
	 */
	public static Time getTime(ResultSet rs, int column) throws SQLException {
		Time i = rs.getTime(column);
		if (rs.wasNull())
			i = new Time(0);
		return i;
	}

	/**
	 * We will represent Times as hh/dd and trade them back and forth with
	 * sister routine stringToTime()
	 * 
	 * @param t
	 *            The time to convert to a String
	 * @return The String with time in hh:mm am/pm
	 */
	public static String timeToString(Time t) {
		String s = t.toString();
		s = s.substring(0, 5);
		if (s.substring(0, 2).compareTo("12") > 0) {
			int ih = Integer.parseInt(s.substring(0, 2)) - 12;
			s = "" + ih + s.substring(2, 5);
			s = s + " pm";
		} else {
			if (s.substring(0, 2).equals("12"))
				s = s + " pm";
			else
				s = s + " am";
		}
		return s;
	}

	/**
	 * convert string to a time in the normal form hh:mm to SQL Time type
	 * 
	 * @param s
	 *            The String to convert to a time
	 * @return The Time
	 */
	public static Time stringToTime(String s) {
		int ih, im;
		String ampm;
		StringTokenizer tk = new StringTokenizer(s, ": ");
		if (tk.countTokens() < 2) {
			Util.prt("stringToTime not enough tokens s=" + s + " cnt="
					+ tk.countTokens());
			return new Time((long) 0);
		}
		String hr = tk.nextToken();
		String mn = tk.nextToken();
		if (debug_flag)
			Util.prt("time to String hr=" + hr + " min=" + mn);
		try {
			ih = Integer.parseInt(hr);
			im = Integer.parseInt(mn);
		} catch (NumberFormatException e) {
			Util.prt("Time: not a integers " + hr + ":" + mn + " string=" + s);
			logger.warn("NumberFormatException:", e);
			return new Time((long) 0);
		}
		if (tk.hasMoreTokens()) {
			ampm = tk.nextToken();
			if (debug_flag)
				Util.prt("timeToString ampm=" + ampm + " is pm="
						+ ampm.equalsIgnoreCase("pm"));
			if (ampm.equalsIgnoreCase("pm") && ih != 12)
				ih += 12;
			else if (ampm.equalsIgnoreCase("am")) {
			} else {
				if (debug_flag)
					Util.prt("Time add on not AM or PM =" + s);
				if (ih < 8)
					ih += 12; // We do not play before 8
			}
		} else {
			if (ih < 8)
				ih += 12; // We do not play before 8

		}

		Time t = new Time((long) ih * 3600000 + im * 60000);
		return t;
	}

	// This sets the default time zone to GMT so that GregorianCalendar uses GMT
	// as the local time zone!
	public static void setModeGMT() {
		TimeZone tz = TimeZone.getTimeZone("GMT+0");
		TimeZone.setDefault(tz);
	}

	/**
	 * Create a SQL date from a year, month, day int. The SQL date comes from MS
	 * since 1970 but the "gregorianCalendar" Class likes to use MONTH based on
	 * 0=January. This does the right Thing so I wont forget later!
	 * 
	 * @param year
	 *            The year
	 * @param month
	 *            The month
	 * @param day
	 *            The day of month
	 * @return Date in sql form
	 */
	public static java.sql.Date date(int year, int month, int day) {
		GregorianCalendar d = new GregorianCalendar(year, month - 1, day);
		return new java.sql.Date(d.getTime().getTime());
	}

	/**
	 * Create a Java date from a year, month, day, hr, min,sec . The SQL date
	 * comes from MS since 1970 but the "gregorianCalendar" Class likes to use
	 * MONTH based on 0=January. This does the right Thing so I wont forget
	 * later!
	 * 
	 * @param year
	 *            The year
	 * @param month
	 *            The month
	 * @param day
	 *            The day of month
	 * @param hr
	 *            The hour of day
	 * @param min
	 *            The minute
	 * @param sec
	 *            The second
	 * @return Date in sql form
	 */
	public static java.util.Date date(int year, int month, int day, int hr,
			int min, int sec) {
		GregorianCalendar d = new GregorianCalendar(year, month - 1, day, hr,
				min, sec);
		return new java.util.Date(d.getTime().getTime());
	}

	/**
	 * return current date (based on system time) as an SQL Date
	 * 
	 * @return The current date as an SQL date
	 */
	public static java.sql.Date today() {
		GregorianCalendar d = new GregorianCalendar();
		// Util.prt("Year="+d.get(Calendar.YEAR)+" mon="+d.get(Calendar.MONTH));
		// Util.prt("d="+d.toString());
		return new java.sql.Date(d.getTime().getTime());
	}

	static GregorianCalendar gstat;

	/**
	 * get a gregorian calendar given a yymmdd encoded date and msecs
	 * 
	 * @param yymmdd
	 *            The encoded date
	 * @param msecs
	 *            Millis since midnight
	 * @return The number of millis since 1970 per GregorianCalendar
	 */
	public synchronized static Long toGregorian2(int yymmdd, int msecs)
			throws RuntimeException {
		if (gstat == null) {
			gstat = new GregorianCalendar();
			gstat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		}
		int yr = yymmdd / 10000;
		if (yr < 100)
			yr = yr + 2000;
		yymmdd = yymmdd - yr * 10000;
		int mon = yymmdd / 100;
		int day = yymmdd % 100;
		int hr = msecs / 3600000;
		msecs = msecs - hr * 3600000;
		int min = msecs / 60000;
		msecs = msecs - min * 60000;
		int secs = msecs / 1000;
		msecs = msecs - secs * 1000;
		if (yr < 2000 || yr > 2030 || mon <= 0 || mon > 12 || hr < 0 || hr > 23
				|| min < 0 || min > 59 || secs < 0 || secs > 59 || msecs < 0
				|| msecs > 999) {
			throw new RuntimeException("toGregorian data out of range yr=" + yr
					+ " mon=" + mon + " day=" + day + " " + hr + ":" + min
					+ ":" + secs + "." + msecs);
		}
		gstat.set(yr, mon - 1, day, hr, min, secs);
		gstat.add(Calendar.MILLISECOND, msecs);
		return gstat.getTimeInMillis();
	}

	public synchronized static GregorianCalendar toGregorian(int yymmdd,
			int msecs) throws RuntimeException {
		int yr = yymmdd / 10000;
		if (yr < 100)
			yr = yr + 2000;
		yymmdd = yymmdd - yr * 10000;
		int mon = yymmdd / 100;
		int day = yymmdd % 100;
		int hr = msecs / 3600000;
		msecs = msecs - hr * 3600000;
		int min = msecs / 60000;
		msecs = msecs - min * 60000;
		int secs = msecs / 1000;
		msecs = msecs - secs * 1000;
		if (yr < 2000 || yr > 2030 || mon <= 0 || mon > 12 || hr < 0 || hr > 23
				|| min < 0 || min > 59 || secs < 0 || secs > 59 || msecs < 0
				|| msecs > 999) {
			throw new RuntimeException("toGregorian data out of range yr=" + yr
					+ " mon=" + mon + " day=" + day + " " + hr + ":" + min
					+ ":" + secs + "." + msecs);
		}
		GregorianCalendar now = new GregorianCalendar();
		now.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		now.set(yr, mon - 1, day, hr, min, secs);
		now.add(Calendar.MILLISECOND, msecs);
		// Util.prt(yr+"/"+mon+"/"+day+" "+hr+":"+min+":"+secs+"."+msecs+" "+Util.asctime(now)+" "+Util.ascdate(now));
		return now;
	}

	/**
	 * given a gregorian calendar return a date encoded yymmdd
	 * 
	 * @param d
	 *            The gregoriancalendar to convert
	 * @return The yymmdd encoded date
	 */
	public static int yymmddFromGregorian(GregorianCalendar d) {
		return d.get(Calendar.YEAR) * 10000 + (d.get(Calendar.MONTH) + 1) * 100
				+ d.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * given a gregorian calendar return a millis since midngith
	 * 
	 * @param d
	 *            The gregoriancalendar to convert
	 * @return The millis since midnight
	 */

	public static int msFromGregorian(GregorianCalendar d) {
		// Util.prt("timeinms="+d.getTimeInMillis());
		return (int) (d.getTimeInMillis() % 86400000L);
	}

	/**
	 * return a time string to the hundredths of second for current time
	 * 
	 * @return the time string hh:mm:ss.hh
	 */
	public static String asctime() {
		return asctime(new GregorianCalendar());
	}

	/**
	 * return a time string to the hundredths of second from a GregorianCalendar
	 * 
	 * @param d
	 *            A gregorian calendar to translate to time hh:mm:ss.hh
	 * @return the time string hh:mm:ss.hh
	 */
	public static String asctime(GregorianCalendar d) {
		if (df == null)
			df = new DecimalFormat("00");
		return df.format(d.get(Calendar.HOUR_OF_DAY)) + ":"
				+ df.format(d.get(Calendar.MINUTE)) + ":"
				+ df.format(d.get(Calendar.SECOND)) + "."
				+ df.format((d.get(Calendar.MILLISECOND) + 5) / 10);
	}

	/**
	 * return a time string to the hundredths of second from a GregorianCalendar
	 * 
	 * @param ms
	 *            A long with a ms from a GregorianCalendar etc
	 * @return the time string hh:mm:ss.hh
	 */
	public static String asctime(long ms) {
		if (gstat == null) {
			gstat = new GregorianCalendar();
			gstat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		}
		gstat.setTimeInMillis(ms);
		if (df == null)
			df = new DecimalFormat("00");
		return df.format(gstat.get(Calendar.HOUR_OF_DAY)) + ":"
				+ df.format(gstat.get(Calendar.MINUTE)) + ":"
				+ df.format(gstat.get(Calendar.SECOND)) + "."
				+ df.format((gstat.get(Calendar.MILLISECOND) + 5) / 10);
	}

	static DecimalFormat df;
	static DecimalFormat df3;

	/**
	 * return a time string to the millisecond from a GregorianCalendar
	 * 
	 * @param d
	 *            A gregorian calendar to translate to time hh:mm:ss.mmm
	 * @return the time string hh:mm:ss.mmm
	 */
	public static String asctime2(GregorianCalendar d) {
		if (df == null)
			df = new DecimalFormat("00");
		if (df3 == null)
			df3 = new DecimalFormat("000");
		return df.format(d.get(Calendar.HOUR_OF_DAY)) + ":"
				+ df.format(d.get(Calendar.MINUTE)) + ":"
				+ df.format(d.get(Calendar.SECOND)) + "."
				+ df3.format(d.get(Calendar.MILLISECOND));
	}

	/**
	 * return a time string to the millisecond from a GregorianCalendar
	 * 
	 * @param ms
	 *            A milliseconds (1970 datum) to translate to time hh:mm:ss.mmm
	 * @return the time string hh:mm:ss.mmm
	 */
	public static String asctime2(long ms) {
		if (gstat == null) {
			gstat = new GregorianCalendar();
			gstat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		}
		gstat.setTimeInMillis(ms);
		if (df == null)
			df = new DecimalFormat("00");
		if (df3 == null)
			df3 = new DecimalFormat("000");
		return df.format(gstat.get(Calendar.HOUR_OF_DAY)) + ":"
				+ df.format(gstat.get(Calendar.MINUTE)) + ":"
				+ df.format(gstat.get(Calendar.SECOND)) + "."
				+ df3.format(gstat.get(Calendar.MILLISECOND));
	}

	/**
	 * give a ip address as 4 bytes convert it to a dotted string
	 * 
	 * @param ip
	 *            Four bytes with raw IP address
	 * @param offset
	 *            An offset in ip where the four raw bytes start
	 * @return string of form nnn.nnn.nnn.nnn with leading zeros to fill out
	 *         space
	 */
	public static String stringFromIP(byte[] ip, int offset) {
		if (df == null)
			df = new DecimalFormat("00");
		if (df3 == null)
			df3 = new DecimalFormat("000");
		return df3.format(((int) ip[offset] & 0xff)) + "."
				+ df3.format(((int) ip[offset + 1] & 0xff)) + "."
				+ df3.format(((int) ip[offset + 2] & 0xff)) + "."
				+ df3.format(((int) ip[offset + 3] & 0xff));

	}

	/***
	 * return the current date as yyyy/mm/dd
	 * 
	 * @return The current data
	 */
	public static String ascdate() {
		return ascdate(new GregorianCalendar());
	}

	/**
	 * return the current date as a yyyy_DDD string
	 * 
	 * @return YYYY_DDD of the current date
	 */
	public static String toDOYString() {
		if (gstat == null)
			gstat = new GregorianCalendar();
		gstat.setTimeInMillis(System.currentTimeMillis());
		return toDOYString(gstat);
	}

	/**
	 * return a DOY formated string from a GregoianCalendar
	 * 
	 * @param gc
	 *            The GregorianCalendar
	 * @return string of form YYYY,DDD,HH:MM:SS
	 */
	public static String toDOYString(GregorianCalendar gc) {
		return gc.get(Calendar.YEAR)
				+ ","
				+ Util.leftPad("" + gc.get(Calendar.DAY_OF_YEAR), 3)
						.replaceAll(" ", "0")
				+ ","
				+ Util.leftPad("" + gc.get(Calendar.HOUR_OF_DAY), 2)
						.replaceAll(" ", "0")
				+ ":"
				+ Util.leftPad("" + gc.get(Calendar.MINUTE), 2).replaceAll(" ",
						"0")
				+ ":"
				+ Util.leftPad("" + gc.get(Calendar.SECOND), 2).replaceAll(" ",
						"0")
				+ "."
				+ Util.leftPad("" + gc.get(Calendar.MILLISECOND), 3)
						.replaceAll(" ", "0");
	}

	/**
	 * return a DOY formated string from a TimeStamp
	 * 
	 * @param ts
	 *            The time stamp
	 * @return string of form YYYY,DDD,HH:MM:SS
	 */
	public static String toDOYString(Timestamp ts) {
		if (gstat == null) {
			gstat = new GregorianCalendar();
			gstat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		}
		gstat.setTimeInMillis(ts.getTime());
		return toDOYString(gstat);
	}

	/***
	 * return the given GreogoianCalendar date as yyyy/mm/dd
	 * 
	 * @param d
	 *            A GregorianCalendar to translate
	 * @return The current data
	 */
	public static String ascdate(GregorianCalendar d) {
		if (df == null)
			df = new DecimalFormat("00");
		return d.get(Calendar.YEAR) + "/"
				+ df.format(d.get(Calendar.MONTH) + 1) + "/"
				+ df.format(d.get(Calendar.DAY_OF_MONTH));
	}

	/***
	 * return the given GreogoianCalendar date as yyyy/mm/dd
	 * 
	 * @param ms
	 *            A miliseconds value to translate (1970 or GregorianCalendar
	 *            datum)
	 * @return The current data
	 */
	public static String ascdate(long ms) {
		if (gstat == null) {
			gstat = new GregorianCalendar();
			gstat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		}
		gstat.setTimeInMillis(ms);
		if (df == null)
			df = new DecimalFormat("00");
		return gstat.get(Calendar.YEAR) + "/"
				+ df.format(gstat.get(Calendar.MONTH) + 1) + "/"
				+ df.format(gstat.get(Calendar.DAY_OF_MONTH));
	}

	/**
	 * return current date (based on system time) as an SQL Date
	 * 
	 * @return the current time in SQL Time form
	 */
	public static Time time() {
		GregorianCalendar d = new GregorianCalendar();
		// Util.prt("Year="+d.get(Calendar.YEAR)+" mon="+d.get(Calendar.MONTH));
		// Util.prt("d="+d.toString());
		return new Time(d.getTime().getTime());
	}

	/**
	 * return current date (based on system time) as an SQL Date
	 * 
	 * @return The curent time/date as a Timestamp
	 */
	public static Timestamp now() {
		GregorianCalendar d = new GregorianCalendar();
		// Util.prt("Year="+d.get(Calendar.YEAR)+" mon="+d.get(Calendar.MONTH));
		// Util.prt("d="+d.toString());
		// Util.prt("time in millis="+d.getTimeInMillis());
		return new Timestamp(d.getTimeInMillis());
	}

	/**
	 * get time in millis (this is the same as System.currentTimeInMillis()
	 * 
	 * @return THe current time in millis
	 */
	public static long getTimeInMillis() {
		return new GregorianCalendar().getTimeInMillis();
	}

	/**
	 * dateToString takes a JDBC date and makes it a mm/dd string
	 * 
	 * @param d
	 *            ate to translate to string
	 * @return The ascii string in yyyy-mm-dd
	 */
	public static String dateToString(java.sql.Date d) {
		if (d == null)
			return "";
		String s = d.toString(); // returns yyyy-mm-dd
		if (s == null)
			return "";
		if (s.equals("null"))
			return "";
		// Util.prt("datetostring="+s);
		StringTokenizer tk = new StringTokenizer(s, "-");

		int yr = Integer.parseInt(tk.nextToken());
		int mon = Integer.parseInt(tk.nextToken());
		int day = Integer.parseInt(tk.nextToken());
		return "" + mon + "/" + day + "/" + yr;

	}

	/**
	 * return the current system time as an SQL Timestamp.
	 * 
	 * @return the current time as a Timestamp
	 */
	public static Timestamp TimestampNow() {
		java.util.Date now = new java.util.Date();
		return new Timestamp(now.getTime());
	}

	/**
	 * Convert a mm/dd string to a full SQL Date
	 * 
	 * @param s
	 *            string to decode to a sql date yyyy/mm/dd
	 * @return The sql date from the string
	 */
	public static java.sql.Date stringToDate(String s) {
		StringTokenizer tk = new StringTokenizer(s, "/");
		if (tk.countTokens() < 2) {
			Util.prt("stringToDate no enough Tokens s=" + s + " cnt="
					+ tk.countTokens());
			return Util.date(1970, 1, 1);
		}
		String mon = tk.nextToken();
		String day = tk.nextToken();
		String yr;
		int m, d, y;
		if (tk.hasMoreTokens()) {
			yr = tk.nextToken();
		} else
			yr = "" + Util.year();
		try {
			m = Integer.parseInt(mon);
			d = Integer.parseInt(day);
			y = Integer.parseInt(yr);
		} catch (NumberFormatException e) {
			Util.prt("dateToString() Month or day not a int mon=" + mon
					+ " day=" + day);
			logger.warn("NumberFormatException:", e);
			return Util.date(Util.year(), 1, 1);
		}
		if (m <= 0 || m > 12) {
			Util.prt("stringToDate : bad month = " + mon + " s=" + s);
			return Util.date(1970, 1, 1);
		}
		if (d <= 0 || d > 31) {
			Util.prt("stringToDate : bad day = " + day + " s=" + s);
			return Util.date(1970, 1, 1);
		}
		if (y < 100) {
			if (y > 80)
				y += 1900;
			else
				y += 2000;
		}

		return Util.date(y, m, d);
	}

	/**
	 * convert a year and day of year to an array in yr,mon,day order
	 * 
	 * @param yr
	 *            The year
	 * @param doy
	 *            The day of the year
	 * @return an array in yr, mon, day
	 * @throws RuntimeException
	 *             if its mis formatted
	 */
	public static int[] ymd_from_doy(int yr, int doy) throws RuntimeException {
		int[] daytab = new int[] { 0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31,
				30, 31 };
		int[] dayleap = new int[] { 0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31,
				30, 31 };
		int j;
		int sum;
		if (yr >= 60 && yr < 100)
			yr = yr + 1900;
		else if (yr < 60 && yr >= 0)
			yr = yr + 2000;
		boolean leap = yr % 4 == 0 && yr % 100 != 0 || yr % 400 == 0; /*
																	 * is it a
																	 * leap year
																	 */
		sum = 0;
		int[] ymd = new int[3];
		ymd[0] = yr;
		if (leap) {
			for (j = 1; j <= 12; j++) {
				if (sum < doy && sum + dayleap[j] >= doy) {
					ymd[1] = j;
					ymd[2] = doy - sum;
					return ymd;
				}
				sum += dayleap[j];
			}
		} else {
			for (j = 1; j <= 12; j++) {
				if (sum < doy && sum + daytab[j] >= doy) {
					ymd[1] = j;
					ymd[2] = doy - sum;
					return ymd;
				}
				sum += daytab[j];
			}
		}
		System.out.println("ymd_from_doy: impossible drop through!   yr=" + yr
				+ " doy=" + doy);
		throw new RuntimeException("ymd_from_DOY : impossible yr=" + yr
				+ " doy=" + doy);
	}

	/**
	 * Convert a mm/dd string to a full Date Of the form mm/dd/yyyy hh:mm:ss or
	 * yyyy,doy hh:mm:ss
	 * 
	 * @param s
	 *            The string to encode
	 * @return The java.util.Date representing the string or a date in 1970 if
	 *         the string is bad.
	 */
	public static java.util.Date stringToDate2(String s) {
		StringTokenizer tk = null;
		String yr = null;
		String mon = null;
		String day = null;
		if (s.indexOf(",") > 0) { // must be yyyy,doy format
			tk = new StringTokenizer(s, ", -:.");
			if (tk.countTokens() == 2) {

			} else if (tk.countTokens() < 4) {
				Util.prt("StringToDate2 not enough tokens for doy form s=" + s
						+ " cnt=" + tk.countTokens());
				return Util.date(1970, 1, 1);
			}
			yr = tk.nextToken();
			int doy = Integer.parseInt(tk.nextToken());
			int[] ymd = ymd_from_doy(Integer.parseInt(yr), doy);
			yr = "" + ymd[0];
			mon = "" + ymd[1];
			day = "" + ymd[2];
		} else {
			tk = new StringTokenizer(s, "/ -:.");
			if (tk.countTokens() < 5 && tk.countTokens() != 3) {
				Util.prt("stringToDate no enough Tokens s=" + s + " cnt="
						+ tk.countTokens());
				return Util.date(1970, 1, 1);
			}
			yr = tk.nextToken();
			mon = tk.nextToken();
			day = tk.nextToken();
		}
		String hr = "00";
		String min = "00";
		String sec = "00";
		String frac = "0";
		int m, d, y, h, mn;
		int sc, ms;
		if (tk.hasMoreTokens()) {
			hr = tk.nextToken();
		}
		if (tk.hasMoreTokens()) {
			min = tk.nextToken();
		}
		if (tk.hasMoreTokens()) {
			sec = tk.nextToken();
		}
		if (tk.hasMoreTokens()) {
			frac = tk.nextToken();

		}
		try {
			m = Integer.parseInt(mon);
			d = Integer.parseInt(day);
			y = Integer.parseInt(yr);
			h = Integer.parseInt(hr);
			mn = Integer.parseInt(min);
			sc = Integer.parseInt(sec);
			ms = Integer.parseInt(frac);
			if (frac.length() == 1)
				ms = ms * 100;
			if (frac.length() == 2)
				ms = ms * 10;
			if (frac.length() == 4)
				ms = ms / 10;
			if (frac.length() == 5)
				ms = ms / 100;
			if (frac.length() == 6)
				ms = ms / 1000;
		} catch (NumberFormatException e) {
			Util.prt("dateToString2() fail to decode ints s=" + s);
			logger.warn("NumberFormatException:", e);
			return Util.date(1970, 1, 1, 0, 0, 0);
		}
		if (m <= 0 || m > 12) {
			Util.prt("stringToDate : bad month = " + mon + " s=" + s);
			return Util.date(1970, 1, 1, 0, 0, 0);
		}
		if (d <= 0 || d > 31) {
			Util.prt("stringToDate : bad day = " + day + " s=" + s);
			return Util.date(1970, 1, 1, 0, 0, 0);
		}
		if (y < 100) {
			if (y > 80)
				y += 1900;
			else
				y += 2000;
		}
		if (h < 0 || h > 23) {
			Util.prt("stringToDate2 : bad hour = " + hr + " s=" + s);
			return Util.date(1970, 1, 1, 0, 0, 0);
		}
		if (mn < 0 || mn > 59) {
			Util.prt("stringToDate2 : bad min = " + mn + " s=" + s);
			return Util.date(1970, 1, 1, 0, 0, 0);
		}
		if (sc < 0 || sc > 59) {
			Util.prt("stringToDate2 : bad sec = " + sc + " s=" + s);
			return Util.date(1970, 1, 1, 0, 0, 0);
		}

		java.util.Date dd = Util.date(y, m, d, h, mn, sc);
		if (ms != 0)
			dd.setTime(dd.getTime() + ms);
		return dd;
	}

	/**
	 * a quick hack to return the current year
	 * 
	 * @return the current year
	 */
	public static int year() {
		GregorianCalendar g = new GregorianCalendar();
		return g.get(Calendar.YEAR);
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
	public static String leftPad(String s, int width) {
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
	 * pad on right side of string to width. Used to create "fixed field" lines
	 * 
	 * @param s
	 *            The string to pad
	 * @param width
	 *            The desired width
	 * @return The padded string to width
	 */
	public static String rightPad(String s, int width) {
		String tmp = "";
		int npad = width - s.length();
		if (npad < 0)
			tmp = s.substring(0, width);
		else if (npad == 0)
			tmp = s;
		else {
			tmp = s;
			for (int i = 0; i < npad; i++)
				tmp += " ";
		}
		return tmp;
	}

	/**
	 * Pad both sides of a string to width Width so String is "centered" in
	 * field
	 * 
	 * @param s
	 *            The string to pad
	 * @param width
	 *            The desired width
	 * @return The padded string to width
	 */
	public static String centerPad(String s, int width) {
		String tmp = "";
		int npad = width - s.length();
		if (npad < 0)
			tmp = s.substring(0, width);
		else if (npad == 0)
			tmp = s;
		else {
			for (int i = 0; i < npad / 2; i++)
				tmp += " ";
			tmp += s;
			for (int i = tmp.length(); i < width; i++)
				tmp += " ";
		}
		return tmp;
	}

	/**
	 * Exit using System.exit() after printing the "in" string. Use so its
	 * easier on Post-mortem to see where exit occured.
	 * 
	 * @param in
	 *            a string to print before exiting.
	 */
	public static void exit(String in) {
		Util.prt(in);
		System.exit(0);
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
	public static String toHex(long i) {
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

	/**
	 * static method that insures a seedname makes some sense. 1) Name is 12
	 * characters long nnssssscccll. 2) All characters are characters, digits,
	 * spaces, question marks or dashes 3) Network code contain blanks 4)
	 * Station code must be at least 3 characters long 5) Channel codes must be
	 * characters in first two places
	 * 
	 * @param name
	 *            A seed string to check
	 * @return True if seename passes tests.
	 */
	public static boolean isValidSeedName(String name) {
		if (name.length() != 12)
			return false;

		char ch;
		// char [] ch = name.toCharArray();
		for (int i = 0; i < 12; i++) {
			ch = name.charAt(i);
			if (!(Character.isLetterOrDigit(ch) || ch == ' ' || ch == '?'
					|| ch == '_' || ch == '-'))
				return false;
		}
		if (name.charAt(0) == ' ' /* || name.charAt(1) == ' ' */)
			return false;
		if (name.charAt(2) == ' ' || name.charAt(3) == ' '
				|| name.charAt(4) == ' ')
			return false;
		if (!(Character.isLetter(name.charAt(7))
				&& Character.isLetter(name.charAt(8)) && Character
					.isLetterOrDigit(name.charAt(9))))
			return false;

		return true;
	}

	public static String cleanIP(String ip) {
		for (int i = 0; i < 2; i++) {
			if (ip.substring(0, 1).equals("0"))
				ip = ip.substring(1);
			ip = ip.replaceAll("\\.0", ".");
		}
		ip = ip.replaceAll("\\.\\.", ".0.");
		return ip;
	}

}
