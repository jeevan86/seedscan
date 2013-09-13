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

public class StationKey
extends Key
implements Comparable<StationKey>
{
    public static final int STATION_EPOCH_BLOCKETTE_NUMBER = 50;

    private String network = null;
    private String name = null;

    // constructor(s)
    public StationKey(Blockette blockette)
    throws WrongBlocketteException
    {
        if (blockette.getNumber() != STATION_EPOCH_BLOCKETTE_NUMBER) {
            throw new WrongBlocketteException();
        }
        network = blockette.getFieldValue(16,0);
        name = blockette.getFieldValue(3,0);
    }
    // MTH:
    public StationKey(Station station)
    {
        this.network = station.getNetwork();
        this.name    = station.getStation();
    }

    // identifiers
    public String getNetwork()
    {
        return new String(network);
    }

    public String getName()
    {
        return new String(name);
    }

    public String toString()
    {
        return new String(network+ "_" +name);
    }

    @Override public int compareTo( StationKey stnKey ) {
      String thisCombo = getNetwork() + getName();
      String thatCombo = stnKey.getNetwork() + stnKey.getName();
      return thisCombo.compareTo(thatCombo);
   }

}

