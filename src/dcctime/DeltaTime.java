/**
 * 
 */
package dcctime;

/**
 * @author fshelly
 *
 */
public class DeltaTime
{
    int	nday;
    byte 	nhour;
    byte 	nmin;
    byte 	nsec;
    short	ntenth_msecs;

    public static final int MaxDeltaInt = 20*86400*1000;

    public DeltaTime()
    {
        nday = 0;
        nhour = 0;
        nmin = 0;
        nsec = 0;
        ntenth_msecs = 0;
    } // constructor DeltaTime()

    /**
     * @param time1 Class value set to time2 - time1
     * @param time2
     */
    public DeltaTime(StdTime time1, StdTime time2)
    {
        int jul1, jul2;
        int dd, dh, dm, ds, dtenth_ms;

        jul1 = DCCTime.YearMonthDay2Julian(time1.year, 1, time1.day);
        jul2 = DCCTime.YearMonthDay2Julian(time2.year, 1, time2.day);

        dd = jul1 - jul2;
        dh = time1.hour - time2.hour;
        dm = time1.minute - time2.minute;
        ds = time1.second - time2.second;
        dtenth_ms = time1.tenth_msec - time2.tenth_msec;

        // Normalize values
        ds += dtenth_ms / 10000;
        dtenth_ms = dtenth_ms % 10000;
        dm += ds / 60;
        ds = ds % 60;
        dh += dm / 60;
        dm = dm % 60;
        dd += dh / 24;
        dh = dh % 24;

        // And save values in class
        nday = dd;
        nhour = (byte)dh;
        nmin = (byte)dm;
        nsec = (byte)ds;
        ntenth_msecs = (short)dtenth_ms;
    } // constructor DeltaTime

    /**
     * @return true if toSeconds exceeds MaxDeltaInt.  Prevents integer overflow
     */
    public boolean ExceedsMaxSeconds()
    {
        if (nday < 1)
            return false;
        if ((MaxDeltaInt / nday) < 24*60*60)
            return true;
        return false;
    }
    public int toSeconds()
    {
        long total;

        if (ExceedsMaxSeconds())
            return MaxDeltaInt;

        total = nday * 24*60*60
            + nhour * 60 * 60
            + nmin * 60
            + nsec;
        if (ntenth_msecs > 0) 
            total = total+1;

        if (total > MaxDeltaInt)
            total = MaxDeltaInt;

        return (int)total;
    } // toSeconds()
} // class DeltaTime
