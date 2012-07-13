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
import asl.seedscan.Channel;
import asl.seedscan.metrics.ChannelArray;

public class StationMeta
{
    private String network = null;
    private String name = null;
    private String comment = null;
    private double latitude;
    private double longitude;
    private double elevation;
    //private ArrayList<ChannelMeta> channels;
    private Hashtable<ChannelKey,ChannelMeta> channels;
//  The loaded StationMeta and all of its ChannelMeta is expected to be valid
//    for the 24-hour period beginning at metaTimestamp
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

    public StationMeta(Station station)
    {
        this.name    = station.getStation();
        this.network = station.getNetwork();
        channels = new Hashtable<ChannelKey,ChannelMeta>();
    }

// Need to check that Station name has <= 5 chars and network == 2 chars 
    public StationMeta(String name, String network)
    {
        this.name    = name;
        this.network = network;
        channels = new Hashtable<ChannelKey,ChannelMeta>();
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
      String location = channel.getLocation();
      String name = channel.getChannel();
      ChannelKey chanKey = new ChannelKey(location, name);
      return getChanMeta(chanKey);
    }

    public ChannelMeta getChanMeta(String location, String name) {
      ChannelKey chanKey = new ChannelKey(location, name);
      return getChanMeta(chanKey);
    }

    public Boolean hasChannels(ChannelArray channelArray) {
      for (Channel channel : channelArray.getChannels() ){
        if (! hasChannel(channel.getLocation(), channel.getChannel()) ){
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

    public Boolean hasChannel(String location, String name) {
      ChannelKey chanKey = new ChannelKey(location, name);
      return hasChannel(chanKey);
    }

    public Boolean hasChannel(ChannelKey channelKey) {
      return channels.containsKey(channelKey);
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
      StringBuilder result = new StringBuilder();
      String NEW_LINE = System.getProperty("line.separator");
      result.append(String.format("%10s%s\t%15s%.2f\n","Station:",name,"Latitude:",latitude) );
      result.append(String.format("%10s%s\t%15s%.2f\t%15s%.2f\n","Network:",network,"Longitude:",longitude, "Elevation:",elevation) );
      result.append(NEW_LINE);
      return result.toString();
    }

}
