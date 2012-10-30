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

package asl.metadata;

import java.util.logging.Logger;

public class Channel
{
    private static final Logger logger = Logger.getLogger("asl.metadata.Channel");

    private Station station  = null; // We're not currently using this ... Do we need it ?
    private String  location = null;
    private String  channel  = null;

    public Channel (String location, String channel)
    {
        //setStation(station);
        setLocation(location);
        setChannel(channel);
    }

    public void setLocation(String location) {
        if (location != null) {
            if (location.length() < 2) {
                throw new RuntimeException("location name MUST be at least 2-characters long");
            }
            this.location = location;
        }
        else {
            this.location = "--"; // If no location given, set location = "--" [Default]
        }
    }

    public void setChannel(String channel) {
        if (channel == null) {
            throw new RuntimeException("channel cannot be null");
        }
    //  I don't know of any channels that aren't exactly 3-characters long (??)
        if (channel.length() < 3) {
            throw new RuntimeException("channel name MUST be at least 3-characters long");
        }
        this.channel = channel;
    }

    public void setStation(Station station) {
        if (station == null) {
            throw new RuntimeException("station cannot be null");
        }
        this.station = station;
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
