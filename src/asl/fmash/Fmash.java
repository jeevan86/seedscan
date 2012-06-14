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

import java.util.ArrayList;

import seed.Utility;

// Version/Type : uint16_t ---- (2 bytes)        : 0
// Start Time   : time_t ------ (4 bytes)        : 2
// End Time     : time_t ------ (4 bytes)        : 6
// Channel ID   : uint16_t ---- (2 bytes)        : 10
// Num Rows     : uint16_t ---- (2 bytes)        : 12
// Data Bitmap  : uint8_t[24] - (24 bytes)       : 14
// Description  : pascal str. - (3-10 bytes)     : 38
// First Avg    : varint ------ (1 - 5 bytes)
// First Hi     : varint ------ (1 - 5 bytes)
// First Low    : varint ------ (1 - 5 bytes)
// Data Bytes   : varint(s) --- (0+ bytes)
// Last Avg     : varint ------ (1 - 5 bytes)
// Last Hi      : varint ------ (1 - 5 bytes)
// Last Low     : varint ------ (1 - 5 bytes)

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 * Class to decode data recorded from the Falcon monitoring system at GSN
 * encoded in fmash format.
 */
public class Fmash
{
    public static final byte NO_CHANGE = 0x00; // No deltas
    public static final byte CHANGES_1 = 0x01; // 1 delta
    public static final byte CHANGES_2 = 0x02; // 2 deltas
    public static final byte CHANGES_3 = 0x03; // 3 deltas
    public static final byte NO_DATA   = 0x07; // No data

    public static final byte TYPE_AVERAGE = 1;
    public static final byte TYPE_HIGH    = 2;
    public static final byte TYPE_LOW     = 3;

    public static final byte MAX_DESCRIPTION =  8;
    public static final byte ROW_MAP_SIZE = 24;

    public static final byte MAX_VERSION = 1;
    public static final byte TM_MINUTE = 60;

    public int        version       = 0;
    public boolean    populated     = false;
    public long       startTime     = 0x7fffffff;
    public long       endTime       = 0x7fffffff;
    public int        rowCount      = 0;
    public int        channelId     = 0;
    public String     description   = new String();
    public ArrayList<FmashRow> rows = new ArrayList<FmashRow>();

    private byte[]    dataBitmap        = new byte[24];
    private byte[]    maps              = null;
    private byte[]    rawMsh            = null;
    private byte[]    descriptionBytes  = null;
    private int       totalLength       = 0;
    private int       dataOffset        = 0;
    private long      firstAverage      = 0;
    private long      firstHigh         = 0;
    private long      firstLow          = 0;
    private long      lastAverage       = 0;
    private long      lastHigh          = 0;
    private long      lastLow           = 0;

    private boolean   swapBytes;

    public Fmash(byte[] rawMsh, boolean bigEndian)
    {
        this.rawMsh = rawMsh;
        totalLength = rawMsh.length;
        swapBytes   = !bigEndian;

        /* Allow debug print of raw hex values
           for (int i=0; i < rawMsh.length; i += 16)
           {
           System.err.printf("%04x: ", i);
           for (int j=i; j < i+16 && j < rawMsh.length; j++)
           {
           System.err.printf(" %02x", rawMsh[j] & 0xff);
           }
           System.err.println();
           }
         */
        try { 
            checkVersion();
            populateMetadata();
            maps = new byte[rowCount];
            populateRows(); 
            populated = true;
        } catch (FmashException e) {
            e.printStackTrace();
            System.err.println("Error cracking open OFC opaque block");
        }
    } // Constructor

    public int      getRowCount()       {return rowCount;}
    public String   getDescription()    {return description;}
    public int      getTotalLength()    {return totalLength;};

    private void checkVersion()
        throws FmashVersionException
    {
        version = Utility.uBytesToInt(rawMsh[0], rawMsh[1], swapBytes);
        if ((version & 0x7fff) > MAX_VERSION) 
        {
            throw new FmashVersionException();
        }
    } // checkVersion()

