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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlocketteTimestamp
{
    private static final Logger logger = LoggerFactory.getLogger(asl.metadata.BlocketteTimestamp.class);

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
// Here's another miss:
//  ** It's corrected now                      date: 1995,284,00:00:00.0000

            String[] dateParts = timestampString.split(",");
//System.out.format("== parseTimestamp(%s) Num dateParts=%d\n", timestampString, dateParts.length );
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

                // There should be no more than three parts:
                if (timeParts.length > 3) {
                	throw new TimestampFormatException();
                }
                if (timeParts.length > 0) {
                    hour = Integer.parseInt(timeParts[0]);
                }
                if (timeParts.length > 1) {
                    minute = Integer.parseInt(timeParts[1]);
                }
                if (timeParts.length > 2) {
                 // Need to handle both "00" and "00.000":
                    String[] secondParts = timeParts[2].split("\\.");
                    if (secondParts.length == 2) {                   // "00.000"
                        second = Integer.parseInt(secondParts[0]);
                    }
                    else if (secondParts.length == 1) {              // "00"
                        second = Integer.parseInt(secondParts[0]);
                    }
                    else  { // Something has gone wrong !!!
                    	//throw new RuntimeException("Dataless.parseTimestamp(): Error parsing Timestamp=" + timestamp);
                    	throw new TimestampFormatException("parseTimestamp: Error parsing Timestamp=" + timestamp);
                    }
                }
//System.out.format("== parseTimestamp(%s) hour=%d minute=%d second=%d\n", timestampString, hour, minute, second );
            }
        } catch (NumberFormatException exception) {
        	throw new TimestampFormatException(exception.getMessage());
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

