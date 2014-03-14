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
package asl.seedscan.metrics;

import asl.metadata.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrossPowerKey
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.CrossPowerKey.class);

    private String key;

    public CrossPowerKey(Channel a, Channel b)
    {
        String aKey = a.toString();
        String bKey = b.toString();
        if (aKey.compareTo(bKey) < 0) {
            key = aKey + bKey;
        }
        else {
            key = bKey + aKey;
        }
    }

    public String getKey()
    {
        return key;
    }

    @Override public String toString()
    {
        return key;
    }

    @Override public int hashCode()
    {
        return key.hashCode();
    }

    @Override public boolean equals(Object obj)
    {
        CrossPowerKey other = (CrossPowerKey)obj;
        return toString().equals(other.toString()) ;
        //return key.equals((String)obj);
    }
}
