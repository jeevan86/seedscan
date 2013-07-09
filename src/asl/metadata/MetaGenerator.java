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

import freq.Cmplx;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.*;

import asl.metadata.meta_new.*;

/**
 * MetaGenerator - Holds metadata for all channels + epochs for a single station
 *                 Currently reads metadata in from a dataless seed file
 *
 * Currently we are searching for a single dataless seed file for each station
 *   (e.g., dataless = DATALESS.IU_ANMO.seed), however,
 * The code is set up to scan a multipe station dataless for the requested station
 * In order to implement multiple station dataless file(s), the I/O must be addressed
 * Note that if the station/network masks ever get implemented in Dataless.processVolume(),
 * then it will no longer be possible to use a single dataless seed file with multiple 
 * station metadata.
 *
 * @author Mike Hagerty <hagertmb@bc.edu> 
 *
 * @param  datalessDir  path to dataless seed files is passed in via config.xml 
 */
public class MetaGenerator
{
    private static final Logger logger = Logger.getLogger("asl.metadata.MetaGenerator");

    private SeedVolume volume = null;
    private Collection<String> rawDataless;
    private ArrayList<String> strings = null;
    private StationData stationData = null;
    private boolean successfullyLoaded = false;
    private String datalessDir = null;

    public MetaGenerator(Station station)
    {
        loadDataless(station);
    }
    public MetaGenerator(Station station, String datalessDir)
    {
        this.datalessDir = datalessDir;
        loadDataless(station);
    }

    private void loadDataless(Station station)
    {
      Dataless dataless   = null;
      String datalessPath = datalessDir;
      String datalessFile = datalessPath + "DATALESS." + station.getNetwork() + "_" + station.getStation() + ".seed"; 

    // II/NE/IW networks have all metadata in a single dataless file for all stations:
      if (station.getNetwork().equals("II")) {
        datalessFile = datalessPath + "DATALESS.II.seed"; 
      }
      else if (station.getNetwork().equals("NE")) {
        datalessFile = datalessPath + "DATALESS.NE.seed"; 
      }
      else if (station.getNetwork().equals("IW")) {
        datalessFile = datalessPath + "DATALESS.IW.seed"; 
      }

      ProcessBuilder pb = new ProcessBuilder("rdseed", "-s", "-f", datalessFile);
      //pb.redirectErrorStream(true);
      strings = new ArrayList<String>();

   // First see if the dataless file even exists
      if (!(new File(datalessFile).exists())) {
        System.out.format("=== MetaGenerator: Dataless file=%s does NOT exist!\n", datalessFile);
        return;
      }

      try {
        Process process = pb.start();
        BufferedReader reader = new BufferedReader( new InputStreamReader(process.getInputStream() ) );
        String line = null;
        while( ( line = reader.readLine() ) != null ) {
           strings.add(line);
        }
        int shellExitStatus = process.waitFor();
        //System.out.println("Exit status" + shellExitStatus);
      }
   // Need to catch both IOException and InterruptedException
      catch (IOException e) {
         System.out.println("Error: IOException Description: " + e.getMessage());
      }
      catch (InterruptedException e) {
         System.out.println("Error: InterruptedException Description: " + e.getMessage());
      }

      if (strings.size() == 0) { // We didn't read any metadata in from the rdseed output ...
      }
      dataless = new Dataless( strings ) ;

      try {
          dataless.processVolume(station); // The network and station masks aren't implemented
      }
      catch (Exception e){
      }

      volume = dataless.getVolume();

      if (volume == null){
         String message = "MetaGenerator: Unable to process dataless seed for: " + station.getNetwork() + "-" + station.getStation();
         throw new RuntimeException(message);
      }

      successfullyLoaded = true;

    } // end loadDataless()

