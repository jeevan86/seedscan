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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogConfig
{
    private static final Logger logger = Logger.getLogger("asl.logging.LogConfig");

    private Level level = null;

 // constructor(s)
    public LogConfig()
    {
        ;
    }

 // ready
    public boolean isReady()
    {
        return level != null;
    }

 // levels
    public void setLevel(String level)
    throws IllegalArgumentException
    {
        setLevel(Level.parse(level));
    }

    public void setLevel(Level level)
    {
        level = level;
    }

    public Level getLevel()
    {
        return level;
    }
}

