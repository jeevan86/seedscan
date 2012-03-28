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

package asl.seedscan.config;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * 
 */
public class Configuration
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.config.Configuration");

    private Hashtable<String,String> configuration = null;

    public Configuration()
    {
        configuration = new Hashtable<String,String>(32, (float)0.75);
    }

    public void put(String key, String value)
    {
        configuration.put(key, value);
    }

    public String get(String key)
    {
        return configuration.get(key);
    }

    public String get(String key, String defaultValue)
    {
        String value = defaultValue;
        if (configuration.containsKey(key)) {
            value = configuration.get(key);
        }
        return value;
    }

    public String remove(String key)
    {
        return configuration.remove(key);
    }

    public Enumeration<String> getKeys()
    {
        return configuration.keys();
    }
}