    private void populateMetadata()
        throws FmashDescriptionLengthException
    {
        int desc_len;

        startTime = Utility.uBytesToLong(rawMsh[2], rawMsh[3],
                rawMsh[4], rawMsh[5], swapBytes);
        endTime = Utility.uBytesToLong(rawMsh[6], rawMsh[7],
                rawMsh[8], rawMsh[9], swapBytes);
        channelId = Utility.bytesToInt(rawMsh[10], rawMsh[11], swapBytes);
        rowCount = Utility.uBytesToInt(rawMsh[12], rawMsh[13], swapBytes);
        for (int i=0; i < 24; i++) {
            dataBitmap[i] = rawMsh[14+i];
        }
        desc_len = (int)(rawMsh[38]);

        if ((desc_len < 1) || (desc_len > 8)) {
            throw new FmashDescriptionLengthException();
        }

        descriptionBytes = new byte[desc_len];

        for (int i=0; i < desc_len; i++) {
            descriptionBytes[i] = rawMsh[39+i];
        }
        description = new String(descriptionBytes);

        // desc_offset + length_byte + description + null_byte
        dataOffset = 38 + 1 + desc_len + 1;
    } // populateMetadata()

    private void populateRows() 
        throws FmashRowException
    {
        long currentTime;
        byte item;
        byte map;
        FmashRow row = null;

        try {
            firstAverage = getVarint();
            firstHigh    = getVarint();
            firstLow     = getVarint();
            lastAverage  = firstAverage;
            lastHigh     = firstHigh;
            lastLow      = firstLow;

            getMaps();

            currentTime = (long)startTime;

            for (int i=0; i < maps.length; i++) {
                map = maps[i];
                switch (map) {
                    case CHANGES_1:
                    case CHANGES_2:
                    case CHANGES_3:
                        item = rawMsh[dataOffset];
                        if ((item & 0x3) == TYPE_AVERAGE) {
                            lastAverage += getVarint();
                        }
                        else if ((item & 0x3) == TYPE_HIGH) {
                            lastHigh += getVarint();
                        }
                        else if ((item & 0x3) == TYPE_LOW) {
                            lastLow += getVarint();
                        }
                        break;
                } // switch statement
                row = new FmashRow(currentTime, lastAverage, lastHigh, lastLow);
                rows.add(row);
                currentTime += TM_MINUTE;
            } // for loop
            lastAverage = getVarint();
            lastHigh    = getVarint();
            lastLow     = getVarint();
        } catch (FmashVarintException e)
        {
            e.printStackTrace();
            throw new FmashRowException();
        }
    } // populateRows()

    private long getVarint()
        throws FmashVarintException
    {
        int       result     = 0;
        byte      value      = 0;

        value = rawMsh[dataOffset++];
        result = (value >> 2) & 0x1f;
        if ((value & 0x80) != 0x80)
        {
            if ((value & 0x40) == 0x40)
                return result | 0xffffffe0;
            else
                return result;
        }

        value = rawMsh[dataOffset++];
        result = result | ((value & 0x7f) << 5);
        if ((value & 0x80) != 0x80)
        {
            if ((value & 0x40) == 0x40)
                return result | 0xfffff000;
            else
                return result;
        }

        value = rawMsh[dataOffset++];
        result = result | ((value & 0x7f) << 12);
        if ((value & 0x80) != 0x80)
        {
            if ((value & 0x40) == 0x40)
                return result | 0xfff80000;
            else
                return result;
        }

        value = rawMsh[dataOffset++];
        result = result | ((value & 0x7f) << 19);
        if ((value & 0x80) != 0x80)
        {
            if ((value & 0x40) == 0x40)
                return result | 0xfc000000;
            else
                return result;
        }

        value = rawMsh[dataOffset++];
        result = result | ((value & 0x3f) << 26);

        return result;
    } // getVarint()

    private void getMaps()
    {
        int pos = 0;
        int rotation = 0;
        for (int i=0; i < rowCount; i++) {
            pos = (i / 8) * 3;
            rotation = i % 8;
            switch (rotation) {
                case 0: maps[i] =  (byte)((dataBitmap[pos]       ) & 0x07); break;
                case 1: maps[i] =  (byte)((dataBitmap[pos]   >> 3) & 0x07); break;
                case 2: maps[i] = (byte)(((dataBitmap[pos]   >> 6) & 0x03)|
                                   (byte)((dataBitmap[pos+1] << 2) & 0x04)); break;
                case 3: maps[i] =  (byte)((dataBitmap[pos+1] >> 1) & 0x07); break;
                case 4: maps[i] =  (byte)((dataBitmap[pos+1] >> 4) & 0x07); break;
                case 5: maps[i] = (byte)(((dataBitmap[pos+1] >> 7) & 0x01)| 
                                   (byte)((dataBitmap[pos+2] << 1) & 0x06)); break;
                case 6: maps[i] =  (byte)((dataBitmap[pos+2] >> 2) & 0x07); break;
                case 7: maps[i] =  (byte)((dataBitmap[pos+2] >> 5) & 0x07); break;
            }
        } // for loop
    } // getMaps()

} // class Fmash

