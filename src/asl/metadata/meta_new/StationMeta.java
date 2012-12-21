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
import java.util.Hashtable;
import java.util.Collections;
import java.util.Calendar;
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
{
    private String network = null;
    private String name = null;
    private String comment = null;
    private double latitude;
    private double longitude;
    private double elevation;
    private Hashtable<ChannelKey,ChannelMeta> channels;
    private Calendar metaTimestamp = null;

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
        this.metaTimestamp = timestamp;
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
        if (! (latitude <= 90. && latitude >= -90) ) {
           throw new RuntimeException("Error: station latitude must be: -90 <= val <= 90");
        }
        if (! (longitude <= 180. && longitude >= -180) ) {
           throw new RuntimeException("Error: station longitude must be: -180 <= val <= 180");
        }
        this.latitude  = latitude;
        this.longitude = longitude;
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
        return metaTimestamp;
    }

//  Look for particular channelMeta (e.g., "00" "VHZ") in channels array.
//    Return it if found, else return null

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

    public Hashtable<ChannelKey,ChannelMeta> getChannelHashTable() {
      return channels;
    }

    public Boolean hasChannel(ChannelKey channelKey) {
      return channels.containsKey(channelKey);
    }
    public Boolean hasChannel(Channel channel) {
      return hasChannel(new ChannelKey(channel.getLocation(), channel.getChannel()) );
    }
    public Boolean hasChannel(String location, String name) {
      return hasChannel(new ChannelKey(location, name) );
    }

    public Boolean hasChannels(ChannelArray channelArray) {
      for (Channel channel : channelArray.getChannels() ){
        if (!hasChannel(channel)){
            return false;
        }
      }
      return true; // If we made it to here then it must've found all channels
    }
    public Boolean hasChannels(String location, String chan1, String chan2, String chan3) {
      if ( hasChannel(location, chan1) && hasChannel(location, chan2) && hasChannel(location, chan3) ){
          return true;
      }
      else {
          return false;
      }
    }

/**
 *  If we're here, then MetricData.valueDigestChanged() must've been handed a derived
 *   channel (e.g., 00-LHND or 10-BHED) that we do not yet have metadata for --> make it
 */
    public void addRotatedChannel(Channel derivedChannel) {

        if (derivedChannel.getChannel().contains("HND") || derivedChannel.getChannel().contains("HED")) {
            // These are channels we know how to rotate
        }
        else {
            // Unknown channel ?
            System.out.format("== addRotatedChannel: Error -- Don't know how to make channel=[%s]\n", derivedChannel);
            return;
        }
        String location    = derivedChannel.getLocation();
        String channelBand = derivedChannel.getChannel().substring(0,1);
        String chan1 = String.format("%sH1", channelBand);
        String chan2 = String.format("%sH2", channelBand);

    // Use the channelBand to decide which horizontal channels to use for rotation
        Channel channel1 = new Channel(location, chan1); 
        Channel channel2 = new Channel(location, chan2); 

        if (hasChannel(channel1)==false || hasChannel(channel2)==false){
            System.out.format("== addRotatedChannel: Error -- Request for rotated channel=[%s] but can't find chan1=[%s] and/or chan2=[%s]\n",
                               derivedChannel, channel1, channel2);
            return;
        }

        Channel channelN = new Channel(location, channel1.getChannel().replace("H1", "HND") );
        Channel channelE = new Channel(location, channel1.getChannel().replace("H1", "HED") );

System.out.format("== addRotatedChannels: channelN=%s & channelE=%s\n", channelN, channelE);

        // Deep copy the ?H1 chanMeta to ?HND and set the azimuth to 0 == NORTH
        ChannelMeta chanMeta1 = getChanMeta(channel1);
        ChannelMeta chanMetaN = chanMeta1.copy(channelN);
        chanMetaN.setAzimuth(0.0);

        // Deep copy the ?H1 chanMeta to ?HED and set the azimuth to 90 == EAST
        ChannelMeta chanMetaE = chanMeta1.copy(channelE);
        chanMetaE.setAzimuth(90.0);

        this.addChannel( new ChannelKey(channelN), chanMetaN);
        this.addChannel( new ChannelKey(channelE), chanMetaE);

        chanMetaN.print();
        chanMetaE.print();
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
