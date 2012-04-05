/*
 * Copyright 2012, United States Geological Survey or
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
package asl.seedscan;

public class ScanFrequency
{
    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;

    private boolean yearRepeating;
    private boolean monthRepeating;
    private boolean dayRepeating;
    private boolean hourRepeating;
    private boolean minuteRepeating;

 // constructor(s)
    public ScanFrequency()
    {
    }

 // year
    public void setYear(int year, boolean repeating)
    {
        this.year = year;
        yearRepeating = repeating;
    }

    public int getYear()
    {
        return year;
    }

    public boolean getYearRepeating()
    {
        return yearRepeating;
    }

 // month
    public void setMonth(int month, boolean repeating)
    {
        this.month = month;
        monthRepeating = repeating;
    }

    public int getMonth()
    {
        return month;
    }

    public boolean getMonthRepeating()
    {
        return monthRepeating;
    }

 // day
    public void setDay(int day, boolean repeating)
    {
        this.day = day;
        dayRepeating = repeating;
    }

    public int getDay()
    {
        return day;
    }

    public boolean getDayRepeating()
    {
        return dayRepeating;
    }

 // hour
    public void setHour(int hour, boolean repeating)
    {
        this.hour = hour;
        hourRepeating = repeating;
    }

    public int getHour()
    {
        return hour;
    }

    public boolean getHourRepeating()
    {
        return hourRepeating;
    }

 // minute
    public void setMinute(int minute, boolean repeating)
    {
        this.minute = minute;
        minuteRepeating = repeating;
    }

    public int getMinute()
    {
        return minute;
    }

    public boolean getMinuteRepeating()
    {
        return minuteRepeating;
    }


}
