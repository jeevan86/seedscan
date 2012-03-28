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
import java.util.Hashtable;
import java.util.logging.Logger;

public class ChannelData
{
    private static final Logger logger = Logger.getLogger("asl.metadata.ChannelData");

    public static final int CHANNEL_EPOCH_INFO_BLOCKETTE_NUMBER = 52;
    public static final int CHANNEL_COMMENT_BLOCKETTE_NUMBER = 59;

    private Hashtable<Calendar, Blockette> comments;
    private Hashtable<Calendar, EpochData> epochs;

    // Constructor(s)
    public ChannelData()
    {
        comments = new Hashtable<Calendar, Blockette>();
        epochs = new Hashtable<Calendar, EpochData>();
    }

    // Comments
    public Calendar addComment(Blockette blockette)
    throws MissingBlocketteDataException,
           TimestampFormatException,
           WrongBlocketteException
    {
        if (blockette.getNumber() != CHANNEL_COMMENT_BLOCKETTE_NUMBER) {
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

    // Epochs
    public Calendar addEpoch(Blockette blockette)
    throws MissingBlocketteDataException,
           TimestampFormatException,
           WrongBlocketteException
    {
        if (blockette.getNumber() != CHANNEL_EPOCH_INFO_BLOCKETTE_NUMBER) {
            throw new WrongBlocketteException();
        }
        String timestampString = blockette.getFieldValue(3, 0);
        if (timestampString == null) {
            throw new MissingBlocketteDataException();
        }
        Calendar timestamp = BlocketteTimestamp.parseTimestamp(timestampString);
        EpochData data = new EpochData(blockette);
        epochs.put(timestamp, data);
        return timestamp;
    }

    public boolean hasEpoch(Calendar timestamp)
    {
        return epochs.containsKey(timestamp);
    }

    public EpochData getEpoch(Calendar timestamp)
    {
        return epochs.get(timestamp);
    }
}

