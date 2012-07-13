/*
 * Copyright 2011, United States Geological Survey or
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

import java.util.logging.Logger;
import asl.metadata.*;

public class Channel
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.Channel");

    private Station station  = null;
    private String  location = "";
    private String  channel  = "";

// Not sure this constructor makes sense / is even used
    public Channel ()
    {
        setStation(station);
        setLocation(location);
        setChannel(channel);
    }

    public Channel (String location, String channel)
    {
        //setStation(station);
        setLocation(location);
        setChannel(channel);
    }


    public void setStation(Station station) {
        if (station == null) {
            throw new RuntimeException("station cannot be null");
        }
        this.station = station;
    }

    public void setLocation(String location) {
        if (location != null) {
            if (location.length() > 2) {
                throw new RuntimeException("location name is too long");
            }
            this.location = location;
        }
    }

    public void setChannel(String channel) {
        if (channel == null) {
            throw new RuntimeException("channel cannot be null");
        }
        if (channel.length() < 1) {
            throw new RuntimeException("channel name is too short");
        }
        if (channel.length() > 3) {
            throw new RuntimeException("channel name is too long");
        }
        this.channel = channel;
    }

    @Override public String toString() {
      return getLocation() + "-" + getChannel();
    }

    public Station getStation() {
        return station;
    }

    public String getLocation() {
        return location;
    }

    public String getChannel() {
        return channel;
    }
}
