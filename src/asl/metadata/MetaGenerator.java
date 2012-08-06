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
import java.util.TreeSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.*;

import asl.metadata.meta_new.*;

public class MetaGenerator
{
    private static final Logger logger = Logger.getLogger("asl.metadata.MetaGenerator");

    private SeedVolume volume = null;
    private Collection<String> rawDataless;
    private ArrayList<String> strings = null;
    private StationData stationData = null;

    public MetaGenerator()
    {
        //loadDataless(file);
        loadDataless();
    }

    private void loadDataless()
    {
      Dataless dataless = null;

      try {
          readFile("/qcwork/datalessSTUFF/littlesANMO");
      }
      catch(IOException e) {
          System.out.println("Error: IOException " + e);
      }
      dataless = new Dataless( strings ) ;

      try {
          dataless.processVolume("IU","ANMO"); // The network and station masks aren't implemented
      }
      catch (Exception e){
      }

      volume = dataless.getVolume();

      if (volume == null){
// Do something here so we don't go on
      }

     } // end loadDataless()

     private void readFile( String file ) throws IOException {
       strings = new ArrayList<String>();
       BufferedReader reader = new BufferedReader( new FileReader (file));
       String line = null;
       while( ( line = reader.readLine() ) != null ) {
           strings.add(line);
       }
     }


    public StationData getStationData(Station station){
      StationKey stnkey = new StationKey(station);
      StationData stationData = volume.getStation(stnkey);
      if (stationData == null) {
         System.out.println("stationData is null ! Exitting!!");
         System.exit(0);
      }
      return stationData;
    }


