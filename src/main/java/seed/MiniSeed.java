/*
 * MiniSeed.java
 *
 * Created on May 24, 2005, 2:00 PM
 *
 *
 */

package seed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.seedscan.Global;


/**
 * This class represents a mini-seed packet. It can translate binary data in a
 * byte array and break apart the fixed data header and other data blockettes
 * and represent them as separate internal structures.
 * 
 * @author David Ketchum
 */
public class MiniSeed {
	/** The Constant datalogger. */
	private static final Logger datalogger = LoggerFactory.getLogger("DataLog");
	
	/** The Constant logger. */
	private static final Logger logger = LoggerFactory
			.getLogger(seed.MiniSeed.class);

	/** The Constant ACTIVITY_CAL_ON. */
	private final static int ACTIVITY_CAL_ON = 1;
	
	/** The Constant ACTIVITY_TIME_CORRECTION_APPLIED. */
	private final static int ACTIVITY_TIME_CORRECTION_APPLIED = 2;
	
	/** The Constant ACTIVITY_BEGIN_EVENT. */
	private final static int ACTIVITY_BEGIN_EVENT = 4;
	
	/** The Constant ACTIVITY_END_EVENT. */
	private final static int ACTIVITY_END_EVENT = 8;
	
	/** The Constant ACTIVITY_POSITIVE_LEAP. */
	private final static int ACTIVITY_POSITIVE_LEAP = 16;
	
	/** The Constant ACTIVITY_NEGATIVE_LEAP. */
	private final static int ACTIVITY_NEGATIVE_LEAP = 32;
	
	/** The Constant ACTIVITY_EVENT_IN_PROGRESS. */
	private final static int ACTIVITY_EVENT_IN_PROGRESS = 64;
	
	/** The Constant IOCLOCK_PARITY_ERROR. */
	private final static int IOCLOCK_PARITY_ERROR = 1;
	
	/** The Constant IOCLOCK_LONG_RECORD. */
	private final static int IOCLOCK_LONG_RECORD = 2;
	
	/** The Constant IOCLOCK_SHORT_RECORD. */
	private final static int IOCLOCK_SHORT_RECORD = 4;
	
	/** The Constant IOCLOCK_START_SERIES. */
	private final static int IOCLOCK_START_SERIES = 8;
	
	/** The Constant IOCLOCK_END_SERIES. */
	private final static int IOCLOCK_END_SERIES = 16;
	
	/** The Constant IOCLOCK_LOCKED. */
	private final static int IOCLOCK_LOCKED = 32;
	
	/** The Constant QUALITY_AMP_SATURATED. */
	private final static int QUALITY_AMP_SATURATED = 1;
	
	/** The Constant QUALITY_CLIPPED. */
	private final static int QUALITY_CLIPPED = 1;
	
	/** The Constant QUALITY_SPIKES. */
	private final static int QUALITY_SPIKES = 1;
	
	/** The Constant QUALITY_GLITCHES. */
	private final static int QUALITY_GLITCHES = 1;
	
	/** The Constant QUALITY_MISSING_DATA. */
	private final static int QUALITY_MISSING_DATA = 1;
	
	/** The Constant QUALITY_TELEMETRY_ERROR. */
	private final static int QUALITY_TELEMETRY_ERROR = 1;
	
	/** The Constant QUALITY_CHARGING. */
	private final static int QUALITY_CHARGING = 1;
	
	/** The Constant QUALITY_QUESTIONABLE_TIME. */
	private final static int QUALITY_QUESTIONABLE_TIME = 1;
	
	/** The ms. */
	private ByteBuffer ms;
	
	/** Our copy of the input data wrapped by ms */
	private byte[] buf;
	
	/** The cracked. */
	private boolean cracked;
	
	/** This one was last cleared */
	private boolean cleared;
	
	/** The length. */
	@SuppressWarnings("unused") //We want to keep this accessible for future use.
	private int length;
	
	/** The int5. */
	private static final DecimalFormat int5 = new DecimalFormat("00000");;
	
	/** Counter as MiniSeed records are created. */
	private static int recordCount;
	
	/** The serial number assigned this record */
	private int recordNumber;

	// components filled out by the crack() routine
	/** The 12 charname in fixed header order SSSSSLLCCCNN */
	private byte[] seed;

	/** 6 character with ascii of sequence */
	private byte[] seq;

	/** Two character indicator normally "D " or "Q " */
	private byte[] indicator;

	/** Bytes with raw fixed header time */
	private byte[] startTime;

	/** Number of samples */
	private short nsamp;

	/** Rate factor from fixed header */
	private short rateFactor;

	/** Rate multiplier from fixed header */
	private short rateMultiplier;

	/** activity flags byte from fixed header */
	private byte activityFlags;

	/** iod flags from fixed header */
	private byte ioClockFlags;

	/** Data quality flags from fixed header */
	private byte dataQualityFlags;

	/** number of "data blockettes" in this record */
	private byte nblockettes;

	/** Time Correction from fixed header */
	@SuppressWarnings("unused") // We want to keep this accessible for future
								// use.
	private int timeCorrection;

	/** Offset in buffer of first byte of data */
	private short dataOffset;

	/** Offset in bytes to first byte of first data blockette */
	private short blocketteOffset;

	/** Portions of time broken out from fixed header */
	private short year, day, husec;

	/** The byte portions of the time from fixed header */
	private byte hour, minute, sec;

	/** This is the Java Gregorian representation of the time */
	private GregorianCalendar time;

	/** This does not round the ms */
	private GregorianCalendar timeTruncated;

	/** julian day from year and doy */
	private int julian;

	/** forward integration constant (from first frame) */
	private int forward;

	/** reverse or ending integration constant from end */
	private int reverse; //

	// These contain information about the "data blockettes" really meta-data
	/**
	 * these wrap the bufnnn below for each blockette found in same order as the
	 * blocketteList
	 */
	private ByteBuffer[] blockettes;

	/** The blockette offsets. */
	private int[] blocketteOffsets;

	/** List of the blockett types found */
	private short[] blocketteList; //

	/**
	 * These bufnnn contain data from the various possible "data blockettes"
	 * found. They are never all defined.
	 */
	private byte[] buf100;
	
	/** The buf200. */
	private byte[] buf200;
	
	/** The buf201. */
	private byte[] buf201;
	
	/** The buf300. */
	private byte[] buf300;
	
	/** The buf310. */
	private byte[] buf310;
	
	/** The buf320. */
	private byte[] buf320;
	
	/** The buf390. */
	private byte[] buf390;
	
	/** The buf395. */
	private byte[] buf395;
	
	/** The buf400. */
	private byte[] buf400;
	
	/** The buf405. */
	private byte[] buf405;
	
	/** The buf500. */
	private byte[] buf500;
	
	/** The buf1000. */
	private byte[] buf1000;
	
	/** The buf1001. */
	private byte[] buf1001;
	
	/** The bb100. */
	private ByteBuffer bb100; // These bufnnn contain data from the various
	
	/** The bb200. */
	private ByteBuffer bb200; // possible "data blockettes" found. They are
	
