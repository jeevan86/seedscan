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
 * OpaqueParser.java
 *
 * Created on October 3, 2012 @ 21:49 MDT
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
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class represents the Blockette 2000 from the SEED standard V2.4 
 *
 * @author Joel Edwards <jdedwards@usgs.gov>
 */
public class OpaqueParser
{
	private static final Logger logger = LoggerFactory.getLogger(seed.OpaqueParser.class);
    private Hashtable<String, OpaqueContext> contexts; 

    /** Creates a new instance of OpaqueParser
     *
     * @param byteOrder - byte order expected for all blockettes
     */
    public OpaqueParser()
    {
        contexts = new Hashtable<String, OpaqueContext>();
    }

    /** process a blockette, adding its opaque data to the context
     *
     * @param blockette - a ByteBuffer containing the raw blockette to process
     * @param byteOrder - the byteOder of the items in this blockette's header
     */
    public void addBlockette(Blockette2000 blk)
    throws BlocketteOrderException,
           BlocketteTypeException,
           OpaqueSegmentOutOfOrderException,
           OpaqueStateException,
           OpaqueStateTransitionException
    {
        OpaqueContext context;
        String key = blk.getTagString();

        if (contexts.containsKey(key)) {
            context = contexts.get(key);
        } else {
            context = new OpaqueContext(key);
            contexts.put(key, context);
        }

        OpaqueState state = blk.getOpaqueState(); // can throw OpaqueStateException
        OpaqueState lastState = context.getState();
        // RECORD
        if ((state == OpaqueState.RECORD) && (lastState != OpaqueState.INIT)) {
            // Record oreinted data must all be enclosed in a single Blockette 2000.
            // If it is split across multiple Blockette 2000s, it should identify as
            // stream-oriented, with continuation segments.
            //throw new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	OpaqueStateTransitionException e = new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	logger.error("OpaqueParser StateTransitionException:", e);
        	return;
        }

        // STREAM
        else if ((state == OpaqueState.STREAM_START) && (lastState != OpaqueState.INIT)) {
            // The first segment in the stream must not be preceded by any other.
            //throw new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	OpaqueStateTransitionException e = new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	logger.error("OpaqueParser StateTransitionException:", e);
        	return;
        }
        else if ((state == OpaqueState.STREAM_MID) && ((lastState != OpaqueState.STREAM_START) && (lastState != OpaqueState.STREAM_MID))) {
            // A stream continuation segment must be preceded by a stream start or stream continuation segment
            //throw new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	OpaqueStateTransitionException e = new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	logger.error("OpaqueParser StateTransitionException:", e);
        	return;
        }
        else if ((state == OpaqueState.STREAM_END) && ((lastState != OpaqueState.STREAM_START) && (lastState != OpaqueState.STREAM_MID))) {
            // A stream end segment must be preceded by a stream start or stream continuation segment
            //throw new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	OpaqueStateTransitionException e = new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	logger.error("OpaqueParser StateTransitionException:", e);
        	return;
        }

        // FILE
        else if ((state == OpaqueState.FILE_START) && (lastState != OpaqueState.INIT)) {
            // The first segment in the file must not be preceded by any other.
            //throw new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	OpaqueStateTransitionException e = new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	logger.error("OpaqueParser StateTransitionException:", e);
        	return;
        }
        else if ((state == OpaqueState.FILE_MID) && ((lastState != OpaqueState.FILE_START) && (lastState != OpaqueState.FILE_MID))) {
            // A file continuation segment must be preceded by a file start or file continuation segment
            //throw new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	OpaqueStateTransitionException e = new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	logger.error("OpaqueParser StateTransitionExceptoin:", e);
        	return;
        }
        else if ((state == OpaqueState.FILE_END) && ((lastState != OpaqueState.INIT) && (lastState != OpaqueState.FILE_START) && (lastState != OpaqueState.FILE_MID))) {
            // A file end segment can be the only segment in a file oriented group
            //throw new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state)); 
        	OpaqueStateTransitionException e = new OpaqueStateTransitionException(String.format("Invalid state transition: from %s to %s", lastState, state));
        	logger.error("OpaqueParser StateTransitionException:", e);
        	return;
        }


        int recordNumber = blk.getRecordNumber();
        int lastRecordNumber = context.getRecordNumber();
        if (recordNumber != (lastRecordNumber + 1)) {
            // Record numbers must be sequential
            //throw new OpaqueSegmentOutOfOrderException(String.format("Invalid record number step: from %d to %d", lastRecordNumber, recordNumber));
        	OpaqueSegmentOutOfOrderException e = new OpaqueSegmentOutOfOrderException(String.format("Invalid record number step: from %d to %d", lastRecordNumber, recordNumber));
        	logger.error("OpaqueParser SegmentOutOfOrderException:", e);
        	return;
        }

        byte[] opaqueData = blk.getOpaqueData();
        context.update(opaqueData, 0, opaqueData.length);
        context.setRecordNumber(recordNumber);
        context.setState(state);
    }
}
