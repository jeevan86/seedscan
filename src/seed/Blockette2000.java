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

/*
 * Blockette2000.java
 * * The blockette 1000 has :
 * 0  UWORD - blockette type == 2000
 * 2  UWORD - offset of next blockette within record (offset to next blockette)
 * 4  UWORD - total blockette length (fixed-section + tags + data)
 * 6  UWORD - offset to opaque data; a.k.a, opaque-header length (fixed-section + tags)
 * 8  ULONG - record number (applies to this stream, continuations will have the same value)
 * 12 UBYTE - word order (0 = little-endian, 1 = big-endian)
 * 13 UBYTE - opaque data flags 
 *               [bit 0] opaque blockette orientation
 *                   0 = record oriented
 *                   1 = stream oriented
 *               [bit 1] packaging bit  (blockette 2000s from multiple SEED data records with 
 *                                       different timetags may be backaged into a single SEED
 *                                       data record. The exact original timetag in each SEED
 *                                       Fixed Data Header is not required for each 
 *                                       blockette 2000)
 *                   0 = allowed     (timing NOT dependent on SEED time tag)
 *                   1 = disallowed  (timing IS  dependent on SEED time tag)
 *               [bits 2-3]
 *                   00 = opaque record identified by record number is completely contained
 *                   within ths opaque blockette
 *                   01 = first opaque blockette for record spanning multiple blockettes
 *                   10 = continuation blockette (2 ... N-1) of record spanning N blockettes
 *                   11 = final blockette for record spanning N blockettes (where N > 1)
 *               [bits 4-5]
 *                   00 = not file oriented
 *                   01 = first blockette of file
 *                   10 = continuation of file
 *                   11 = last blockette of file
 * 14 UBYTE - number of tags
 *
 * 15 VAR   - ASCII tags (number indicated by previous field) each terminated by ~ character
 *                        recommended fields are:
 *               a)  Record Type      - idenfier for this type of record
 *               b)  Vendor Name      - equipment vendor/manufacturer
 *               c)  Model Number     - equipment model number
 *               d)  Software Version - version number
 *               e)  Firmware Version - version number
 *
 * ?? OPAQUE - raw opaque data (opaque_data length = total_blockette_length - offset_to_opqueue_data)
 *
 * Created on September 25, 2012 @ 08:38 MDT
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package seed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/** This class represents the Blockette 2000 from the SEED standard V2.4 
 *
 * @author Joel Edwards <jdedwards@usgs.gov>
 */
public class Blockette2000 {

    //ByteBuffer buffer;
    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    // Blockette 2000 Data Fields
    private short nextBlocketteOffset = 0;
    private int recordNumber = 0;
    private ByteOrder wordOrder = ByteOrder.BIG_ENDIAN;
    private byte flags = 0;

    private Collection<String> tags = null;

    private byte[] opaqueData = null;

    private void _init(ByteOrder byteOrder)
    {
        this.byteOrder = byteOrder;
        tags = new ArrayList<String>();
    }

    /** Creates a new instance of Blockette2000
     *
     * @param byteOrder - byte order of the blockette 2000 header
     */
    public Blockette2000(ByteOrder byteOrder)
    {
        _init(byteOrder);
    }

    /** Creates a new instance of Blockette2000
     *
     * @param blockette - The raw bytes from an existing blockette 2000
     * @param byteOrder - byte order of the blockette 2000 header
     */
    public Blockette2000(Collection<ByteBuffer> blockettes, ByteOrder byteOrder)
    throws WrongBlocketteNumberException
    {
        reload(blockettes, byteOrder);
    }

    /** get the resulting blockettes
     *
     * @param blockettes - one or more type 2000 blockettes which contain an opaque data set
     * @param byteOrder  - the byte order to the blockettes header (not the opaque data itself)
     */
    public void reload(Collection<ByteBuffer> blockettes, ByteOrder byteOrder)
    throws WrongBlocketteNumberException
    {
        _init(byteOrder);
        byte[] data;
        for (ByteBuffer bb: blockettes) {
            bb.order(byteOrder);
            int blocketteType = (int) bb.getShort();
            if (blocketteType != 2000) {
                throw new WrongBlocketteNumberException("Require type 2000, received type " + blocketteType);
            }

            nextBlocketteOffset = bb.getShort();
            int totalBlocketteLength = bb.getShort();
            int offsetToOpaqueData = bb.getShort();
            recordNumber = bb.getInt();
            wordOrder = (bb.get() == 0) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
            flags = bb.get();

            int tagCount = bb.get();
            int tagsLength = offsetToOpaqueData - 15 /* Fixed part of header */ ;
            byte[] tagsBuffer = new byte[tagsLength];
            bb.get(tagsBuffer, 0, tagsLength);
            String tagsString = new String(tagsBuffer);
            String[] parts = tagsString.split("~");
            for (int i = 0; i < (parts.length - 1); i++) {
                tags.add(parts[i]);
            }
            
            int opaqueLength = totalBlocketteLength - offsetToOpaqueData;
            opaqueData = new byte[opaqueLength];
            bb.get(opaqueData, 0, opaqueLength);
        }
    }