	/** The bb201. */
	private ByteBuffer bb201; // never all defined.
	
	/** The bb300. */
	private ByteBuffer bb300;
	
	/** The bb310. */
	private ByteBuffer bb310;
	
	/** The bb320. */
	private ByteBuffer bb320;
	
	/** The bb390. */
	private ByteBuffer bb390;
	
	/** The bb395. */
	private ByteBuffer bb395;
	
	/** The bb400. */
	private ByteBuffer bb400;
	
	/** The bb405. */
	private ByteBuffer bb405;
	
	/** The bb500. */
	private ByteBuffer bb500;
	
	/** The bb1000. */
	private ByteBuffer bb1000;
	
	/** The bb1001. */
	private ByteBuffer bb1001;
	
	/** The b1000. */
	private Blockette1000 b1000;
	
	/** The b1001. */
	private Blockette1001 b1001;

	/** The b2000s. */
	private ArrayList<Blockette2000> b2000s;

	// Data we need from the type 1000 and 1001
	/** The order. */
	private byte order; // 0=little endian, 1 = big endian
	
	/** The swap. */
	private boolean swap; // If set, this MiniSeed needs to be swapped.
	
	/** The rec length. */
	private int recLength; // in bytes
	
	/** The encoding. */
	private byte encoding; // 10=Steim1, 11=Steim2, 15=NSN
	
	/** The timing quality. */
	private byte timingQuality; // 1001 - from 0 to 100 %
	
	/** The micro sec offset. */
	private byte microSecOffset; // offset from the 100 of USecond in time code
	
	/** The nframes. */
	private byte nframes; // in compressed data (Steim method only)

	/** The dbg. */
	private static boolean dbg = false;

	/**
	 * Gets the blk2000s.
	 *
	 * @return the blk2000s
	 */
	public Collection<Blockette2000> getBlk2000s() {
		return b2000s;
	}

	/**
	 * if true, this MiniSeed object is cleared and presumably available for
	 * reuse.
	 *
	 * @return true, if is clear
	 */
	public boolean isClear() {
		return cleared;
	}

	/**
	 * ms set the number of samples. Normally used to truncate a time series say
	 * to make it just long enough to fill the day!
	 * 
	 * @param ns
	 *            The number of samples
	 */
	public void setNsamp(int ns) {
		nsamp = (short) ns;
		ms.position(30);
		ms.putShort(nsamp);
	}

	/**
	 * Fix location code.
	 */
	private void fixLocationCode() {
		if (seed[5] != ' ' && !Character.isUpperCase(seed[5])
				&& !Character.isDigit(seed[5]))
			seed[5] = ' ';
		if (seed[6] != ' ' && !Character.isUpperCase(seed[6])
				&& !Character.isDigit(seed[6]))
			seed[6] = ' ';
	}

	/**
	 * Gets the record number.
	 *
	 * @return the record number
	 */
	public int getRecordNumber() {
		return recordNumber;
	}

	/**
	 * Creates a new instance of MiniSeed.
	 *
	 * @param inbuf            An array of binary miniseed data
	 * @throws IllegalSeednameException             if the name does not pass muster
	 */
	public MiniSeed(byte[] inbuf) throws IllegalSeednameException {
		buf = new byte[inbuf.length];
		System.arraycopy(inbuf, 0, buf, 0, inbuf.length);
		ms = ByteBuffer.wrap(buf);
		blockettes = new ByteBuffer[4];
		blocketteList = new short[4];
		blocketteOffsets = new int[4];
		init(); // init will set swapping of ms
		recordNumber = recordCount++;
	}

	/**
	 * Inits the.
	 *
	 * @throws IllegalSeednameException the illegal seedname exception
	 */
	private void init() throws IllegalSeednameException {
		length = buf.length;
		cracked = false;
		cleared = false;
		encoding = 0;
		recLength = buf.length; // this will be overridden by blockette 1000 if
								// present
		order = -100;
		nframes = 0;
		microSecOffset = 0;
		timingQuality = 101;
		if (seed == null)
			seed = new byte[12];
		if (seq == null)
			seq = new byte[6];
		if (indicator == null)
			indicator = new byte[2];
		if (startTime == null)
			startTime = new byte[10];
		if (isHeartBeat())
			return;
		swap = swapNeeded(buf, ms);
		if (swap)
			ms.order(ByteOrder.LITTLE_ENDIAN);

		// crack the seed name so we can check its legality
		ms.clear();
		ms.get(seq).get(indicator).get(seed).get(startTime);
		// MasterBlock.checkSeedName(getSeedName());
		nsamp = ms.getShort();
		rateFactor = ms.getShort();
		rateMultiplier = ms.getShort();
		activityFlags = ms.get();
		ioClockFlags = ms.get();
		dataQualityFlags = ms.get();
		nblockettes = ms.get();
		timeCorrection = ms.getInt();
		dataOffset = ms.getShort();
		blocketteOffset = ms.getShort();
		ms.position(68);
		forward = ms.getInt();
		reverse = ms.getInt();
		fixLocationCode();
		if (swap && dbg)
			logger.debug("   *************Swap is needed! ************* "
					+ getSeedName());
	}

	/**
	 * Crack is heart beat.
	 *
	 * @param buf the buf
	 * @return true, if successful
	 */
	public static boolean crackIsHeartBeat(byte[] buf) {
		boolean is = true;
		for (int i = 0; i < 6; i++)
			if (buf[i] != 48 || buf[i + 6] != 32 || buf[i + 12] != 32) {
				is = false;
				break;
			}
		return is;
	}

	/**
	 * Is this mini-seed a heart beat. These packets have all zero sequence #
	 * and all spaces in the net/station/location/channel
	 * 
	 * @return true if sequences is all zero and first 12 chars are blanks
	 */
	public boolean isHeartBeat() {
		boolean is = true;
		for (int i = 0; i < 6; i++)
			if (buf[i] != 48 || buf[i + 6] != 32 || buf[i + 12] != 32) {
				is = false;
				break;
			}
		return is;
	}

	/**
	 * This returns the time data as a 4 element array with hour,minute, sec,
	 * and hsec from a raw miniseed buffer in buf. This routine would be used to
	 * extract a bit of data from a raw buffer without going to the full effort
	 * of creating a MiniSeed object from it.
	 * 
	 * @param buf
	 *            A array with a miniseed block in raw form
	 * @return The time in a 4 integer array
	 * @throws IllegalSeednameException
	 *             if the buf is clearly not miniseed
	 */
	public static int[] crackTime(byte[] buf) throws IllegalSeednameException {
		ByteBuffer bb = ByteBuffer.wrap(buf);
		if (swapNeeded(buf))
			bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.position(24);
		// short year = bb.getShort();
		// short day = bb.getShort();
		int[] time = new int[4];
		time[0] = bb.get() & 0x000000ff; // hour
		time[1] = bb.get() & 0x000000ff; // minute
		time[2] = bb.get() & 0x000000ff;
		bb.get();
		time[3] = bb.getShort() & 0x0000ffff;
		return time;
	}

