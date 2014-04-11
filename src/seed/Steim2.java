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
//import gov.usgs.anss.edge.*;
import java.util.GregorianCalendar;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *  Class for decoding or encoding Steim2-compressed data blocks
 *  to or from an array of integer values.
 * <p>
 * Steim compression scheme Copyrighted by Dr. Joseph Steim.<p>
 * <dl>
 * <dt>Reference material found in:</dt>
 * <dd>
 * Appendix B of SEED Reference Manual, 2nd Ed., pp. 119-125
 * <i>Federation of Digital Seismic Networks, et al.</i>
 * February, 1993
 * </dd>
 * <dt>Coding concepts gleaned from code written by:</dt>
 * <dd>Guy Stewart, IRIS, 1991</dd>
 * <dd>Tom McSweeney, IRIS, 2000</dd>
 * </dl>
 *
 * @author Philip Crotwell (U South Carolina)
 * @author Robert Casey (IRIS DMC)
 * @version 10/23/2002
 */

public class Steim2 {
  private static final Logger logger = LoggerFactory.getLogger(seed.Steim2.class);
  static int xminus1;
  static boolean dbg;
  static boolean strictRIC=false;
  static boolean traceBackErrors;
  static String reverseError;
  static String sampleCountError;
  static int [] frameNsamp = new int[63];
  static int [] frameReverse = new int[63];
  static int [] frameForward = new int[63];
  static StringBuffer sb;
  public static void setStrictRIC(boolean t) {strictRIC=t;}
  public static String getDebugString() {return sb.toString();}
  static public void setTracebackErrors(boolean t) {traceBackErrors=t;}
  static public void setDebug(boolean t) {dbg=t;}
  /** return if the last decode had a reverse integration error, this error does not cause a SteimException
   *@return true if last decode had a reverse integeration error*/
  static public boolean hadReverseError() {return (reverseError != null);}
  /** get some text documenting the last reverse integration error
   *@return The text describing the reverse integration error*/
  static public String getReverseError() {return (reverseError == null? "": reverseError);}
  /** return if the last decode had a reverse integration error, this error does not cause a SteimException
   *@return true if last decode had a reverse integeration error*/
  static public boolean hadSampleCountError() {return (sampleCountError != null);}
  /** get some text documenting the last sample count error
   *@return The text describing the reverse integration error*/
  static public String getSampleCountError() {return (sampleCountError == null? "": sampleCountError);}
  static public int getXminus1() {return xminus1;}
  /** return an array with the number of samples in each frame of the last decode
   *@return An array with the number of samples in each of the last decode*/
  static public int [] getFrameNsamp() {return frameNsamp;}
  /** return an array with the value of the reverse integration constant for each frame in last decode
   *@return An array with the reverse integration constants from last decode */
  static public int [] getFrameReverse() {return frameReverse;}
  /** return an array with the value of the reverse integration constant for each frame in last decode
   *@return An array with the reverse integration constants from last decode */
  static public int [] getFrameForward() {return frameForward;}
	/**
	 *  Decode the indicated number of samples from the provided byte array and
	 *  return an integer array of the decompressed values.  Being differencing
	 *  compression, there may be an offset carried over from a previous data
	 *  record.  This offset value can be placed in <b>bias</b>, otherwise leave
	 *  the value as 0.
	 *  @param b input byte array to be decoded
	 *  @param numSamples the number of samples that can be decoded from array
	 *  <b>b</b>
	 *  @param swapBytes if true, swap reverse the endian-ness of the elements of
	 *  byte array <b>b</b>.
	 *  @param bias the first difference value will be computed from this value.
	 *  If set to 0, the method will attempt to use the X(0) constant instead.
	 *  @return int array of length <b>numSamples</b>.
	 *  @throws SteimException - encoded data length is not multiple of 64
	 *  bytes.
	 */	public static int[] decode(byte[] b, int numSamples, boolean swapBytes, int bias) throws SteimException {
		if (b.length % 64 != 0) {
		  //throw new SteimException("encoded data length is not multiple of 64 bytes (" + b.length + ")"); 
			SteimException e = new SteimException("encoded data length is not multiple of 64 bytes (" + b.length + ")");
			logger.error("Steim2 SteimException:", e);
			return null;
		}
    dbg=false;
    for(int i=0; i<63; i++) {frameReverse[i]=2147000000; frameNsamp[i] = 0;frameForward[i]=-2147000000;}
    reverseError=null;
    sampleCountError=null;
		int[] samples = new int[numSamples];
    if(numSamples == 0) return samples;
		int[] tempSamples;
		int numFrames = b.length / 64;
    if(numFrames < 1 || numFrames > 64) throw new SteimException("# frames unknown = "+numFrames);
		int current = 0;
		int start=0, end=0;
		int firstData=0;
		int lastValue = 0;
    
    if(dbg ){
      if(sb == null) sb=new StringBuffer(10000);
      else sb.delete(0,  sb.length()-1);
    }
    int lastCurrent=0;
		//System.err.println("DEBUG: number of samples: " + numSamples + ", number of frames: " + numFrames + ", byte array size: " + b.length);
		for (int i=0; i< numFrames; i++ ) {
			//System.err.println("DEBUG: start of frame " + i);
			tempSamples = extractSamples(b, i*64, swapBytes);   // returns only differences except for frame 0
			firstData = 0; // d(0) is byte 0 by default
			if (i==0) {   // special case for first frame
				lastValue = bias; // assign our X(-1)
				xminus1=lastValue;
				// x0 and xn are in 1 and 2 spots
				start = tempSamples[1];  // X(0) is byte 1 for frame 0
				end = tempSamples[2];    // X(n) is byte 2 for frame 0
				firstData = 3; // d(0) is byte 3 for frame 0
				//System.err.println("DEBUG: frame " + i + ", bias = " + bias + ", x(0) = " + start + ", x(n) = " + end);
				// if bias was zero, then we want the first sample to be X(0) constant
        if(tempSamples.length < 4)
          Util.prt("   **** Bad tempsamples (<3) in first frame!="+tempSamples.length);
        else if (bias == 0) {
          lastValue = start - tempSamples[3];  // X(-1) = X(0) - d(0)
          xminus1 = lastValue;
        }
			}
			//System.err.print("DEBUG: ");
			for (int j = firstData; j < tempSamples.length && current < numSamples; j++) {
				samples[current] = lastValue + tempSamples[j];  // X(n) = X(n-1) + d(n)
				lastValue = samples[current];
				//System.err.print("d(" + (j-firstData) + ")" + tempSamples[j] + ", x(" + current + ")" + samples[current] + ";");
				current++;
			}
			//System.err.println("DEBUG: end of frame " + i);
      if(i == 0) frameNsamp[i] = current;
      else frameNsamp[i] = current - lastCurrent;   // number of samples in this frame
      if(current > 0) frameReverse[i] = samples[current-1];
      frameForward[i] = samples[lastCurrent];
      lastCurrent = current;
      if(current >= numSamples) break;                // no need to process the empty frames
		}  // end for each frame...
    if(current <= 0) {
      //System.out.println("Steim2 illegal # current");
      //throw new SteimException("Steim2 found no samples in block");
    	SteimException e = new SteimException("Steim2 found no samples in block");
    	logger.error("Steim2 SteimException:", e);
    	return null;
    }
    if(samples[current-1] != end && (end != 0 || strictRIC)) {       // if end is zero, presume it was never set and hence is not an error
      reverseError="Steim2 reverse integration error is="+samples[current-1]+"!="+end+" expected at "+(current-1);
      //System.out.println("Steim2 reverse integration error is="+samples[current-1]+"!="+end+" rev constant");
      if(traceBackErrors) {
        RuntimeException e = new RuntimeException("Steim2 rev int err (non-fatal) is="+samples[current-1]+"!="+end+" rev constant");
        logger.error("Steim2 RuntimeException:", e);
      }
    }
    if(current != numSamples) {
      sampleCountError="Steim2 sample count error got "+current+" expected "+numSamples;
      //System.out.println(sampleCountError);
      if(traceBackErrors) {
        RuntimeException e = new RuntimeException("Steim2 sample Count err (non-fatal) is="+current+" expected"+numSamples);
        logger.error("Steim2 RuntimeException:", e);
      }
    }
		return samples;
	}

