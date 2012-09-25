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
                 [bit 0] opaque blockette orientation
                     0 = record oriented
                     1 = stream oriented
                 [bit 1] packaging bit  (blockette 2000s from multiple SEED data records with 
                                         different timetags may be backaged into a single SEED
                                         data record. The exact original timetag in each SEED
                                         Fixed Data Header is not required for each 
                                         blockette 2000)
                     0 = allowed     (timing NOT dependent on SEED time tag)
                     1 = disallowed  (timing IS  dependent on SEED time tag)
                 [bits 2-3]
                     00 = opaque record identified by record number is completely contained
                     within ths opaque blockette
                     01 = first opaque blockette for record spanning multiple blockettes
                     10 = continuation blockette (2 ... N-1) of record spanning N blockettes
                     11 = final blockette for record spanning N blockettes (where N > 1)
                 [bits 4-5]
                     00 = not file oriented
                     01 = first blockette of file
                     10 = continuation of file
                     11 = last blockette of file
 * 14 UBYTE - number of tags

 * 15 VAR   - ASCII tags (number indicated by previous field) each terminated by ~ character
                          recommended fields are:
                 a)  Record Type      - idenfier for this type of record
                 b)  Vendor Name      - equipment vendor/manufacturer
                 c)  Model Number     - equipment model number
                 d)  Software Version - version number
                 e)  Firmware Version - version number

 * ?? OPAQUE - raw opaque data (opaque_data length = total_blockette_length - offset_to_opqueue_data)

 * Created on September 25, 2012 @ 08:38 MDT
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package seed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;

/** This class represents the Blockette 2000 from the SEED standard V2.4 
 * Blockette2000.java
 * * The blockette 2000 has :
 * 0  i*2 blockette type  is 2000
 * 2  i*2 Next Blockette byte number (offset to next blockette)
 * 4  Encoding format - 10 = steimI, 11=steim2, 
 * 5  word order 0 little endian, 1=big endian
 * 6  record length power (2^b(6) = length) 
 * 7 reserved

 *
 * @author Joel Edwards <jdedwards@usgs.gov>
 */
public class Blockette2000 {

    //ByteBuffer buffer;
    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    // Blockette 2000 Data Fields
    private int nextBlocketteOffset;
    private int recordNumber;
    private ByteOrder wordOrder;
    private Collection<String> tags;

    private int flags;

    private byte[] opaqueBuffer;
    private int opaqueOffset;
    private int opaqueLength;

    private void _init(ByteOrder byteOrder)
    {
        this.byteOrder = byteOrder;
        tags = new ArrayList<String>();
    }

    /** Creates a new instance of Blockette2000
     *
     * @param bigEndian - true if word order of the blockettes fields is big-endian, otherwise little-endian
     */
    public Blockette2000(ByteOrder byteOrder)
    {
        _init(byteOrder);
    }

    /** Creates a new instance of Blockette2000
     *
     * @param blockette - The raw bytes from an existing blockette 2000
     * @param bigEndian - true if word order of the blockettes fields is big-endian, otherwise little-endian
     */
    public Blockette2000(byte[] blockette, ByteOrder byteOrder)
    throws WrongBlocketteNumberException
    {
        reload(blockette, byteOrder);
    }

    public void reload(byte[] blockette, ByteOrder byteOrder)
    throws WrongBlocketteNumberException
    {
        _init(byteOrder);
        ByteBuffer bb = ByteBuffer.wrap(blockette);
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
        
        opaqueOffset = 0;
        opaqueLength = totalBlocketteLength - offsetToOpaqueData;
        opaqueBuffer = new byte[opaqueLength];
        bb.get(opaqueBuffer, opaqueOffset, opaqueLength);
    }

    /** get the raw bytes
     *
     * @return The raw bytes in this 2000 block
     */
    public byte[] getBytes()
    {
        int blocketteLength = 0;
        ByteBuffer bb = ByteBuffer.allocate(blocketteLength);
        return bb.array();
    }

    public void setNextBlocketteOffset(int offset)
    {
        nextBlocketteOffset = offset;
    }

    public int getNextBlocketteOffset() {
        return nextBlocketteOffset;
    }

    public void setWordOrder(ByteOrder wordOrder)
    {
        this.wordOrder = wordOrder;
    }

    public ByteOrder getWordOrder()
    {
        return wordOrder;
    }

    public void setFlags(int flags)
    {
        this.flags = flags;
    }

    public int getFlags()
    {
        return flags;
    }

    public void setFlagStreamOriented(boolean streamOriented)
    {
        if (streamOriented) {
            flags |= 0x01;
        }
        else {
            flags &= (0xff ^ 0x01);
        }
    }

    public boolean getFlagStreamOriented()
    {
        return ((flags & 0x01) == 0x01);
    }

    public void setFlagStrictPackaging(boolean strictPackaging)
    {
        if (strictPackaging) {
            flags |= 0x02;
        }
        else {
            flags &= (0xff & 0x02);
        }
    }


    public boolean getFlagStrictPackaging()
    {
        return ((flags & 0x02) == 0x02);
    }

    public void setTags(Collection<String> tags)
    {
        this.tags = tags;
    }

    public Collection<String> getTags()
    {
        return tags;
    }

}