    /** get the resulting blockettes
     *
     * @return One or more type 2000 blockettes assembled from the supplied values
     */
    public Collection<ByteBuffer> getBlockettes(int maxBlocketteLength)
    {
        ArrayList<ByteBuffer> blockettes = new ArrayList<ByteBuffer>();

        short headerLength = 15; // Length of fixed part of header
        for (String tag: tags) {
            headerLength += tag.length() + 1;
        }
        int maxDataLength = maxBlocketteLength - headerLength;
        int bytesRemaining = opaqueData.length;

        boolean first = true;
        while (bytesRemaining > 0) {
            int dataBytes = Math.min(maxDataLength, bytesRemaining);
            bytesRemaining -= dataBytes;
            
            if (first) {
            }

            int blocketteLength = headerLength + dataBytes;

            ByteBuffer bb = ByteBuffer.allocate(blocketteLength);

            bb.putShort((short)2000);
            bb.putShort(nextBlocketteOffset);
            bb.putShort((short)blocketteLength);
            bb.putShort(headerLength);
            bb.putInt(recordNumber);
            bb.put((byte)((wordOrder == ByteOrder.LITTLE_ENDIAN) ? 0 : 1));
            bb.put(flags);
            bb.put((byte)tags.size());
            for (String tag: tags) {
                bb.put((tag + "~").getBytes());
            }
            bb.put(opaqueData);

            blockettes.add(bb);
        }

        return blockettes;
    }

    public void setNextBlocketteOffset(short offset)
    {
        nextBlocketteOffset = offset;
    }

    public short getNextBlocketteOffset() {
        return nextBlocketteOffset;
    }

    public void setRecordNumber(int recordNumber)
    {
        this.recordNumber = recordNumber;
    }

    public int getRecordNumber()
    {
        return recordNumber;
    }

    public void setWordOrder(ByteOrder wordOrder)
    {
        this.wordOrder = wordOrder;
    }

    public ByteOrder getWordOrder()
    {
        return wordOrder;
    }

    public void setFlags(byte flags)
    {
        this.flags = flags;
    }

    public int getFlags()
    {
        return flags;
    }

//  Orientation Flag (Stream or Record)
    public void setStreamOriented(boolean streamOriented)
    {
        if (streamOriented) {
            flags |= 0x01;
        }
        else {
            flags &= (0xff ^ 0x01);
        }
    }

    public boolean getStreamOriented()
    {
        return ((flags & 0x01) == 0x01);
    }


//  Packaging Flag (If strict, do not re-package opaque data from MiniSEED records with different timestamps
    public void setStrictPackaging(boolean strictPackaging)
    {
        if (strictPackaging) {
            flags |= 0x02;
        }
        else {
            flags &= (0xff & 0x02);
        }
    }

    public boolean getStrictPackaging()
    {
        return ((flags & 0x02) == 0x02);
    }

// === Fragmentation Flags ===

//  Fragmentation: Single Record - all data for this record id is contained in this blockette
    public void setFragSingleRecord()
    {
        flags &= (0xff & 0x0c);
    }

    public boolean getFragSingleRecord()
    {
        return ((flags & 0x0c) == 0x00);
    }

//  Fragmentation: First Blockette - first blockette in a stream
    public void setFragFirstBlockette()
    {
        flags &= (0xff & 0x0c);
        flags |= 0x04;
    }

    public boolean getFragFirstBlockette()
    {
        return ((flags & 0x0c) == 0x04);
    }

//  Fragmentation: Continuation Blockette - one of the middle blockettes (not first or last) in a stream
    public void setFragContinuationBlockette()
    {
        flags &= (0xff & 0x0c);
        flags |= 0x0c;
    }

    public boolean getFragContinuationBlockette()
    {
        return ((flags & 0x0c) == 0x0c);
    }

//  Fragmentation: Last Blockette - last blockette in a stream
    public void setFragLastBlockette()
    {
        flags &= (0xff & 0x0c);
        flags |= 0x08;
    }

    public boolean getFragLastBlockette()
    {
        return ((flags & 0x0c) == 0x08);
    }


// === File Blockette Flags ===

//  File: Single Record - all data for this record id is contained in this blockette
    public void setFileSingleRecord()
    {
        flags &= (0xff & 0x30);
    }

    public boolean getFileSingleRecord()
    {
        return ((flags & 0x30) == 0x00);
    }

//  File: First Blockette - first blockette in a stream
    public void setFileFirstBlockette()
    {
        flags &= (0xff & 0x30);
        flags |= 0x10;
    }

    public boolean getFileFirstBlockette()
    {
        return ((flags & 0x30) == 0x10);
    }

//  File: Continuation Blockette - one of the middle blockettes (not first or last) in a stream
    public void setFileContinuationBlockette()
    {
        flags &= (0xff & 0x30);
        flags |= 0x20;
    }

    public boolean getFileContinuationBlockette()
    {
        return ((flags & 0x30) == 0x20);
    }

//  File: Last Blockette - last blockette in a stream
    public void setFileLastBlockette()
    {
        flags &= (0xff & 0x30);
        flags |= 0x30;
    }

    public boolean getFileLastBlockette()
    {
        return ((flags & 0x30) == 0x30);
    }


// === Tags ===

    public void setTags(Collection<String> tags)
    {
        this.tags = tags;
    }

    public Collection<String> getTags()
    {
        return tags;
    }



// === Opaque Data ===
    public void setOpaqueData(byte[] data, int offset, int length)
    {
        opaqueData = Arrays.copyOfRange(data, offset, offset+length);
    }

    public byte[] getOpaqueData()
    {
        return opaqueData;
    }
}
