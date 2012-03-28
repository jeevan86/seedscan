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
import java.util.Hashtable;
import java.util.logging.Logger;

public class SeedVolume
{
    private static final Logger logger = Logger.getLogger("asl.metadata.SeedVolume");

    private Blockette volumeInfo = null;
    private ArrayList<Blockette> stationLocators;
    private Hashtable<String, StationData> stations;

    public SeedVolume()
    {
        stations = new Hashtable<String, StationData>();
        stationLocators = new ArrayList<Blockette>();
    }

    public SeedVolume(Blockette volumeInfo)
    {
        this.volumeInfo = volumeInfo;
        stations = new Hashtable<String, StationData>();
        stationLocators = new ArrayList<Blockette>();
    }

    public void addStation(String stationID, StationData data)
    {
        stations.put(stationID, data);
    }

    public boolean hasStation(String stationID)
    {
        return stations.containsKey(stationID);
    }

    public StationData getStation(String stationID)
    {
        return stations.get(stationID);
    }

    public void setVolumeInfo(Blockette volumeInfo)
    {
        this.volumeInfo = volumeInfo;
    }

    public Blockette getVolumeInfo()
    {
        return this.volumeInfo;
    }

    public void addStationLocator(Blockette stationLocator)
    {
        stationLocators.add(stationLocator);
    }

    public ArrayList<Blockette> getStationLocators()
    {
        return stationLocators;
    }
}

