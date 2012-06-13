/**
 * 
 */
package dcctime;

/**
 * @author fshelly
 *
 */
public class StdTime
{
    short year;
    short day;
    byte hour;
    byte minute;
    byte second;
    short tenth_msec;

    boolean validDate=true;

    public StdTime()
    {
        year = 0;
        day = 0;
        hour = 0;
        minute = 0;
        second = 0;
        tenth_msec = 0;
    } // constructor StdTime()

    public StdTime(short year, short day, byte hour, byte minute, 
            byte second, short tenth_msec)
    {

        if (year < 1 || year > 3000 || day < 1 || day > 366 
                || hour < 0 || hour > 24 || minute < 0 || minute > 60 
                || second < 0 || second > 60 || tenth_msec < 0 || tenth_msec > 10000)
        {
            this.year = 0;
            this.day = 0;
            this.hour = 0;
            this.minute = 0;
            this.second = 0;
            this.tenth_msec = 0;
            validDate = false;			
        }
        this.year = year;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.tenth_msec = tenth_msec;

        // Normalize date
        Normalize();
        validDate = true;
    } // full constructor

    public StdTime(short year, byte month, byte dom, byte hour, byte minute, 
            byte second, short tenth_msec)
    {
        if (year < 1 || year > 3000 || month < 1 || month > 12 || dom < 1 || dom > 366 
                || hour < 0 || hour > 24 || minute < 0 || minute > 60 
                || second < 0 || second > 60 || tenth_msec < 0 || tenth_msec > 10000)
        {
            this.year = 0;
            this.day = 0;
            this.hour = 0;
            this.minute = 0;
            this.second = 0;
            this.tenth_msec = 0;
            validDate = false;			
        }
        this.year = year;
        this.day = (short) DCCTime.YearMonthDay2Doy(year, month, dom);
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.tenth_msec = tenth_msec;

        // Normalize date
        Normalize();
        validDate = true;
    } // full constructor

    /**
     * Converts a date and time string into StdTime class
     * @param String Date Format yyyy/mm/dd
     * @param String Time Format hh:mm:ss
     */
    public StdTime(String Date, String Time)
    {
        int month, dom;
        year = 0;
        day = 0;
        hour = 0;
        minute = 0;
        second = 0;
        tenth_msec = 0;
        validDate = false;

        // Verify basic format
        if (Date.length() != 10)
            return;
        if (Time.length() != 8)
            return;

        if (Date.charAt(4) != '/')
            return;
        if (Date.charAt(7) != '/')
            return;

        if (Time.charAt(2) != ':')
            return;
        if (Time.charAt(5) != ':')
            return;

        if (!Character.isDigit(Date.charAt(0))) return;
        if (!Character.isDigit(Date.charAt(1))) return;
        if (!Character.isDigit(Date.charAt(2))) return;
        if (!Character.isDigit(Date.charAt(3))) return;
        if (!Character.isDigit(Date.charAt(5))) return;
        if (!Character.isDigit(Date.charAt(6))) return;
        if (!Character.isDigit(Date.charAt(8))) return;
        if (!Character.isDigit(Date.charAt(9))) return;

        if (!Character.isDigit(Time.charAt(0))) return;
        if (!Character.isDigit(Time.charAt(1))) return;
        if (!Character.isDigit(Time.charAt(3))) return;
        if (!Character.isDigit(Time.charAt(4))) return;
        if (!Character.isDigit(Time.charAt(6))) return;
        if (!Character.isDigit(Time.charAt(7))) return;

        year = Short.valueOf(Date.substring(0, 4)).shortValue();
        month = Integer.valueOf(Date.substring(5,7)).intValue();
        dom = Integer.valueOf(Date.substring(8,10)).intValue();
        day = (short) DCCTime.YearMonthDay2Doy(year, month, dom);

        hour = Byte.valueOf(Time.substring(0, 2)).byteValue();
        minute = Byte.valueOf(Time.substring(3,5)).byteValue();
        second = Byte.valueOf(Time.substring(6,8)).byteValue();

        // Normalize date
        Normalize();
        validDate = true;
    } // constructor

    public void Normalize()
    {
        int iDay, iHour, iMin, iSec;

        iSec = second + tenth_msec / 10000;
        tenth_msec = (short) (tenth_msec % 1000);
        second = (byte) (iSec % 60);
        iMin = minute + iSec / 60;
        minute = (byte) (iMin % 60);
        iHour = hour + iMin / 60;
        hour = (byte) (iHour % 24);
        iDay = day + iHour / 24;

        while (iDay > 366 || (!DCCTime.isLeapYear(year) && iDay > 365))
        {
            if (DCCTime.isLeapYear(year))
                iDay -= 366;
            else
                iDay -= 365;
            year++;
        } // while day wraps past end of the year
        day = (short) iDay;
    } // Normalize
} // class StdTime
