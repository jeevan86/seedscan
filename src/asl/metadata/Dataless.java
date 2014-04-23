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

/* 
 * Follwing is a depiction of the structure generated as a result of 
 * parsing the output of `evalresp -s`
 * 
 *
 * SeedVolume
 *  |
 *   - volumeInfo (Blockette)
 *  |
 *   - stationLocators (ArrayList<Blockette>)
 *  |
 *   - stations (Hashtable<String, StationData>)
 *      |
 *       - 'NN_SSSS' (String)
 *          :
 *         data (StationData)
 *          |
 *           - comments (Hashtable<Calendar, Blockette>)
 *              |
 *               - timestamp (Calendar)
 *                  :
 *                 comment (Blockette)
 *          | 
 *           - epochs (Hashtable<Calendar, Blockette>)
 *              |
 *               - timestamp (Calendar)
 *                  :
 *                 epoch (Blockette)
 *          | 
 *           - channels (Hashtable<String, ChannelData>)
 *              |
 *               - 'LL-CCC' (String)
 *                  :
 *                 data (ChannelData)
 *                      |
 *                       - comments (Hashtable<Calendar, Blockette>)
 *                          |
 *                           - timestamp (Calendar)
 *                              :
 *                             comment (Blockette)
 *                      |
 *                       - epochs (Hashtable<Calendar, EpochData>)
 *                          |
 *                           - timestamp (Calendar)
 *                              :
 *                             epoch (EpochData)
 *                              |
 *                               - format (Blockette)
 *                              |
 *                               - info (Blockette)
 *                              |
 *                               - misc (ArrayList<Blockette>)
 *                              |
 *                               - stages (Hashtable<Integer, StageData>)
 *                                  |
 *                                   - stageIndex (Integer)
 *                                      :
 *                                     data (StageData)
 *
 *
 */

