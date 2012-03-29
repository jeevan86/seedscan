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

import java.util.logging.Logger;

public class ChannelKey
extends Key
{
    private static final Logger logger = Logger.getLogger("asl.metadata.ChannelKey");

    public static final int CHANNEL_EPOCH_BLOCKETTE_NUMBER = 52;

    private String location = null;
    private String name = null;

    // constructor(s)
    public ChannelKey(Blockette blockette)
    throws WrongBlocketteException
    {
        if (blockette.getNumber() != CHANNEL_EPOCH_BLOCKETTE_NUMBER) {
            throw new WrongBlocketteException();
        }
        location = blockette.getFieldValue(3,0);
        name = blockette.getFieldValue(4,0);
    }

    // identifiers
    public String getLocation()
    {
        return new String(location);
    }

    public String getName()
    {
        return new String(name);
    }

    // overrides abstract method from Key class
    public String toString()
    {
        return new String(location+ "-" +name);
    }

}

