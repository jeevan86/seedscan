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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import asl.seedscan.config.Configuration;

public class LogFileHandler
extends StreamHandler
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.LogFileHandler");

    private Formatter formatter;
    private Level level = null;
    private File directory = null;
    private String prefix = null;
    private String suffix = null;
    private String logFileName = null;

    private Calendar lastRecordTime = null;
    private Calendar recordTime = null;

 // constructor(s)
    public LogFileHandler()
    {
        super();
        lastRecordTime = (Calendar)(new GregorianCalendar());
        lastRecordTime.setTimeInMillis(0);
        recordTime = (Calendar)(new GregorianCalendar());
        recordTime.setTimeInMillis(0);
        formatter = new Formatter(new StringBuilder(), Locale.US);
    }

    private boolean isReady()
    {
        return (level     == null) ? false :
               (directory == null) ? false :
               (prefix    == null) ? false :
               (suffix    == null) ? false : true;
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
            if (isReady()) {
                long timestamp = record.getMillis();
                recordTime.setTimeInMillis(timestamp);
                // replace the log file if we have crossed a day boundary
                if ((lastRecordTime == null) || (!isSameDay(lastRecordTime, recordTime))) {
                    logFileName = formatter.format("%2$s%1$tY-%1$tm-%1$td%3$s", recordTime, prefix, suffix).toString();
                    File logFilePath = new File(directory, logFileName);
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

 // level
    public void setLevel(Level level)
    {
        this.level = level;
    }

    public void setLevel(String level)
    throws IllegalArgumentException
    {
        this.level = Level.parse(level);
    }

    public Level getLevel()
    {
        return level;
    }

 // directory
    public void setDirectory(File directory)
    {
        this.directory = directory;
    }

    public void setDirectory(String directory)
    throws FileNotFoundException,
           IOException,
           NullPointerException,
           SecurityException
    {
        File path = new File(directory);
        if (!path.exists()) {
            throw new FileNotFoundException("Path '" +directory+ "' does not exist");
        }
        if (!path.isDirectory()) {
            throw new IOException("Path '" +directory+ "' is not a directory");
        }
        if (!path.canWrite()) {
            throw new SecurityException("Not permitted to write to directory '" +directory+ "'");
        }
        this.directory = path;
    }

    public File getDirectory()
    {
        return directory;
    }

 // prefix
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public String getPrefix()
    {
        return prefix;
    }

 // suffix
    public void setSuffix(String suffix)
    {
        this.suffix = suffix;
    }

    public String getSuffix()
    {
        return suffix;
    }

 // log file name
    public String getLogFileName()
    {
        return logFileName;
    }

 // convenience methods
    public void setFromConfiguration(Configuration configuration)
    throws BadLogConfigurationException
    {
        try {
            setDirectory(configuration.get("log-directory"));
            setPrefix(configuration.get("log-prefix"));
            setSuffix(configuration.get("log-suffix"));
            setLevel(configuration.get("log-level"));
        } catch (FileNotFoundException exception) {
            logger.warning("Could not locate or create log file. Details: " + exception);
            throw new BadLogConfigurationException(exception.toString());
        } catch (IllegalArgumentException exception) {
            logger.warning("Invalid log level. Details: " + exception);
            throw new BadLogConfigurationException(exception.toString());
        } catch (IOException exception) {
            logger.warning("I/O error occurred. Details: " + exception);
            throw new BadLogConfigurationException(exception.toString());
        } catch (NullPointerException exception) {
            logger.warning("Null directory. Details: " + exception);
            throw new BadLogConfigurationException(exception.toString());
        } catch (SecurityException exception) {
            logger.warning("Insufficient permissions. Details: " + exception);
            throw new BadLogConfigurationException(exception.toString());
        }
    }

}
