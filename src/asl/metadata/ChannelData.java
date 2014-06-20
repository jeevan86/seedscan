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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelData
{
    private static final Logger logger = LoggerFactory.getLogger(asl.metadata.ChannelData.class);

    public static final int CHANNEL_EPOCH_INFO_BLOCKETTE_NUMBER = 52;
    public static final int CHANNEL_COMMENT_BLOCKETTE_NUMBER    = 59;

    private Hashtable<Calendar, Blockette> comments;
    private Hashtable<Calendar, EpochData> epochs;
    private String location = null;
    private String name = null;

    // constructor(s)
    //public ChannelData(String location, String name)
    public ChannelData(ChannelKey channelKey)
    {
        this.location = channelKey.getLocation();
        this.name     = channelKey.getName();
        comments = new Hashtable<Calendar, Blockette>();
        epochs = new Hashtable<Calendar, EpochData>();
    }

    // identifiers
    public String getLocation()
    {
        return location;
    }

    public String getName()
    {
        return name;
    }

    // comments
    public Calendar addComment(Blockette blockette)
    throws MissingBlocketteDataException,
           TimestampFormatException,
           WrongBlocketteException
    {
        if (blockette.getNumber() != CHANNEL_COMMENT_BLOCKETTE_NUMBER) {
        	throw new WrongBlocketteException();
        }
        //Epoch epochNew = new Epoch(blockette);
        String timestampString = blockette.getFieldValue(3, 0);
        if (timestampString == null) {
        	throw new MissingBlocketteDataException();
        }
        try {
        	Calendar timestamp = BlocketteTimestamp.parseTimestamp(timestampString);
        	comments.put(timestamp, blockette);
        	return timestamp;
        } catch(TimestampFormatException e) {
        	throw e;
        }
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
    throws MissingBlocketteDataException,
           TimestampFormatException,
           WrongBlocketteException
    {
        if (blockette.getNumber() != CHANNEL_EPOCH_INFO_BLOCKETTE_NUMBER) {
        	throw new WrongBlocketteException();
        }
        //Epoch epochNew = new Epoch(blockette);
        String timestampString = blockette.getFieldValue(22, 0);

        if (timestampString == null) {
        	throw new MissingBlocketteDataException();
        }
        try {
        	Calendar timestamp = BlocketteTimestamp.parseTimestamp(timestampString);
        	EpochData data = new EpochData(blockette);
        	epochs.put(timestamp, data);
        	return timestamp;
        } catch (TimestampFormatException e) {
        	throw e;
        }
    }

    public boolean hasEpoch(Calendar timestamp)
    {
        return epochs.containsKey(timestamp);
    }

    public EpochData getEpoch(Calendar timestamp)
    {
        return epochs.get(timestamp);
    }

// containsEpoch - search through epochs of current channeldata
//                 return true if epochTime is contained.
/**
Epoch index
-----------                                           ONLY THIS EPOCH!
    0      newest startTimestamp - newest endTimestamp (may be "null")
    1             ...            -         ...
    2             ...            -         ...
    .             ...            -         ...
   n-1     oldest startTimestamp - oldest endTimestamp
**/
    //public boolean containsEpoch(Calendar epochTime)
    public Calendar containsEpoch(Calendar epochTime)
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
      EpochData epoch = null;

// epochs keys(=timestamps) are now sorted with the newest first
// most likely the first (=newest) epoch will be the one we want
// So check the first epoch for the epochTime and if it's not
//   found (and nEpochs > 1), check the older epochs

      timestamp = epochtimes.get(0);
      epoch = epochs.get(timestamp);
      startTimeStamp = epoch.getStartTime();
      endTimeStamp   = epoch.getEndTime();
      if (endTimeStamp == null) {            // The first Epoch is open
         if (epochTime.getTimeInMillis() >= startTimeStamp.getTimeInMillis() ) {
           containsEpochTime = true;
         }
      }                                      // The first Epoch is closed
      else if (epochTime.getTimeInMillis() >= startTimeStamp.getTimeInMillis() &&
               epochTime.getTimeInMillis() <= endTimeStamp.getTimeInMillis() ) {
           containsEpochTime = true;
      }

      if (!containsEpochTime && nEpochs > 1) { // Search the older epochs if necessary
        for (int i=1; i<nEpochs; i++){
           timestamp = epochtimes.get(i); 
           epoch = epochs.get(timestamp);
           startTimeStamp = epoch.getStartTime();
           endTimeStamp   = epoch.getEndTime();
           if (endTimeStamp == null) {            // This Epoch is open - we don't allow that here!
              System.out.println("Error: Older Epoch has Open End Time (=null)");
              //if (epochTime.getTimeInMillis() >= startTimeStamp.getTimeInMillis() ) {
                //containsEpochTime = true;
              //}
              break;
           } 
           else if (epochTime.getTimeInMillis() >= startTimeStamp.getTimeInMillis() &&
                    epochTime.getTimeInMillis() <= endTimeStamp.getTimeInMillis() ) {
               containsEpochTime = true;
               break;
           }
        }
      }

  // These should be the latest ones we checked, so they are correct IF the epochTime was found
      String epochDateString  = EpochData.epochToDateString(epochTime);
      String startDateString  = EpochData.epochToDateString(startTimeStamp);
      String endDateString    = EpochData.epochToDateString(endTimeStamp);
      if (containsEpochTime){
        //System.out.format("----ChannelData %s-%s Epoch: [%s - %s] contains EpochTime=%s\n",getLocation(), getName(), startDateString,endDateString,epochDateString);
        return startTimeStamp;
      }
      else {
        //System.out.format("----ChannelData EpochTime=%s was NOT FOUND!!\n",epochDateString);
        return null;
      }

    }

    public void printEpochs()
    {
      //TreeSet<Calendar> epochtimes = new TreeSet<Calendar>();
      //epochtimes.addAll(epochs.keySet());

      ArrayList<Calendar> epochtimes = new ArrayList<Calendar>();
      epochtimes.addAll(epochs.keySet());
      Collections.sort(epochtimes);

      for (Calendar timestamp : epochtimes ){
// timestamp is the Hashtable key and "should" be the same as 
// EpochData.getStartTime()
// String startDate = EpochData.epochToDateString(timestamp);

        EpochData epoch = epochs.get(timestamp);
        Calendar startTimeStamp = epoch.getStartTime();
        Calendar endTimeStamp   = epoch.getEndTime();
        String startDateString  = EpochData.epochToDateString(startTimeStamp);
        String endDateString    = EpochData.epochToDateString(endTimeStamp);

        System.out.format("----ChannelData Epoch: %s - %s\n", startDateString, endDateString);
        //blockette.print();
      }
    }

}

