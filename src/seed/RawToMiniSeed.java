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
 * RawToMiniSeed.java
 *
 * Created on June 29, 2005, 3:12 PM
 *
 */

package seed;
import java.util.TreeMap;
import java.util.Map;
import java.util.Iterator;
import java.text.DecimalFormat;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
//import java.io.*;
//import gov.usgs.anss.edgehydra.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**

 * This class allows a static entry where a the user submits a seed name, time
 * and an array of 32-bit "in-the-clear" ints with a chunk of time series and this
 * routine will output mini-seed blockettes ready for writing to the data system.
 * (In fact they are written using "IndexBlock.writeMiniSeed()").  This keeps a 
 * static list (via TreeMap) of the channels known to this routine which it then
 * uses to keep the context of the mini-seed data going.
 *
 *Notes on Steim2 compression : The Steim 2 contains 0-N data differences the value
 * of data[0] and data[n-1].  The first difference always takes data[0] to the last 
 * value of the previous frame.  So, the only time you need outside information is
 * for the first buffer if you are trying to get the first difference to match.  If
 * not, the first difference can be zero and the prior reverse constant is unknowable.
 * To support making this first difference be right during testing, the ability to
 * pass the last value is supported in the call to  add timeseries.  Practically there
 * is no reason to pass such a value other than for trying to exactly match some 
 * known series.
 * 
 * @author davidketchum
 */
// MTH: getting a lot of redunduant cast warnings ...
@SuppressWarnings("cast")
public class RawToMiniSeed {
  // Static variables
	private static final Logger logger = LoggerFactory.getLogger(seed.RawToMiniSeed.class);
	
  static private DecimalFormat df6; 
  static private final Map<String, RawToMiniSeed> chans = (Map<String,RawToMiniSeed>)
      Collections.synchronizedMap(new TreeMap<String, RawToMiniSeed>());     // The list of known channels
  static private final Map<String, RawToMiniSeed> secondChans= (Map<String,RawToMiniSeed>)
      Collections.synchronizedMap(new TreeMap<String, RawToMiniSeed>());     // The list of known channels
  static private final Map<String, RawToMiniSeed> oob = (Map<String,RawToMiniSeed>)
      Collections.synchronizedMap(new TreeMap<String, RawToMiniSeed>());     // The list of known channels
  static private String debugChannel="";
  //static private boolean nohydra;       // If true, nothing will generate hydra output
  
  
  // Steim2 compressor working variables
  double rate;
  long periodUSec;
  int integration;
  // the next 3 must be correct before each return to maintain context
  int reverse;                        // last sample of compression to date
  int [] data;                        // up to 8 samples that might have been left over
  int ndata;                          // number of left over samples
  long earliestTime;                  // Time of first sample in this RTMS in micros
  long difftime;                      // time of first difference in d in micros
  int julian;                         // julian day of diff time
  int currentFrame;                   // Current frame where data is going
  int currentWord;                    // Current word in the frame where data goes
  
  // Space for the mini-seed records
  int numFrames;                      // number of Steim frames for one record
  String seedname;                    // The seed name for this channel
  FixedHeader hdr;                    // storage for fixed header
  int startSequence;
  SteimFrames [] frames;              // storage for Steim 64 byte frames
  int ns;                             // Number of samples compress in this group of frames
  int nextDiff;                       // next difference to compress (index to data/diff)
  // Internal variables
  int backblk;                        // block number to write next
  private byte [] lastputbuf;
  MiniSeedOutputHandler overrideOutput;
  static boolean dbg;
  static StringBuffer sb=new StringBuffer(100);
  
  // Dead timer
  long lastUpdate;
  //public static void setNoHydra(boolean t) {nohydra=t;}
  @Override
  public String toString() {
    return seedname+" "+timeFromUSec(earliestTime)+" to "+timeFromUSec(difftime)+
        " ns="+ns+" nxtDiff="+nextDiff+" ndata="+ndata+" rev="+reverse;
  }
  
  
  public String toStringObject() {
    String s = super.toString(); 
    int beg=s.length()-12;
    if(beg < 0) beg=0;
    return s.substring(beg);
  }
  public String lastTime() {
    return stringFromUSec( (long) (difftime+ndata*periodUSec+0.01));
  }
  public String earliestTime() {return stringFromUSec(earliestTime);}
  private String stringFromUSec(long usec) { 
    if(df6 == null) df6 = new DecimalFormat("000000");
    int [] ymd = SeedUtil.fromJulian(julian);
    int ms=(int) (usec/1000);
    int hr= ms/3600000;
    ms = ms % 3600000;
    int min=ms /60000;
    ms = ms % 60000;
    int sec=ms/1000;
    ms = ms %1000;
    return ""+df6.format(ymd[0]).substring(2,6)+"-"+df6.format(ymd[1]).substring(4,6)+"-"+
        df6.format(ymd[2]).substring(4,6)+" "+df6.format(hr).substring(4,6)+":"+
        df6.format(min).substring(4,6)+":"+df6.format(sec).substring(4,6)+"."+
        df6.format(ms).substring(3,6);
  }
  public static void setDebugChannel(String s) {debugChannel=s;}
  public void setStartSequence(int i) {startSequence=i;}
  public static String timeFromUSec(long usec) { 
    if(df6 == null) df6 = new DecimalFormat("000000");
    int ms=(int) (usec/1000);
    int hr= ms/3600000;
    ms = ms % 3600000;
    int min=ms /60000;
    ms = ms % 60000;
    int sec=ms/1000;
    ms = ms %1000;
    return ""+df6.format(hr).substring(4,6)+":"+
        df6.format(min).substring(4,6)+":"+df6.format(sec).substring(4,6)+"."+
        df6.format(ms).substring(3,6);
  }  /** return the useconds since the julian fiducial time as a long
   * @return the absolute microseconds of last data sample in this compression rec
   */
  public long getJulianUSec() {
    return (long) (((long) julian*86400000000L)+difftime+ndata*periodUSec);
  }

  /** get seedname of channel
   *@return the seedname of the channel*/
  public String getSeedname() {return seedname;}
  /** return time since last thing happed on this channel
   *@return the time in millis since the last modification or creation of this channel
   */
  public long lastAge() {return (System.currentTimeMillis() - lastUpdate);}
  /** return digitizing rate
   *@return The digitizing rate*/
  public double getRate() {return rate;}
  /** set a new digit rate (overrides rate  used at creation
   *@param rt The new rate to use */
  public void setRate(double rt) {rate = rt; hdr.setRate(rt);}
  /** for debuging a string which represents the building of the RawToMiniSeed details
   *@return The string representing the debugging detail */
  static public String getDebugString() {if(sb == null) return ""; else return sb.toString();}
  /** set the debug flag 
   *@param t The boolean value of the debug flag */
  public static void setDebug(boolean t) {dbg=t;}
  /** check all RTMS on all 3 compression streams and force out if last update older than ms
   *@param ms The number of milliseconds of age needed to cause forceout*/
  public synchronized static void forceStale(int ms) {
    if(chans == null || secondChans == null || oob == null) return;
    Iterator<RawToMiniSeed> itr = null;
    synchronized(chans) {
      itr = chans.values().iterator();
      while(itr.hasNext()) {
        RawToMiniSeed rm = itr.next();
        if(rm.lastAge() > ms) {
          Util.prta("RTMS: timeout stale primary age="+rm.lastAge()+" "+rm);
          rm.forceOut();
          itr.remove();
        }
      }
    }
    synchronized(secondChans) {
      itr = secondChans.values().iterator();
      while(itr.hasNext()) {
        RawToMiniSeed rm = itr.next();
        if(rm.lastAge() > ms) {
          Util.prta("RTMS: timeout stale 2nd chans  age="+rm.lastAge()+" "+rm);
          rm.forceOut();
          itr.remove();
        }
      }
    }
    synchronized(oob) {   
      itr = oob.values().iterator();
      while(itr.hasNext()) {
        RawToMiniSeed rm = itr.next();
        if(rm.lastAge() > ms) {
          Util.prta("RTMS: timeout stale OOB chans  age="+rm.lastAge()+" "+rm);
          rm.forceOut();
          itr.remove();
        }
      }
    }
  }
  /** Write out a time series for the given seed name, time series, time and
   * optional flags.  This presumes no knowlege of the prior reverse integration 
   * constant (the last data value of the previously compressed block).
   *@param x Array of ints with time series
   *@param nsamp Number of samples in x
   *@param seedname Internal seedname as an ascii (NNSSSSSCCCLL)
   *@param year The year of the 1st sample
   *@param doy The day-of-year of first sample
   *@param sec Seconds since midnight of the first sample
   *@param micros Microseconds (fraction of a second) to add to seconds for first sample
   *@param rt The nominal digitizing rate in Hertz
   *@param activity The activity flags per SEED volume (ch 8 pg 93)
   *@param IOClock The I/O and clock flags per SEED volume
   *@param quality The clock quality as defined in SEED volume
   *@param timingQuality The clock quality as defined in SEED volume blockette1001
   * @param par The parent thread to use for logging
   */
  public static void addTimeseries(int [] x, int nsamp, String seedname, 
      int year, int doy, int sec, int micros, 
      double rt, int activity, int IOClock, int quality, int timingQuality) {
    addTimeseries(x, nsamp, seedname,year,doy,sec,micros,rt,activity,IOClock, quality,
        timingQuality,0);
  }