	/**
	 * Return the year from an uncracked miniseedbuf.
	 *
	 * @param buf            Buffer with miniseed header
	 * @return The year
	 * @throws IllegalSeednameException             if the buffer clearly is not mini-seed
	 */
	public static int crackYear(byte[] buf) throws IllegalSeednameException {
		ByteBuffer bb = ByteBuffer.wrap(buf);
		if (swapNeeded(buf))
			bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.position(20);
		return (int) bb.getShort();
	}

	/**
	 * Return the day of year from an uncracked miniseedbuf.
	 *
	 * @param buf            Buffer with miniseed header
	 * @return The day of year
	 * @throws IllegalSeednameException             if the buffer clearly is not mini-seed
	 */
	public static int crackDOY(byte[] buf) throws IllegalSeednameException {
		ByteBuffer bb = ByteBuffer.wrap(buf);
		if (swapNeeded(buf))
			bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.position(22);
		return (int) bb.getShort();
	}

	/**
	 * This returns the digitizing rate from a raw miniseed buffer in buf. This
	 * routine would be used to extract a bit of data from a raw buffer without
	 * going to the full effort of creating a MiniSeed object from it.
	 * 
	 * @param buf
	 *            A array with a miniseed block in raw form
	 * @return The digitizing rate as a double. 0. if the block factor and
	 *         multipler are invalid.
	 * @throws IllegalSeednameException
	 *             if the buffer clearly is not mini-seed
	 */
	public static double crackRate(byte[] buf) throws IllegalSeednameException {
		ByteBuffer bb = ByteBuffer.wrap(buf);
		if (swapNeeded(buf))
			bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.position(32);
		short rateFactor = bb.getShort();
		short rateMultiplier = bb.getShort();
		double rate = rateFactor;
		// if rate > 0 its in hz, < 0 its period.
		// if multiplier > 0 it multiplies, if < 0 it divides.
		if (rateFactor == 0 || rateMultiplier == 0)
			return 0;
		if (rate >= 0) {
			if (rateMultiplier > 0)
				rate *= rateMultiplier;
			else
				rate /= -rateMultiplier;
		} else {
			if (rateMultiplier > 0)
				rate = -rateMultiplier / rate;
			else
				rate = -1. / (-rateMultiplier) / rate;
		}
		return rate;
	}

	/**
	 * This returns the seedname in NSCL order from a raw miniseed buffer in
	 * buf. This routine would be used to extract a bit of data from a raw
	 * buffer without going to the full effort of creating a MiniSeed object
	 * from it.
	 * 
	 * @param buf
	 *            A array with a miniseed block in raw form
	 * @return The seedname in NSCL order
	 */
	public static String crackSeedname(byte[] buf) {
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.position(8);
		byte[] seed = new byte[12];
		bb.get(seed);
		String s = new String(seed);
		return s.substring(10, 12) + s.substring(0, 5) + s.substring(7, 10)
				+ s.substring(5, 7);
	}

	/**
	 * Safe letter.
	 *
	 * @param b the b
	 * @return the string
	 */
	private static String safeLetter(byte b) {
		char c = (char) b;
		return Character.isLetterOrDigit(c) || c == ' ' ? "" + c : Util
				.toHex((byte) c);
	}

	/**
	 * To string raw.
	 *
	 * @param buf the buf
	 * @return the string
	 */
	private static String toStringRaw(byte[] buf) {
		ByteBuffer bb = ByteBuffer.wrap(buf);
		StringBuilder tmp = new StringBuilder(100);
		bb.position(0);
		for (int i = 0; i < 6; i++)
			tmp.append(safeLetter(bb.get()));
		tmp.append(" ");
		bb.position(18);
		for (int i = 0; i < 2; i++)
			tmp.append(safeLetter(bb.get()));
		bb.position(8);
		for (int i = 0; i < 5; i++)
			tmp.append(safeLetter(bb.get()));
		bb.position(15);
		for (int i = 0; i < 3; i++)
			tmp.append(safeLetter(bb.get()));
		bb.position(13);
		for (int i = 0; i < 2; i++)
			tmp.append(safeLetter(bb.get()));
		bb.position(20);
		short i2 = bb.getShort();
		tmp.append(" " + i2 + " " + Util.toHex(i2));
		i2 = bb.getShort();
		tmp.append(" " + i2 + " " + Util.toHex(i2));
		tmp.append(" " + bb.get() + ":" + bb.get() + ":" + bb.get());
		bb.get();
		i2 = bb.getShort();
		tmp.append("." + i2 + " " + Util.toHex(i2));
		i2 = bb.getShort();
		tmp.append(" ns=" + i2);
		i2 = bb.getShort();
		tmp.append(" rt=" + i2);
		i2 = bb.getShort();
		tmp.append("*" + i2);
		bb.position(39);
		tmp.append(" nb=" + bb.get());
		bb.position(44);
		i2 = bb.getShort();
		tmp.append(" d=" + i2);
		i2 = bb.getShort();
		tmp.append(" b=" + i2);
		return tmp.toString();
	}

	/**
	 * Swap needed.
	 *
	 * @param buf the buf
	 * @return true, if successful
	 * @throws IllegalSeednameException the illegal seedname exception
	 */
	private static boolean swapNeeded(byte[] buf)
			throws IllegalSeednameException {
		ByteBuffer bb = ByteBuffer.wrap(buf);
		return swapNeeded(buf, bb);
	}

	/**
	 * Swap needed.
	 *
	 * @param buf the buf
	 * @param bb the bb
	 * @return the boolean
	 * @throws IllegalSeednameException the illegal seedname exception
	 */
	private static Boolean swapNeeded(byte[] buf, ByteBuffer bb)
			throws IllegalSeednameException {
		
		String qualityFlagsStr = Global.CONFIG.getQualityflags();
		List<String> qualityFlags = Arrays.asList(qualityFlagsStr.split(","));
		
		boolean swap = false;

		if( (buf[0] >= '0' && buf[0] <= '9') && (buf[1] >= '0' && buf[1] <= '9') && (buf[2] >= '0' && buf[2] <= '9') && (buf[3] >= '0' && buf[3] <= '9') && buf[7] == ' ' &&
		  (
				  qualityFlags.contains("All") || 
				  qualityFlags.contains(String.valueOf((char)buf[6]))
		  )
		)
		{
			
			bb.position(39); // position # of blockettes that follow
			int nblks = bb.get(); // get it
			int offset = 0;
			if (nblks > 0) {
				bb.position(46); // position offset to first blockette
				offset = bb.getShort();
				if (offset > 64 || offset < 48) { // This looks like swap is needed
					bb.order(ByteOrder.LITTLE_ENDIAN);
					bb.position(46);
					offset = bb.getShort(); // get byte swapped version
					if (offset > 200 || offset < 0) {
						datalogger.error("MiniSEED: cannot figure out if this is swapped or not!!! Assume not. offset="
								+ offset + " " + toStringRaw(buf));
						RuntimeException e = new RuntimeException(
								"Cannot figure swap from offset ");
						datalogger.error("RuntimeException:", e);
					} else
						swap = true;
				}
				for (int i = 0; i < nblks; i++) {
					if (offset < 48 || offset > 64) {
						logger.error("Illegal offset trying to figure swapping off="
								+ Util.toHex(offset) + " nblks=" + nblks
								+ " seedname="
								+ Util.toAllPrintable(crackSeedname(buf)) + " "
								+ toStringRaw(buf));
						break;
					}
					bb.position(offset);
					int type = bb.getShort();
					int oldoffset = offset;
					offset = bb.getShort();
					// ByteOrder order=null;
					if (type == 1000) {
						bb.position(oldoffset + 5); // this should be word order
						if (bb.get() == 0) {
							if (swap)
								return swap;
							logger.error("Offset said swap but order byte in b1000 said not to! "
									+ toStringRaw(buf));
							return false;
						} else
							return false;
					}
				}
			} else { // This block does not have blockette 1000, so make decision
						// based on where the data starts!
				bb.position(44);
				offset = bb.getShort();
				if (offset < 0 || offset > 512)
					return true;
				return false;
			}
		}
		else
		{
			throw new IllegalSeednameException("Bad seq # or [DQR] "
					+ toStringRaw(buf));
		}
		return swap;
	}