    public boolean isLoaded() {
      return successfullyLoaded;
    } 

/**
 * loadDataless() reads in the entire dataless seed file (all stations)
 * getStationData returns the metadata for a single station for all epochs
 * It is called by getStationMeta below.
 */
    private StationData getStationData(Station station){
      StationKey stnkey = new StationKey(station);
      StationData stationData = volume.getStation(stnkey);
      if (stationData == null) {
         System.out.println("stationData is null ==> This COULD be caused by incorrect network code INSIDE seedfile ...");
         return null;
      }
      return stationData;
    }

/**
 * getStationMeta 
 * Calls getStationData to get the metadata for all epochs for this station,
 * Then scans through the epochs to find and return the requested epoch
 * metadata.
 *
 * @station   - The station for which metadata is requested
 * @timestamp - The (epoch) timestamp for which metadata is requested
 * 
 * ChannelData - Contains all Blockettes for a particular channel, for ALL epochs
 * EpochData   - Constains all Blockettes for a particular channel, for the REQUESTED epoch only.
 * ChannelMeta - Our (minimal) internal format of the channel response.
 *               Contains the first 3 (0, 1, 2) response stages for the REQUESTED epoch only.
 *               ChannelMeta.setDayBreak() = true if we detect a change in metadata on the 
 *                                           requested timestamp day.
 */

    public StationMeta getStationMeta(Station station, Calendar timestamp){
      StationKey stnkey = new StationKey(station);  // Kind of redundant ...
      //System.out.format("===== getStationMeta(): station=%s net=%s epoch date=%s\n",stnkey.getName(),stnkey.getNetwork(),EpochData.epochToDateString(timestamp));

      StationData stationData = getStationData(station);
      if (stationData == null) { // This can happen if the file DATALESS.IW_LKWY.seed doesn't match
        return null;             //   the name INSIDE the dataless (= US_LKWY) ... so the keys don't match
      }
 // Scan stationData for the correct station blockette (050) for this timestamp - return null if it isn't found
      Blockette blockette     = stationData.getBlockette(timestamp);

      if (blockette == null){
        System.out.println("MetaGenerator.getStationMeta(): CAN'T FIND STATION METADATA FOR REQUESTED EPOCH");
        return null;
      }
      else { // Uncomment to print out a Blockette050 each time getStationMeta is called
        //blockette.print();
      }

      StationMeta stationMeta = null;
      try {
        stationMeta = new StationMeta(blockette, timestamp);
      }
      catch (WrongBlocketteException e ){
        System.out.println("ERROR: Could not create new StationMeta(blockette) !!");
        System.exit(0);
      }


 // Get this StationData's ChannelKeys and sort:
      Hashtable<ChannelKey, ChannelData> channels = stationData.getChannels();
      TreeSet<ChannelKey> keys = new TreeSet<ChannelKey>();
      keys.addAll(channels.keySet());
      for (ChannelKey key : keys){
        //System.out.println("==Channel:"+key );
        ChannelData channel = channels.get(key);
        //ChannelMeta channelMeta = new ChannelMeta(key,timestamp);
//System.out.format("== MetaGen: newChannelMeta(key=%s station=%s)\n", key, station);
        ChannelMeta channelMeta = new ChannelMeta(key,timestamp,station);

     // See if this channel contains the requested epoch time and if so return the key (=Epoch Start timestamp)
      //channel.printEpochs();
        Calendar epochTimestamp = channel.containsEpoch(timestamp);
        if (epochTimestamp !=null){
           EpochData epochData = channel.getEpoch(epochTimestamp);

     // If the epoch is closed, check that the end time is at least 24 hours later than the requested time
           if (epochData.getEndTime() != null ){  
         // Make sure this epoch end time is > requested time + 24 hours
              long epochStart = epochData.getStartTime().getTimeInMillis();
              long epochEnd   = epochData.getEndTime().getTimeInMillis();
              if ( epochEnd <  (timestamp.getTimeInMillis() + 24 * 3600 * 1000) ) {
                channelMeta.setDayBreak(); // set channelMeta.dayBreak = true
              }
           }

           channelMeta.processEpochData(epochData);
           stationMeta.addChannel(key, channelMeta);
        // channelMeta.print();

        }
        else { // The channel does NOT contain the epoch time --> reset ChannelMeta to null
          //System.out.format("==No Response found for this Channel + Epoch\n");
            channelMeta = null;
        }
      }

      return stationMeta;
     
    }

}
