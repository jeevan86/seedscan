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

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.InterruptedException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Formatter;

import asl.util.Hex;
import seed.IllegalSeednameException;
import seed.MiniSeed;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 * 
 * Reads MiniSEED records from multiple files, and pushes them into a queue
 * to be processed by a supported class (seed {@link SeedSplitProcessor}).
 */
public class SeedInputStream
implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedsplitter.SeedInputStream.class);
    private static final Formatter formatter = new Formatter();

    public static int MAX_RECORD_SIZE = 16384;
    public static int BLOCK_SIZE = 256;

    private DataInputStream m_inputStream = null;
    private LinkedBlockingQueue<ByteBlock> m_queue = null;
    private boolean m_running = false;
    private byte[] m_buffer = null;
    private int m_bufferBytes = 0;
    private int m_skippedBytes = 0;
    private boolean m_indicateLast = true;
    private String m_digest_algorithm = "MD5";
    private MessageDigest m_digest = null;

    /**
     * Constructor.
     * 
     * @param inStream 		The stream from which to read MiniSEED records.
     * @param queue			The processing queue into which the MiniSEED records are placed.
     * @param indicateLast 	An indicator of whether this is the last record for this stream.
     * @param disableDigest	A flag to disable assembling a digest of this stream's contents.
     */
    public SeedInputStream(DataInputStream inStream, LinkedBlockingQueue<ByteBlock> queue, boolean indicateLast, boolean disableDigest) {
        m_inputStream = inStream;
        m_queue = queue;
        m_buffer = new byte[MAX_RECORD_SIZE];
        m_indicateLast = indicateLast;
        if (!disableDigest) {
            try {
                m_digest = MessageDigest.getInstance(m_digest_algorithm);
            } catch (NoSuchAlgorithmException e) {
            	logger.warn("SeedInputStream NoSuchAlgorithmException:", e);
            }
        }
    }

    /**
     * Constructor.
     * 
     * @param inStream 		The stream from which to read MiniSEED records.
     * @param queue			The processing queue into which the MiniSEED records are placed.
     * @param indicateLast 	An indicator of whether this is the last record for this stream.
     */
    public SeedInputStream(DataInputStream inStream, LinkedBlockingQueue<ByteBlock> queue, boolean indicateLast) {
        this(inStream, queue, indicateLast, false);
    }

    /**
     * Constructor.
     * 
     * @param inStream 	The stream from which to read MiniSEED records.
     * @param queue		The processing queue into which the MiniSEED records are placed.
     */
    public SeedInputStream(DataInputStream inStream, LinkedBlockingQueue<ByteBlock> queue) {
        this(inStream, queue, true, false);
    }

    /**
     * Returns this stream's MessageDigest
     * 
     * @return the raw digest for this stream.
     */
    public byte[] getDigest() {
        byte[] result = null;
        if (m_digest != null ) {
            try {
                result = ((MessageDigest)m_digest.clone()).digest();
            }
            catch (CloneNotSupportedException ex) {
            	logger.warn("SeedInputStream CloneNotSupportedException:", ex);
            }
        }
        return result;
    }

    /**
     * Returns a hex version of the digest for this stream.
     * 
     * @return a String version of the for this stream.
     */
    public String getDigestString() {
        String result = null;
        if (m_digest != null ) {
            try {
                result = Hex.byteArrayToHexString(((MessageDigest)m_digest.clone()).digest());
            } catch (CloneNotSupportedException ex) {
            	logger.error("SeedInputStream CloneNotSupportedException:", ex);
            } catch (IllegalArgumentException ex) {
            	logger.error("SeedInputStream IllegalArgumentException:", ex);
            }
        }
        return result;
    }

    /**
     * Causes this thread to halt gracefully.
     */
    public void halt() {
        m_running = false;
    }

    /**
     * Reads data from the input stream, assembles full SEED records and 
     * pushes them into the queue for processing.
     */
    @Override
    public void run() {
        /* Read chunks of data in from input stream. 
           Once a complete record has been assembled,
           put it into the queue for processing */
        m_running = true;
        int recordLength = -1;
        int bytesRead = 0;
        int indicator;
        ByteBlock last = new ByteBlock(null, 0, true, true);
        ByteBlock end  = new ByteBlock(null, 0, true, false);
        while (m_running) {
            try {
                if (m_bufferBytes < BLOCK_SIZE) {
                    bytesRead = m_inputStream.read(m_buffer, m_bufferBytes, BLOCK_SIZE - m_bufferBytes);
                    if (bytesRead < 0) {
                        logger.debug("SeedInputStream Thread> I think we're done here...");
                        if (m_indicateLast) {
                            m_queue.put(last);
                        } else {
                            m_queue.put(end);
                        }
                        m_running = false;
                        continue;
                    }
                    m_bufferBytes += bytesRead;

                    // Update the contents of our SHA-1 digest.
                    if (m_digest != null) {
                        m_digest.update(Arrays.copyOfRange(m_buffer, m_bufferBytes, bytesRead));
                    }

                    if (m_bufferBytes == BLOCK_SIZE) {
                        indicator = m_buffer[6] & 0xFF;
                        // Make sure we find either the SEED standard indicator 'D'
                        // or the NEIC post processed data indicator 'Q'
                        // or the one mentioned on the IRIS website 'M'
                        /*                 'D'                    'M'                    'Q'  */
                        if ((indicator != 0x44) && (indicator != 0x4D) && (indicator != 0x51)) { 
                            m_skippedBytes += m_bufferBytes;
                            m_bufferBytes = 0;
                            logger.debug(formatter.format("Skipping bad indicator: 0x%x\n", indicator).toString());
                        }
                        else {
                            try {
                                recordLength = MiniSeed.crackBlockSize(m_buffer);
                            } catch (IllegalSeednameException e) {
                                logger.debug("Invalid Format, Skipping Chunk.", e);
                                m_skippedBytes += m_bufferBytes;
                                m_bufferBytes = 0;
                            }
                            /*
                            firstBlockette = ((m_buffer[46] & 0xFF) << 8) | (m_buffer[47] & 0xFF);
                            if (m_bufferBytes <= (firstBlockette + 6)) {
                                logger.debug("First blockette should be within the first 256 bytes");
                                m_bufferBytes = 0;
                            }
                            else if ((((m_buffer[firstBlockette] & 0xFF) << 8) | (m_buffer[firstBlockette+1] & 0xFF)) != 1000) {
                                logger.debug("First record should be of type 1000, not type " + (m_buffer[firstBlockette] & 0xFF));
                                m_bufferBytes = 0;
                            } 
                            else {
                                recordLength = (int)(java.lang.Math.pow(2, m_buffer[firstBlockette + 6]));
                            }
                            */
                        }
                    }
                } else {
                    m_bufferBytes += m_inputStream.read(m_buffer, m_bufferBytes, recordLength - m_bufferBytes);
                    if (m_bufferBytes == recordLength) {
                        m_queue.put(new ByteBlock(m_buffer, recordLength, m_skippedBytes));
                        m_bufferBytes = 0;
                        m_skippedBytes = 0;
                    }
                }
            } catch (IOException e) {
                logger.error("SeedInputStream IOException:", e);
            } catch (InterruptedException e) {
                logger.error("SeedInputStream InterruptedException:", e);
            }
        }
    }
}