	/**
	 * Crack block size.
	 * 
	 * This method attempts to determine the block size in blockette 1000. If a
	 * record has field 17 (position 44) is 0 it is changed to 64. This prevents
	 * issues with CU stations having OCF channels that are invalid.
	 *
	 * @param buf
	 *            the seed buffer
	 * @return the block size
	 * @throws IllegalSeednameException
	 *             occurs if the blockette has an in valid name or channel. This
	 *             can only occur if a BlockSizeException has happened already.
	 * @throws BlockSizeException
	 *             occurs if either a blockette Offset is too small or too large
	 *             OR if there is no blockette 1000.
	 */
	public static int crackBlockSize(byte[] buf)
			throws IllegalSeednameException, BlockSizeException {
		ByteBuffer bb = ByteBuffer.wrap(buf);
		if (swapNeeded(buf))
			bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.position(39); // position # of blockettes that follow
		int nblks = bb.get(); // get it
		bb.position(44); //The position of the data offset and end of data header.
		int dataOffset = bb.getShort();
		if(dataOffset == 0){
			logger.warn("Data Offset is 0. Either there is no data or there is a problem. Treating as if it was 64.");
			dataOffset = 64; //If it is a record with no data, set to 64.
		}
		bb.position(46); // position offset to first blockette and end of fixed data header
		int offset = bb.getShort();
		for (int i = 0; i < nblks; i++) {
			if (offset < 48 || offset >= dataOffset) {
				String message = "Illegal offset trying to crackBlockSize() blocketteOffset="
						+ offset + " dataOffset= " + dataOffset + " nblks=" + nblks + " seedname="
						+ crackSeedname(buf);
				throw new BlockSizeException(message);
			}
			bb.position(offset);
			int type = bb.getShort();
			int oldoffset = offset;
			offset = bb.getShort();
			if (type == 1000) {
				bb.position(oldoffset + 6);
				return 1 << bb.get();
			}
		}
		/*If we got here we never found a blockette 1000*/
		String message = "Missing blockette 1000 trying to crackBlockSize() blocketteOffset="
				+ offset + " dataOffset= " + dataOffset + " nblks=" + nblks + " seedname="
				+ crackSeedname(buf);
		throw new BlockSizeException(message);

	}

