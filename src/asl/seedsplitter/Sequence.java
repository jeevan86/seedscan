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
package asl.seedsplitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import asl.security.MemberDigest;

/**
 * @author	Joel D. Edwards <jdedwards@usgs.gov>
 *
 */
public class Sequence 
extends MemberDigest
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedsplitter.Sequence.class);

    public static final int BLOCK_SIZE = 4096;
    private static TimeZone m_tz = TimeZone.getTimeZone("GMT");;

    private BlockPool m_pool = null;

    private ArrayList<int[]> m_blocks = null;
    private int[] m_block = null;
    private int m_length = 0;
    private int m_remainder = 0;

    private long m_startTime = 0; // Microseconds since the epoch
    private double m_sampleRate = 0.0;
    private long m_interval = 0;

    public Object clone()
    throws CloneNotSupportedException
    {
    	try {
	        Sequence sequence = new Sequence();
	        sequence.m_startTime  = m_startTime;
	        sequence.m_sampleRate = m_sampleRate;
	        sequence.m_interval   = m_interval;
	        for (int[] block: m_blocks) {
	            sequence.extend(block, 0, block.length); }
	        sequence.m_length     = m_length;
	        sequence.m_remainder  = m_remainder;
	        return sequence;
    	} catch (CloneNotSupportedException e) {
    		throw e;
    	}
    }

    /**
     * Creates a new instance of this object.
     */
    public Sequence()
    throws CloneNotSupportedException,
    	   RuntimeException
    {
        super();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        m_pool = new BlockPool(BLOCK_SIZE);
        _reset();
    }

    /**
     * Flushes all entries from the time series, but does not remove metadata.
     */
    private void _reset() 
    {
        m_length = 0;
        m_blocks = new ArrayList<int[]>(8);
        this._addBlock();
    }

    /**
     * Creates a new empty data block and adds it to the block list.
     */
    private void _addBlock() 
    {
        m_block = m_pool.getNewBlock();
        m_blocks.add(m_block);
        m_remainder = BLOCK_SIZE;
    }

    /**
     * Sets the timestamp of the first data point.
     * 
     * @param   startTime   timestamp of first data point
     */
    @Override
    protected void addDigestMembers()
    {
        addToDigest(m_startTime);
        addToDigest(m_sampleRate);
        int remaining = m_blocks.size();
        for (int[] block: m_blocks) {
            int numSamples = (--remaining > 0) ? BLOCK_SIZE : (BLOCK_SIZE - m_remainder);
            for (int i = 0; i < numSamples; i++) {
                addToDigest(block[i]);
            }
        }
    }

    /**
     * Sets the timestamp of the first data point.
     * 
     * @param   startTime   timestamp of first data point
     */
    public void setStartTime(long startTime)
    {
        m_startTime = startTime;
    }

    /**
     * Sets the time series' sample rate in Hz.
     * 
     * @param   sampleRate  sample rate in Hz
     */
    public void setSampleRate(double sampleRate) 
    throws IllegalSampleRateException
    {
        try {
            m_interval = sampleRateToInterval(sampleRate);
            m_sampleRate = sampleRate; 
        } catch (IllegalSampleRateException e) {
        	throw e;
        }
    }

    /**
     * Resets the time-series, flushing all data.
     */
    public void clear()
    {
        _reset();
        m_startTime  = 0;
        m_sampleRate = 0.0;
        m_interval   = 0;
    }

    /**
     * Extends the time-series by adding the specified data to the internal buffer.
     *
     * @param   buffer      The int array from which the data points should be copied.
     * @param   offset      Offset within buffer at which copying should begin.
     * @param   length      The number of elements to copy from 
     *
     * @throws  IndexOutOfBoundsException - if copying would cause access of data outside array bounds.
     * @throws  ArrayStoreException - if an element in the buffer array could not be stored because of a type mismatch.
     * @throws  NullPointerException - if offset is null.
     */
    public void extend(int[] buffer, int offset, int length)
    throws IndexOutOfBoundsException,
           ArrayStoreException,
           NullPointerException
    {
        int copySize = 0;
        while (length > 0) {
            copySize = (m_remainder > length) ? length : m_remainder;
//System.out.format("=== Sequence.extend(): length=%d remainder=%d copySize=%d\n", length, m_remainder, copySize);
            System.arraycopy(buffer, offset, m_block, BLOCK_SIZE - m_remainder, copySize);
            if (m_remainder <= length) {
                this._addBlock();
            } else {
                m_remainder -= copySize;
            }
            offset   += copySize;
            m_length += copySize;
            length   -= copySize;
        }
    }

    /**
     * Trims the sequence such that its data is within the specified time range.
     *
     * @param   startTime   The new starting data point is at or later than this point.
     * @param   endTime     The new ending data point is at or earlier than this point.
     */
    public void trim(long startTime, long endTime) 
    {
        if (startTime > endTime) {
            clear();
        } else if (startTime > this.getEndTime()) {
            clear();
        } else if (endTime < m_startTime) {
            clear();
        } else if ((startTime > m_startTime) || (endTime < this.getEndTime())) {
            if (startTime < m_startTime) {
                startTime = m_startTime;
            }
            if (endTime > this.getEndTime()) {
                endTime = this.getEndTime();
            }
            try {
	            Sequence newSequence = new Sequence();
	            newSequence.m_startTime = m_startTime;
	            newSequence.m_interval = m_interval;
	            newSequence.m_sampleRate = m_sampleRate;

                int[] series = this.getSeries(startTime, endTime);
                newSequence.extend(series, 0, series.length);
                this.swapData(newSequence);
            } catch (SequenceRangeException e) {
            	String message = "Sequence RangeException: Sequence Range Error in trim(). This should never happen!";
            	logger.error(message, e);
            } catch (RuntimeException e) {
            	logger.error("Sequence RuntimeException:", e);
            } catch (CloneNotSupportedException e) {
            	logger.error("Sequence CloneNotSupportedException:", e);
            }
        }
    }

    /**
     * Trims the sequence such that its data starts at or after the specified time.
     *
     * @param   startTime   The new starting data point is at or later than this point.
     */
    public void trimStart(long startTime) {
        this.trim(startTime, this.getEndTime());
    }

    /**
     * Trims the sequence such that its data ends at or before the specified time.
     *
     * @param   endTime     The new ending data point is at or earlier than this point.
     */
    public void trimEnd(long endTime) {
        this.trim(m_startTime, endTime);
    }


    /**
     * Swaps the data contained within two Sequences such that they now 
     * contain each other's data. This is not a thread safe operation, and 
     * requires that all of the data components be synchronized correctly
     * in order to prevent any being set while this operation is in progress.
     *
     * @param seq   The Sequence with which this Sequence's data will be swapped.
     */
    public void swapData(Sequence seq) {
        ArrayList<int[]> tempBlocks = m_blocks;
        int[]   tempBlock      = m_block;
        int     tempLength     = m_length;
        int     tempRemainder  = m_remainder;
        long    tempStartTime  = m_startTime;
        double  tempSampleRate = m_sampleRate;
        long    tempInterval   = m_interval;

        m_blocks     = seq.m_blocks;
        m_block      = seq.m_block;
        m_length     = seq.m_length;
        m_remainder  = seq.m_remainder;
        m_startTime  = seq.m_startTime;
        m_sampleRate = seq.m_sampleRate;
        m_interval   = seq.m_interval;

        seq.m_blocks     = tempBlocks;
        seq.m_block      = tempBlock;
        seq.m_length     = tempLength;
        seq.m_remainder  = tempRemainder;
        seq.m_startTime  = tempStartTime;
        seq.m_sampleRate = tempSampleRate;
        seq.m_interval   = tempInterval;
    }

    /**
     * Merges this Sequence into the target Sequence, removing all data points
     * from this Sequence. 
     *
     * A SequenceIntervalMismatchException is thrown when the Sequences do not
     * have the same interval.
     *
     * A SequenceMergeRangeException is thrown when the Sequences are not
     * contiguous, or do not overlap.
     * 
     * A SequenceTimingException is thrown when the Sequences startTimes are 
     * not synchronized by a factor of the interval.
     *
     * @param seq   The Sequence into which the data points from this sequence will be merged.
     *
     */
    public synchronized void mergeInto(Sequence seq) 
    throws SequenceIntervalMismatchException,
           SequenceMergeRangeException,
           SequenceTimingException,
           BlockSizeMismatchException
    {
        if (m_interval != seq.m_interval) {
            throw new SequenceIntervalMismatchException();
        }

        //if ((Math.abs(m_startTime - seq.m_startTime) % m_interval) != 0) {
        //    throw new SequenceTimingException();
        //}

        // Allow for a fudge factor of 1 millisecond if sample 
        // rate is less than 100 Hz.
        //
        // Is this a good idea, or would it be better to simply 
        // report a gap so the user is aware of the jump?
        //
        long intervalAdjustment = m_interval / 100;

        logger.debug("");
        logger.debug("    Source DataSet: " + DataSet.timestampToString(this.getStartTime()) + " to " + DataSet.timestampToString(this.getEndTime()) + " (" + this.getLength() + " data points)");
        logger.debug("    Target DataSet: " + DataSet.timestampToString(seq.getStartTime()) + " to " + DataSet.timestampToString(seq.getEndTime()) + " (" + seq.getLength() + " data points)");
        if (m_startTime > seq.getEndTime()) {
            if (((m_startTime - seq.getEndTime()) < (m_interval - intervalAdjustment)) ||
                ((m_startTime - seq.getEndTime()) > (m_interval + intervalAdjustment))) {
                logger.debug("Source is more than 1 data point after target. (difference = " +(m_startTime - seq.getEndTime())+ " ms OR " +((m_startTime - seq.getEndTime())/m_interval)+ " data points)");
                throw new SequenceMergeRangeException("Source is more than 1 data point after target.");
            }
        }

        if (this.getEndTime() < seq.m_startTime) {
            if (((seq.m_startTime - this.getEndTime()) < (m_interval - intervalAdjustment)) ||
                ((seq.m_startTime - this.getEndTime()) > (m_interval + intervalAdjustment))) {
                logger.debug("Target is more than 1 data point after source. (difference = " +(seq.m_startTime - this.getEndTime())+ ")");
                throw new SequenceMergeRangeException("Target is more than 1 data point after source.");
            } else {
                logger.debug("Swapping source and target prior to merge.");
                try {
                	seq.mergeInto(this);
                } catch (BlockSizeMismatchException e) {
                	logger.error("Sequence BlockSizeMismatchException:", e);
                }
                seq.swapData(this);
                return;
            }
        }
        
        if (this.subSequenceOf(seq)) {
            logger.debug("Subsequence. Just _reset().");
            //MTH: 2013-04-27 This seems wrong:
            //seq._reset();
            DataSet dataSet = (DataSet)this;
            System.out.format("== Sequence.mergeInto() [%s_%s %s-%s] this=[%s-%s] is a subSequence of seq=[%s-%s] --> this._reset()\n", 
                dataSet.getNetwork(), dataSet.getStation(), dataSet.getLocation(), dataSet.getChannel(), 
                DataSet.timestampToString(this.getStartTime()), DataSet.timestampToString(this.getEndTime()), 
                DataSet.timestampToString(seq.getStartTime()), DataSet.timestampToString(seq.getEndTime()) 
            );
            this._reset();
            return;
        }

        if (m_startTime < seq.m_startTime) {
            logger.debug("Swapping source and target prior to merge.");
            try {
            	seq.mergeInto(this);
            } catch (BlockSizeMismatchException e) {
            	logger.error("Sequence BlockSizeMismatchException:", e);
            }
            seq.swapData(this);
            return ;
        }

        BlockPool pool = seq.m_pool;
        int[] block = null;

        // We are going to flush the old data away through this process,
        // so let's do it now, and keep the old data around. This should
        // prevent others from messing with it while we are working.
        ArrayList<int[]> blocks = m_blocks;
        long startTime = m_startTime;
        long interval = m_interval;
        int remainder = m_remainder;
        this._reset();

        // skipCount is the number of data points we need to skip in order
        // to prevent a data overlap.
        int skipCount = 0;
        if (startTime <= seq.getEndTime()) {
            skipCount = (int)((seq.getEndTime() - startTime) / interval + 1);
        }

        // In order to ensure no data overlaps, we need to burn of full
        // blocks that are in the overlapping range. Any remaining part
        // of a block that remains will be handled in the copy logic
        // below, which takes into account the remaining skipCount value.
        while (skipCount >= BLOCK_SIZE) {
            skipCount -= BLOCK_SIZE;
            block = blocks.remove(0);
        }

        int blockCount  = blocks.size();
        int blockOffset = 0;
        int blockLength = 0;
        int copyLength  = 0;
        for (int i = 0; i < blockCount; i++) {
            block = blocks.remove(0);
            // If we end up on the last block, we need to update the block length to
            // compensate for the skipped data points, and force the copyLength
            // computation below to be updated with this information.
            blockLength = BLOCK_SIZE - skipCount;
            // This automatically handles and updates the skipCount on the first iteration
            // If skipCount is greater than one, we jump a way into the first block,
            // after the first block, we always start at zero.
            blockOffset = skipCount;
            skipCount = 0;
            // If we have reached the last block, then remainder applies.
            if (i == (blockCount - 1)) {
                copyLength = blockLength - remainder;
            } else {
                copyLength = blockLength;
            }
            // Append the block's contents to the target Sequence
            if (copyLength > 0) {
                seq.extend(block, blockOffset, copyLength);
            }
            // Add the block to the target Sequence's BlockPool after its contents have been copied.
            try {
                pool.addBlock(block);
            } catch (BlockSizeMismatchException e) {
                //throw new RuntimeException("Impossible situation! BlockSizeMismatchException on BlockPool.addBlock()", e); // This should never happen
            	//String message = "Sequence BlockSizeMismatchException: BlockPool.addBlock() Impossible situation!";
            	throw e;
            }
        }
    }

    /**
     * Returns this Sequence's BlockPool
     *
     * @return This Sequence's BlockPool.
     */
    public BlockPool getBlockPool()
    {
        return m_pool;
    }


    /**
     * Returns the timestamp of the first data point.
     *
     * @return  starting timestamp
     */
    public long getStartTime() 
    { 
        return m_startTime; 
    }

    /**
     * Returns the timestamp of the last data point.
     *
     * @return  ending timestamp
     */
    public long getEndTime() 
    { 
        return m_startTime + (m_interval * (m_length - 1));
    }

    /**
     * Returns the interval (in microseconds) based on the sample rate.
     *
     * @return  data point interval in microseconds
     */
    public long getInterval() 
    { 
        return m_interval; 
    }

    /**
     * Returns the sample rate (in Hz) of the time series.
     *
     * @return  sample rate in Hz
     */
    public double getSampleRate() 
    {
        return m_sampleRate; 
    }

    /**
     * Returns a reference the block at the specified index.
     *
     * @param index     The index of the desired block
     *
     * @return refernce to the int array at the specified index.
     * @throws ArrayIndexOutOfBoundsException - index out of range (index < 0 || index >= getLength()).
     */
    public int[] getBlock(int index) 
    throws ArrayIndexOutOfBoundsException 
    {
        return m_blocks.get(index);
    }

    /**
     * Returns the number of valid data points in the block located at this index.
     *
     * @param index     The index of the desired block
     *
     * @return refernce the number of valid data points in the block located at this index.
     * @throws ArrayIndexOutOfBoundsException - index out of range (index < 0 || index >= getLength()).
     */
    public int getBlockSize(int index) 
    throws ArrayIndexOutOfBoundsException 
    { 
        if (index > m_blocks.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return ((index <= m_blocks.size()) ? BLOCK_SIZE - m_remainder : BLOCK_SIZE); 
    }

    /**
     * Returns the number of blocks contained within this Sequence.
     *
     * @return      Returns the number of blocks contained within this Sequence.
     */
    public int getBlockCount() 
    {
        return m_blocks.size(); 
    }

    /**
     * Returns the number of data points.
     *
     * @return  Returns the number of data points.
     */
    public int getLength() 
    {
        return m_length; 
    }

    /**
     * Returns a new Array containing a subset of the data points in this sequence.
     *
     * @param index     The index of the first returned data point.
     * @param count     The number of data points to return.
     *
     * @return Returns a new Array containing a subset of the data points in this sequence.
     */
    public int[] getSeries(int index, int count)
    throws IndexOutOfBoundsException,
           SequenceRangeException
    {
        if (index >= m_length) {
            throw new IndexOutOfBoundsException();
        }
        if ((index + count) > m_length) {
            throw new SequenceRangeException();
        }
        if (m_length == 0) {
            return null;
        }

        int[] series = new int[count];
        int[] block = null;
        int numBlocks = m_blocks.size();
        int finalBlock = numBlocks - 1;
        int seriesLength = 0;

        int blockLength = BLOCK_SIZE;
        int burn = index / blockLength; // "burn off" this many blocks before copying the first block
        int jump = index % blockLength; // start at this index within the first block copied

        for (int i=burn; (i < numBlocks) && (count > 0); i++) {
            block = m_blocks.get(i);
            blockLength = BLOCK_SIZE - jump;
            if (i == finalBlock) {
                blockLength = blockLength - m_remainder;
            }
            if (blockLength > count) {
                blockLength = count;
            }
            System.arraycopy(block, jump, series, seriesLength, blockLength);
            seriesLength += blockLength;
            count -= blockLength;
            jump = 0;
        }

        return series;
    }

    /**
     * Returns a new Array containing all of the data points in this sequence.
     *
     * @return Returns a new Array containing all of the data points in this sequence.
     */
    public int[] getSeries() 
    {
        try {
            return this.getSeries(0, m_length);
        } catch (IndexOutOfBoundsException e) {
        	logger.error("Sequence IndexOutOfBoundsException:", e);
        } catch (SequenceRangeException e) {
        	logger.error("Sequence RangeException:", e);
        }
        return null;
    }

    /**
     * Returns a new Array containing the specified number of data points 
     * starting at the specified timestamp.
     *
     * @param startTime     The first returned data point must start at or after this timestamp.
     * @param count         The number of data points to return.
     *
     * @return Returns a new Array containing all of the data points in this sequence.
     */
    public int[] getSeries(long startTime, int count)
    throws SequenceRangeException,
    	   IndexOutOfBoundsException
    {
        int[] series = null;
        int index = 0;
        if (startTime < m_startTime) {
            throw new SequenceRangeException();
        } else {
            long diff = startTime - m_startTime;
            index = (int)(diff / m_interval + (((diff % m_interval) > 0) ? 1 : 0));
        }
            
        try {
            series = this.getSeries(index, count);
        } catch (IndexOutOfBoundsException e) {
        	throw e;
        }
        return series;
    }

    /**
     * Returns an array of integer values that falls within the specified 
     * range. The specified start and end times must be within the range of
     * the actual data.
     * 
     * @param startTime		The first value should be at or after this point in time.
     * @param endTime		The last value should be at or before this point in time.
     * @return	An array of int values.
     * @throws SequenceRangeException 	If the requested window is not contained within this Sequence.
     */
    public int[] getSeries(long startTime, long endTime)
    throws SequenceRangeException,
    	   IndexOutOfBoundsException
    {
        int[] series = null;
        int index = 0;
        int count = 0;
        if (endTime > this.getEndTime()) {
            throw new SequenceRangeException();
        }
        if (startTime < m_startTime) {
            throw new SequenceRangeException();
        }
        count = (int)((endTime - startTime) / m_interval);
        index = (int)(((startTime - m_startTime) + (m_interval / 2)) / m_interval);

        try {
            series = this.getSeries(index, count);
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
        return series;
    }

/* comparison methods */
    /**
     * Determines if this Sequence has a start time which is earlier than
     * the start time of the supplied reference Sequence (may overlap).
     * 
     * @param seq	The reference Sequence with which to compare this Sequence.
     * @return	A boolean value; true if this Sequence starts before the start of the reference Sequence, otherwise false.
     */
    public boolean startsBefore(Sequence seq) 
    {
        return (this.m_startTime < seq.m_startTime);
    }

    /**
     * Determines if this Sequence has a start time which is later than
     * the end time of the supplied reference Sequence (no overlap if true).
     * 
     * @param seq	The reference Sequence with which to compare this Sequence.
     * @return	A boolean value; true if this Sequence starts after the end of the reference Sequence, otherwise false.
     */
    public boolean startsAfter(Sequence seq) 
    {
        return (this.m_startTime > seq.getEndTime());
    }

    /**
     * Determines if this Sequence has an end time which is earlier than
     * the start time of the supplied reference Sequence (no overlap if true).
     * 
     * @param seq	The reference Sequence with which to compare this Sequence.
     * @return	A boolean value; true if this Sequence ends before the start time of the reference Sequence, otherwise false.
     */
    public boolean endsBefore(Sequence seq) 
    {
        return (this.getEndTime() < seq.m_startTime);
    }

    /**
     * Determines if this Sequence has an end time which is later than
     * the end time of the supplied reference Sequence (may overlap).
     * 
     * @param seq	The reference Sequence with which to compare this Sequence.
     * @return	A boolean value; true if this Sequence ends after the end time of the reference Sequence, otherwise false.
     */
    public boolean endsAfter(Sequence seq) 
    {
        return (this.getEndTime() > seq.getEndTime());
    }

    /**
     * Determines if this Sequence has any overlap with the
     * supplied reference Sequence.
     * 
     * @param seq	The reference Sequence with which to compare this Sequence.
     * @return	A boolean value; true if this Sequence overlaps with the suppplied sequence.
     */
    public boolean overlaps(Sequence seq)
    {
        return !(this.endsBefore(seq) || this.startsAfter(seq));
    }

    /**
     * Reports whether this Sequence contains data within the specified
     * time range.
     * 
     * @param startTime		Start time for the test range.
     * @param endTime		End time for the test range.
     * @return	A boolean value: true if this time range is available, otherwise false.
     */
    public boolean containsRange(long startTime, long endTime)
    {
        boolean result = false;
        if ((startTime >= this.m_startTime) && (endTime <= this.getEndTime())) {
            result = true;
        }
        return result;
    }

    /**
     * Reports whether two Sequences have the same frequency, start time,
     * and data point count (does not compare individual values).
     * 
     * @param seq	The reference Sequence with which to compare this Sequence.
     * @return	A boolean value: true if Sequences are the same, otherwise false.
     */
    public boolean equals(Sequence seq) 
    {
        boolean result = true;
        if (m_interval != seq.m_interval) {
            result = false;
        } else if (m_startTime != seq.m_startTime) {
            result = false;
        // We check length instead of endTime because getEndTime requires a
        // calculation for both Sequences.
        } else if (m_length != seq.m_length) {
            result = false;
        }
        return result;
    }

    /**
     * Reports whether this Sequence is a sub-sequence of the supplied
     * reference Sequence.
     * 
     * @param seq	The reference sequence with which to compare this Sequence.
     * @return	A boolean value: true if the range of this Sequence is within the range of the reference Sequence.
     * @throws SequenceIntervalMismatchException if the reference Sequence's frequency does not match that of this Sequence.
     */
    public Boolean subSequenceOf(Sequence seq) 
    throws SequenceIntervalMismatchException 
    {
        if (seq.m_interval != m_interval) {
            throw new SequenceIntervalMismatchException();
        }
        return ((m_startTime >= seq.m_startTime) && (this.getEndTime() <= seq.getEndTime()));
    }

/* static methods */
    /**
     * Checks a sample rate to ensure it is valid, returning the associated interval in microseconds.
     * 
     * @param sampleRate	The sample rate to be verified and converted.
     * @return	A long integer value representing the supplied sample rate as an interval.
     * @throws IllegalSampleRateException if the supplied sample rate is not one of the accpeted values.
     */
    public static long sampleRateToInterval(double sampleRate) 
    throws IllegalSampleRateException 
    {
        long interval;
        if      (sampleRate ==    0.001) interval = 1000000000L;
        else if (sampleRate ==    0.01 ) interval =  100000000L;
        else if (sampleRate ==    0.1  ) interval =   10000000L;
        else if (sampleRate ==    1.0  ) interval =    1000000L;
        else if (sampleRate ==    2.5  ) interval =     400000L;
        else if (sampleRate ==    4.0  ) interval =     250000L;
        else if (sampleRate ==    5.0  ) interval =     200000L;
        else if (sampleRate ==   10.0  ) interval =     100000L;
        else if (sampleRate ==   20.0  ) interval =      50000L;
        else if (sampleRate ==   40.0  ) interval =      25000L;
        else if (sampleRate ==   50.0  ) interval =      20000L;
        else if (sampleRate ==  100.0  ) interval =      10000L;
        else if (sampleRate ==  200.0  ) interval =       5000L;
        else if (sampleRate ==  250.0  ) interval =       4000L;
        else if (sampleRate ==  400.0  ) interval =       2500L;
        else if (sampleRate ==  500.0  ) interval =       2000L;
        else if (sampleRate == 1000.0  ) interval =       1000L;
        else if (sampleRate == 2000.0  ) interval =        500L;
        else if (sampleRate == 4000.0  ) interval =        250L;
        else if (sampleRate == 5000.0  ) interval =        200L;
        else {
        	throw new IllegalSampleRateException("The selected sample rate (" + sampleRate + " Hz) is not supported.");
        }
        return interval;
    }

    /**
     * Converts a microsecond granualarity timestamp into a human readable string.
     *
     * @param   timestamp - microseconds since the Epoch (January 1, 1970 00:00:00.000000 GMT)
     * @return  Returns the timestamp as a human readable string of the format YYYY/MM/DD HH:MM:SS.mmmmmm
     */
    public static String timestampToString(long timestamp) 
    {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        String result = null;
        GregorianCalendar cal = new GregorianCalendar(m_tz);
        cal.setTimeInMillis(timestamp/1000);
        result = String.format("%04d/%02d/%02d %02d:%02d:%02d.%06d",
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH) + 1,   // MTH: Java uses months 0-11 ...
                                cal.get(Calendar.DAY_OF_MONTH),
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                cal.get(Calendar.SECOND),
                               (cal.get(Calendar.MILLISECOND) * 1000 + (timestamp % 10)));
        return result;
    }

    /**
     * Returns the timestamp of the first data point.
     *
     * @param   sequences - a Collection of Sequences which will be collapsed into a single sequence, disregarding gaps and removing duplicates
     * @return  new sequence containing
     */
    public static Sequence collapse(Collection<Sequence> sequences)
    throws SequenceIntervalMismatchException,
           SequenceMergeRangeException,
           SequenceTimingException,
           CloneNotSupportedException,
           RuntimeException
    {
        Sequence collapsed = null;
        try {
	        for (Sequence sequence: sequences) {
	            if (collapsed == null) {
	                collapsed = (Sequence)sequence.clone();
	            }
	            else if (collapsed.overlaps(sequence)) {
	            	try {
	            		((Sequence)sequence.clone()).mergeInto(collapsed);
	            	} catch (BlockSizeMismatchException e) {
	            		logger.error("Sequence BlockSizeMismatchException:", e);
	            	}
	            }
	            else {
	                if (collapsed.getInterval() != sequence.getInterval()) {
	                    throw new SequenceIntervalMismatchException();
	                }
	                Sequence source = sequence;
	                if (collapsed.startsAfter(sequence)) {
	                    source = collapsed;
	                    collapsed = (Sequence)sequence.clone();
	                }
	                //append newSeq to collapsed
	                int remaining = source.m_blocks.size();
	                for (int[] block: source.m_blocks) {
	                    int numSamples = (--remaining > 0) ? BLOCK_SIZE : (BLOCK_SIZE - source.m_remainder);
	                    collapsed.extend(block, 0, numSamples);
	                }
	            }
	        }
	        return collapsed;
        } catch (CloneNotSupportedException e) {
        	throw e;
        } catch (RuntimeException e) {
        	throw e;
        }
    }
}

