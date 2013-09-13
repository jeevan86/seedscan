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

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Hashtable;

public class StageData
{
    private static final Logger logger = Logger.getLogger("asl.metadata.StageData");

    private int stageID;
    private Hashtable<Integer, Blockette> blockettes;

    // Constructor(s)
    public StageData(int stageID)
    {
        this.stageID = stageID;
        blockettes = new Hashtable<Integer, Blockette>();
    }

    // stageID
    public int getStageID()
    {
        return stageID;
    }

    // blockettes
    public int addBlockette(Blockette blockette)
    throws DuplicateBlocketteException
    {
        int blocketteNumber = blockette.getNumber();
        if (blockettes.containsKey(blocketteNumber)) {
// MTH: Most of the old IU_ANMO.--.??? channels have duplicate stage 3 blockette 054's --> IS THIS AN ERROR ?
//System.out.format("**** addBlockette() BLOCKETTES already CONTAINS blockette Number %d\n", blocketteNumber);
// MTH: This throw is causing the reading of file zfoo (=rdseed -s) to cease:
            //throw new DuplicateBlocketteException();
        }
        blockettes.put(blocketteNumber, blockette);
        return blocketteNumber;
    }

    public boolean hasBlockette(int blocketteNumber)
    {
        return blockettes.containsKey(blocketteNumber);
    }

    public Blockette getBlockette(int blocketteNumber)
    {
        return blockettes.get(blocketteNumber);
    }

    public Hashtable<Integer, Blockette> getBlockettes()
    {
        return blockettes;
    }
}