import asl.worker.CancelledException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dataless
{
    private static final Logger logger = LoggerFactory.getLogger(asl.metadata.Dataless.class);

    private SeedVolume volume;
    private Collection<String> rawDataless;
    private ArrayList<Blockette> blockettes;
    private boolean complete;

    private double percent;
    private double lastPercent;
    private double count;
    private double total;
    private double skipped;
    private double comments;
    private String stage;
    private String line;
    
    private boolean cancelRequested = false;

    public Dataless(Collection<String> rawDataless)
    {
        this.rawDataless = rawDataless;
        complete = false;
    }
// This should be the one we use until station/network masks are implemented
    public void processVolume() 
    throws CancelledException, 
    	   DatalessParseException
    {
    	processVolume("now", "is the time");
    }

    public void processVolume(Station station)
    throws CancelledException, 
    	   DatalessParseException
    {
    	processVolume(station.getNetwork(), station.getStation());
    }

    public void processVolume(String networkMask, String stationMask)
    throws CancelledException,
           DatalessParseException
    {
        boolean failed = true;
        try {
            parse();
            assemble();
            complete = true;
            failed = false;
        } catch (BlocketteFieldIdentifierFormatException exception) {
            logger.warn("Malformed blocketted field identifier.", exception);
        } catch (BlocketteOutOfOrderException exception) {
            logger.warn("Out of order blockette.", exception);
        } catch (DuplicateBlocketteException exception) {
            logger.warn("Unexpected duplicate blockette.", exception);
        } catch (MissingBlocketteDataException exception) {
            logger.warn("Blockette is missing requird data.", exception);
        } catch (TimestampFormatException exception) {
            logger.warn("Invalid timestamp format.", exception);
        } catch (WrongBlocketteException exception) {
            logger.warn("Wrong blockettte.", exception);
        } catch (CancelledException exception) {
        	logger.warn("Cancelled exception.", exception);
            failed = false;
        }

        if (failed) {
        	throw new DatalessParseException();
        }
    }

    public void cancel()
    {
        cancelRequested = true;
    }

    public boolean cancelled()
    {
        return cancelRequested;
    }

    private void checkCancel()
    throws CancelledException
    {
        if (cancelled()) {
        	throw new CancelledException();
        }
    }

    private void parse()
    throws BlocketteFieldIdentifierFormatException,
           CancelledException
    {
        if (rawDataless == null) {
            return;
        }

        blockettes = new ArrayList<Blockette>();
        Hashtable<Integer, Blockette> blocketteMap = new Hashtable<Integer, Blockette>();

        total = rawDataless.size();
        count = 0.0;
        skipped = 0.0;
        comments = 0.0;
        percent = 0.0;
        lastPercent = 0.0;
        stage = "Parsing Dataless";

        for (String line: rawDataless) {
        	try {
        		checkCancel();
        	} catch (CancelledException e) {
        		throw e;
        	}

            count++;
            percent = Math.floor(count / total * 100.0);
            if (percent > lastPercent) {
                lastPercent = percent;
                // TODO: call to update progress
                //   progress(stage, count, total)
            }

            line = line.trim();
            this.line = line;
//System.out.format(" Dataless.parse(): line=%s\n",line);

            // assume we are going to skip this line
            skipped++;

            if (line == "") {
                continue;
            }
            if (line.startsWith("#")) {
                comments++;
                continue;
            }
            if (!line.startsWith("B")) {
                continue;
            }

            // we are not skipping this line, so revert the increment
            skipped--;

            String[] lineParts = line.split("\\s",2);
            String    word  = lineParts[0];               // e.g.,  word = "B010F13-18"
            String lineData = lineParts[1];               // e.g., lineData = "  SEED Format version: ..."

            int blocketteNumber    = Integer.parseInt(word.substring(1,4) );
            String fieldIdentifier = word.substring(5,word.length());

//System.out.format("  Dataless.parse(): blocketteNumber=%d fieldIdentifier=%s lineData=%s\n",blocketteNumber,fieldIdentifier,lineData);

            Blockette blockette;
            // If the blockette does not exists, or the attempt to add field data
            // reported that this should be part of a new blockette, we create
            // a new blockette, and add this data to it instead.

            try {
	            if ((!blocketteMap.containsKey(blocketteNumber)) ||
	                (!blocketteMap.get(blocketteNumber).addFieldData(fieldIdentifier, lineData)))
	            {
	                blockette = new Blockette(blocketteNumber);
	                blocketteMap.put(blocketteNumber, blockette);
	                blockettes.add(blockette);
	                blockette.addFieldData(fieldIdentifier, lineData);
	                //System.out.format("  Dataless.parse(): new blockette number=%d fieldIdentifier=%s\n", blocketteNumber, fieldIdentifier );
	            }
            } catch (BlocketteFieldIdentifierFormatException e) {
            	throw e;
            }
        }
    }

    private void assemble()
    throws BlocketteFieldIdentifierFormatException,
           BlocketteOutOfOrderException,
           CancelledException,
           DuplicateBlocketteException,
           MissingBlocketteDataException,
           TimestampFormatException,
           WrongBlocketteException
    {
        if (blockettes == null) {
            return;
        }

        total = blockettes.size();
//System.out.format("== assemble() blockettes.size() = %f\n", total);
        count = 0.0;
        skipped = 0.0;
        comments = 0.0;
        percent = 0.0;
        lastPercent = 0.0;
        stage = "Assembling Data";

        StationData station = null;
        ChannelData channel = null;
        EpochData epoch = null;

        for (Blockette blockette: blockettes)
        {
        	try {
        		checkCancel();
        	} catch (CancelledException e) {
        		throw e;
        	}

            count++;
            percent = Math.floor(count / total * 100.0);
            if (percent > lastPercent) {
                lastPercent = percent;
                // TODO: call to update progress
                //   progress(stage, count, total)
            }

            int blocketteNumber = blockette.getNumber();

            switch (blocketteNumber) {
                case 10:
                // MTH: As it stands, a Dataless object is only expecting to create
                //      a *single* Volume corresponding to a single Network dataless
                //      This could be modified so that we close out a previously created
                //      SeedVolume and create a new one when we encounter a Blockette B010.
                //      Then a List<SeedVolume> could be handed back via getVolumes() ...
                    if (volume != null) {
                    	throw new DuplicateBlocketteException();
                    }
                    volume = new SeedVolume(blockette);
                    break;
                case 11:
                    if (volume == null) {
                    	throw new BlocketteOutOfOrderException();
                    }
                    volume.addStationLocator(blockette);
                    break;
                case 50:
                    if (volume == null) {
                    	throw new BlocketteOutOfOrderException();
                    }
                    try {
	                    StationKey stationKey = new StationKey(blockette);
	                
	                    //System.out.format("  Dataless: blockette 50, stationKey=%s\n",stationKey);
	                    if (!volume.hasStation(stationKey)) {
	                    	//System.out.format("  Dataless: call new StationData(%s,%s)\n",stationKey.getNetwork(), stationKey.getName() );
	                        station = new StationData(stationKey.getNetwork(), stationKey.getName());
	                        volume.addStation(stationKey, station);
	                    } else {
	                        station = volume.getStation(stationKey);
	                        //System.out.format("  Dataless: getStation, stationKey=%s station name=%s\n",stationKey,station.getName());
	                    }
                    } catch(WrongBlocketteException e) {
                    	throw e;
                    }
                    
                    try {
                    	station.addEpoch(blockette);
                    } catch(TimestampFormatException e) {
                    	throw e;
                    } catch(WrongBlocketteException e) {
                    	throw e;
                    } catch(MissingBlocketteDataException e) {
                    	throw e;
                    }
                    break;
                case 51:
                    if (station == null) {
                    	throw new BlocketteOutOfOrderException();
                    }
                    try {
                    	station.addComment(blockette);
                    } catch (TimestampFormatException e) {
                    	throw e;
                    } catch (WrongBlocketteException e) {
                    	throw e;
                    } catch (MissingBlocketteDataException e) {
                    	throw e;
                    }
                    break;
                case 52:
                    if (station == null) {
                    	throw new BlocketteOutOfOrderException();
                    }
                    //ChannelKey channelKey = new ChannelKey(blockette);
                    ChannelKey channelKey = null;
                    try {
                        channelKey = new ChannelKey(blockette);
                    }
                    catch (WrongBlocketteException e) {
                    	logger.error(String.format("Dataless: caught new ChannelKey Exception:", e));
                    }
                    if (!station.hasChannel(channelKey)) {
                        //channel = new ChannelData(channelKey.getLocation(), channelKey.getName());
                        channel = new ChannelData(channelKey);
                        station.addChannel(channelKey, channel);
                    } else {
                        channel = station.getChannel(channelKey);
                    }
                    try {
                    	Calendar epochKey = channel.addEpoch(blockette);
                    	epoch = channel.getEpoch(epochKey);
                    } catch (MissingBlocketteDataException e) {
                    	throw e;
                    } catch (TimestampFormatException e) {
                    	throw e;
                    } catch (WrongBlocketteException e) {
                    	throw e;
                    }
                    break;
                case 30:
                    if (epoch == null) {
                    	throw new BlocketteOutOfOrderException();
                    }
                    epoch.setFormat(blockette);
                    break;
                case 59:
                    if (channel == null) {
                    	throw new BlocketteOutOfOrderException();
                    }
                    try {
                    	channel.addComment(blockette);
                    } catch (MissingBlocketteDataException e) {
                    	throw e;
                    } catch (TimestampFormatException e) {
                    	throw e;
                    } catch (WrongBlocketteException e) {
                    	throw e;
                    }
                    break;
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 61:
                case 62:
                    if (epoch == null) {
                    	throw new BlocketteOutOfOrderException();
                    }
/** MTH: I see the following output from rdseed -s:
  *      B053F04	Stage Sequence Number:	1
  *      B058F03	Stage Sequence Number:	1
  *      B054F04	Stage Sequence Number:	2
  *      B057F03	Stage Sequence Number:	2
  *      B058F03	Stage Sequence Number:	2
  *      B054F04	Stage Sequence Number:	3
**/
                    //int stageKey = Integer.parseInt(blockette.getFieldValue(3, 0));
                    int stageKey;
                    if (blocketteNumber == 57 || blocketteNumber == 58){
                        stageKey = Integer.parseInt(blockette.getFieldValue(3, 0));
                    }
                    else {
                        stageKey = Integer.parseInt(blockette.getFieldValue(4, 0));
                    }
                    
                    if (!epoch.hasStage(stageKey)) {
                        epoch.addStage(stageKey, new StageData(stageKey));
                    }
                    StageData stage = epoch.getStage(stageKey);
                    stage.addBlockette(blockette);
                    break;
                default:
                    if (epoch == null) {
                    	throw new BlocketteOutOfOrderException();
                    }
                    epoch.addMiscBlockette(blockette);
                    break;
            }
        }
    }

    public SeedVolume getVolume(){
      return volume;
    }
}

