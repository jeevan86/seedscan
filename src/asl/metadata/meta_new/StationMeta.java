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
package asl.metadata.meta_new;

import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.TreeSet;
import asl.metadata.*;


/**
 * Our internal representation of a station's metadata
 * Holds all channel metadata for a single station for a specified day
 *
 * @author Mike Hagerty <hagertmb@bc.edu>
 *
 * @param timestamp     the date (exact day) for which we want the station's metadata
 */
public class StationMeta
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger("asl.metadata.meta_new.StationMeta");

    private String network = null;
    private String name = null;
    private String comment = null;
    private double latitude;
    private double longitude;
    private double elevation;
    private Hashtable<ChannelKey,ChannelMeta> channels;
    private Calendar metaTimestamp = null;

    private Blockette blockette50 = null;

    // constructor(s)

    public StationMeta(Blockette blockette, Calendar timestamp)
    throws WrongBlocketteException
    {
        if (blockette.getNumber() != 50) {  // We're expecting a station blockette (B050)
            throw new WrongBlocketteException();
        }
        this.network = blockette.getFieldValue(16,0);
        this.name    = blockette.getFieldValue(3,0);
        this.latitude = Double.parseDouble(blockette.getFieldValue(4,0));
        this.longitude= Double.parseDouble(blockette.getFieldValue(5,0));
        this.elevation= Double.parseDouble(blockette.getFieldValue(6,0));
        channels = new Hashtable<ChannelKey,ChannelMeta>();
        this.metaTimestamp = (Calendar)timestamp.clone();
        this.blockette50= blockette;
    }

    public void setLatitude(double latitude)
    {
        if (! (latitude <= 90. && latitude >= -90) ) {
           throw new RuntimeException("Error: station latitude must be: -90 <= val <= 90");
        }
        this.latitude = latitude;
    }
    public void setLongitude(double longitude)
    {
        if (! (longitude <= 180. && longitude >= -180) ) {
           throw new RuntimeException("Error: station longitude must be: -180 <= val <= 180");
        }
        this.longitude = longitude;
    }
    public void setLatLon(double latitude, double longitude)
    {
        this.setLatitude(latitude);
        this.setLongitude(longitude);
    }
    public void setElevation(double elevation)
    {
        this.elevation = elevation;
    }
    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public void addChannel(ChannelKey chanKey, ChannelMeta channel)
    {
      channels.put(chanKey, channel);
    }

    public String getStation(){
      return name;
    }
    public String getNetwork(){
      return network;
    }
    public String getComment(){
      return comment;
    }
    public double getLatitude(){
      return latitude;
    }
    public double getLongitude(){
      return longitude;
    }
    public int getNumberOfChannels() {
        return channels.size();
    }
    public Calendar getTimestamp() {
        return (Calendar)metaTimestamp.clone();
    }

/**  
 *  Look for particular channelMeta (e.g., "00" "VHZ") in channels Hashtable.
 *    Return it if found, else return null
 */
    public ChannelMeta getChanMeta(ChannelKey chanKey) {
      if (channels.containsKey(chanKey) ){
          return channels.get(chanKey);
      }
      else {
          return null;
      }
    }
    public ChannelMeta getChanMeta(Channel channel) {
      return getChanMeta(new ChannelKey(channel.getLocation(), channel.getChannel()) );
    }
    public ChannelMeta getChanMeta(String location, String name) {
      return getChanMeta(new ChannelKey(location, name));
    }
/**
 *  Return the entire channels Hashtable
 */
    public Hashtable<ChannelKey,ChannelMeta> getChannelHashTable() {
      return channels;
    }