  /** Write out a time series for the given seed name, time series, time and
   * optional flags.  This presumes no knowlege of the prior reverse integration 
   * constant (the last data value of the previously compressed block).
   *@param x Array of ints with time series
   *@param nsamp Number of samples in x
   *@param seedname Internal seedname as an ascii (NNSSSSSCCCLL)
   *@param year The year of the 1st sample
   *@param doy The day-of-year of first sample
   *@param sec Seconds since midnight of the first sample
   *@param micros Microseconds (fraction of a second) to add to seconds for first sample
   *@param rt The nominal digitizing rate in Hertz
   *@param activity The activity flags per SEED volume (ch 8 pg 93)
   *@param IOClock The I/O and clock flags per SEED volume
   *@param quality The clock quality as defined in SEED volume
   * @param timingQuality The timint quality (0-100)
   * @param par The parent thread to use for logging.
   *@param lastValue Only needed if exact match to existing seed is needed.
   */
  // DEBUG: 7/26/2007 try this whole routine synchronized
  public static void addTimeseries(int [] x, int nsamp, String seedname, 
      int year, int doy, int sec, int micros, 
      double rt, int activity, int IOClock, int quality,int timingQuality,
      int lastValue) {
    if(nsamp == 0) return;            // nothing to do!
    //if(!nohydra) Hydra.send(seedname, year, doy, sec, micros,nsamp, x, rt);
    /*if(chans == null) chans = (Map<String,RawToMiniSeed>)
      Collections.synchronizedMap(new TreeMap<String, RawToMiniSeed>());    // create tree map if first time
    if(secondChans == null) secondChans = (Map<String,RawToMiniSeed>)
      Collections.synchronizedMap(new TreeMap<String, RawToMiniSeed>());    // create tree map if first time
    if(oob == null) oob = (Map<String,RawToMiniSeed>)
      Collections.synchronizedMap(new TreeMap<String, RawToMiniSeed>());    // create tree map if first time*/
    if(seedname.indexOf("IMGEA0 SHZ") >= 0 || seedname.equals(debugChannel))
      dbg=true;  // force debug on for a single channel for DEBUG
    else dbg=false;
    if(dbg) {
      if(sb == null) sb = new StringBuffer(10000);
      else if(sb.length() > 0) sb.delete(0, sb.length());
      Util.prt("RTMS: Add TS "+seedname+" "+timeFromUSec(sec*1000000L+micros)+" rt="+rt+" ns="+nsamp+
           " lst="+lastValue);
    }
    // Some input day used year doy and the offset may lap into next day, if so fix it here
    while(micros > 1000000) {
      micros -= 1000000;
      sec++;
    }  
    while(sec >= 86400) {      // Do seconds say its a new day?
      Util.prta("RTMS: day adjust new yr="+year+" doy="+doy+" sec="+sec+" usec="+micros+" seedname="+seedname);
      int jul = SeedUtil.toJulian(year,doy);
      sec -= 86400;
      jul++;
      int [] ymd = SeedUtil.fromJulian(jul);
      year = ymd[0];
      doy = SeedUtil.doy_from_ymd(ymd);
      Util.prta("RTMS: day adjust new yr="+year+" doy="+doy+" sec="+sec+" seedname="+seedname);
    }

    // find this channel in tree map or create it, check for out-of-band or 2nd
    // fill data as well.
    RawToMiniSeed rm = (RawToMiniSeed) chans.get(seedname);
    RawToMiniSeed rm2 = (RawToMiniSeed) secondChans.get(seedname);
    RawToMiniSeed rm3 = (RawToMiniSeed) oob.get(seedname);
    if(dbg) Util.prta("RTMS: rm ="+rm+"\nrm2="+rm2+"\nrm3="+rm3);
    
    
    // If the oob or secondChans list has not been used lately, force out the data
    if(rm != null) {
      if(rm.lastAge() > 120000) {
        if(dbg) Util.prta("RTMS: timeout primary chans "+seedname+" age="+rm.lastAge()+
            " now="+System.currentTimeMillis()+" "+rm);
        rm.forceOut(); 
        synchronized( chans) {
          chans.remove(seedname);
        }
        rm = null;
        if(rm2 != null) {
          if(dbg) Util.prta("RTMS: promote rm2 to rm for "+seedname);
          chans.put(seedname, rm2); 
          synchronized(secondChans) {
            secondChans.remove(seedname);
          }
          rm = rm2;
          rm2 = null;
          if(rm3 != null) {
          if(dbg) Util.prta("RTMS: promote rm3 to rm2 for "+seedname);
            synchronized(secondChans) {
              secondChans.put(seedname,rm3);
            }
            synchronized(oob) {
              oob.remove(seedname); 
            }
            rm2=rm3;
            rm3=null;
          }
        }
      }
    }
    
    // If the oob or secondChans list has not been used lately, force out the data
    if(rm2 != null) {
      if(rm2.lastAge() > 120000) {
        if(dbg) Util.prta("RTMS: timeout 2nd chans "+seedname+" age="+rm2.lastAge()+
            " now="+System.currentTimeMillis()+" "+rm2);
        rm2.forceOut(); 
        synchronized( secondChans) {
          secondChans.remove(seedname);
        }
        rm2=null;
        if(rm3 != null) {
          if(dbg) Util.prta("RTMS: promote rm3 to rm2 for "+seedname);
          synchronized( secondChans) {
            secondChans.put(seedname, rm3);
          } 
          synchronized(oob) {
            oob.remove(seedname);
          } 
          rm2=rm3;
          rm3=null;
        }
      }
    }
    if(rm3 != null) {
      if(rm3.lastAge() > 120000) {
        if(dbg) Util.prta("RTMS: timeout OOB chan "+seedname+" lastage="+rm3.lastAge()+
            " now="+System.currentTimeMillis()+" "+rm3);
        rm3.forceOut(); 
        synchronized(oob) {
          oob.remove(seedname);
        }
        rm3 = null;
      }
    }
    
    
    // order the rms by getJulianUSec() (last sample time), done by 3 comparison and then swaps
    if(rm != null && rm2 != null) {
      if(rm.getJulianUSec() < rm2.getJulianUSec()) { //switch these two
        if(dbg) Util.prta("RTMS: "+seedname+" Swap rm and rm2 "+ seedname+" "+rm.lastTime()+" "+rm2.lastTime()+
            " df="+(rm2.getJulianUSec()-rm.getJulianUSec())/1000000);
        synchronized(chans) {
          chans.remove(seedname);
        }
        synchronized(secondChans) {
          secondChans.remove(seedname);
        }
        synchronized(chans) {
          chans.put(seedname, rm2);
        }
        synchronized(secondChans) {
          secondChans.put(seedname, rm);
        }
        RawToMiniSeed rmtmp = rm2;
        rm2=rm;
        rm=rmtmp;
      }
    }
    if(rm != null && rm3 != null) {
      if(rm.getJulianUSec() < rm3.getJulianUSec()) { //switch these two
        if(dbg) Util.prta("RTMS: "+seedname+" Swap rm and rm3 "+ seedname+" "+rm.lastTime()+" "+rm3.lastTime()+
            " df="+(rm3.getJulianUSec()-rm.getJulianUSec())/1000000);
        synchronized(chans) {
          chans.remove(seedname);
        }
        synchronized(oob) {
          oob.remove(seedname);
        }
        synchronized(chans) {
          chans.put(seedname, rm3);
        }
        synchronized(oob) {
          oob.put(seedname, rm);
        }
        RawToMiniSeed rmtmp = rm3;
        rm3=rm;
        rm=rmtmp;
      }   
    }
    if(rm2 != null && rm3 != null) {
      if(rm2.getJulianUSec() < rm3.getJulianUSec()) { //switch these two
        if(dbg) Util.prta("RTMS: "+seedname+" Swap rm2 and rm3 "+ seedname+" "+rm2.lastTime()+" "+rm3.lastTime()+
            " df="+(rm3.getJulianUSec()-rm2.getJulianUSec())/1000000);
        synchronized(secondChans) {
          secondChans.remove(seedname);
        }
        synchronized(oob) {
          oob.remove(seedname);
        }
        synchronized(secondChans) {
          secondChans.put(seedname, rm3);
        }
        synchronized(oob) {
          oob.put(seedname, rm2);
        }
        RawToMiniSeed rmtmp = rm3;
        rm3=rm2;
        rm2=rmtmp;
      }   
    }
    
    //if(dbg) Util.prt("rm="+rm+" rm2="+rm2+" rm3="+rm3);
    if(rm == null) {
      Util.prta("RTMS: create new channel for "+seedname+" rate="+rt+" rm="+rm+" rm2="+rm2+" rm3="+rm3);
      rm = new RawToMiniSeed(seedname, rt, 7, year, doy,sec,micros, 0);
      if(dbg) RawToMiniSeed.setDebug(dbg);
      synchronized(chans) {
        chans.put(seedname, rm);
      }
    }
    else {            // check to see if a OOB would be better
      long gap = rm.gapCalc(year,doy, sec,micros); 
      //long gapb = rm.gapCalcBegin(year,doy,sec,micros,nsamp);
      if(dbg) Util.prt("RTMS: "+seedname+" ns="+nsamp+" sc="+sec+" gp="+gap+
        "\nRTMS: rm ="+rm+"\nRTMS: rm2="+rm2+"\nRTMS: rm3="+rm3);
      
      // check gap with main compression, if it continuous at end
      // or later in time, stick with it (i.e. this is inverse, check others!)
      if( Math.abs(gap) > 500000./rm.getRate() /*&& gap < 0*/) {
        if(rm2 != null) {
          long gap2 = rm2.gapCalc(year,doy, sec, micros);
          //long gap2b = rm2.gapCalcBegin(year,doy,sec,micros,nsamp);
          if(Math.abs(gap2) < 500000./rm.getRate()) {
            rm = rm2;
            gap = gap2;
            //if(gap2b < gap) gap=gap2b;
          } else if(rm3 != null) {
            long gap3 = rm3.gapCalc(year,doy,sec,micros);
            rm = rm3;
            gap = gap3;
          } else {            // rm3 is null use it for this one unless its close
                              // to rm2 or rm
            if(dbg) Util.prta("RTMS: create new OOB (3rd) channel for "+seedname+
                " rate="+rt+" "+RawToMiniSeed.timeFromUSec(sec*1000000L+micros));
            rm3 = new RawToMiniSeed(seedname, rt, 7, year, doy,sec,micros, 800000);
            if(dbg) RawToMiniSeed.setDebug(dbg);
            synchronized(oob) {
              oob.put(seedname, rm3);
            }
            rm = rm3;
            gap = 0;
              // end rm3 is null
          }
        } else {            // rm2 is null
          if(dbg) Util.prta("RTMS: create new second channel for "+seedname+
              " rate="+rt+" "+RawToMiniSeed.timeFromUSec(sec*1000000L+micros));
          //new RuntimeException("Create new second! ").printStackTrace(par.getPrintStream()); //DEBUG: get stack trace!
          rm2 = new RawToMiniSeed(seedname, rt, 7, year, doy,sec,micros,900000);
          if(dbg) RawToMiniSeed.setDebug(dbg);
          synchronized(secondChans) {
            secondChans.put(seedname, rm2);
          }
          rm = rm2;
          gap = 0;
        }
          
      }                     // end if gap is continuous or in future
    }                       // end else this is not a new channel
    if(dbg) Util.prt("RTMS: rm chosen is "+rm+" adding "+timeFromUSec(sec*1000000L+micros)+" ns="+nsamp);

    
    if(dbg && rm == rm2) Util.prta("RTMS: use 2ndChans "+seedname+" sec="+sec);
    if(dbg && rm == rm3) Util.prta("RTMS: use OOB chan "+seedname+" sec="+sec);
    
    if( Math.abs(rm.getRate() - rt)/rt > 0.001) {
      Util.prta("  *** ??? rate change on "+seedname+" from "+rm.getRate()+" to "+rt);
      rm.setRate(rt);
    }
    if(dbg) Util.prta("RTMS: enter process rm="+rm+" ns="+nsamp+" rt="+rm.getRate()+" "+rm.getSeedname());
    // processes the time series, time and flags.
    rm.process(x, nsamp, year,doy, sec,micros, activity,IOClock, quality, timingQuality, lastValue, (rm == rm3));
    if(dbg) Util.prta("RTMS: exit process "+rm.getSeedname());
        
  }
  /** Write out a time series for the tiven seed name, time series, time and
   * optional flags, etc.
   *@param seedname The Seed name of the channel that needs to be forced out
   * @return a copy of the buffer just sent through putbuf
   */
  public static synchronized byte [] forceout(String seedname) {
    //if(chans == null) chans = Collections.synchronizedMap( new TreeMap<String, RawToMiniSeed>());    // create tree map if first time
    
    // find this channel in tree map or create it
    RawToMiniSeed rm = chans.get(seedname);
    if(rm == null) {
        Util.prta("forceout(): *** Call to forceout on non-existant channel="+seedname);
        if(dbg) sb.append("call to forceout on non-existing channel!! = "+seedname+"\n");
        return null;
    }
    
    // processes the time series, time and flags. 
    rm.forceOut();
    byte [] buf = rm.getLastPutbuf();
    rm.clear();
    return buf;
  }
  /** set a MiniSeedOutputHandler object to output the data
   * @param obj An object which implements MiniSeedOutputHander to use for putbuf()
   */
  public void setOutputHandler(MiniSeedOutputHandler obj) {
    overrideOutput = obj;
  }
  /** write stuff to the parents log file
   */
  private void prt(String s) {Util.prt(s); }
  private void prta(String s) {Util.prta(s);}
  /** Creates a new instance of RawToMiniSeed 
   * @param name The seedname of the channel to create
   *@param rt nominal data rate of the channel
   *@param nFrames Number of frames to put in the output Mini-seed (7 for 512, 63 for 4096)
   *@param year Year of the first sample for this
   *@param doy Day-of-year of the first sample
   *@param sec integer number of seconds since midnight for 1st sample
   *@param micros Fractional Microseconds of first sample
   * @param startSeq Start any compression with this sequence number
   * @param par The parent thread for logging.
   */
  public RawToMiniSeed(String name , double rt, int nFrames, 
      int year, int doy, int sec, int micros, int startSeq) {
    seedname=name;
    numFrames=nFrames;
    overrideOutput=null;
    frames = new SteimFrames[numFrames];
    startSequence=startSeq;
    for(int i=0; i<numFrames; i++) frames[i] = new SteimFrames();
    hdr = new FixedHeader(seedname, rt, numFrames, year, doy, sec, micros, startSequence);
    difftime = sec*1000000L+micros;         // no data in time buffer, set it to current
    earliestTime=difftime;
    // This is a runtime exception catch for impossible Julian day conversion to try to 
    // see them more clearly
    try{
      julian = SeedUtil.toJulian(year,doy);
    } catch (RuntimeException e) {
      prta("RuntimeException special catch in RTMS: seed="+name+" yr="+year+" doy="+doy+
          " sec="+sec);
      //e.printStackTrace();
      logger.error("RuntimeException:", e);
      julian=SeedUtil.toJulian(1972,1);   // give it some Julian day just in case
    }
    ndata=0;
    rate=rt;
    lastUpdate = System.currentTimeMillis();
    data = new int[8];           // Most difference which can be leftover
    reverse=2147000000;
    integration=0;
    currentWord=0;
    currentFrame=0;
    ns=0;
    periodUSec = (long) (1000000./rate+0.01);
  }
  /** calculate the gap from the last sample in this RawToMiniSeed stream and the
   *time represented by sec and micros
   * @param year The year
   * @param doy The day of year
   *@param  sec Time since midnight of first sample
   *@param  micros Micros to add to seconds of first sample
   *@return The time difference in micros
   */
  public long gapCalc(int year,int doy, int sec, int micros) {
    long time = SeedUtil.toJulian(year,doy)*86400000000L + sec*1000000L+micros;
    long gap = (long) (time - difftime-ndata*periodUSec - julian*86400000000L);
    return gap;
  }
  /** calculate the gap from the first sample in this RawToMiniSeed stream and the
   *time represented by sec and micros
   * @param year The year
   * @param doy The day of year
   *@param  sec Time since midnight of first sample
   *@param  micros Micros to add to seconds of first sample
   *@param  n Number of samples in packet
   *@return The time difference in micros
   */
  public long gapCalcBegin(int year,int doy, int sec, int micros, int n) {
    long time = SeedUtil.toJulian(year,doy)*86400000000L + sec*1000000L+micros;
    long gap = (long) (julian*86400000000L + earliestTime -  (time+n/rate*1000000.+0.001));
    
    // if the gap is on the order of days, check to make sure it is not a day problem
    /*if(Math.abs(gap) >= 86399999999L) {
      int jul = SeedUtil.toJulian(year,doy);
      if(jul != julian) {
        gap = gap + (jul - julian)*86400000000L;
      }
    }*/
    return gap;
  }
  /** process a time series into this RawToMiniSeed - used by static functions addTimeSeries
   * to actually modify the channel synchronized so this cannot have two sets in process at
   * same time.  If this channel is configured as a hydra bound channel, it is put in the
   * HydraQueue for processing (if it is latter in time than the last one!)
   *@param ts Array of ints with time series
   *@param nsamp Number of samples in x
   *@param year The year of the 1st sample
   *@param doy The day-of-year of first sample
   *@param sec Seconds since midnight of the first sample
   *@param micros Microseconds (fraction of a second) to add to seconds for first sample
   *@param activity The activity flags per SEED volume (ch 8 pg 93)
   *@param IOClock The I/O and clock flags per SEED volume
   *@param quality The data quality as defined in SEED volume
   *@param timingQuality The timing quality for blockette 1001
   *@param lastValue Only needed if exact match to existing seed is needed.
   *
   */
  public synchronized void process(int [] ts, int nsamp, int year, int doy, int sec, int micros,
      int activity, int IOClock, int quality, int timingQuality,  int lastValue) {
    process(ts, nsamp, year, doy, sec, micros, activity, IOClock, quality, timingQuality, lastValue, false);
  }
  /** process a time series into this RawToMiniSeed - used by static functions addTimeSeries
   * to actually modify the channel synchronized so this cannot have two sets in process at
   * same time.  If this channel is configured as a hydra bound channel, it is put in the
   * HydraQueue for processing (if it is latter in time than the last one!)
   *@param ts Array of ints with time series
   *@param nsamp Number of samples in x
   *@param year The year of the 1st sample
   *@param doy The day-of-year of first sample
   *@param sec Seconds since midnight of the first sample
   *@param micros Microseconds (fraction of a second) to add to seconds for first sample
   *@param activity The activity flags per SEED volume (ch 8 pg 93)
   *@param IOClock The I/O and clock flags per SEED volume
   *@param quality The data quality as defined in SEED volume
   *@param timingQuality The timing quality for blockette 1001
   * @param isRM3 If true, this is expected to be hacked up so we do not put out any SendEvents.
   *@param lastValue Only needed if exact match to existing seed is needed.
   *
   */
  public synchronized void process(int [] ts, int nsamp, int year, int doy, int sec, int micros,
      int activity, int IOClock, int quality, int timingQuality,  int lastValue, boolean isRM3) {
    // The time internally is of the first difference.  We need to check that there is
    // not a time gap which should cause us to close out this one

    
    long gap = gapCalc(year,doy, sec,micros);
    boolean midnightProcess=false;
    lastUpdate = System.currentTimeMillis();

    // Check time for formal correctness and report if not
    if( (!isRM3 && julian != SeedUtil.toJulian(year,doy) && 
            !(julian+1 == SeedUtil.toJulian(year,doy) && sec <100)) || // its just a packet on the next day early
            sec < 0 || sec >= 86400 || micros > 1000000 || micros < 0) {
      prta("RTMS: this  ="+toString());
      prta("RTMS: odd rm="+chans.get(seedname));
      prta("RTMS: odd rm2="+secondChans.get(seedname));
      prta("RTMS: odd rm3="+oob.get(seedname));
      RuntimeException e = new RuntimeException("RTMS: "+seedname+" julian="+julian+" "+SeedUtil.toJulian(year,doy)+"  time not right! yr="+year+" doy="+doy+" sec="+sec+" micros="+micros);
      logger.error("RuntimeException:", e);
    }
    
    // If this data is from the next day, cut off the old day
    if(SeedUtil.toJulian(year,doy) != julian) {
      prta("RTMS: "+seedname+" New day on input (EOD?) :"+julian+" != "+SeedUtil.toJulian(year,doy)+
          " sec="+sec+" usec="+micros);
      forceOut(reverse);
      clear();
      julian = SeedUtil.toJulian(year,doy);
      hdr.setStartTime(year,doy,sec,micros);
      earliestTime = sec*1000000L+micros;
      difftime=earliestTime;
    }
    
    // iIf all  of these are true a force out has occurred, this is new data, do not check for gaps
    if(currentWord == 0 && reverse == 2147000000 && ns == 0 && currentFrame == 0) {
      hdr.setStartTime(year,doy,sec,micros);
      earliestTime=sec*1000000L+micros;
    }
    // If this is earlier than this first, check for continuity the other way!
    else {
      if( (sec*1000000L+micros) < earliestTime) {
        gap = gapCalcBegin(year, doy, sec, micros, nsamp);
        if(dbg) sb.append("RTMS: "+seedname+" "+earliestTime()+" discont2 force out gap="+(gap/1000000.)+" sec\n");
        if(dbg) prta("RTMS: "+seedname+" "+earliestTime()+" discont2 frc out gap="+(gap/1000000.));
        forceOut(reverse);
        clear();
        // Since this is not continuous, need to reset header time
        hdr.setStartTime(year, doy, sec, micros);  
        //else prt("RTMS: "+seedname+" "+earliestTime()+" add begin continuous");
        earliestTime=sec*1000000L+micros;
        difftime=earliestTime;
        
      }
      // Its later than first time, see if it is a gap at the end
      else {
        // If this gap is bigger than 1/2 sample (1/2 sample is 500000./rate in micros)
        if(Math.abs(gap) > 500000./rate) {
          if(dbg) sb.append("RTMS: "+seedname+" "+lastTime()+" discont force out gap="+(gap/1000000.)+" sec\n");
          if(dbg) prta("RTMS: "+seedname+" "+lastTime()+" discont frc out gap="+(gap/1000000.)+
              " Sec fr="+currentFrame+" wd="+currentWord+" ns="+ns+" rt="+rate+" rev="+reverse+" lst="+lastValue);
          forceOut(reverse);
          clear();
          earliestTime = sec*1000000L+micros;     // set initial time to this buffer
          difftime=earliestTime;
          hdr.setStartTime(year,doy,sec,micros);
        }
      }
    }

    
    // Set the flags for all putbufs from this call to process according to the flags
    hdr.setActivity(activity); 
    hdr.setIOClock(IOClock);
    hdr.setQuality(quality);
    hdr.setTimingQuality(timingQuality);

    // while we have enough differences, prepare to move them
    int [] x = new int[Math.max(0,ndata)+nsamp];        // space for the time series
    int [] d = new int [Math.max(0,ndata)+nsamp];       // space for the differences
    if(d.length == 0) prta("***** there are no samples in d!"+toString());
    for(int i=0; i<ndata; i++) x[i]=data[i];// put left over data into data array
    for(int i=0; i<nsamp; i++) x[i+ndata] = ts[i];
    if(reverse == 2147000000) {
      d[0]=
          x[0]-lastValue;       // starting up you do not know the reverse
    }
    else {
      d[0]=
          x[0]-reverse;                 // else use it to get first difference
      if(lastValue != reverse && dbg) 
        sb.append(seedname+" Last != reverse "+lastValue+" != "+reverse+"\n");
    }
    for(int i=1; i<nsamp+ndata; i++) d[i] = x[i]-x[i-1];  // compute the differences
    nextDiff=0;
    boolean somePacked=true;
    
    // we need to get the current key 
    int key=0;
    if(currentWord == 0) {
      key=0;
      currentWord=1;
    }
    else key=frames[currentFrame].get(0);  // Get the current key
    int dnib=-1;         // per manual coding of 2nd key (stored in data word high order)
    int bits=0;         // number of bits in the packing
    int npack=0;        // Number of samples to pack
    int ck=0;           // coding of key in main key word
    int loops=0;
    while (somePacked) {
      loops++;
      if(loops % 100000 == 0) {
        prta("  ***** ception Infinite loop process seed="+seedname+" nextdiff="+nextDiff);
        System.exit(0);
      }
      // check for startup conditions, start of new frame
      if(currentFrame == 0 && currentWord == 1 && nextDiff < x.length) {
        frames[0].put(x[nextDiff],1);        // forward integration constant
        if(dbg && sb == null) prt("RTMS: null sb and dbg is on "+seedname);
        if(dbg && x == null) prt("RTMS: null x and dbg is on "+seedname);
        if(dbg) sb.append("Word 1 is hdr "+Integer.toHexString(x[nextDiff])+"\n");
        currentWord=3;                  // Skip the key, ia0 and ian
        key = 0;
        frames[currentFrame].put(key, 0);            // mark key as zero
      }

      if( (somePacked = packOK(d, nextDiff,7, 8))) {            // 7x4 bit diffs
        ck=3; dnib=2; npack=7; bits=4;
      } else if( (somePacked = packOK(d,nextDiff, 6, 16))) {    // 6x5 bit diffs
        ck=3;dnib=1; npack=6; bits=5;
      } else if( (somePacked = packOK(d,nextDiff, 5, 32))) {    // 5x6 bit diffs
        ck=3;dnib=0; npack=5; bits=6;
      } else if( (somePacked = packOK(d,nextDiff, 4, 128))) {   // 4x8 bit diffs
        ck=1;dnib=-1; npack=4; bits=8;
      } else if( (somePacked = packOK(d,nextDiff, 3, 512))) {   // 3x10 bit diffs
        ck=2;dnib=3; npack=3; bits=10;
      } else if( (somePacked = packOK(d,nextDiff, 2, 16384))) { // 2x15 bit diffs
        ck=2;dnib=2; npack=2; bits=15;
      } else  {                                                 // 1x30 bit diff
        if(Math.abs(d[nextDiff]) > 536870912)
          prta("RTMS: "+seedname+" **** diff bigger than 30 bits "+d[nextDiff]+" packet will be broken"+year+":"+doy+" sec="+sec);
        ck=2;dnib=1; npack=1; bits=30;
        somePacked=true;
      } 
      if(somePacked ) { 
        // If this packing crosses the day boundary, adjust it so it does not and cut off
        // midnightProcess insures this is only dones once in a process buffer
        long begbuf=((long)sec*1000000L+micros+(nextDiff+npack-ndata)*periodUSec) ;
        //if(dbg) prta("RTMS: "+seedname+" test midnight "+begbuf+" mid="+midnightProcess);
        if( !midnightProcess &&
          ( begbuf > (86400000000L + periodUSec -1L) ||   // This is on the next day
            begbuf <= 0 ))  { // packet on new day, ndata on other side
          //               The next day + 1 sample less a bit
          midnightProcess = true;
          int ndataold=ndata;
          int nleft =0;
          if(begbuf < 0) nleft = (int) ((-begbuf + periodUSec -1L) / periodUSec);
          else nleft = (int) (((86400000000L + periodUSec -1L) -
              // The current time of the next samples                       usecs/samples
              ((long) sec*1000000L + micros+ (nextDiff - ndata)*periodUSec))/periodUSec);
           //if(dbg)
              prta("RTMS: "+seedname+" EOD cut off ndata begbuf="+begbuf+" needed="+nleft+" ndataold="+ndataold+
                 " nextDiff="+nextDiff+" nsamp="+nsamp+" ns="+ns+" sec="+sec+" usec="+micros+" period="+periodUSec+" data.len="+data.length);
          if(nleft <= (nsamp+ndata - nextDiff)) {  // Do we have enough data for a midnight cutoff

            // There are ndata that need to be forced into this frame,  move the desired samples
            // to the begining of the data buffer to fake forceOut() to use these samples.
            for(int i=0; i<nleft; i++) data[i]=x[nextDiff+i];
            ndata = nleft;
            frames[currentFrame].put(key,0);          // save any mods made to key so forceOut gets it
            if(nextDiff > 0) forceOut(x[nextDiff-1]); // used faked data to force it out
            else forceOut(reverse);           // note : no call to clear() as we are just faking
            // update time to next sample in header
            int julian2 = SeedUtil.toJulian(year,doy);
            julian2++;
            int [] ymd = SeedUtil.fromJulian(julian2);
            year=ymd[0];
            int newdoy = SeedUtil.doy_from_ymd(ymd);
            // we added a day so adjust micros by that amount.
            doy = newdoy;
            sec = sec - 86400;
            julian=julian2;   // Do not force out again on day change
  //          hdr.setStartTime(ymd[0], doy, sec, micros+(nextDiff-ndataold+ndata)*periodUSec);
  //          earliestTime = sec*1000000L+micros;
            nextDiff += nleft;                // adjust nextDiff for the number of samps just out
            hdr.setStartTime(ymd[0], doy, sec, micros+(nextDiff - ndataold)*periodUSec);
            earliestTime = sec * 1000000L+micros+(nextDiff - ndataold)*periodUSec;
            ndata=ndataold;             // finished with fakeout, restore ndata
            prta("RTMS: "+seedname+" EOD cut done ndata="+ndata+" nextDiff="+nextDiff+
                " ns="+ns+" sec="+sec+" usec="+micros);
            //difftime = (long) sec * 1000000L + (long) micros + (long) ((nextDiff-ndata)/rate*1000000.);
            currentWord=1;
            key=0;
            continue;                         // go to bottom of loop to start new packing
          }
        }

        // Check to see if there is enough data remaining to do this compression
        if(npack > (nsamp+ndata - nextDiff)) {
          for(int i=nextDiff; i< nsamp+ndata; i++) data[i-nextDiff] = x[i];
          frames[currentFrame].put(key,0);        // save the key for next time
          
          // The time of the first data sample in the difference array (for gap detection)
          // time of ndata + samples into the new data (nextDiff-ndata) at rate
          difftime = (long) sec * 1000000L + (long) micros + (long) ((nextDiff-ndata)*periodUSec);
          julian = SeedUtil.toJulian(year,doy);
          if(difftime > 86400000000L) { 
            //julian++; // Mar 5, 2008 - if data is compressed that already does not cross the boundary, we commented this
                        // so the check at the top of the routine would cause the data to be cut off on julian day skip.
            difftime -= 86400000000L;
          }
          ndata = nsamp+ ndata - nextDiff;
          // reverse is the data value just proceeding the difference array
          if(nextDiff > 0) reverse=x[nextDiff-1];
          else {
            //Util.prt("Process: set reverse first sample minus diff="+data[0]+" "+d[0]+
            //    " nsamp="+nsamp+" "+seedname+" "+stringFromUSec(difftime));
            reverse = data[0]-d[0];    // 1/08 cut the BS - reverse has to be data minus the difference (to get same diff!)
          }
          //else reverse=lastValue;   // if we dont have the value, use the last one!
          return;                     // There is not enough data to compress yet
        }
        
        

        
        // Pack up npack samples of bits using the ck and dnib
        int w=0;
        int mask = 0x3FFFFFFF;
        if(bits < 30) mask = mask >> (30 - bits);
        for (int i=0; i<npack; i++) w = (w << bits) | (d[nextDiff++] & mask);
        ns += npack;
        if(dnib != -1) 
          w = w | (dnib <<30);
        key = (key << 2) | ck;
        if(dbg) {
          String s="Word "+currentWord+" is "+ck+" dnib="+dnib+" ns="+ns+" diff("+bits+" bit)=";
          for(int i=0; i<npack; i++) s=s+d[nextDiff+i-npack]+" ";
          sb.append(s+"\n");
        }
        frames[currentFrame].put(w, currentWord);
        
        // Advance to next word, if this is end-of-frame, do the book keeping
        currentWord++;
        if(currentWord >= 16) {
          frames[currentFrame].put(key, 0);
          if(dbg) sb.append("key="+Integer.toHexString(key)+"\n");
          key=0;                              // clear key for next run
          if(currentFrame == numFrames -1) {
            if(dbg) sb.append("Word 2 of zero is hdr "+Integer.toHexString(x[nextDiff-1])+"\n");
            frames[0].put(x[nextDiff-1], 2);
            reverse = x[nextDiff-1];
          }
          currentFrame++;
          currentWord =1;           // point to first data word, word 0 will get the key
          
          // Is this new frame beyond the last frame (record is full?)
          if(currentFrame == numFrames) {    // yes, output the record and do book keeping
            currentFrame = 0;
            byte [] buf = new byte[64+numFrames*64];
            ByteBuffer bf = ByteBuffer.wrap(buf);
            bf.clear();
            hdr.setNSamp(ns);                 // set the number of samples in fixed hdr
            hdr.setActualNFrames(numFrames);
            ns=0;
            
            bf.put(hdr.getBytes());     // get the hdr 9
            // put all of the frames into the buffer
            for(int i=0; i<numFrames; i++) 
              bf.put(frames[i].getBytes());
            // call routine to dispose of this frame
            if(dbg) sb.append("Call putbuf with len="+(numFrames+1)*64+" override="+overrideOutput+"\n");
            putbuf(bf.array(), (numFrames+1)*64);
            /*try {
              byte [] fr = new byte[numFrames*64];
              System.arraycopy(bf.array(), 64, fr, 0, numFrames*64);
              int [] samples = Steim2.decode(fr, hdr.getNSamp(), false);
              prt("NSamples returned="+samples.length);
            }
            catch(SteimException e) {prt("Put buf Steim Exception");}*/
            // calculate based on latest time available the start time of next record
            hdr.setStartTime(year,doy,sec,((long)micros)+((long)((nextDiff-ndata)/rate*1000000.)));
            // increment the sequence number for the fixed header
            hdr.incrementSequence();
          }
        }     // if(currentWord < 16)
      }       // if somePacked
    }         // while (somePacked)
    prt("****** Unusual exit from process!");
    System.exit(0);
  }
  
