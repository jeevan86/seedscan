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
import java.util.ArrayList;

public class ChannelArray
{
    private static final Logger logger = Logger.getLogger("asl.metadata.ChannelArray");

    private ArrayList<Channel> channels = null;

    public ChannelArray (String location, String channel1, String channel2, String channel3)
    {
        //Channel channel = null;
        channels = new ArrayList();
        //channel  = new Channel(location, channel1);
        //channels.add(channel);
        //channel  = new Channel(location, channel2);
        //channels.add(channel);
        //channel  = new Channel(location, channel3);
        //channels.add(channel);
        channels.add(new Channel(location,channel1) );
        channels.add(new Channel(location,channel2) );
        channels.add(new Channel(location,channel3) );
    }

    public ArrayList<Channel> getChannels() {
        return channels;
    }

}