/**
 *  Return ordered primary + secondary seismic channels that have matching band
 *  
 *  e.g., if band = "LH" then return channels[0] = "00-LHZ"
 *                                   channels[1] = "00-LH1" -or- "00-LHN"                             
 *                                   channels[2] = "00-LH2" -or- "00-LHE"                             
 *                                   channels[3] = "10-LHZ"
 *                                   channels[4] = "10-LH1" -or- "10-LHN"                             
 *                                   channels[5] = "10-LH2" -or- "10-LHE"                             
 */
    public List<Channel> getZNEChannelArray(String band) {
        if (!Channel.validBandCode(band.substring(0,1)) || !Channel.validInstrumentCode(band.substring(1,2)) ) {
            return null;
        }
        TreeSet<ChannelKey> keys = new TreeSet<ChannelKey>();
        keys.addAll(channels.keySet());

        ArrayList<Channel> channelArrayList = new ArrayList<Channel>();

        String[] location = {"00", "10"}; 
        String[] chan     = {band+"Z", band+"1", band+"2", band+"Z", band+"N", band+"E"}; 

    // Add found Primary + Secondary seismic channels to channel array in order:
        for (int i=0; i<2; i++){
            for (int j=0; j<3; j++){
                if (keys.contains(     new ChannelKey(location[i], chan[j]) ) ){
                    channelArrayList.add( new Channel(location[i], chan[j]) );
                }
                else if (keys.contains(new ChannelKey(location[i], chan[j+3]) ) ){
                    channelArrayList.add( new Channel(location[i], chan[j+3]) );
                }
                else {
                    logger.severe( String.format("Error: CAN'T find Channel=[%s-%s] OR Channel=[%s-%s] in metadata",
                                                  location[i], chan[j], location[i], chan[j+3]) );
                }
            }
        }

        return channelArrayList;
    }

/**
 *  Return ArrayList of all channels contained in this stationMeta
 *    that match the 2-char band
 *  e.g., if band = "LH" then return "00-LHZ", "00-LH1", "00-LH2", "00-LHN", "00-LHE",
 *                                   "10-LHZ", "10-LH1", "10-LH2", ..
 *                           -or-    "---LHZ", "---LH1", "---LH2", "---LHN", "---LHE", 
 */
    public List<Channel> getChannelArray(String band) {
        if (!Channel.validBandCode(band.substring(0,1)) || !Channel.validInstrumentCode(band.substring(1,2)) ) {
            return null;
        }
        TreeSet<ChannelKey> keys = new TreeSet<ChannelKey>();
        keys.addAll(channels.keySet());

        ArrayList<Channel> channelArrayList = new ArrayList<Channel>();

        for (ChannelKey channelKey : keys){
            Channel channel = channelKey.toChannel();

            //if (channel.getChannel().contains(band) ){
          // We need to restrict ourselves to 3-char channels so we don't also return
          //   the derived channels (e.g., LHND, LHED)
            if (channel.getChannel().contains(band) && channel.getChannel().length() == 3){
                //System.out.format("== Channel [%s] contains band [%s]\n", channel, band);
                channelArrayList.add(channel);
            }
        }
        return channelArrayList;
    }

/** Handle horizontal naming conventions (e.g., LH1,2 versus LHN,E)
 *
 *                                    "00"        "LH"         "1"
 */
    public Channel getChannel(String location, String band, String comp) {

        String name = null;

        if (comp.equals("1")) {
            name = band + "1";
            if (hasChannel(location, name)) {       // Do we have LH1 ?
                return new Channel(location, name);
            }
            else {
                name = band + "N";
                if (hasChannel(location, name)) {       // Do we have LHN ?
                    return new Channel(location, name); 
                }
            }
        return null;

        }
        else if (comp.equals("2")) {
            name = band + "2";
            if (hasChannel(location, name)) {       // Do we have LH2 ?
                return new Channel(location, name);
            }
            else {
                name = band + "E";
                if (hasChannel(location, name)) {       // Do we have LHE ?
                    return new Channel(location, name); 
                }
            }
        return null;

        }
        else {
            System.out.format("== StationMeta.getChannel: Channel=[%d-%d%d] NOT FOUND\n",location, band, comp);
            return null;
        }
    }