  // These variables are used by the putbuf related routines
  static byte [] lastbuf;
  static boolean newPutbuf;
  /** for debugging returns state of "newPutbuf" boolean which is set by putbuf()
   * whenever new output is written.  User could monitor this for change to "true" a
   * and then use getLastBuf to retrieve the output.
   *@return Current falue of newPutbuf boolean
   */
  static public boolean newPutbuf() { return newPutbuf;}
  /** reset the newPutbuf flag and zero the lastBuf for debugging use 
   * @param t Value to set newPutbuf to
   */
  static public void setNewPutbuf(boolean t) {
    if(!newPutbuf && lastbuf != null) 
      for(int i=0; i<lastbuf.length; i++) lastbuf[i]=0;
    newPutbuf=t;
  }
  public byte [] getLastPutbuf() {return lastputbuf;}
  /** this routine puts a complete mini-seed blockette in buf out to the correct file
   * using the EdgeFile related routines.  For debug purposes it stores the "newPutBuf"
   * to indicate this was called by internal processing since the last time that flag was
   * reset and puts a copy of the last written buffer in lastbuf.
   */
  private void putbuf(byte [] buf, int size) {
    if(overrideOutput != null) {
      overrideOutput.putbuf(buf, size);
      return;
    }

    // This is what happens if no override output is set up, not much use
    newPutbuf=true;
    if(lastbuf == null) lastbuf = new byte[size];
    if(lastputbuf == null) lastputbuf = new byte[size];
    System.arraycopy(buf, 0, lastbuf, 0, size);
    System.arraycopy(buf, 0, lastputbuf, 0, size);
    //prt("Put buf size="+size);
    
    try {
      MiniSeed ms = new MiniSeed(buf);
      if(ms.getTimeInMillis() % 86400000L + (ms.getNsamp()-1)/ms.getRate()*1000. > 86400000) 
        prt("*** RTMS: created block that spans midnight!="+ms);
      //prt("RTMS: Putbuf rw="+(backblk+"    ").substring(0,5)+" ms="+ms.toString());
      // Write to the data files 
      if(dbg) sb.append("putbuf(): writeMiniSeed for ms="+ms);
    }
    catch (IllegalSeednameException e) {
      prt("Putbuf: illegal seedname="+e.getMessage());
      logger.error("IllegalSeednameException:", e);
    }

  }
  /** for debug purposes, get the last data buffer written out by putbuf
   *@return The last output of putbuf
   */
  public static byte [] getLastbuf() {return lastbuf;}

