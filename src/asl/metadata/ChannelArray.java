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

import org.apache.log4j.Logger;
import java.util.ArrayList;

public class ChannelArray
{
    private static final Logger logger = Logger.getLogger("asl.metadata.ChannelArray");

    private ArrayList<Channel> channels = null;

    public ChannelArray (String location, String channel1, String channel2, String channel3)
    {
        channels = new ArrayList<Channel>();
        channels.add(new Channel(location,channel1) );
        channels.add(new Channel(location,channel2) );
        channels.add(new Channel(location,channel3) );
    }
    public ChannelArray (String location, String channel1) // For testing
    {
        channels = new ArrayList<Channel>();
        channels.add(new Channel(location,channel1) );
    }
    public ChannelArray (Channel channelA, Channel channelB)
    {
        channels = new ArrayList<Channel>();
        channels.add(channelA);
        channels.add(channelB);
    }

    public ArrayList<Channel> getChannels() {
        return channels;
    }

}
