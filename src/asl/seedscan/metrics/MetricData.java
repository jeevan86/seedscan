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
package asl.seedscan.metrics;

import asl.seedscan.Channel;
import asl.metadata.meta_new.StationMeta;
import asl.seedsplitter.DataSet;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import java.util.logging.Logger;

public class MetricData
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.MetricData");

    private Hashtable<String, ArrayList<DataSet>> data;
    //private Hashtable<String, StationData> metadata;
    private StationMeta metadata;
    private Hashtable<String, String> synthetics;

  //constructor(s)
    public MetricData(Hashtable<String, ArrayList<DataSet>> data, 
                      StationMeta metadata,
                      Hashtable<String, String> synthetics)
    {
        this.data = data;
        this.metadata = metadata;
        this.synthetics = synthetics;
    }

    public MetricData(Hashtable<String, ArrayList<DataSet>> data, 
                      StationMeta metadata)
    {
        this.data = data;
        this.metadata = metadata;
    }

    public MetricData(Hashtable<String, ArrayList<DataSet>> data)
    {
        this.data = data;
    }

    public StationMeta getMetaData()
    {
        return metadata;
    }

  // getChannelData()
  // Returns ArrayList<DataSet> = All DataSets for a given channel (e.g., "00-BHZ")

    public ArrayList<DataSet> getChannelData(String location, String name)
    {
        String locationName = location + "-" + name;
        Set<String> keys = data.keySet();
        for (String key : keys){          // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
           if (key.contains(locationName) ){
            //System.out.format(" key=%s contains locationName=%s\n", key, locationName);
              return data.get(key);       // return ArrayList<DataSet>
           }
        }
        return null;           
    }

    public ArrayList<DataSet> getChannelData(Channel channel)
    {
        return getChannelData(channel.getLocation(), channel.getChannel() );           
    }


}
