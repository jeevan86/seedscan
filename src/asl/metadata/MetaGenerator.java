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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freq.Cmplx;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.io.IOException;
import java.io.*;

import asl.metadata.meta_new.*;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * MetaGenerator - Holds metadata for all networks x stations x channels x epochs
 *                 Currently reads metadata in from network dataless seed files
 *
 * @author Mike Hagerty <hagertmb@bc.edu> 
 *
 */
public class MetaGenerator
    extends UnicastRemoteObject
    implements MetaInterface
{
    private static final Logger logger = LoggerFactory.getLogger(asl.metadata.MetaGenerator.class);

    private static final long serialVersionUID = 1L;

    /**
    * Each datalessDir/XX.dataless file is read into a separate SeedVolume 
    * keyed by network (e.g., XX)
    */
    private Hashtable<NetworkKey, SeedVolume> volumes = null;

    private static MetaGenerator instance;

    private boolean successfullyLoaded = false;

    /**
    * Private constructor to ensure singleton
    */
    private MetaGenerator() throws RemoteException
    {
        volumes = new Hashtable<NetworkKey, SeedVolume>();
    }

    static public MetaGenerator getInstance() throws RemoteException{
        if (instance == null) {
            instance = new MetaGenerator();
        }
        return instance;
    }

    public String getString() throws RemoteException {
        return new String("This-string-is-from-MetaGenerator");
    }

    /**
    *
    * loadDataless - Look in datalessDir for all files of form XX.dataless
    *                where XX = network {II, IU, NE, etc.}
    * 
    * @param  datalessDir  path to dataless seed files, read from config.xml 
    */
    public void loadDataless(String datalessDir)
    {
        File dir = new File(datalessDir);
        if (!dir.exists()) {
            logger.error("Path '" +dir+ "' does not exist.");
            System.exit(0);
        }
        else if (!dir.isDirectory()) {
            logger.error("Path '" +dir+ "' is not a directory.");
            System.exit(0);
        }

        FilenameFilter textFilter = new FilenameFilter() {
          public boolean accept(File dir, String name) {
              if ( name.endsWith(".dataless") && (name.length() == 11) ) {
                  return true;
              } else {
                  return false;
              }
          }
        };

        String[] files    = dir.list(textFilter);

        for (String fileName : files) {
            String datalessFile = dir + "/" + fileName;
            System.out.format("== MetaGenerator: rdseed -f [datalessFile=%s]\n", datalessFile);
            ProcessBuilder pb = new ProcessBuilder("rdseed", "-s", "-f", datalessFile);

            ArrayList<String> strings = new ArrayList<String>();

            SeedVolume volume = null;

            try {
                Process process = pb.start();
                BufferedReader reader = new BufferedReader( new InputStreamReader(process.getInputStream() ) );
                String line = null;
                while( ( line = reader.readLine() ) != null ) {
                    strings.add(line);
                }
                int shellExitStatus = process.waitFor();
            }
        // Need to catch both IOException and InterruptedException
            catch (IOException e) {
                System.out.println("Error: IOException Description: " + e.getMessage());
            }
            catch (InterruptedException e) {
                System.out.println("Error: InterruptedException Description: " + e.getMessage());
            }

            Dataless dataless = new Dataless( strings ) ;

            try {
                dataless.processVolume(); 
                volume = dataless.getVolume();
            }
            catch (Exception e){
                System.out.format("== MetaGenerator: Error processing dataless volume for file=[%s]:%s\n", 
                    fileName, e.getMessage());
            }

            if (volume == null){
                System.out.format("== MetaGenerator: Error processing dataless volume==null! for file=[%s]\n",
                    fileName);
                System.exit(0);
            }
            else {
                addVolume(volume);
            }

        } // end for loop over XX.dataless files

        successfullyLoaded = true;

    } // end loadDataless()

    private void addVolume( SeedVolume volume ) {
        NetworkKey networkKey = volume.getNetworkKey();
        if (volumes.containsKey(networkKey)) {
            System.out.format("== MetaGenerator Error: Attempting to load volume networkKey=[%s] --> Already loaded!\n",
                              networkKey);
        }
        else {
            volumes.put( networkKey, volume );
        }
    }

    public boolean isLoaded() {
        return successfullyLoaded;
    } 

    public void print() {
        if (volumes == null) {
            System.out.format("== MetaGenerator.print() - No SeedVolumes have been loaded!\n");
        }
        else {
            for (NetworkKey key : volumes.keySet()) {
                System.out.format("== MetaGenerator Found SeedVolume for Network:[%s]\n", key );
                SeedVolume volume = volumes.get(key);
                //volume.printStations();
                List<Station> stations = volume.getStationList();
                for (Station station : stations) {
                    System.out.format("     == SeedVolume contains Station:[%s]\n", station );
                }
            }
        }
    }

    /*
    * Return a list of all stations contained in all volumes
    */
    public List<Station> getStationList() throws RemoteException {
        if (volumes == null) {
            return null;
        }
        List<Station> allStations = new ArrayList<Station>();
        for (NetworkKey key : volumes.keySet()) {
            SeedVolume volume = volumes.get(key);
            List<Station> stations = volume.getStationList();
            allStations.addAll(stations);
        }
        return allStations;
    }

/**
 * loadDataless() reads in the entire dataless seed file (all stations)
 * getStationData returns the metadata for a single station for all epochs
 * It is called by getStationMeta below.
 */
    private StationData getStationData(Station station){
        SeedVolume volume = volumes.get( new NetworkKey(station.getNetwork()) ) ; 
        if (volume == null) {
            System.out.format("== MetaGenerator.getStationData() - Volume==null for Station=[%s]\n", station);
            System.exit(0);
        }
        StationData stationData = volume.getStation(new StationKey(station));
        if (stationData == null) {
            System.out.println("stationData is null ==> This COULD be caused by incorrect network code INSIDE seedfile ...");
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

    public StationMeta getStationMeta(Station station, Calendar timestamp) throws RemoteException {

        StationData stationData = getStationData(station);
        if (stationData == null) { // This can happen if the file DATALESS.IW_LKWY.seed doesn't match
            System.out.format("== [UTC %s] MetaGenerator getStationMeta request:\t\t[%s]\t[%s]\tNOT FOUND!\n",            
              EpochData.epochToDateString(Calendar.getInstance()), station,EpochData.epochToDateString(timestamp));
            return null;             //   the name INSIDE the dataless (= US_LKWY) ... so the keys don't match
        }
 // Scan stationData for the correct station blockette (050) for this timestamp - return null if it isn't found
        Blockette blockette     = stationData.getBlockette(timestamp);

        if (blockette == null){
            System.out.format("== [UTC %s] MetaGenerator getStationMeta request:\t\t[%s]\t[%s]\tNOT FOUND!\n",            
              EpochData.epochToDateString(Calendar.getInstance()), station,EpochData.epochToDateString(timestamp));
            return null;
        }
        else { // Uncomment to print out a Blockette050 each time getStationMeta is called
            //blockette.print();
            System.out.format("== [UTC %s] MetaGenerator getStationMeta request:\t\t[%s]\t[%s]\n",            
              EpochData.epochToDateString(Calendar.getInstance()), station,EpochData.epochToDateString(timestamp));
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
            ChannelMeta channelMeta = new ChannelMeta(key,timestamp,station);

     // See if this channel contains the requested epoch time and if so return the key 
     //                                                       (=Epoch Start timestamp)
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
            }
            else { // The channel does NOT contain the epoch time --> reset ChannelMeta to null
                channelMeta = null;
            }
        }

        return stationMeta;
    }

    public static void main(String[] args) {
        System.out.println("=== Start MetaGenerator Server ====");
        try {
            MetaGenerator metaGen = getInstance();
            metaGen.loadDataless("/Users/mth/mth/ASLData/dcc/metadata/dataless");
            metaGen.print();

            Naming.rebind("MetaGen", metaGen);
            System.out.println("== MetaGen Server is ready");
        }
        catch (Exception e) {
            System.out.format("== MetaGen Server failed:%s\n", e.getMessage() );
        }
    }

}