  /** forceOut takes any remaining data in the data (ndata) and adds it to the
   * end of the current frame and then calls putbuf.  If this is actually the end
   * of all of the data, the user must call clear() afterward to reset all of the
   * counters.  This is because day roll overs just fake up a data[ndata] and call
   * this routine but do not want other data about the current context to be changed
   *The reverse integration constant used is the one stored in the object
   */
  public synchronized void forceOut() {forceOut(reverse);}
  
  /** forceOut takes any remaining data in the data (ndata) and adds it to the
   * end of the current frame and then calls putbuf.  If this is actually the end
   * of all of the data, the user must call clear() afterward to reset all of the
   * counters.  This is because day roll overs just fake up a data[ndata] and call
   * this routine but do not want other data about the current context to be changed
   *@param reverse  The data point before the beginning of this set of data frames
   */
  public synchronized void forceOut(int reverse) {
    // We must be done so position key to top of word before forcing out
    if(currentFrame == 0 && currentWord <= 1) {
      currentWord=0;
      return;     // nothing to force out!
    }
    if(dbg) sb.append("Force out called reverse="+reverse+"\n");
    if(currentWord >= 16) {  //DEBUG: this should nebvver happen, but it does!!!!
      prta(seedname+"Exception: forceoutCalled with currentWord>=16="+currentWord+
        " ndata="+ndata);
      // clear the output related variables (things into the frames).
     // Set up for new compression buffer
      ns=0;
      currentFrame = 0;
      currentWord=0;
      reverse=2147000000;         // mark reverse is unknown, we forced it out
    }

    if(dbg) prta(seedname+" forceOut() called with rev="+reverse+" ndata="+ndata);
    int key = frames[currentFrame].get(0);
    // is there any data sitting in ndata, we must force it out as well
    if(ndata > 0) {
      int dnib=-1;         // per manual coding of 2nd key (stored in data word high order)
      int bits=0;         // number of bits in the packing
      int npack=0;        // Number of samples to pack
      int ck=0;           // coding of key in main key word
      int [] d = new int [ndata];       // space for the differences 

      if(reverse == 2147000000) d[0] = 0;         // no prior data, so difference is unknown
      else d[0]=data[0]-reverse;                 // else use it to get first difference
      for(int i=1; i<ndata; i++) d[i] = data[i]-data[i-1];  // compute the differences
      
      if(ndata == 7) {            // 7x4 bit diffs
        ck=3; dnib=2; npack=7; bits=4;
      } else if(ndata == 6) {    // 6x5 bit diffs
        ck=3;dnib=1; npack=6; bits=5;
      } else if(ndata == 5) {    // 5x6 bit diffs
        ck=3;dnib=0; npack=5; bits=6;
      } else if(ndata == 4) {   // 4x8 bit diffs
        ck=1;dnib=-1; npack=4; bits=8;
      } else if(ndata == 3) {   // 3x10 bit diffs
        ck=2;dnib=3; npack=3; bits=10;
      } else if(ndata == 2) { // 2x15 bit diffs
        ck=2;dnib=2; npack=2; bits=15;
      } else  {               // 1x30 bit diff
        ck=2;dnib=1; npack=1; bits=30;
      } 
      if(!packOK(d,0, npack, 1<<(bits-1))) {
        prt(seedname+" Steim2 Exception: force out does not work!  bits="+bits+" npack="+npack+" ndata="+ndata+
            " reverse="+reverse+" limit="+(1<<(bits-1)));
        prt(seedname+" ns="+ns+" curWord="+currentWord+" curFrm="+currentFrame+" nextdiff="+nextDiff);
        for(int i=0; i<ndata; i++) prt("d["+i+"]="+d[i]+" data["+i+"]="+data[i]);
        RuntimeException e = new RuntimeException("force out does not work");
        logger.error("RuntimeException:", e);
        //System.exit(0);     // extreme action!
      }

      // Pack up npack samples of bits using the ck and dnib
      int w=0;
      int mask = 0x3FFFFFFF;
      if(bits < 30) mask = mask >> (30 - bits);
      int nd=0;
      for (int i=0; i<npack; i++) w = (w << bits) | (d[nd++] & mask);
      ns += npack;
      if(dnib != -1) 
        w = w | (dnib <<30);
      key = (key << 2) | ck;
      if(dbg) {
        String s="Force out Word "+currentWord+" is "+ck+" dnib="+dnib+" ns="+ns+" diff("+bits+" bit)=";
        for(int i=0; i<npack; i++) s=s+d[nd+i-npack]+" ";
        sb.append(s+"\n");
        //prt("ForceOut buf="+sb.toString());
      }
      if(currentWord >= 16) prta("ERR:Forceout current word="+currentWord+" bits="+bits+" ndata="+ndata);
      frames[currentFrame].put(w, currentWord);
      currentWord++;
    }
    while(currentWord < 16) {
      key = key << 2;                               // move key up
      frames[currentFrame].put(0, currentWord);   // zero out the unused words in the frame
      currentWord++;
    } 
    frames[currentFrame].put(key,  0);
    // put all zeros in any remaining frames
    for(int i=currentFrame+1; i<numFrames; i++) 
      for(int j=0; j<16; j++) frames[i].put(0, j);
    if(ndata <= 0) frames[0].put(reverse, 2);   // not clear it can be < 0????
    else frames[0].put(data[ndata-1], 2);      // reverse integration is last data sample
    
    // clean up and force the output buffer
    byte [] buf = new byte[64+numFrames*64];
    ByteBuffer bf = ByteBuffer.wrap(buf);
    bf.clear();
    hdr.setNSamp(ns);                 // set the number of samples in fixed hdr
    hdr.setActualNFrames(currentFrame+1);

    bf.put(hdr.getBytes());     // get the hdr 
    // put all of the frames into the buffer
    for(int i=0; i<numFrames; i++) 
      bf.put(frames[i].getBytes());
    // call routine to dispose of this frame
    putbuf(bf.array(), (numFrames+1)*64);
    
    // clear the output related variables (things into the frames).
   // Set up for new compression buffer
    ns=0;
    currentFrame = 0;
    currentWord=0;
    reverse=2147000000;         // mark reverse is unknown, we forced it out
 
    // calculate based on latest time available the start time of next record
    //hdr.setStartTime(year,doy,sec,((long)micros)+((long)((nextDiff-ndata)/rate*1000000.)));
    // increment the sequence number for the fixed header
    hdr.incrementSequence();

    
  } 
  /** Get the RawToMiniSeed record for the given channel name from the static tree of same
   *@param seedname The 12 character seed name for the channel
   *@return A RawToMiniSeed for the channel or null if one is not yet defined
   */
  public static RawToMiniSeed getRawToMiniSeed(String seedname){
    if(chans == null) return null;
    return (RawToMiniSeed) chans.get(seedname);
  }


