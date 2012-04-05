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
package asl.seedscan;

import java.util.ArrayList;

public class Scan
{
    private String pathPattern;
    private ScanFrequency scanFrequency;
    private ArrayList<ScanOperation> scanOperations;
    private int startDepth;
    private int scanDepth;

    public Scan()
    {
    }

 // path pattern
    public void setPathPattern(String pathPattern)
    {
        this.pathPattern = pathPattern;
    }

    public String getPathPattern()
    {
        return pathPattern;
    }

 // scan frequency
    public void setScanFrequency(ScanFrequency scanFrequency)
    {
        this.scanFrequency = scanFrequency;
    }

    public ScanFrequency getScanFrequency()
    {
        return scanFrequency;
    }

 // scan operations
    public void setScanOperations(ArrayList<ScanOperation> scanOperations)
    {
        this.scanOperations = scanOperations;
    }

    public ArrayList<ScanOperation> getScanOperations()
    {
        return scanOperations;
    }

 // start depth
    public void setStartDepth(int startDepth)
    {
        this.startDepth = startDepth;
    }

    public int getStartDepth()
    {
        return startDepth;
    }

 // scan depth
   public void setScanDepth(int scanDepth)
   {
       this.scanDepth = scanDepth;
   }

   public int getScanDepth()
   {
       return scanDepth;
   }
}