	/**
	 * this routine takes a binary buf and breaks it into the fixed data header
	 * and any other data blockettes that are present. Note : creating a
	 * miniseed object does nothing but store the data. Anything that needs to
	 * use the "cracked" data structures should call crack first. If the record
	 * has been previously cracked, no processing is done.
	 */
	private void crack() {
		if (cracked)
			return;
		synchronized (this) {

			// timeCorrection=new byte[4];
			if (isHeartBeat())
				return;

			// Bust up the time and convert to parts and to a GregorianCalendar
			ByteBuffer timebuf = ByteBuffer.wrap(startTime);
			if (swap)
				timebuf.order(ByteOrder.LITTLE_ENDIAN);

			year = timebuf.getShort();
			day = timebuf.getShort();
			hour = timebuf.get();
			minute = timebuf.get();
			sec = timebuf.get();
			timebuf.get();
			husec = timebuf.getShort();
			julian = SeedUtil.toJulian(year, day);

			// Note : jan 1, 1970 is time zero so 1 must be subtracted from the
			// day
			long millis = (year - 1970) * 365L * 86400000L + (day - 1)
					* 86400000L + ((long) hour) * 3600000L + ((long) minute)
					* 60000L + ((long) sec) * 1000L;
			millis += ((year - 1969) / 4) * 86400000L; // Leap years past but
														// not this one!

			if (time == null)
				time = new GregorianCalendar();
			if (timeTruncated == null)
				timeTruncated = new GregorianCalendar();
			time.setTimeInMillis(millis + (husec + 5) / 10);
			timeTruncated.setTimeInMillis(millis + husec / 10);

			/*
			 * prt("sq="+new String(seq)+" ind="+new String(indicator)+
			 * " name="+new String(seed)+" ns="+nsamp+" nblk="+nblockettes+
			 * " blkoff="+blocketteOffset+" dataOff="+dataOffset);
			 */
			// This is the "terminator" blocks for rerequests for the GSN, most
			// LOGS have
			// nsamp set to number of characters in buffer!
			if (seed[7] == 'L' && seed[8] == 'O' && seed[9] == 'G'
					&& nsamp == 0)
				nblockettes = 0;

			// If data blockettes are present, process them
			// blockettes is array with ByteBuffer of each type of blockette.
			// BlocketteList is
			// the blockette type of each in the same order
			if (nblockettes > 0) {
				if (blockettes.length < nblockettes) { // if number of
														// blockettes is bigger
														// than reserved.
					if (nsamp > 0)
						logger.warn("Unusual expansion of blockette space in MiniSeed nblks="
								+ nblockettes
								+ " length="
								+ blockettes.length
								+ " "
								+ getSeedName()
								+ " "
								+ ""
								+ year
								+ " "
								+ int5.format(day).substring(2, 5)
								+ ":"
								+ int5.format(hour).substring(3, 5)
								+ ":"
								+ int5.format(minute).substring(3, 5)
								+ ":"
								+ int5.format(sec).substring(3, 5)
								+ "."
								+ int5.format(husec).substring(1, 5));
					blockettes = new ByteBuffer[nblockettes];
					blocketteList = new short[nblockettes];
					blocketteOffsets = new int[nblockettes];
				}
				ms.position(blocketteOffset);
				short next = blocketteOffset;
				for (int blk = 0; blk < nblockettes; blk++) {
					blocketteOffsets[blk] = next;
					short type = ms.getShort();
					// System.out.format("==== MiniSeed.crack: blockette type=[%d]\n",
					// type);
					// This is the problem when blockette 1001 was not swapped
					// for a shor ttime 2009,128-133
					if (type == -5885) {
						if (time.get(Calendar.YEAR) == 2009
								&& time.get(Calendar.DAY_OF_YEAR) >= 128
								&& time.get(Calendar.DAY_OF_YEAR) <= 133) {
							ms.position(ms.position() - 2);
							ms.putShort((short) 1001);
							type = 1001;
						}
					}

					if (dbg)
						logger.debug(blk
								+ "**** MS: Blockette type="
								+ type + " off=" + next);
					blocketteList[blk] = type;
					if (next < 48 || next >= 400) {
						if (nsamp > 0)
							logger.error("Bad position in blockettes next2=" + next);
						nblockettes = (byte) blk;
						break;
					}
					ms.position((int) next);
					switch (type) {
					case 100: // Sample Rate Blockette
						if (buf100 == null) {
							buf100 = new byte[12];
							bb100 = ByteBuffer.wrap(buf100);
						}
						blockettes[blk] = bb100;
						ms.get(buf100);
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 100
						next = blockettes[blk].getShort();
						break;
					case 200: // Generic Event Detection
						if (buf200 == null) {
							buf200 = new byte[28];
							bb200 = ByteBuffer.wrap(buf200);
						}
						blockettes[blk] = bb200;
						ms.get(buf200);
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 200
						next = blockettes[blk].getShort();
						break;
					case 201: // Murdock Event Detection
						if (buf201 == null) {
							buf201 = new byte[60];
							bb201 = ByteBuffer.wrap(buf201);
						}
						blockettes[blk] = bb201;
						ms.get(buf201);
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 201
						next = blockettes[blk].getShort();
						break;
					case 300: // Step Calibration
						if (buf300 == null) {
							buf300 = new byte[32];
							bb300 = ByteBuffer.wrap(buf300);
						}
						blockettes[blk] = bb300;
						ms.get(buf300);
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 300
						next = blockettes[blk].getShort();
						break;
					case 310: // Sine Calbration
						if (buf310 == null) {
							buf310 = new byte[32];
							bb310 = ByteBuffer.wrap(buf310);
						}
						ms.get(buf310);
						blockettes[blk] = bb310;
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 310
						next = blockettes[blk].getShort();
						break;
					case 320: // Pseudo-random calibration
						if (buf320 == null) {
							// MTH:
							// buf320=new byte[32];
							buf320 = new byte[64];
							bb320 = ByteBuffer.wrap(buf320);
						}
						ms.get(buf320);
						blockettes[blk] = bb320;
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 320
						next = blockettes[blk].getShort();
						break;
					case 390: // Generic Calibration
						if (buf390 == null) {
							buf390 = new byte[28];
							bb390 = ByteBuffer.wrap(buf390);
						}
						ms.get(buf390);
						blockettes[blk] = bb390;
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 330
						next = blockettes[blk].getShort();
						break;
					case 395: // Calibration abort
						if (buf395 == null) {
							buf395 = new byte[16];
							bb395 = ByteBuffer.wrap(buf395);
						}
						blockettes[blk] = bb395;
						ms.get(buf395);
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 395
						next = blockettes[blk].getShort();
						break;
					case 400: // Beam blockette
						if (buf400 == null) {
							buf400 = new byte[16];
							bb400 = ByteBuffer.wrap(buf400);
						}
						ms.get(buf400);
						blockettes[blk] = bb400;
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 400
						next = blockettes[blk].getShort();
						break;
					case 405: // Beam Delay
						if (buf405 == null) {
							buf405 = new byte[6];
							bb405 = ByteBuffer.wrap(buf405);
						}
						ms.get(buf405);
						blockettes[blk] = bb405;
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 405
						next = blockettes[blk].getShort();
						break;
					case 500: // Timing BLockette
						if (buf500 == null) {
							buf500 = new byte[200];
							bb500 = ByteBuffer.wrap(buf500);
						}
						ms.get(buf500);
						blockettes[blk] = bb500;
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 500
						next = blockettes[blk].getShort();
						break;
					case 1000:
						if (buf1000 == null) {
							buf1000 = new byte[8];
							bb1000 = ByteBuffer.wrap(buf1000);
							b1000 = new Blockette1000(buf1000);
						}
						ms.get(buf1000);
						blockettes[blk] = bb1000;
						b1000.reload(buf1000);
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 1000
						next = blockettes[blk].getShort();
						encoding = blockettes[blk].get();
						order = blockettes[blk].get();
						recLength = 1;
						recLength = recLength << blockettes[blk].get();
						if (dbg)
							logger.debug("MS: Blk 1000 length**2=" + recLength
									+ " order=" + order + " next=" + next
									+ " encoding=" + encoding);
						break;
					case 1001: // data extension (Quanterra only?)
						if (buf1001 == null) {
							buf1001 = new byte[8];
							bb1001 = ByteBuffer.wrap(buf1001);
							b1001 = new Blockette1001(buf1001);
						}
						ms.get(buf1001);
						blockettes[blk] = bb1001;
						b1001.reload(buf1001);
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 1001
						next = blockettes[blk].getShort();
						timingQuality = blockettes[blk].get();
						microSecOffset = blockettes[blk].get();
						blockettes[blk].get(); // reserved
						nframes = blockettes[blk].get();
						if (dbg)
							logger.debug("MS: Blk 1001 next=" + next + " timing="
									+ timingQuality + " uSecOff="
									+ microSecOffset + " nframes=" + nframes);
						break;
					case 2000:
						// duplicate the fixed portion of the header and load it
						byte[] buf2000 = new byte[Blockette2000.FIXED_LENGTH];
						ms.mark();
						ms.get(buf2000);
						Blockette2000 b2000 = new Blockette2000(buf2000);
						// get the actual size of the blockette, and add the
						// full blockette to the list
						short actualLength = b2000.getBlocketteLength();
						if (actualLength != buf2000.length) {
							buf2000 = new byte[actualLength];
							ms.reset();
							ms.get(buf2000);
							b2000 = new Blockette2000(buf2000);
						}
						if (b2000s == null) {
							b2000s = new ArrayList<Blockette2000>();
						}
						b2000s.add(b2000);
						break;
					default:
						if (dbg)
							logger.debug("MS: - unknown blockette type=" + type);
						byte[] tmp = new byte[4];
						ms.get(tmp);
						blockettes[blk] = ByteBuffer.wrap(tmp);
						if (swap)
							blockettes[blk].order(ByteOrder.LITTLE_ENDIAN);
						blockettes[blk].clear();
						blockettes[blk].getShort(); // type = 1001
						next = blockettes[blk].getShort();
						break;
					}
				}
			}

			cracked = true;
		} // end of synchronized on this!
	}

