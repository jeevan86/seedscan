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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.logging.Logger;

public class StationData
{
    private static final Logger logger = Logger.getLogger("asl.metadata.StationData");

    public static final int STATION_EPOCH_BLOCKETTE_NUMBER = 50;
    public static final int STATION_COMMENT_BLOCKETTE_NUMBER = 51;

    private Hashtable<Calendar, Blockette> comments;
    private Hashtable<Calendar, Blockette> epochs;
    private Hashtable<ChannelKey, ChannelData> channels;
    private String network = null;
    private String name = null;

    // Constructor(s)
    public StationData(String network, String name)
    {
        comments = new Hashtable<Calendar, Blockette>();
        epochs = new Hashtable<Calendar, Blockette>();
        channels = new Hashtable<ChannelKey, ChannelData>();
    }

    // identifiers
    public String getNetwork()
    {
        return network;
    }

    public String getName()
    {
        return name;
    }

    // comments
    public Calendar addComment(Blockette blockette)
    throws TimestampFormatException,
           WrongBlocketteException,
           MissingBlocketteDataException
    {
        if (blockette.getNumber() != STATION_COMMENT_BLOCKETTE_NUMBER) {
            throw new WrongBlocketteException();
        }
        String timestampString = blockette.getFieldValue(3, 0);
        if (timestampString == null) {
            throw new MissingBlocketteDataException();
        }
        Calendar timestamp = BlocketteTimestamp.parseTimestamp(timestampString);
        comments.put(timestamp, blockette);
        return timestamp;
    }

    public boolean hasComment(Calendar timestamp)
    {
        return comments.containsKey(timestamp);
    }

    public Blockette getComment(Calendar timestamp)
    {
        return comments.get(timestamp);
    }

    // epochs
    public Calendar addEpoch(Blockette blockette)
    throws TimestampFormatException,
           WrongBlocketteException,
           MissingBlocketteDataException
    {
        if (blockette.getNumber() != STATION_EPOCH_BLOCKETTE_NUMBER) {
            throw new WrongBlocketteException();
        }
        String timestampString = blockette.getFieldValue(13, 0);
        if (timestampString == null) {
            throw new MissingBlocketteDataException();
        }
        Calendar timestamp = BlocketteTimestamp.parseTimestamp(timestampString);
        epochs.put(timestamp, blockette);
        return timestamp;
    }

    public boolean hasEpoch(Calendar timestamp)
    {
        return epochs.containsKey(timestamp);
    }

    public Blockette getEpoch(Calendar timestamp)
    {
        return epochs.get(timestamp);
    }


    // channels
    public void addChannel(ChannelKey key, ChannelData data)
    {
        channels.put(key, data);
    }

    public boolean hasChannel(ChannelKey key)
    {
        return channels.containsKey(key);
    }

    public ChannelData getChannel(ChannelKey key)
    {
        return channels.get(key);
    }
}

