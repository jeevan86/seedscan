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
package asl.metadata;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

public class BlocketteTimestamp
{
    private static final Logger logger = Logger.getLogger("asl.metadata.BlocketteTimestamp");

    public static Calendar parseTimestamp(String timestampString)
    throws TimestampFormatException
    {
        Calendar timestamp = new GregorianCalendar();
        int year = 1;
        int dayOfYear = 1;
        int hour = 0;
        int minute = 0;
        int second = 0;

        try {
// MTH: this will work for something like: End date: 2012,158,23:59
//                                but NOT: End date: 2012,158:23:59 - Probably should be more robust (??)
            String[] dateParts = timestampString.split(",");
            // There should be no more than three parts:
            //   year,day-of-year,time
            if (dateParts.length > 3) {
                throw new TimestampFormatException();
            }
            if (dateParts.length > 0) {
                year = Integer.parseInt(dateParts[0]);
            }
            // An empty date is invalid
            else {
                throw new TimestampFormatException();
            }
            if (dateParts.length > 1) {
                dayOfYear = Integer.parseInt(dateParts[1]);
            }

            if (dateParts.length > 2) {
                String[] timeParts = dateParts[2].split(":");
                // There should be no more than three parts:
                //   hour,minute,second
                if (timeParts.length > 3) {
                    throw new TimestampFormatException();
                }
                if (timeParts.length > 2) {
                    second = Integer.parseInt(timeParts[2]);
                }
                if (timeParts.length > 1) {
                    minute = Integer.parseInt(timeParts[1]);
                }
                if (timeParts.length > 0) {
                    hour = Integer.parseInt(timeParts[0]);
                }
            }
        } catch (NumberFormatException exception) {
            throw new TimestampFormatException();
        }

        timestamp.set(Calendar.YEAR, year);
        timestamp.set(Calendar.DAY_OF_YEAR, dayOfYear);
      //timestamp.set(Calendar.HOUR, hour);
      //MTH: I think we want to use 24-hour calendar here to get correct Epoch timestamp
        timestamp.set(Calendar.HOUR_OF_DAY, hour);
        timestamp.set(Calendar.MINUTE, minute);
        timestamp.set(Calendar.SECOND, second);
        timestamp.set(Calendar.MILLISECOND, 0);
        timestamp.set(Calendar.ZONE_OFFSET, 0);

        return timestamp;
    }
}