	/**
	 * create a basic string from the fixed data header representing this
	 * packet. any other data blockettes are added at the end to indicate their
	 * presence
	 * 
	 * @return A representative string
	 */
	@Override
	public String toString() {
		crack();
		String rt = getRate() + "     ";

		rt = getSeedName() + " " + new String(seq) + " "
				+ new String(indicator) + getTimeString() + " n="
				+ (nsamp + "    ").substring(0, 5)
				+ "rt="
				+ rt.substring(0, 5)
				+ " dt="
				+ (dataOffset + "  ").substring(0, 3)
				+
				// " ln="+(recLength+"   ").substring(0,4)+
				// " en="+(encoding+" ").substring(0,2)+
				" off=" + (blocketteOffset + "   ").substring(0, 3) + " #f="
				+ getUsedFrameCount() + " nb=" + nblockettes
				+ (swap ? "S" : "");
		for (int i = 0; i < nblockettes; i++) {
			if (blocketteList[i] == 1000)
				rt += " " + b1000.toString();
			else if (blocketteList[i] == 1001)
				rt += b1001.toString();
			else
				rt += " (" + blocketteList[i] + ")";
		}
		if (nblockettes < 2)
			rt += " bsiz=" + recLength;
		String f = "";
		if (order == 0)
			f += "S";
		if (swap)
			f += "S";
		if ((activityFlags & ACTIVITY_CAL_ON) != 0)
			f += "C";
		if ((activityFlags & ACTIVITY_TIME_CORRECTION_APPLIED) != 0)
			f += "*";
		if ((activityFlags & ACTIVITY_BEGIN_EVENT) != 0)
			f += "T";
		if ((activityFlags & ACTIVITY_END_EVENT) != 0)
			f += "t";
		if ((activityFlags & ACTIVITY_POSITIVE_LEAP) != 0)
			f += "+";
		if ((activityFlags & ACTIVITY_NEGATIVE_LEAP) != 0)
			f += "-";
		if ((activityFlags & ACTIVITY_EVENT_IN_PROGRESS) != 0)
			f += "E";
		if ((ioClockFlags & IOCLOCK_PARITY_ERROR) != 0)
			f += "P";
		if ((ioClockFlags & IOCLOCK_LONG_RECORD) != 0)
			f += "L";
		if ((ioClockFlags & IOCLOCK_SHORT_RECORD) != 0)
			f += "s";
		if ((ioClockFlags & IOCLOCK_START_SERIES) != 0)
			f += "O";
		if ((ioClockFlags & IOCLOCK_END_SERIES) != 0)
			f += "F";
		if ((ioClockFlags & IOCLOCK_LOCKED) == 0)
			f += "U";
		if ((dataQualityFlags & QUALITY_AMP_SATURATED) != 0)
			f += "A";
		if ((dataQualityFlags & QUALITY_CLIPPED) != 0)
			f += "c";
		if ((dataQualityFlags & QUALITY_SPIKES) != 0)
			f += "G";
		if ((dataQualityFlags & QUALITY_GLITCHES) != 0)
			f += "g";
		if ((dataQualityFlags & QUALITY_MISSING_DATA) != 0)
			f += "M";
		if ((dataQualityFlags & QUALITY_TELEMETRY_ERROR) != 0)
			f += "e";
		if ((dataQualityFlags & QUALITY_CHARGING) != 0)
			f += "d";
		if ((dataQualityFlags & QUALITY_QUESTIONABLE_TIME) != 0)
			f += "q";

		return rt + f;
	}

	/**
	 * return number of used data frames. We figure this by looking for all zero
	 * frames from the end!
	 * 
	 * @return Number of used frames based on examining for zeroed out frames
	 *         rather than b1001
	 */
	public int getUsedFrameCount() {
		crack();
		int i;
		for (i = Math.min(buf.length, recLength) - 1; i >= dataOffset; i--) {
			if (buf[i] != 0)
				break;
		}
		return (i - dataOffset + 64) / 64; // This is the data frame # used
	}

	/**
	 * return state of swap as required by the Steim decompression routines and
	 * our internal convention. We both assume bytes are in BIG_ENDIAN order and
	 * swap bytes if the records is not. Stated another way, we swap whenever
	 * the compressed data indicates it is is little endian order.
	 * 
	 * @return True if bytes need to be swapped
	 */
	public boolean isSwapBytes() {
		return swap;
	}

	/**
	 * return the blocksize or record length of this mini-seed.
	 *
	 * @return the blocksize of record length
	 */
	public int getBlockSize() {
		crack();
		return recLength;
	}

	/**
	 * retun number of samples in packet.
	 *
	 * @return # of samples
	 */
	public int getNsamp() {
		return (int) nsamp;
	}

	/**
	 * return number of data blockettes.
	 *
	 * @return # of data blockettes
	 */
	public int getNBlockettes() {
		return (int) nblockettes;
	}

	/**
	 * return the seed name of this component in nnssssscccll order.
	 *
	 * @return the nnssssscccll
	 */
	public String getSeedName() {
		String s = new String(seed);
		return s.substring(10, 12) + s.substring(0, 5) + s.substring(7, 10)
				+ s.substring(5, 7);
	}

	/**
	 * return the two char data block type, normally "D ", "Q ", etc.
	 *
	 * @return the indicator
	 */
	public String getIndicator() {
		return new String(indicator);
	}

	/**
	 * return the encoding.
	 *
	 * @return the encoding
	 */
	public boolean isBigEndian() {
		return (order != 0);
	}

	/**
	 * return the encoding.
	 *
	 * @return the encoding
	 */
	public int getEncoding() {
		crack();
		return encoding;
	}

	/**
	 * return year from the first sample time.
	 *
	 * @return the year
	 */
	public int getYear() {
		crack();
		return year;
	}

	/**
	 * return the day-of-year from the first sample time.
	 *
	 * @return the day-of-year of first sample
	 */
	public int getDay() {
		crack();
		return day;
	}

	/**
	 * return the day-of-year from the first sample time.
	 *
	 * @return the day-of-year of first sample
	 */
	public int getDoy() {
		crack();
		return day;
	}

	/**
	 * return the hours.
	 *
	 * @return the hours of first sample
	 */
	public int getHour() {
		crack();
		return hour;
	}

	/**
	 * return the minutes.
	 *
	 * @return the minutes of first sample
	 */
	public int getMinute() {
		crack();
		return minute;
	}

	/**
	 * return the seconds.
	 *
	 * @return the seconds of first sample
	 */
	public int getSeconds() {
		crack();
		return sec;
	}

	/**
	 * return the 100s of uSeconds.
	 *
	 * @return 100s of uSeconds of first sample
	 */
	public int getHuseconds() {
		crack();
		return husec;
	}

	/**
	 * return Julian day (a big integer) of the first sample's year and day.
	 *
	 * @return The 1st sample time
	 */
	public int getJulian() {
		crack();
		return julian;
	}

	/**
	 * return the activity flags from field 12 of the SEED fixed header
	 * <p>
	 * Bit Description
	 * <p>
	 * 0 Calibration signals present
	 * <p>
	 * 1 Time Correction applied.
	 * <p>
	 * 2 Beginning of an event, station trigger
	 * <p>
	 * 3 End of an event, station detriggers
	 * <p>
	 * 4 Positive leap second occurred in packet
	 * <p>
	 * 5 Negative leap second occurred in packet
	 * <p>
	 * 6 Event in Progress
	 * <p>
	 * 
	 * @return The activity flags
	 */
	public byte getActivityFlags() {
		crack();
		return activityFlags;
	}

