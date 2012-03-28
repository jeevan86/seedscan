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
package asl.fmash;

import seed.Utility;

/**
 * @author fshelly
 * Class to decompress Opaque blockette 2000 seed records written to the OFC
 * channel for the Falcon system monitor.
 */
public class FmashOFC
{
    public int []   averageValues=null;
    public int []   lowValues=null;
    public int []   highValues=null;
    public long   	startTime;
    public long   	endTime;
    public String  	name;
    public boolean	isContinuation;
    public int      type;
    public int      recordLength;
    public int      dataOffset;
    public int      recordNumber;
    public int      wordOrder;
    public byte     dataFlags;
    public int      headerFieldCount;
    public String   headerFields;

    private byte [] continuationData;

    // Accessors
    public boolean  getBigEndian()    {return wordOrder != 0;}
    public String   getName()         {return name;}
    public int      getType()         {return type;}
    public int      getLength()       {return recordLength;}
    public int      getDataOffset()   {return dataOffset;}
    public int      getRecordNumber() {return recordNumber;}
    public int      getWordOrder()    {return wordOrder;}
    public byte     getDataFlags()    {return dataFlags;}
    public String   getHeaderFields() {return headerFields;}
    public boolean  getContinuation() {return isContinuation;}
    public long     getStartTime()    {return startTime;}
    public long     getEndTime()      {return endTime;}
    public int[]    getAverageData()  {return averageValues;}
    public int[]    getLowData()      {return lowValues;}
    public int[]    getHighData()     {return highValues;}

    private boolean swapBytes;

    public FmashOFC(byte[] blockette2000)
    {
        isContinuation = false;

        wordOrder = blockette2000[12];
        swapBytes = wordOrder == 0;
        type = Utility.uBytesToInt(blockette2000[0], 
                blockette2000[1], swapBytes);
        recordLength = Utility.uBytesToInt(blockette2000[4], 
                blockette2000[5], swapBytes);
        dataOffset = Utility.uBytesToInt(blockette2000[6], 
                blockette2000[7], swapBytes);
        recordNumber = Utility.bytesToInt(blockette2000[8], 
                blockette2000[9],
                blockette2000[10],
                blockette2000[11], swapBytes);

        dataFlags = blockette2000[13];
        headerFieldCount = ((int)blockette2000[14] & 0xff);
        headerFields = new String(blockette2000).substring(15,dataOffset);

        if (type == 2000)
        {
            // Assume we have an OFC opaque blockette
            // Make sure it is not a continuation blockette
            if ((dataFlags & 0x28) != 0)
            {
                // User should never call constructor on a continuation blockette
                System.err.println("FmashOFC constructor called with a continuation blockette");
                return;
            } // were any continuation bits set

            // Handle case where this starts a continuation series
            if (((dataFlags & 0x0c) == 0x04) ||
                    ((dataFlags & 0x30) == 0x10))
            {
                isContinuation = true;
                continuationData = new byte[recordLength - 15];
                System.arraycopy(blockette2000, dataOffset, 
                        continuationData, 0, recordLength-dataOffset);
                return;
            }

            // Decompress the data
            byte[] rawData = new byte[recordLength - 15];
            System.arraycopy(blockette2000, dataOffset, 
                    rawData, 0, recordLength-dataOffset);
            Fmash crack = new Fmash(rawData, wordOrder != 0);
            name = crack.getDescription();
            startTime = crack.startTime;
            endTime = crack.endTime;

            if (crack.getRowCount() > 0)
            {
                averageValues = new int[crack.getRowCount()];
                lowValues = new int[crack.getRowCount()];
                highValues = new int[crack.getRowCount()];
            }
            for (int i=0; i < crack.getRowCount(); i++)
            { 
                averageValues[i] = (int) crack.rows.get(i).m_average;
                lowValues[i] = (int) crack.rows.get(i).m_low;
                highValues[i] = (int) crack.rows.get(i).m_high;
            }

        } // We have an OFC Falcon blockette 2000

    } // constructor FmashOFC()

    // Should be called when bContinue is true
    // When bContinue goes back to false, record is done
    public boolean continued(byte[] blockette2000)
    {
        wordOrder = blockette2000[12];
        swapBytes = wordOrder == 0;
        type = Utility.uBytesToInt(blockette2000[0], 
                blockette2000[1], swapBytes);
        recordLength = Utility.uBytesToInt(blockette2000[4], 
                blockette2000[5], swapBytes);
        dataOffset = Utility.uBytesToInt(blockette2000[6], 
                blockette2000[7], swapBytes);
        recordNumber = Utility.bytesToInt(blockette2000[8], 
                blockette2000[9],
                blockette2000[10],
                blockette2000[11], swapBytes);

        dataFlags = blockette2000[13];
        headerFieldCount = ((int)blockette2000[14] & 0xff);
        headerFields = new String(blockette2000).substring(15,dataOffset);

        if (type == 2000)
        {
            // We have an OFC opaque blockette
            // Make sure it is a continuation blockette
            if (((dataFlags & 0x28) == 0) ||
                    ((dataFlags & 0x0c) == 0x04) ||
                    ((dataFlags & 0x30) == 0x10))
            {
                // User should never call continued on a non continuation blockette
                System.err.println("FmashOFC continued called with a non continuation blockette");
                isContinuation = false;
                return isContinuation;
            } // were any continuation bits set

            // Append the new data to the old
            byte[] rawData = new byte[recordLength - 15 + continuationData.length];
            System.arraycopy(blockette2000, dataOffset, 
                    rawData, continuationData.length, recordLength-dataOffset);
            System.arraycopy(continuationData, 0, rawData, 0, continuationData.length);
            continuationData = rawData;			

            // See if this is the last record in continuation stream
            if (!(((dataFlags & 0x0c) == 0x08) ||
                        ((dataFlags & 0x30) == 0x30)))
            {
                // We've appended the next round of data, done for now
                return isContinuation;
            }

            // Decompress the data
            Fmash crack = new Fmash(rawData, wordOrder != 0);
            name = crack.getDescription();
            if (crack.getRowCount() > 0)
            {
                averageValues = new int[crack.getRowCount()];
                lowValues = new int[crack.getRowCount()];
                highValues = new int[crack.getRowCount()];
            }
            for (int i=0; i < crack.getRowCount(); i++)
            { 
                averageValues[i] = (int) crack.rows.get(i).m_average;
                lowValues[i] = (int) crack.rows.get(i).m_low;
                highValues[i] = (int) crack.rows.get(i).m_high;
            }

            isContinuation = false;
        } // We have an OFC Falcon blockette 2000

        return isContinuation;
    } // continued()

} // class FmashOFC