    public StationMeta getStationMeta(Station station, Calendar timestamp){
      StationKey stnkey = new StationKey(station);  // Kind of redundant ...
      System.out.format("===== getStationMeta(): station=%s net=%s epoch date=%s\n",stnkey.getName(),stnkey.getNetwork(),EpochData.epochToDateString(timestamp));

      StationData stationData = getStationData(station);
 // Scan stationData for the correct station blockette (050) for this timestamp - return null if it isn't found
      Blockette blockette     = stationData.getBlockette(timestamp);

      if (blockette == null){
        System.out.println("MetaGenerator.getStationMeta(): CAN'T FIND STATION METADATA FOR REQUESTED EPOCH");
      }
      else {
        blockette.print();
      }

      StationMeta stationMeta = null;
      try {
        stationMeta = new StationMeta(blockette, timestamp);
        //stationMeta = new StationMeta(blockette);
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
        System.out.println("==Channel:"+key );
        ChannelData channel = channels.get(key);

        //ChannelMeta channelMeta = new ChannelMeta(key);
        ChannelMeta channelMeta = new ChannelMeta(key,timestamp);

     // See if this channel contains the requested epoch time and if so return the key (=Epoch Start timestamp)
      //channel.printEpochs();
        Calendar epochTimestamp = channel.containsEpoch(timestamp);
        if (epochTimestamp !=null){
           StageData stage     = null; 
           blockette = null;
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

    // Process Stage 0:
           if (epochData.hasStage(0)) {
             stage = epochData.getStage(0); 
             if (stage.hasBlockette(58)) {
               blockette = stage.getBlockette(58); 
               Double Sensitivity = Double.parseDouble(blockette.getFieldValue(4, 0));
//B058F05     Frequency of sensitivity:              2.000000E-02 HZ
               //Double frequencyOfSensitivity = Double.parseDouble(blockette.getFieldValue(5, 0));
               String frequencyOfSensitivity = blockette.getFieldValue(5, 0);

         // Set the Stage 0 Gain = The Sensitivity:
               DigitalStage responseStage = new DigitalStage(0, 'D', Sensitivity);
               channelMeta.addStage(0, responseStage);
               //channelMeta.addStage(responseStage);
             }
             else {
               System.out.println("Error: Stage 0 does not appear to contain Blockette Number = 58");
             }

           }

           else {
               System.out.println("== There is NO Stage:0 for this Channel + Epoch !");
           }

    // Process Stage 1:
           if (epochData.hasStage(1)) {
             stage = epochData.getStage(1); 
             PoleZeroStage pz;
             double Gain=0;

             if (stage.hasBlockette(58)) {
               blockette = stage.getBlockette(58); 
               Gain = Double.parseDouble(blockette.getFieldValue(4, 0));
               //Double frequencyOfGain = Double.parseDouble(blockette.getFieldValue(5, 0));
               String frequencyOfGain = blockette.getFieldValue(5, 0);
             }
             else {
               System.out.println("Error: Stage 1 does not appear to contain Blockette Number = 58");
             }

          // Get polezero blockette B053:
             if (stage.hasBlockette(53)) {
               blockette = stage.getBlockette(53); 
               //blockette.print(); 
               String TransferFunctionType = blockette.getFieldValue(3, 0);
               String ResponseInUnits = blockette.getFieldValue(5, 0);
               String ResponseOutUnits = blockette.getFieldValue(6, 0);
               Double A0Normalization = Double.parseDouble(blockette.getFieldValue(7, 0));
               Double frequencyOfNormalization = Double.parseDouble(blockette.getFieldValue(8, 0));
               int numberOfZeros = Integer.parseInt(blockette.getFieldValue(9, 0));
               int numberOfPoles = Integer.parseInt(blockette.getFieldValue(14, 0));
               ArrayList<String> RealPoles = blockette.getFieldValues(15);
               ArrayList<String> ImagPoles = blockette.getFieldValues(16);
               ArrayList<String> RealZeros = blockette.getFieldValues(10);
               ArrayList<String> ImagZeros = blockette.getFieldValues(11);
           // There are channels (e.g., LWS) that have a blockette 53 but have NO poles and/or NO zeros
             //int nPoles = RealPoles.size();
             //int nZeros = RealZeros.size();

               char[] respType  = TransferFunctionType.toCharArray();
               //char respType = TransferFunctionType.substring(0,0);

               pz = new PoleZeroStage(1, respType[0], Gain);
               channelMeta.addStage(1, pz);
               //channelMeta.addStage(pz);
               pz.setNormalization(A0Normalization);
               pz.setInputUnits(ResponseInUnits);
               pz.setOutputUnits(ResponseOutUnits);

               for (int i=0; i<numberOfPoles; i++){
                 Double pole_re = Double.parseDouble(RealPoles.get(i));
                 Double pole_im = Double.parseDouble(ImagPoles.get(i));
                 Complex pole_complex = new Complex(pole_re, pole_im);
                 pz.addPole(pole_complex);
               }
               for (int i=0; i<numberOfZeros; i++){
                 Double zero_re = Double.parseDouble(RealZeros.get(i));
                 Double zero_im = Double.parseDouble(ImagZeros.get(i));
                 Complex zero_complex = new Complex(zero_re, zero_im);
                 pz.addZero(zero_complex);
               }
             }
             else {
               System.out.println("Error: Stage 1 does not appear to contain Blockette Number = 53");
             }

           }
           else {
               System.out.println("== There is NO Stage:1 for this Channel + Epoch !");
           }

    // Process Stage 2:
           if (epochData.hasStage(2)) {
             stage = epochData.getStage(2); 
             double Gain = 0;
             char[] respType=null;
             String ResponseInUnits = null;
             String ResponseOutUnits = null;
             if (stage.hasBlockette(54)) {
               blockette = stage.getBlockette(54); 
               String TransferFunctionType = blockette.getFieldValue(3, 0);
               respType  = TransferFunctionType.toCharArray();
               ResponseInUnits = blockette.getFieldValue(5, 0);
               ResponseOutUnits = blockette.getFieldValue(6, 0);
             }
             else {
               System.out.println("Error: Stage 2 does not appear to contain Blockette Number = 54");
             }
             if (stage.hasBlockette(58)) {
               blockette = stage.getBlockette(58); 
               Gain = Double.parseDouble(blockette.getFieldValue(4, 0));
               //Double frequencyOfGain = Double.parseDouble(blockette.getFieldValue(5, 0));
               String frequencyOfGain = blockette.getFieldValue(5, 0);
               DigitalStage responseStage = new DigitalStage(2, respType[0], Gain);
               responseStage.setInputUnits(ResponseInUnits);
               responseStage.setOutputUnits(ResponseOutUnits);
               channelMeta.addStage(2, responseStage);
               //channelMeta.addStage(responseStage);
             }
             else {
               System.out.println("Error: Stage 2 does not appear to contain Blockette Number = 58");
             }
           }
           else {
               System.out.println("== There is NO Stage:2 for this Channel + Epoch !");
           }

           channelMeta.setAzimuth(epochData.getAzimuth() );
           channelMeta.setDepth(epochData.getDepth() );
           channelMeta.setDip(epochData.getDip() );
           channelMeta.setSampleRate(epochData.getSampleRate() );
           channelMeta.setInstrumentType(epochData.getInstrumentType() );
           stationMeta.addChannel(key, channelMeta);
           //System.out.format("==Dip=%.2f Azim=%.2f SampRate=%.2f\n",epochData.getDip(), epochData.getAzimuth(), epochData.getSampleRate() ); 
           //System.out.format("==Dip=%.2f Azim=%.2f SampRate=%.2f\n",channelMeta.getDip(), channelMeta.getAzimuth(), channelMeta.getSampleRate() ); 
        }
        else {
             System.out.format("==No Response found for this Channel + Epoch\n");
        }
      }

      return stationMeta;
     
    }

  // Use timestamp t
    public void parseStationData(){
    }

}