	/**
	 * return the data qualit flags from field 13 of the Seed fixed header
	 * <p>
	 * bit Description
	 * <p>
	 * 0 Amplifier staturation detected
	 * <p>
	 * 1 Digitizer clipping detected
	 * <p>
	 * 2 Spikes detected
	 * <p>
	 * 3 Glitches detected
	 * <p>
	 * 4 Missing/padded data present
	 * <p>
	 * 5 Telemetry synchronization error
	 * <p>
	 * 6 A digial filter may be "charging"
	 * <p>
	 * 7 Time tag is questionable.
	 *
	 * @return The data quality flags
	 */
	public byte getDataQualityFlags() {
		crack();
		return dataQualityFlags;
	}

	/**
	 * Return the IO and clock flags from field 13 of fixed header
	 * <p>
	 * Bit Description
	 * <p>
	 * 0 Station volume parity error possibly present
	 * <p>
	 * 1 Long record read( possibly no problem)
	 * <p>
	 * 2 Short record read(record padded)
	 * <p>
	 * 3 Start of time series
	 * <p>
	 * 4 End of time series
	 * <p>
	 * 5 Clock locked.
	 *
	 * @return The IO and clock flags
	 */
	public byte getIOClockFlags() {
		crack();
		return ioClockFlags;
	}

	/**
	 * return the raw time bytes in an array.
	 *
	 * @return The raw time bytes in byte array
	 */
	public byte[] getRawTimeBuf() {
		crack();
		byte[] b = new byte[startTime.length];
		System.arraycopy(startTime, 0, b, 0, startTime.length);
		return b; // return copy of time bytes.
	}

	/**
	 * return the time in gregroian calendar millis.
	 *
	 * @return GregorianCalendar millis for start time
	 */
	public long getTimeInMillis() {
		crack();
		return time.getTimeInMillis();
	}

	/**
	 * return a GregorianCalendar set to the 1st sample time, it is a copy just
	 * so users do not do calculations with it and end up changing the mini-seed
	 * record time. This time is rounded in millis
	 * 
	 * @return The 1st sample time
	 */
	public GregorianCalendar getGregorianCalendar() {
		crack();
		GregorianCalendar e = new GregorianCalendar();
		e.setTimeInMillis(time.getTimeInMillis());
		return e;
	}

	/**
	 * return the time in gregroian calendar millis, this time is truncated.
	 *
	 * @return GregorianCalendar millis for start time
	 */
	public long getTimeInMillisTruncated() {
		crack();
		return timeTruncated.getTimeInMillis();
	}

	/**
	 * return a GregorianCalendar set to the 1st sample time, it is a copy just
	 * so users do not do calculations with it and end up changing the mini-seed
	 * record time. This time is truncated in millis
	 * 
	 * @return The 1st sample time
	 */
	public GregorianCalendar getGregorianCalendarTruncated() {
		crack();
		GregorianCalendar e = new GregorianCalendar();
		e.setTimeInMillis(timeTruncated.getTimeInMillis());
		return e;
	}

	/**
	 * give a standard time string for the 1st sample time.
	 *
	 * @return the time string yyyy ddd:hh:mm:ss.hhhh
	 */
	public String getTimeString() {
		crack();
		return "" + year + " " + int5.format(day).substring(2, 5) + ":"
				+ int5.format(hour).substring(3, 5) + ":"
				+ int5.format(minute).substring(3, 5) + ":"
				+ int5.format(sec).substring(3, 5) + "."
				+ int5.format(husec).substring(1, 5);
	}

	/**
	 * Gets the end time.
	 *
	 * @return the end time
	 */
	public GregorianCalendar getEndTime() {
		crack();
		GregorianCalendar end = new GregorianCalendar();
		end.setTimeInMillis(time.getTimeInMillis()
				+ (long) ((nsamp - 1) / getRate() * 1000. + 0.5));
		return end;
	}

	/**
	 * Gets the next expected time in millis.
	 *
	 * @return the next expected time in millis
	 */
	public long getNextExpectedTimeInMillis() {
		crack();
		return time.getTimeInMillis()
				+ (long) ((nsamp) / getRate() * 1000. + 0.5);
	}

	/**
	 * Gets the end time string.
	 *
	 * @return the end time string
	 */
	public String getEndTimeString() {
		crack();
		GregorianCalendar end = new GregorianCalendar();
		if (getRate() < 0.0001)
			end.setTimeInMillis(time.getTimeInMillis());
		else
			end.setTimeInMillis(time.getTimeInMillis()
					+ (long) ((nsamp - 1) / getRate() * 1000. + 0.5));
		int iy = end.get(Calendar.YEAR);
		int doy = end.get(Calendar.DAY_OF_YEAR);
		int hr = end.get(Calendar.HOUR_OF_DAY);
		int min = end.get(Calendar.MINUTE);
		int sc = end.get(Calendar.SECOND);
		int msec = end.get(Calendar.MILLISECOND);
		return "" + iy + " " + int5.format(doy).substring(2, 5) + ":"
				+ int5.format(hr).substring(3, 5) + ":"
				+ int5.format(min).substring(3, 5) + ":"
				+ int5.format(sc).substring(3, 5) + "."
				+ int5.format(msec).substring(2, 5);
	}

	/**
	 * return the rate factor from the seed header.
	 *
	 * @return The factor
	 */
	public short getRateFactor() {
		return rateFactor;
	}

	/**
	 * return the rate multiplier from the seed header.
	 *
	 * @return The multiplier
	 */
	public short getRateMultiplier() {
		return rateMultiplier;
	}

	/**
	 * return the sample rate.
	 *
	 * @return the sample rate
	 */
	public double getRate() {
		crack();
		double rate = rateFactor;
		// if rate > 0 its in hz, < 0 its period.
		// if multiplier > 0 it multiplies, if < 0 it divides.
		if (rateFactor == 0 || rateMultiplier == 0)
			return 0;
		if (rate >= 0) {
			if (rateMultiplier > 0)
				rate *= rateMultiplier;
			else
				rate /= -rateMultiplier;
		} else {
			if (rateMultiplier > 0)
				rate = -rateMultiplier / rate;
			else
				rate = -1. / (-rateMultiplier) / rate;
		}
		return rate;
	}

	/**
	 * Return the duration of this packet.
	 * 
	 * @return the duration in seconds.
	 */
	public double getDuration() {
		crack();
		return getNsamp() / getRate();
	}

	/**
	 * get the raw buffer representing this Miniseed-record. Beware you are
	 * getting the actual buffer so changing it will change the underlying data
	 * of this MiniSeed object. If this is done after crack, some of the
	 * internals will not reflect it.
	 * 
	 * @return the raw data byte buffer
	 */
	public byte[] getBuf() {
		return buf;
	}

