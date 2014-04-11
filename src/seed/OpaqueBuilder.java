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

/*
 * OpaqueBuilder.java
 *
 * Created on October 4, 2012 @ 06:26 MDT
 *
 */

package seed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class represents the Blockette 2000 from the SEED standard V2.4 
 *
 * @author Joel Edwards <jdedwards@usgs.gov>
 */
public class OpaqueBuilder
{
	private static final Logger logger = LoggerFactory.getLogger(seed.OpaqueBuilder.class);
    private boolean finished = false;

    private OpaqueContext context; 
    private Collection<String> tags;
    private ByteOrder blocketteByteOrder;
    private ByteOrder opaqueByteOrder;
    private short maxBlocketteLength;
    private short maxOpaqueLength;
    private boolean fileOriented;
    private boolean strictPackaging;

    private ArrayList<Blockette2000> completed;
    private byte[] buffer;
    private short  bytesUsed;

    /** Creates a new instance of OpaqueBuilder
     *
     * @param byteOrder - byte order expected for all blockettes
     */
    public OpaqueBuilder(Collection<String> tags,
                         ByteOrder blocketteByteOrder,
                         ByteOrder opaqueByteOrder,
                         short maxBlocketteLength,
                         boolean fileOriented,
                         boolean strictPackaging)
    {
        completed = new ArrayList<Blockette2000>();

        String tagString = Blockette2000.tagsToTagString(tags);
        byte[] tagBuffer = Blockette2000.tagStringToByteArray(tagString);

        this.tags = tags;
        this.blocketteByteOrder = blocketteByteOrder;
        this.opaqueByteOrder = opaqueByteOrder;
        this.maxBlocketteLength = maxBlocketteLength;
        maxOpaqueLength = (short)(maxBlocketteLength - Blockette2000.FIXED_LENGTH - tagBuffer.length);
        this.fileOriented = fileOriented;
        this.strictPackaging = strictPackaging;

        context = new OpaqueContext(tagString);
        buffer = new byte[maxOpaqueLength];
        bytesUsed = 0;
    }

    // Add new data, creating new Blockettes as necessary
    public void update(byte[] opaqueData, int offset, int length)
    throws BuilderFinishedException,
           OpaqueStateException,
           OpaqueStateTransitionException
    {
        if (finished) {
            //throw new BuilderFinishedException("This OpaqueBuilder's finish() method has already been called.");
        	BuilderFinishedException e = new BuilderFinishedException("This OpaqueBuilder's finish() method has already been called.");
        	logger.error("OpaqueBuilder BuilderFinishedException:", e);
        	return;
        }
        if (length == 0) {
            return;
        }

        int remaining = length;
        int srcOffset = offset;

        while (remaining > (buffer.length - bytesUsed)) {
            int copyLength = buffer.length - bytesUsed;
            System.arraycopy(opaqueData, srcOffset, buffer, bytesUsed, copyLength);
            srcOffset += copyLength;
            remaining -= copyLength;
            bytesUsed = 0;

            // When we are ready to construct a Blockette2000
            OpaqueState lastState = context.getState();
            int recordNumber = context.getRecordNumber() + 1;

            OpaqueState state;
            switch (lastState) {
                case INIT         : state = fileOriented ? OpaqueState.FILE_START : OpaqueState.STREAM_START; break;
                case STREAM_START : 
                case STREAM_MID   : state = OpaqueState.STREAM_MID; break;
                case FILE_START   : 
                case FILE_MID     : state = OpaqueState.FILE_MID; break;
                default : 
                	//throw new OpaqueStateTransitionException(String.format("Invalid mid-stream state: %s", lastState));
                	OpaqueStateTransitionException e = new OpaqueStateTransitionException(String.format("Invalid mid-stream state: %s", lastState));
                	logger.error("OpaqueBuilder StateTransitionException:", e);
                	return;
            }

            Blockette2000 blk = new Blockette2000();
            blk.setByteOrder(blocketteByteOrder);
            blk.setRecordNumber(recordNumber);
            blk.setOpaqueByteOrder(opaqueByteOrder);
            blk.setStrict(strictPackaging);
            blk.setOpaqueState(state);
            blk.setTags(tags);
            blk.setOpaqueData(buffer, 0, buffer.length);

            completed.add(blk);

            context.setRecordNumber(recordNumber + 1);
            context.setState(state);
        }

        if (remaining > 0) {
            System.arraycopy(opaqueData, srcOffset, buffer, bytesUsed, remaining);
        }
    }

    // Wrap what's left of the data in a blockette
    public void finish()
    {
        finished = true;
        // Push out any remaining data into the final blockette,
        // which should be one of RECORD, STREAM_END, or FILE_END
    }

    public boolean isFinished()
    {
        return finished;
    }

    public Blockette2000 pop()
    {
        Blockette2000 blk = null;
        if (!completed.isEmpty()) {
            blk = completed.remove(0);
        }
        return blk;
    }

    public Collection<Blockette2000> popAll()
    {
        ArrayList<Blockette2000> results = new ArrayList<Blockette2000>();
        while (!completed.isEmpty()) {
            results.add(pop());
        }
        return results;
    }

    public Blockette2000 peek()
    {
        Blockette2000 blk = null;
        if (!completed.isEmpty()) {
            blk = completed.get(0);
        }
        return blk;
    }

    public Collection<Blockette2000> peekAll()
    {
        ArrayList<Blockette2000> results = new ArrayList<Blockette2000>();
        while (!completed.isEmpty()) {
            results.add(peek());
        }
        return results;
    }
}
