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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class represents the Blockette 2000 from the SEED standard V2.4 
 *
 * @author Joel Edwards <jdedwards@usgs.gov>
 */
public class Blockette2000
extends Blockette
{
	private static final Logger logger = LoggerFactory.getLogger(seed.Blockette2000.class);
    public static final short FIXED_LENGTH = 15;
    public static final ByteOrder DEFAULT_WORD_ORDER = ByteOrder.BIG_ENDIAN; 
    public static final Charset TAG_CHARSET = Charset.forName("UTF-8");

    @Override public short blocketteNumber()
    {
        return 2000;
    }

    /** Creates a new instance of Blockette2000
     *
     */
    public Blockette2000()
    throws OpaqueStateException
    {
        super(FIXED_LENGTH);

        bb.position(4);
        bb.putShort(FIXED_LENGTH); // Blockette length is header length for now
        bb.putShort(FIXED_LENGTH); // Offset is same as header length when no tags are present

        bb.position(14);
        bb.put((byte)0); // Zero tags to start

        setRecordNumber(0);
        setOpaqueByteOrder(DEFAULT_WORD_ORDER);
        setStrict(false);
        try { setOpaqueState(OpaqueState.RECORD); }
        catch (OpaqueStateException ex) { 
        	throw ex;
        }
     }

    /** Creates a new instance of Blockette2000
     *
     * @param blockette - The raw bytes from an existing blockette 2000
     */
    public Blockette2000(byte[] b)
    {
        super(b);
    }

    public short getBlocketteLength()
    {
        bb.position(4);
        return bb.getShort();
    }

    public short getHeaderLength()
    {
        bb.position(6);
        return bb.getShort();
    }

// Record number
    public void setRecordNumber(int recordNumber)
    {
        bb.position(8);
        bb.putInt(recordNumber);
    }

    public int getRecordNumber()
    {
        bb.position(8);
        return bb.getInt();
    }

// Opaque data word order
    public void setOpaqueByteOrder(ByteOrder byteOrder)
    {
        buf[12] = (byte)((byteOrder == ByteOrder.LITTLE_ENDIAN) ? 0 : 1);
    }

    public ByteOrder getOpaqueByteOrder()
    {
        return (buf[12] == 0) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }

// Data uses timestamp from the MiniSEED record
    public void setStrict(boolean strict)
    {
        if (strict) {
            buf[13] |= 0x02;
        } else {
            buf[13] &= 0xfd;
        }
    }

    public boolean getStrict()
    {
        return ((buf[13] & 0x02) == 0x02);
    }

// States drive the orientation and continuation flags
    public void setOpaqueState(OpaqueState state)
    throws OpaqueStateException
    {
        // Mask off all state bits
        buf[13] &= 0xc2;
        switch (state) {
            case RECORD       : break; // newFlags |= 0x00
            case STREAM_START : buf[13] |= 0x05; break;
            case STREAM_MID   : buf[13] |= 0x0d; break;
            case STREAM_END   : buf[13] |= 0x09; break;
            case FILE_START   : buf[13] |= 0x10; break;
            case FILE_MID     : buf[13] |= 0x20; break;
            case FILE_END     : buf[13] |= 0x30; break;
            default   : 
            	throw new OpaqueStateException(String.format("State value %s", state));
        }
    }

    public OpaqueState getOpaqueState()
    throws OpaqueStateException
    {
        OpaqueState state;
        switch (buf[13] & 0x3d) {
            case 0x00 : state = OpaqueState.RECORD; break;
            case 0x05 : state = OpaqueState.STREAM_START; break;
            case 0x0d : state = OpaqueState.STREAM_MID; break;
            case 0x09 : state = OpaqueState.STREAM_END; break;
            case 0x10 : state = OpaqueState.FILE_START; break;
            case 0x20 : state = OpaqueState.FILE_MID; break;
            case 0x30 : state = OpaqueState.FILE_END; break;
            default   : 
            	throw new OpaqueStateException(String.format("State flags 0x%02x", buf[13] & 0x3d));
        }
        return state;
    }

// === Tags ===
    public void setTags(Collection<String> tags)
    {
        short opaqueLength = getOpaqueLength();
        short oldBlocketteLength = getBlocketteLength();
        short oldHeaderLength = getHeaderLength();
        short oldTagsLength = (short)(oldHeaderLength - FIXED_LENGTH);

        String tagString = tagsToTagString(tags);
        byte[] newTagsBuffer = tagStringToByteArray(tagString);

        short newTagsLength = (short)newTagsBuffer.length;
        short adjustment = (short)(newTagsLength - oldTagsLength);
        if (adjustment != 0) {
            short newBlocketteLength = (short)(oldBlocketteLength + adjustment);
            short newHeaderLength = (short)(oldHeaderLength + adjustment);

            // Move the opaque data so there is just enough room for the new
            // tags between it and the header
            reallocateBuffer(newBlocketteLength);
            System.arraycopy(buf, oldHeaderLength, buf, newHeaderLength, opaqueLength);

            // Update the length of the blockette
            bb.position(4);
            bb.putShort(newBlocketteLength);

            // Update the length of the header
            bb.position(6);
            bb.putShort(newHeaderLength);
        }
        System.arraycopy(newTagsBuffer, 0, buf, FIXED_LENGTH, newTagsLength);

        // Update the number of tags in the blockette
        bb.position(14);
        bb.put((byte)tags.size());
    }

    public Collection<String> getTags()
    {
        ArrayList<String> tags = new ArrayList<String>(getTagCount());
        String[] parts = getTagString().split("~");
        for (int i = 0; i < getTagCount(); i++) {
            tags.add(parts[i]);
        }
        return tags;
    }

    public String getTagString()
    {
        return new String(buf, FIXED_LENGTH, getTagsLength(), TAG_CHARSET);
    }

    public byte getTagCount()
    {
        return buf[14];
    }

    public short getTagsLength()
    {
        return (short)(getHeaderLength() - FIXED_LENGTH);
    }

    public static String tagsToTagString(Collection<String> tags)
    {
        String tagString = "";
        for (String tag: tags) {
            tagString += tag + "~";
        }
        return tagString;
    }

    public static byte[] tagStringToByteArray(String tagString)
    {
        return tagString.getBytes(TAG_CHARSET);
    }

    public static byte[] tagsToByteArray(Collection<String> tags)
    {
        return tagStringToByteArray(tagsToTagString(tags));
    }

// === Opaque Data ===
    public void setOpaqueData(byte[] data, int offset, int length)
    {
        short headerLength = getHeaderLength();
        short oldBlocketteLength = getBlocketteLength();
        short oldOpaqueLength = (short)(oldBlocketteLength - headerLength);
        short adjustment = (short)(length - oldOpaqueLength);

        if (adjustment != 0) {
            short newBlocketteLength = (short)(oldBlocketteLength + adjustment);
            bb.position(4);
            bb.putShort(newBlocketteLength);
            reallocateBuffer(newBlocketteLength);
        }
        System.arraycopy(data, offset, buf, headerLength, length);
    }

    public byte[] getOpaqueData()
    {
        return Arrays.copyOfRange(buf, getHeaderLength(), getOpaqueLength());
    }

    public short getOpaqueLength()
    {
        return (short)(getBlocketteLength() - getHeaderLength());
    }
}