  /** cause this channel to be clearred for a new compression buffer.  ANy data in
   *progress is discarded.
   */
  public void clear() {

    // Set up for new compression buffer
    ns=0;
    nextDiff=0;
    ndata=0;
    currentFrame = 0;
    currentWord=0;
    reverse=2147000000;         // mark reverse is unknown, we forced it out
    
  }
  /** return true of the given data differeces will bit in to ns samples of size
   * limite by limit 
   */
  private boolean packOK(int [] d, int i, int ns, int limit) {
    int low = -limit;
    boolean ok=true;
    for(int j=i; j<i+ns; j++) {
      if(j < d.length) {
        if(d[j] < low || d[j] >= limit) {  // <= is not needed < should be o.k.
          ok=false;
          break;
        }
      } else break;         // not enough data break out
    }
    return ok;
  }
  
  /** This internal class represents and implements translation to/from the fixed
   * 48 byte header of mini-seed blockettes.
   */
  class FixedHeader {
    int sequence;
    int startSequence;
    double rate;          // Digitizing rate in hertz 
    String seedname;      // 12 character seed name
    int numFrames;        // Number of 64 byte Steim Frames in a whole record (for blockette1000)
    int actualFrames;     // Number of frames in this particular one
    short year; 
    short doy;
    long time;
    short rateFactor;       // Rate converted to integer
    short rateMultiplier;   // Divisor if needed for rate
    short nsamp;
    byte activity,IOClock, quality,timingQuality;
    byte usecs;
    /** increment the sequence number of this header */
    public void incrementSequence() {sequence++; if(sequence < 0 || sequence >= 1000000) sequence=0;}
    /** instanticate a Fixed header 
     *@param name The Seed name
     *@param rt Nominal data rate in Hz
     *@param nFrames Number of steim 64 byte frames in the attached data portion
     *@param yr Year of first sample in this Mini-seed blockette
     *@param dy Day-of-year of 1st sample
     *@param sec integral # of seconds since midnight
     *@param micros Fractional Microseconds of 1st sample */
    public FixedHeader(String name, double rt, int nFrames, int yr, int dy, int sec, int micros,
        int startSeq) {
      // rearrange the seed name to internal format
      seedname=name;
      rate=rt;
      activity=0; IOClock=0; quality=0; timingQuality=0;
      startSequence = startSeq;
      setRate(rate);

      if(name.substring(0,5).equals("XXMPR")) Util.prt("set rate rate="+rate+" fact="+rateFactor+" mult="+rateMultiplier);
      numFrames=nFrames;
      sequence=startSequence;
      year = (short) yr;
      doy = (short) dy;
      time = sec*1000000L+micros;
    }

