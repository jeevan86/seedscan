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
package asl.seedscan.metadata;

/* 
 * Follwing is a depiction of the structure generated as a result of 
 * parsing the output of `evalresp -s`
 * 
 *
 * Dataless
 *  |
 *   - volumeInfo (Blockette)
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
 *           - channels (Hashtable<String, StationData>)
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
 *                               - format (Hashtable<Integer, StageData>)
 *                                  |
 *                                   - stageIndex (Integer)
 *                                      :
 *                                     data (StageData)
 *
 *
 */

import java.util.Hashtable;

public class SeedVolume
{
    private Blockette volumeInfo;
    private Hashtable<String, StationData> stations;

    public SeedVolume(Blockette volumeInfo)
    {
        this.volumeInfo = volumeInfo;
        this.stations = new Hashtable<String, StationData>();
    }

    public void addStation(String stationID, StationData data)
    {
        stations.put(stationID, data);
    }
}

