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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import asl.seedscan.config.Configuration;
import asl.seedscan.config.LogConfig;

public class LogFileHandler
extends StreamHandler
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.LogFileHandler");

    private Formatter formatter;
    private LogConfig config = null;
    private String logFileName = null;

    private Calendar lastRecordTime = null;
    private Calendar recordTime = null;

 // constructor(s)
    public LogFileHandler(LogConfig config)
    {
        super();
        this.config = config;
        lastRecordTime = (Calendar)(new GregorianCalendar());
        lastRecordTime.setTimeInMillis(0);
        recordTime = (Calendar)(new GregorianCalendar());
        recordTime.setTimeInMillis(0);
        formatter = new Formatter(new StringBuilder(), Locale.US);
    }

 // log file name
    public String getLogFileName()
    {
        return logFileName;
    }


 // iplement parent class abstract methods
    public void close()
    {
        super.close();
    }

    public void flush()
    {
        super.flush();
    }

    public void publish(LogRecord record)
    {
        try {
            if (config.isReady()) {
                long timestamp = record.getMillis();
                recordTime.setTimeInMillis(timestamp);
                // replace the log file if we have crossed a day boundary
                if ((lastRecordTime == null) || (!isSameDay(lastRecordTime, recordTime))) {
                    logFileName = formatter.format("%2$s%1$tY-%1$tm-%1$td%3$s", recordTime, config.getPrefix(), config.getSuffix()).toString();
                    File logFilePath = new File(config.getDirectory(), logFileName);
                    super.setOutputStream((OutputStream)(new BufferedOutputStream(new FileOutputStream(logFilePath))));
                }
                super.publish(record);
                // update the last timestamp
                lastRecordTime.setTimeInMillis(timestamp);
            }
        } catch (FileNotFoundException exception) {
            logger.warning("Could not create new log file");
        }
    }

 // private helper methods
    public static boolean isSameDay(Calendar dateA, Calendar dateB)
    {
        return (dateA.get(Calendar.YEAR)  != dateB.get(Calendar.YEAR))  ? false :
               (dateA.get(Calendar.MONTH) != dateB.get(Calendar.MONTH)) ? false :
               (dateA.get(Calendar.DAY_OF_MONTH) != dateB.get(Calendar.DAY_OF_MONTH)) ? false : true;
    }

}