/*
 *  Same as above but limit return channels to those containing the specified location
 *       as well as the specified band
 */
    public List<Channel> getChannelArray(String location, String band) {
        if (!Channel.validLocationCode(location)) {
            return null;
        }
        if (!Channel.validBandCode(band.substring(0,1)) || !Channel.validInstrumentCode(band.substring(1,2)) ) {
            return null;
        }
        TreeSet<ChannelKey> keys = new TreeSet<ChannelKey>();
        keys.addAll(channels.keySet());

        ArrayList<Channel> channelArrayList = new ArrayList<Channel>();

        for (ChannelKey channelKey : keys){
            Channel channel = channelKey.toChannel();

            if (channel.getChannel().contains(band) && channel.getLocation().equals(location) ){
                //System.out.format("== Channel [%s] contains band [%s]\n", channel, band);
                channelArrayList.add(channel);
            }
        }   
        return channelArrayList;
    }

/**
 *  Return a (sorted) ArrayList of channels that are continuous (channelFlag="C?")
 **/
    public List<Channel> getContinuousChannels() {
        TreeSet<ChannelKey> keys = new TreeSet<ChannelKey>();
        keys.addAll(channels.keySet());

        ArrayList<Channel> channelArrayList = new ArrayList<Channel>();

        for (ChannelKey channelKey : keys){
            Channel channel = channelKey.toChannel();
            String channelFlags  = getChanMeta(channelKey).getChannelFlags();

            if (channelFlags.substring(0,1).equals("C") ){
                channelArrayList.add(channel);
            }
        }
        return channelArrayList;
    }


 // boolean hasChannel methods:

    public boolean hasChannel(ChannelKey channelKey) {
      return channels.containsKey(channelKey);
    }
    public boolean hasChannel(Channel channel) {
      return hasChannel(new ChannelKey(channel.getLocation(), channel.getChannel()) );
    }
    public boolean hasChannel(String location, String name) {
      return hasChannel(new ChannelKey(location, name) );
    }
    public boolean hasChannels(ChannelArray channelArray) {
      for (Channel channel : channelArray.getChannels() ){
        if (!hasChannel(channel)){
            return false;
        }
      }
      return true; // If we made it to here then it must've found all channels
    }
    public boolean hasChannels(String location, String chan1, String chan2, String chan3) {
      if ( hasChannel(location, chan1) && hasChannel(location, chan2) && hasChannel(location, chan3) ){
          return true;
      }
      else {
          return false;
      }
    }

    public boolean hasChannels(String location, String band) {
        if (!Channel.validLocationCode(location)) {
            //return null;
            return false;
        }
        if (!Channel.validBandCode(band.substring(0,1)) || !Channel.validInstrumentCode(band.substring(1,2)) ) {
            //return null;
            return false;
        }
    // First try kcmp = "Z", "1", "2"
        ChannelArray chanArray = new ChannelArray(location, band + "Z", band + "1", band + "2");
        if (hasChannels(chanArray)) {
            return true;
        }    
    // Then try kcmp = "Z", "N", "E"
        chanArray = new ChannelArray(location, band + "Z", band + "N", band + "E");
        if (hasChannels(chanArray)) {
            return true;
        }    
    // If we're here then we didn't find either combo --> return false
        return false;
    }



/**
 *  Not sure yet if we need this to drive addRotatedChannel below 
 *  e.g., if we need to rotate 2 channels at one time
 */
    public void addRotatedChannelMeta(String location, String channelPrefix) {  
        String northChannelName = channelPrefix + "ND"; 
        String eastChannelName  = channelPrefix + "ED";
        addRotatedChannel(location, northChannelName);
        addRotatedChannel(location,  eastChannelName);
    }

