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

public class LogLevels
{
    private static final Logger logger = Logger.getLogger("asl.logging.LogLevels");

    private Hashtable<String, Level> levels = null;

 // constructor(s)
    public LogLevels()
    {
        levels = new Hashtable<String, Level>();
    }

 // levels
    public void setLevel(String name, String level)
    throws IllegalArgumentException
    {
        setLevel(name, Level.parse(level));
    }

    public void setLevel(String name, Level level)
    {
        logger.config("Level: '"+name+"' -> '"+level.toString()+"'");
        levels.put(name, level);
        Logger.getLogger(name).setLevel(level);
    }

    public Level getLevel(String name)
    {
        return levels.get(name);
    }

    public Enumeration<String> getLevelNames()
    {
        return levels.keys();
    }
}