	/**
	 * Abbreviated, zero-bias version of decode().
	 *
	 * @see edu.iris.Fissures.codec.Steim2#decode(byte[],int,boolean,int)
	 */
	public static int[] decode(byte[] b, int numSamples, boolean swapBytes) throws SteimException {
		// zero-bias version of decode
		return decode(b,numSamples,swapBytes,0);
	}

	public static byte[] encode(int[] samples) {
		byte[] b = new byte[0];
		return b;
	}

	/**
	 * Extracts differences from the next 64 byte frame of the given compressed
	 * byte array (starting at offset) and returns those differences in an int
	 * array.
	 * An offset of 0 means that we are at the first frame, so include the header
	 * bytes in the returned int array...else, do not include the header bytes
	 * in the returned array.
	 * @param bytes byte array of compressed data differences
	 * @param offset index to begin reading compressed bytes for decoding
	 * @param swapBytes reverse the endian-ness of the compressed bytes being read
	 * @return integer array of difference (and constant) values
	 */
	protected static int[] extractSamples(byte[] bytes,
			int offset, 
			boolean swapBytes) 
	{
		/* get nibbles */
		int nibbles = Utility.bytesToInt(bytes[offset], 
				bytes[offset+1], 
				bytes[offset+2], 
				bytes[offset+3], 
				swapBytes);
		int currNibble = 0;
    if(dbg) sb.append("Key="+Integer.toHexString(nibbles)+"\n");
		int dnib = 0;
		int[] temp = new int[106]; // 7 samples * 15 long words + 1 nibble int
		int tempInt;
		int currNum = 0;
		for (int i=0; i<16; i++) {
			currNibble = (nibbles >> (30 - i*2 ) ) & 0x03;
			switch (currNibble) {
				case 0:
					//System.out.println("0 means header info");
					// only include header info if offset is 0 and in first 3 words (forward and rev constant)
					if (offset == 0 && currNum <= 3) { // added currNum < 3 12/06 to fix concatenated block problem
						temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)],
								bytes[offset+(i*4)+1],
								bytes[offset+(i*4)+2],
								bytes[offset+(i*4)+3],
								swapBytes);
            if(dbg) sb.append("Word "+i+" is hdr "+Integer.toHexString(temp[currNum-1])+"\n");
					}
					break;
				case 1:
					//System.out.println("1 means 4 one byte differences");
					temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)]);
					temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)+1]);
					temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)+2]);
					temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)+3]);
          if(dbg) sb.append("Word "+i+" is 8 byte ns="+(currNum-4)+"diff="+bytes[offset+(i*4)]+" "+
              bytes[offset+(i*4)+1]+" "+
              bytes[offset+(i*4)+2]+" "+bytes[offset+(i*4)+3]+" val="+temp[currNum-4]+" "+
              temp[currNum-3]+" "+temp[currNum-2]+" "+temp[currNum-1]+"\n");
					break;
				case 2:
					tempInt = Utility.bytesToInt(bytes[offset+(i*4)], 
							bytes[offset+(i*4)+1],
							bytes[offset+(i*4)+2], 
							bytes[offset+(i*4)+3], 
							swapBytes);
					dnib = (tempInt >> 30) & 0x03;
					switch (dnib) {
						case 1:
							//System.out.println("2,1 means 1 thirty bit difference");
							temp[currNum++] = (tempInt << 2) >> 2;
              if(dbg) sb.append("Word "+i+" is 02 dnib="+dnib+" ns="+(currNum-1)+"diff(30 bit)="+temp[currNum-1]+"\n");
							break;
						case 2:
							//System.out.println("2,2 means 2 fifteen bit differences");
							temp[currNum++] = (tempInt << 2) >> 17;  // d0
							temp[currNum++] = (tempInt << 17) >> 17; // d1
              if(dbg) sb.append("Word "+i+" is 02 dnib="+dnib+" ns="+(currNum-2)+"diff(15 bit)="+temp[currNum-2]+
                  " "+temp[currNum-1]+"\n");
							break;
						case 3:
							//System.out.println("2,3 means 3 ten bit differences");
							temp[currNum++] = (tempInt << 2) >> 22;  // d0
							temp[currNum++] = (tempInt << 12) >> 22; // d1
							temp[currNum++] = (tempInt << 22) >> 22; // d2
              if(dbg) sb.append("Word "+i+" is 02 dnib="+dnib+" ns="+(currNum-3)+"diff(10 bit)="+temp[currNum-3]+
                  " "+temp[currNum-2]+" "+temp[currNum-1]+"\n");
							break;
						default:
							//System.out.println("default");
					}
					break;
				case 3:
					tempInt = Utility.bytesToInt(bytes[offset+(i*4)], 
							bytes[offset+(i*4)+1],
							bytes[offset+(i*4)+2], 
							bytes[offset+(i*4)+3],
							swapBytes);
					dnib = (tempInt >> 30) & 0x03;
					// for case 3, we are going to use a for-loop formulation that
					// accomplishes the same thing as case 2, just less verbose.
					int diffCount = 0;  // number of differences
					int bitSize = 0;    // bit size
					int headerSize = 0; // number of header/unused bits at top
					switch (dnib) {
						case 0:
							//System.out.println("3,0 means 5 six bit differences");
							headerSize = 2;
							diffCount = 5;
							bitSize = 6;
							break;
						case 1:
							//System.out.println("3,1 means 6 five bit differences");
							headerSize = 2;
							diffCount = 6;
							bitSize = 5;
							break;
						case 2:
							//System.out.println("3,2 means 7 four bit differences, with 2 unused bits");
							headerSize = 4;
							diffCount = 7;
							bitSize = 4;
							break;
						default:
							//System.out.println("default");
					}
					if (diffCount > 0) {
						for (int d=0; d<diffCount; d++) {  // for-loop formulation
							temp[currNum++] = ( tempInt << (headerSize+(d*bitSize)) ) >> (((diffCount-1)*bitSize) + headerSize);
						}
            if(dbg) {
              String s ="Word "+i+" is 03 dnib="+dnib+" ns="+(currNum-4)+" diff("+bitSize+" bit)=";
              for(int ii=0; ii<diffCount; ii++) s = s + temp[currNum+ii-diffCount]+" ";
              sb.append(s+"\n");
            }
					}
			}
		}
		int[] out = new int[currNum];
		System.arraycopy(temp, 0, out, 0, currNum);
		return out;
	}

	/**
	 * Static method for testing the decode() method.
	 * @param args not used
	 * @throws SteimException from called method(s)
	 */
	public static void main(String[] args) throws SteimException {
    Util.setModeGMT();
    GregorianCalendar end = new GregorianCalendar(2006,3,15,23,59);
    Util.prt(Util.ascdate(end)+" "+Util.asctime2(end)+" ms="+end.getTimeInMillis());
    int dom = end.get(Calendar.DAY_OF_MONTH);
    long time=end.getTimeInMillis();
    int ms=60002;
    //end.setTimeInMillis(time+ms);
    end.add(Calendar.MILLISECOND, ms);
    Util.prt(Util.ascdate(end)+" "+Util.asctime2(end)+
        " ms="+end.getTimeInMillis()+" df="+(end.getTimeInMillis()-time));
    Util.prt("Dom = "+dom+" end dom="+end.get(Calendar.DAY_OF_MONTH));
		byte[] b = new byte[64];
		@SuppressWarnings("unused")
    int[] temp;

		for (int i=0; i< 64 ; i++) {
			b[i] = 0x00;
		}
		b[0] = 0x01;
		b[1] = (byte)0xb0;
		System.out.println(b[1]);
		b[2] = (byte)0xff;
		b[3] = (byte)0xff;

		b[4] = 0;
		b[5] = 0;
		b[6] = 0;
		b[7] = 0;

		b[8] = 0;
		b[9] = 0;
		b[10] = 0;
		b[11] = 0;

		b[12] = 1;
		b[13] = 2;
		b[14] = 3;
		b[15] = 0;

		b[16] = 1;
		b[17] = 1;
		b[18] = 0;
		b[19] = 0;

		b[20] = 0;
		b[21] = 1;
		b[22] = 0;
		b[23] = 0;
		temp = Steim2.decode(b, 17, false);
	}

}