/**
 *  If handed a derivedChannelName like "LHND" (or "LHED"), try to create it from
 *   LH1 (or LH2) channel metadata.  If these aren't found, look for LHN (or LHE)
 *   instead.
 */
    public void addRotatedChannel(String location, String derivedChannelName) {

        String origChannelName = null;
        double azimuth;

        boolean found = false;

        if (derivedChannelName.contains("ND") ) {                         // Derived     Orig
            origChannelName = derivedChannelName.replace("ND", "1");       // "LHND" --> "LH1"
            if (hasChannel(new Channel(location, origChannelName)) ){
                found = true;
            }
            else {   // try ...
                origChannelName = derivedChannelName.replace("ND", "N");   // "LHND" --> "LHN"
                if (hasChannel(new Channel(location, origChannelName)) ){
                    found = true;
                }
            }
            azimuth = 0.;   // NORTH
        }
        else if (derivedChannelName.contains("ED") ) {                    // Derived     Orig
            origChannelName = derivedChannelName.replace("ED", "2");       // "LHED" --> "LH2"
            if (hasChannel(new Channel(location, origChannelName)) ){
                found = true;
            }
            else {   // try ...
                origChannelName = derivedChannelName.replace("ED", "E");   // "LHED" --> "LHE"
                if (hasChannel(new Channel(location, origChannelName)) ){
                    found = true;
                }
            }
            azimuth = 90.;  // EAST
        }
        else {
            System.out.format("== addRotatedChannel: Error -- Don't know how to make channel=[%s]\n", derivedChannelName);
            return;
        }

        if (!found) {
            System.out.format("== addRotatedChannel: Error -- StationMeta doesn't contain horizontal channels "
                            + "needed to make channel=[%s]\n", derivedChannelName);
            return;
        }

        Channel origChannel    = new Channel(location, origChannelName);
        Channel derivedChannel = new Channel(location, derivedChannelName);

        // Deep copy the orig chanMeta to the derived chanMeta and set the derived chanMeta azimuth
        ChannelMeta derivedChannelMeta = (getChanMeta(origChannel)).copy(derivedChannel);
        derivedChannelMeta.setAzimuth(azimuth);
        this.addChannel( new ChannelKey(derivedChannel), derivedChannelMeta);
    }


    public void print() {
      System.out.print(this);
      ArrayList<ChannelKey> chanKeys = new ArrayList<ChannelKey>();
      chanKeys.addAll(channels.keySet() );
      Collections.sort(chanKeys);
      for (ChannelKey chanKey : chanKeys){
        channels.get(chanKey).print();
      }
    }

    public void printStationInfo() {

      blockette50.print();

/**
      StringBuilder result = new StringBuilder();
      String NEW_LINE = System.getProperty("line.separator");
      result.append(String.format("%s\n","============================================================") );
      result.append(String.format("%10s%s\t%15s%.2f\t%15s[%s]\n","Station:",name,"Latitude:",latitude,"Meta Timestamp:", 
                                   EpochData.epochToDateString(getMetaTimestamp()) ) );
      result.append(String.format("%10s%s\t%15s%.2f\t%15s%.2f\n","Network:",network,"Longitude:",longitude, "Elevation:",elevation) );
      result.append(String.format("%10s%s\t\n", "Comment:", comment) );
      result.append(String.format("%s\n","============================================================") );
      result.append(NEW_LINE);
      System.out.print(result.toString());
**/
      //return result.toString();
    }

    @Override public String toString() {
      return network + "_" + name;
    /**
      StringBuilder result = new StringBuilder();
      String NEW_LINE = System.getProperty("line.separator");
      result.append(String.format("%10s%s\t%15s%.2f\n","Station:",name,"Latitude:",latitude) );
      result.append(String.format("%10s%s\t%15s%.2f\t%15s%.2f\n","Network:",network,"Longitude:",longitude, "Elevation:",elevation) );
      result.append(NEW_LINE);
      return result.toString();
    **/
    }

}