    /** return the bytes of this Fixed header in a 64 byte array. Since a ByteBuffer is used
     * to put data into the buffer, the data are in BIG ENDIAN order.
     *@return The 64 bytes with data for the 48 byte header */
    public byte [] getBytes() {
      byte [] b = new byte[64];
      Arrays.fill(b, (byte) 0); 
      ByteBuffer buf = ByteBuffer.wrap(b); 
      if(df6 == null) df6 = new DecimalFormat("000000");
      buf.clear();
      buf.put(df6.format(sequence).substring(0,6).getBytes());
      buf.put((byte) 'D');
      buf.put((byte) ' ');
      // store name in seed manual order (pg 92 Chap 8 on fixed header)
      buf.put( (seedname.substring(2, 7)+seedname.substring(10, 12)+
          seedname.substring(7, 10)+seedname.substring(0,2)).getBytes());
      buf.putShort(year).putShort(doy);
      long t = time;
      buf.put((byte) (t/3600000000L));
      t = time % 3600000000L;
      buf.put((byte) (t/60000000L));
      t = t % 60000000L;
      buf.put((byte) (t/1000000L));
      t = t % 1000000L;
      buf.put((byte) 0).putShort((short) (t/100));
      buf.putShort(nsamp);
      buf.putShort(rateFactor);
      buf.putShort(rateMultiplier);
      buf.put(activity).put(IOClock).put(quality).put((byte) 2);// one block follows
      buf.putInt(0);
      buf.putShort((short) 64).putShort((short) 48);
      
      // This is the blockette 1000 (next block) encoding Steim2==11, Word order 1 (big endian)
      buf.putShort((short) 1000).putShort((short) 56).put((byte) 11).put((byte) 1);
      if(numFrames == 7) buf.put((byte) 9);
      else if(numFrames == 63) buf.put((byte) 12);
      buf.put((byte) 0);    // reserved 1000
      // blockette 1001 ,  next blockette (none),  timing quality, usecs
      buf.putShort((short) 1001).putShort((short) 0).put((byte) timingQuality).put((byte) usecs);
      // reserved, number of blockettes
      buf.put((byte) 0).put((byte) actualFrames);
      
      return buf.array();
    }
    /** return # of samples in data represented by this header 
     *@return the # of samples in data represented by this header */
    public int getNSamp() {return (int) nsamp;}
    /** set number of samples represented by this header 
     *@param n The number of samples to which will be represented by this header */
    public void setNSamp(int n) {nsamp=(short) n;}
    /** set the digitizing rate
     *
     * @param rate The rate in Hz
     */
    public void setRate(double rate) {
      // Convert Rate to integer scheme used by seed
      if(rate < 1) {
        double r = 1./rate;                           // period in seconds
        if(Math.abs(r - Math.round(r)) < 0.0001) {    // is it an even period in seconds
          rateFactor = (short) (-r-.001);            // yes, set it neg for period mult 1
          rateMultiplier = (short) 1;
        }
        else {
          rateMultiplier=-1;                    // scale period to  fit short, set mult to be divisor
          while(r < 3275) {
            r = r *10;
            rateMultiplier = (short) (rateMultiplier * 10);
          }
          rateFactor = (short) Math.round(-r);
        }
      }
      // Its > 1, so do hz
      else {
        // check to see if this is an integer, if so, the GSN prefers multipliers of 1
        if(Math.abs(rate - Math.round(rate)) < 0.00001) {
          rateFactor = (short) (rate+0.001);
          rateMultiplier = (short) 1;
        }
        else {
          double r = rate;   // convert to period
          rateMultiplier=-1;
          while(r < 3275) {
            r = r*10;
            rateMultiplier= (short) (rateMultiplier*10);
          }
          rateFactor = (short) Math.round(r);
        }
      }
    }    /** set the activity flags
     *@param a The activity flags per the SEED documentation
     */
    public void setActivity(int a) {activity=(byte) a;}
    /** set the quality flags 
     *@param a The Quality flags as per the SEED documentation */
    public void setQuality(int a) {quality=(byte) a;}
    /** set the I/O and Clock flags
     *@param a The I/O and clock flags per the SEED docs */
    public void setIOClock(int a) {IOClock=(byte) a;}
    /** set timing quality
     *@param the timing quality */
    public void setTimingQuality(int i) {timingQuality=(byte) i;}
    /** public void set the number of frames in this header
     *@param the actual number of frames *
     */
    public void setActualNFrames(int nf){actualFrames=nf;}
    /** set the start time of the first sample 
     *@param yr Year of first sample in this Mini-seed blockette
     *@param dy Day-of-year of 1st sample
     *@param sec integral # of seconds since midnight
     *@param micros Fractional Microseconds of 1st sample 
     */
    public void setStartTime(int yr, int dy, int sec, long micros) {
      year=(short) yr; doy= (short) dy; time = sec*1000000L+micros;
    }
  }
  /** This class makes it easier to represent Steim Data frames of 64 bytes each
   *for implementing the compressor.  It basically allows putting and gettting 4 bytes
   *at a time to the encapsulated arrays.  No matter what kind of computer we are on,
   *this data is written in high endian order
   */
  class SteimFrames {
    byte [] w;
    public SteimFrames() {
      w = new byte[64];
    }
    /** put the int i in the steim frame at index */
    public void put(int i, int index) {
      if(index >= 16) prta("Index is out of range in steimFrame PUT"+index);
      for(int j=0; j<4; j++) {
        w[index*4+3-j] = (byte) (i & 0xFF);
        i = i >> 8;
      }
    }
    /** get the int from the index offset of the Steim Frame */
    public int get(int index) {
      int i=0;
      for(int j=0; j<4; j++) {
        i = i << 8;
        i = i | (((int) w[index*4+j]) & 0xff);
      }
      return i;
    }
    /** return all 64 bytes represented by this Steim Frame */
    public byte [] getBytes() {return w;}
  }
}
