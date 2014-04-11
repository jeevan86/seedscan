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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkKey extends Key
{
    private static final Logger logger = LoggerFactory.getLogger(asl.metadata.NetworkKey.class);

    public static final int DATALESS_VOLUME_BLOCKETTE_NUMBER = 10;

    private String network = null;

    // constructor(s)
    public NetworkKey(Blockette blockette)
    throws WrongBlocketteException
    {
        if (blockette.getNumber() != DATALESS_VOLUME_BLOCKETTE_NUMBER) {
            //throw new WrongBlocketteException();
        	WrongBlocketteException e = new WrongBlocketteException();
        	logger.error("NetworKey WrongBlocketteException:", e);
        	return;
        }
        network = blockette.getFieldValue(9,0);
    }

    public NetworkKey(String network) {
        this.network = network;
    }


    public String getNetwork()
    {
        return network;
    }

    public String toString()
    {
        return network;
    }
}

