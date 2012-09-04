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

public class MetaGenerator
{
    private static final Logger logger = Logger.getLogger("asl.metadata.MetaGenerator");

    private SeedVolume volume = null;
    private Collection<String> rawDataless;
    private ArrayList<String> strings = null;
    private StationData stationData = null;

    public MetaGenerator(Station station)
    {
        loadDataless(station);
    }

    private void loadDataless(Station station)
    {

     // MTH: We could handle one dataless with multiple stations ... and then use station filters in processVolume() below 
     //      or we could expect one dataless per station.
     // We probably need a flag to switch between either expectation
     // For now this is hardwired to expect one dataless per station (e.g., dataless=DATALESS.IU_ANMO.seed)
     // ** Note that if the station/network masks get implemented in Dataless.processVolume(), then it will no longer
     //    be possible to use a single dataless seed file with multiple station metadata.

      Dataless dataless   = null;
     // MTH: This is a param we should likely read in from config.xml:
      String datalessPath = "/Users/mth/mth/Projects/dcc/metadata/dataless/";
      String datalessFile = datalessPath + "DATALESS." + station.getNetwork() + "_" + station.getStation() + ".seed"; 
      ProcessBuilder pb = new ProcessBuilder("rdseed", "-s", "-f", datalessFile);
      //pb.redirectErrorStream(true);
      strings = new ArrayList<String>();

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

     } // end loadDataless()


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
               String temp[] = blockette.getFieldValue(5, 0).split(" ");
               Double frequencyOfSensitivity = Double.parseDouble(temp[0]);

         // Set the Stage 0 Gain = The Sensitivity:
               DigitalStage responseStage = new DigitalStage(0, 'D', Sensitivity, frequencyOfSensitivity);
               channelMeta.addStage(0, responseStage);
               //channelMeta.addStage(responseStage);
             }
             else {
               System.out.format("== Error: Stage 0 does not appear to contain Blockette B058 [Channel=%s-%s]\n", channel.getLocation(), channel.getName());
             }

           }
