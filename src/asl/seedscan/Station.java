/*
 * Copyright 2011, United States Geological Survey or
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

package asl.seedscan;


public class Station
{
    private String network = null;
    private String station = null;

    public Station(String network, String station)
    {
        setNetwork(network);
        setStation(station);
    }

    public void setNetwork(String network) {
        if (network != null) {
            if (network.length() > 2) {
                throw new RuntimeException("network name is too long");
            }
            else if (network.length() == 0) {
                this.network = null;
            } 
            else {
                this.network = network;
            }
        }
    }

    public void setStation(String station) {
        if (station == null) {
            throw new RuntimeException("station cannot be null");
        }
        if (station.length() < 1) {
            throw new RuntimeException("station name is too short");
        }
        if (station.length() > 5) {
            throw new RuntimeException("station name is too long");
        }
        this.station = station;
    }


    public String getNetwork() {
        return network;
    }

    public String getStation() {
        return station;
    }
}
