/**
 * 
 */
package dcctime;

/**
 * Defines utility routines used by some of the other dcctime package classes
 * @author fshelly
 *
 */
public class DCCTime
{
    private static int CAL_CONS = 1720982; // Nov, 1, 1BC

    private static int DaysInMonth[] = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    /**
     * Converts year,month,day into julian day.
     * This algorithm is only accurate from 1-Mar-1900 to 28-Feb-2100.
     * @param int year
     * @param int month (1-12)
     * @param int day
     * @return int The julian day matching year,day
     */
    public static int YearMonthDay2Julian(int year, int month, int day)
    {
        int julian, yearprime, monprime, dayprime;

        yearprime = year;
        monprime = month + 1;
        dayprime = day;

        if (month == 1 || month == 2) {
            yearprime = year - 1;
            monprime = month + 13;
        }

        julian = dayprime + CAL_CONS;
        julian += (36525 * yearprime) / 100;
        julian += (306001 * monprime) / 10000;

        return(julian);
    } // YearMonthDay2Julian()

    public static boolean isLeapYear(int year)
    {
        if ((year %4 == 0 && year % 100 != 0) || year % 1000 == 0)
            return true;
        return false;
    } // isLeapYear()

    public static int YearMonthDay2Doy(int year, int month, int day)
    {
        int doy=0;

        if (isLeapYear(year)) DaysInMonth[2] = 29;
        for (int iMonth=1; iMonth < month && iMonth <= 12; iMonth++)
        {
            doy += DaysInMonth[iMonth];
        }
        doy += day;	

        return doy;
    } // YearMonthDay2Doy
} // class DCCTime
