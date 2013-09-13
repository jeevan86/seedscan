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

import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Collections;
import java.text.SimpleDateFormat;

public class StationData
{
    private static final Logger logger = Logger.getLogger("asl.metadata.StationData");

    public static final int STATION_EPOCH_BLOCKETTE_NUMBER   = 50;
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
        this.name = name;
        this.network = network;
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
        //Epoch epochNew = new Epoch(blockette);
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
        //Epoch epochNew = new Epoch(blockette);
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


/**
Epoch index
-----------                                           ONLY THIS EPOCH!
    0      newest startTimestamp - newest endTimestamp (may be "null")
    1             ...            -         ...
    2             ...            -         ...
    .             ...            -         ...
   n-1     oldest startTimestamp - oldest endTimestamp
**/
 // public boolean containsEpoch(Calendar epochTime)

 // Return the correct Blockette 050 for the requested epochTime
 // Return null if epochTime not contained
    public Blockette getBlockette(Calendar epochTime)
    {
      boolean containsEpochTime = false;

      ArrayList<Calendar> epochtimes = new ArrayList<Calendar>();
      epochtimes.addAll(epochs.keySet());
      Collections.sort(epochtimes);
      Collections.reverse(epochtimes);
      int nEpochs = epochtimes.size();

      Calendar startTimeStamp = null;
      Calendar endTimeStamp = null;
      Calendar timestamp = null;

// Loop through Blockettes (B050) and pick out epoch end dates
      for (int i=0; i<nEpochs; i++){
        endTimeStamp  = null;
        startTimeStamp= epochtimes.get(i);
        Blockette blockette    = epochs.get(startTimeStamp);
        String timestampString = blockette.getFieldValue(14, 0);
        if (!timestampString.equals("(null)") ) {
          try {
            endTimeStamp = BlocketteTimestamp.parseTimestamp(timestampString);
          }
          catch (TimestampFormatException e) {
            System.out.println("StationData.printEpochs() Error converting timestampString=" + timestampString);
          }
        }
        if (endTimeStamp == null) {            // This Epoch is open
          if (epochTime.getTimeInMillis() >= startTimeStamp.getTimeInMillis() ) {
            containsEpochTime = true;
            return blockette;
            //break;
          }
        }                                      // This Epoch is closed
        else if (epochTime.getTimeInMillis() >= startTimeStamp.getTimeInMillis() &&
                 epochTime.getTimeInMillis() <= endTimeStamp.getTimeInMillis() ) {
            containsEpochTime = true;
            return blockette;
            //break;
        }
      } // for
      return null; // If we made it to here than we are returning blockette==null

  // These should be the latest ones we checked, so they are correct IF the epochTime was found
  /**
      String epochDateString  = EpochData.epochToDateString(epochTime);
      String startDateString  = EpochData.epochToDateString(startTimeStamp);
      String endDateString    = EpochData.epochToDateString(endTimeStamp);
      if (containsEpochTime){
        System.out.format("----StationData %s-%s Epoch: [%s - %s] contains EpochTime=%s\n",network,name,startDateString,endDateString,epochDateString);
      }
      else {
        System.out.format("----StationData EpochTime=%s was NOT FOUND!!\n",epochDateString);
      }

      return containsEpochTime;
   **/

    }

// Loop through all station (=Blockette 050) epochs and print summary

    public void printEpochs()
    {
      TreeSet<Calendar> epochtimes = new TreeSet<Calendar>();
      epochtimes.addAll(epochs.keySet());

      for (Calendar timestamp : epochtimes ){
        String startDate = EpochData.epochToDateString(timestamp);

        Blockette blockette = epochs.get(timestamp);
        String timestampString = blockette.getFieldValue(14, 0);
        String endDate = timestampString;
        if (!timestampString.equals("(null)") ) {
          try {
            Calendar endtimestamp = BlocketteTimestamp.parseTimestamp(timestampString);
            endDate = EpochData.epochToDateString(endtimestamp);
          }
          catch (TimestampFormatException e) {
            System.out.println("StationData.printEpochs() Error converting timestampString=" + timestampString);
          }
        }
        System.out.format("==StationData Epoch: %s - %s\n",startDate,endDate);
        //blockette.print();
      }
    }

// Sort channels and print out
    public void printChannels()
    {
      TreeSet<ChannelKey> keys = new TreeSet<ChannelKey>();
      keys.addAll(channels.keySet());

      for (ChannelKey key : keys){
        System.out.println("==Channel:"+key );
        ChannelData channel = channels.get(key);
        channel.printEpochs();
      }
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
    public Hashtable<ChannelKey, ChannelData> getChannels()
    {
        return channels;
    }



}