/**
  Channels VM? and LD? do NOT appear to have a stage 0
**/
           else {
             String excludeCodes = "MD";
             if (!excludeCodes.contains(channel.getName().substring(1,2))) {
               System.out.format("== Error: There is NO Stage 0 for this epoch [Channel=%s-%s]\n", channel.getLocation(), channel.getName());
             }
           }

    // Process Stage 1:
           if (epochData.hasStage(1)) {

             stage = epochData.getStage(1); 

             double Gain=0;
             double frequencyOfGain=0;
             if (stage.hasBlockette(58)) {        // We have a gain blockette in this stage
               blockette = stage.getBlockette(58); 
               Gain = Double.parseDouble(blockette.getFieldValue(4, 0));
               String temp[] = blockette.getFieldValue(5, 0).split(" ");
               frequencyOfGain = Double.parseDouble(temp[0]);
             }
             else if (channel.getName().charAt(1) != 'M') {  // Mass positions, eg., VMZ, VM1, VM2 .. don't have a B058 in stage 1
               System.out.format("== Error: Stage 1 does not appear to contain Blockette B058 [Channel=%s-%s]\n", channel.getLocation(), channel.getName());
             }

             if (stage.hasBlockette(62)) {        // This is a polynomial stage, e.g., ANMO_IU_00_VMZ
               PolynomialStage polyStage;
               blockette = stage.getBlockette(62); 
               //blockette.print(); 
               String TransferFunctionType = blockette.getFieldValue(3, 0); // Should be "P [Polynomial]"
               String ResponseInUnits  = blockette.getFieldValue(5, 0);
               String ResponseOutUnits = blockette.getFieldValue(6, 0);
               String PolynomialApproximationType = blockette.getFieldValue(7, 0); // e.g., "M [MacLaurin]"
               Double lowerFrequencyBound = Double.parseDouble(blockette.getFieldValue(9, 0));
               Double upperFrequencyBound = Double.parseDouble(blockette.getFieldValue(10, 0));
               Double lowerApproximationBound = Double.parseDouble(blockette.getFieldValue(11, 0));
               Double upperApproximationBound = Double.parseDouble(blockette.getFieldValue(12, 0));
               int numberOfCoefficients = Integer.parseInt(blockette.getFieldValue(14, 0));
               ArrayList<String> RealCoefficients = blockette.getFieldValues(15);
               ArrayList<String> ImagCoefficients = blockette.getFieldValues(16);
               char[] respType  = TransferFunctionType.toCharArray();
               polyStage = new PolynomialStage(1, respType[0], Gain, frequencyOfGain);
               channelMeta.addStage(1, polyStage);
               polyStage.setInputUnits(ResponseInUnits);
               polyStage.setOutputUnits(ResponseOutUnits);
               polyStage.setLowerFrequencyBound(lowerFrequencyBound);
               polyStage.setUpperFrequencyBound(upperFrequencyBound);
               polyStage.setLowerApproximationBound(lowerApproximationBound);
               polyStage.setUpperApproximationBound(upperApproximationBound);
               polyStage.setPolynomialApproximationType(PolynomialApproximationType);

               for (int i=0; i<numberOfCoefficients; i++){
                 Double coeff_re = Double.parseDouble(RealCoefficients.get(i));
                 Double coeff_im = Double.parseDouble(ImagCoefficients.get(i));
                 Cmplx coefficient = new Cmplx(coeff_re, coeff_im);
                 polyStage.addCoefficient(coefficient);
               }
             } // end process blockette(62) = polynomial stage

             if (stage.hasBlockette(53)) {        // This is a pole-zero stage
               PoleZeroStage pz;
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

               pz = new PoleZeroStage(1, respType[0], Gain, frequencyOfGain);
               channelMeta.addStage(1, pz);
               //channelMeta.addStage(pz);
               pz.setNormalization(A0Normalization);
               pz.setInputUnits(ResponseInUnits);
               pz.setOutputUnits(ResponseOutUnits);
  
               for (int i=0; i<numberOfPoles; i++){
                 Double pole_re = Double.parseDouble(RealPoles.get(i));
                 Double pole_im = Double.parseDouble(ImagPoles.get(i));
                 Cmplx pole_complex = new Cmplx(pole_re, pole_im);
                 pz.addPole(pole_complex);
               }
               for (int i=0; i<numberOfZeros; i++){
                 Double zero_re = Double.parseDouble(RealZeros.get(i));
                 Double zero_im = Double.parseDouble(ImagZeros.get(i));
                 Cmplx zero_complex = new Cmplx(zero_re, zero_im);
                 pz.addZero(zero_complex);
               }

             } // end blockette(53) = pole-zero stage

           } // end epoch has stage 1
           else {
               System.out.format("== Error: There is NO Stage 1 for this epoch: [Channel=%s-%s]\n", channel.getLocation(), channel.getName());
           }

    // Process Stage 2:
           if (epochData.hasStage(2)) {
             stage = epochData.getStage(2); 
             double Gain = 0;
             double frequencyOfGain = 0;
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
               System.out.format("== Error: Stage 2 does not appear to contain Blockette B054 [Channel=%s-%s]\n", channel.getLocation(), channel.getName());
             }
             if (stage.hasBlockette(58)) {
               blockette = stage.getBlockette(58); 
               Gain = Double.parseDouble(blockette.getFieldValue(4, 0));
               String temp[] = blockette.getFieldValue(5, 0).split(" ");
               frequencyOfGain = Double.parseDouble(temp[0]);
               DigitalStage responseStage = new DigitalStage(2, respType[0], Gain, frequencyOfGain);
               responseStage.setInputUnits(ResponseInUnits);
               responseStage.setOutputUnits(ResponseOutUnits);
               channelMeta.addStage(2, responseStage);
               //channelMeta.addStage(responseStage);
             }
             else {
               System.out.format("== Error: Stage 2 does not appear to contain Blockette B058 [Channel=%s-%s]\n", channel.getLocation(), channel.getName());
             }
           }
/**
     [java] == Error: There is NO Stage 2 for this epoch: [Channel=30-LDO]
     [java] == Error: There is NO Stage 2 for this epoch: [Channel=50-LIO]
     [java] == Error: There is NO Stage 2 for this epoch: [Channel=50-LKO]
     [java] == Error: There is NO Stage 2 for this epoch: [Channel=50-LRH]
     [java] == Error: There is NO Stage 2 for this epoch: [Channel=50-LRI]
     [java] == Error: There is NO Stage 2 for this epoch: [Channel=50-LWD]
     [java] == Error: There is NO Stage 2 for this epoch: [Channel=50-LWS]
**/
           else {
             String excludeCodes = "MDIKRW"; // Channel codes that we DON'T expect to have a stage 2 (e.g., VM?, LD?, LIO, etc.)
             if (!excludeCodes.contains(channel.getName().substring(1,2))) {
               System.out.format("== Error: There is NO Stage 2 for this epoch: [Channel=%s-%s]\n", channel.getLocation(), channel.getName());
             }
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
             //System.out.format("==No Response found for this Channel + Epoch\n");
        }
      }

      return stationMeta;
     
    }

  // Use timestamp t
    public void parseStationData(){
    }

}
