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

/* Station and Channel are the only station/channel classes that should be seen by 
 *   clients of asl.metadata.
 * They are the fundamental building blocks of StationKey, ChannelKey and ChannelArray
 */

public class Station
    implements java.io.Serializable
{
    private static final Logger logger = LoggerFactory.getLogger(asl.metadata.Station.class);
    private static final long serialVersionUID = 1L;

    private String network = null;
    private String station = null;

    public Station(String network, String station)
    {
    	try {
    		setNetwork(network);
    		setStation(station);
    	} catch (StationException e) {
    		logger.error("StationException:", e);
    	}
    }

// MTH: in the spirit of "fail early", I'm going to make this even more restrictive:
//      Network must be exactly 2 characters long
//      Station must be either 3 or 4 characters long

    public void setNetwork(String network) 
    throws StationException
    {
        if (network != null) {
            if (!(network.length()==2) && !(network.length()==1)) {
                throw new StationException("network name MUST be 1 or 2-character string!");
            }
            else {
                this.network = network;
            }
        }
        else {
            throw new StationException("network name CANNOT be null!");
        }
    }

    public void setStation(String station) 
    throws StationException
    {
        if (station == null) {
            throw new StationException("station cannot be null");
        }
        if (station.length() < 3 || station.length() > 5)  {
            throw new StationException("Station name MUST be between 3 and 5 characters long");
        }
        this.station = station;
    }


    public String getNetwork() {
        return network;
    }

    public String getStation() {
        return station;
    }

    @Override public String toString() {
      return this.getNetwork() + "_" + this.getStation();
    }

}