	/**
	 * return the record length from the blockette 1000. Note the length might
	 * also be determined by getBuf().length as to how this mini-seed was
	 * created (buf is a copy of the input buffer).
	 * 
	 * @return the length if blockette 1000 present, zero if it is not
	 */
	public int getRecLength() {
		crack();
		return recLength;
	}

	/**
	 * return a blockette 100.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette100() {
		crack();
		return buf100;
	} // These getBlockettennn contain data from the various

	/**
	 * return a blockette 200.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette200() {
		crack();
		return buf200;
	} // possible "data blockettes" found. They are

	/**
	 * return a blockette 201.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette201() {
		crack();
		return buf201;
	} // never all defined.

	/**
	 * return a blockette 300.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette300() {
		crack();
		return buf300;
	}

	/**
	 * return a blockette 310.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette310() {
		crack();
		return buf310;
	}

	/**
	 * return a blockette 320.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette320() {
		crack();
		return buf320;
	}

	/**
	 * return a blockette 390.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette390() {
		crack();
		return buf390;
	}

	/**
	 * return a blockette 395.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette395() {
		crack();
		return buf395;
	}

	/**
	 * return a blockette 400.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette400() {
		crack();
		return buf400;
	}

	/**
	 * return a blockette 405.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette405() {
		crack();
		return buf405;
	}

	/**
	 * return a blockette 500.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette500() {
		crack();
		return buf500;
	}

	/**
	 * return a blockette 1000.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette1000() {
		crack();
		return buf1000;
	}

	/**
	 * return a blockette 1001.
	 *
	 * @return the blockette or null if this blockette is not in mini-seed
	 *         record
	 */
	public byte[] getBlockette1001() {
		crack();
		return buf1001;
	}

	/**
	 * return the forward integration constant.
	 *
	 * @return the forward integration constant
	 */
	public int getForward() {
		crack();
		return forward;
	}

	/**
	 * return the offset to the data.
	 *
	 * @return the offset to the data in bytes
	 */
	public int getDataOffset() {
		return (int) dataOffset;
	}

	/**
	 * return reverse integration constant.
	 *
	 * @return The reverse integration constant
	 */
	public int getReverse() {
		crack();
		return reverse;
	}

	/**
	 * get the sequence number as an int!.
	 *
	 * @return the sequence number
	 */
	public int getSequence() {
		crack();
		return Integer.parseInt(new String(seq));
	}

	/**
	 * return the timing quality byte from blockette 1000.
	 *
	 * @return the timing quality byte from blockette 1001 or -1 if it does not
	 *         exist
	 */
	public int getTimingQuality() {
		crack();
		if (b1001 == null)
			return -1;
		return b1001.getTimingQuality();
	}

	/**
	 * return the number of used frames from from blockette 1000.
	 *
	 * @return the # used frames from blockette 1001 or -1 if it does not exist
	 */
	public int getB1001FrameCount() {
		crack();
		if (b1001 == null)
			return -1;
		return b1001.getFrameCount();
	}

	/**
	 * return the number of used frames from from blockette 1000.
	 *
	 * @return the # used frames from blockette 1001 or -1 if it does not exist
	 */
	public int getB1001USec() {
		crack();
		if (b1001 == null)
			return -1;
		return b1001.getUSecs();
	}

	/**
	 * the unit test main.
	 *
	 * @param n the new b1000 length
	 */
	/**
	 * Set the length of the packet to n in the Blockette 1000
	 * 
	 * @param n
	 *            the record length, anything but 512 or 4096 will generate some
	 *            warning output
	 */
	public void setB1000Length(int n) {
		crack();
		if (n != 512 && n != 4096)
			logger.warn("Unusual B1000 len=" + n + " " + toString());
		// Change length of record in Blockette1000 and change it in buf1000 and
		// in data buffer.
		if (b1000 != null) {
			b1000.setRecordLength(n);
			System.arraycopy(b1000.getBytes(), 0, buf1000, 0, 8);
			for (int j = 0; j < blocketteList.length; j++) {
				if (blocketteList[j] == 1000) {
					System.arraycopy(b1000.getBytes(), 0, buf,
							blocketteOffsets[j], 8);
					break;
				}
			}
			cracked = false;
		}
		return;
	}

	/**
	 * Sets the b1001 frame count.
	 *
	 * @param n the new b1001 frame count
	 */
	public void setB1001FrameCount(int n) {
		// Change #frames in framce count in B1ockette 1001 and store in buf1001
		// and data buffer
		crack();
		if (b1001 != null) {
			b1001.setFrameCount(n);
			System.arraycopy(b1001.getBytes(), 0, buf1001, 0, 8);
			for (int j = 0; j < blocketteList.length; j++) {
				if (blocketteList[j] == 1001) {
					System.arraycopy(b1001.getBytes(), 0, buf,
							blocketteOffsets[j], 8);
					break;
				}
			}
			cracked = false;
		}
	}

	/**
	 * Sets the b1001 usec.
	 *
	 * @param n the new b1001 usec
	 */
	public void setB1001Usec(int n) {
		// Change #frames in framce count in B1ockette 1001 and store in buf1001
		// and data buffer
		crack();
		if (b1001 != null) {
			b1001.setUSecs(n);
			System.arraycopy(b1001.getBytes(), 0, buf1001, 0, 8);
			for (int j = 0; j < blocketteList.length; j++) {
				if (blocketteList[j] == 1001) {
					System.arraycopy(b1001.getBytes(), 0, buf,
							blocketteOffsets[j], 8);
					break;
				}
			}
			cracked = false;
		}
	}

	/**
	 * Decomp.
	 *
	 * @return the int[]
	 * @throws SteimException the steim exception
	 * @throws BlockSizeException the block size exception
	 */
	public int[] decomp() throws SteimException, BlockSizeException {
		int rev = 0;
		int[] samples = null;

		try {
			if (getBlockSize() > dataOffset) {
				int framelen = getBlockSize() - dataOffset;
				byte[] frames = new byte[framelen];
				System.arraycopy(buf, dataOffset, frames, 0, framelen);

				if (getEncoding() == 10)
					samples = Steim1.decode(frames, getNsamp(), swap, rev);
				if (getEncoding() == 11)
					samples = Steim2.decode(frames, getNsamp(), swap, rev);

			} else {
				StringBuilder message = new StringBuilder();
				message.append(String
						.format("BlockSizeException: (blockSize:[{%s}]) > (dataOffset:[{%s}])\n",
								getBlockSize(), dataOffset));
				throw new BlockSizeException(message.toString());
			}
		} catch (SteimException e) {
			throw e;
		}

		// Would adding this block "as is" cause a reverse constant error (or
		// steim error)? If so, restore block
		// to state before adding this one, write it out, and make this block
		// the beginning of next output block
		if (Steim2.hadReverseError() || Steim2.hadSampleCountError()) {
			if (Steim2.hadReverseError())
				logger.error("Decomp  " + Steim2.getReverseError() + " "
						+ toString());
			if (Steim2.hadSampleCountError())
				logger.error("decomp " + Steim2.getSampleCountError() + " "
						+ toString());
			return null;
		}
		return samples;
	}
}
