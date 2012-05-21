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
package asl.logging;

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

public class LogDatabaseHandler
extends StreamHandler
{
    private static final Logger logger = Logger.getLogger("asl.logging.LogDatabaseHandler");

    private Formatter formatter;
    private LogDatabaseConfig config = null;

    private boolean connected = false;

 // constructor(s)
    public LogDatabaseHandler(LogDatabaseConfig config)
    {
        super();
        this.config = config;
        formatter = new Formatter(new StringBuilder(), Locale.US);
    }

 // iplement parent class abstract methods
    public void close()
    {
        ; // TODO: close database connection
    }

    public void flush()
    {
        ; // TODO: commit to the database
    }

    public void publish(LogRecord record)
    {
        if ((config != null) && (config.isReady())) {
            // open the connection if it is closed
            if (!connected) {
                ; // TODO: Connect to the database
            }

            // TODO: Inject records into the database

            // update the last timestamp
        }
    }
}
