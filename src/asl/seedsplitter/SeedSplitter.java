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

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import seed.Blockette320;

import javax.swing.SwingWorker;

import asl.concurrent.FallOffQueue;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 * 
 * The SeedSplitter class reads MiniSEED records from a list of files,
 * filters out records that don't match the filters (if supplied),
 * de-duplicates the data, orders it based on date, and breaks it up
 * into DataSets based on continuity and station/channel info.
 */
public class SeedSplitter 
extends SwingWorker<Hashtable<String,ArrayList<DataSet>>, SeedSplitProgress> 
{
    private static final Logger logger = Logger.getLogger("asl.seedsplitter.SeedSplitter");

    public static final int NETWORK  = 1;
    public static final int STATION  = 2;
    public static final int LOCATION = 4;
    public static final int CHANNEL  = 8;

    // Consider changing the T,V types
    // T may be alright, but V should be some sort of progress indicator 
    // along the lines of (file # out of total, byte count out of total, percent complete)
    private File[] m_files;
    private String[] m_digests;
    private Hashtable<String,ArrayList<DataSet>> m_table;
    private LinkedBlockingQueue<ByteBlock> m_recordQueue;
    private LinkedBlockingQueue<DataSet>   m_dataQueue;
    private FallOffQueue<SeedSplitProgress> m_progressQueue;
    private FallOffQueue<SeedSplitProgress> m_externalProgressQueue = null;
    private SeedSplitProgress m_lastProgress = null;

    private Pattern m_patternNetwork  = null;
    private Pattern m_patternStation  = null;
    private Pattern m_patternLocation = null;
    private Pattern m_patternChannel  = null;

//MTH
    private Hashtable<String,ArrayList<Integer>> m_qualityTable;
    private Hashtable<String,ArrayList<Blockette320>> m_calTable;

    /**
     * Hidden initializer which is called by all constructors.
     * 
     * @param fileList 	List of files from which to read in the MiniSEED data.
     */
    private void _construct(File[] fileList) {
        m_files = fileList;
        m_table = null;

        m_recordQueue = new LinkedBlockingQueue<ByteBlock>(1024);
        //m_dataQueue = new LinkedBlockingQueue<DataSet>(64);

        // We want things to drop off this queue quickly so the GUI stays 
        // up-to-date and spends very little time updating the status bar.
        // An eight element buffer should keep it snappy. The FallOffQueue
        // ensures that the GUI don't wait on progress updates for too long.
        // Keep an eye on this so we can decrease the size if the GUI lags,
        // or increase the size if the GUI is jerky.
        m_progressQueue = new FallOffQueue<SeedSplitProgress>(1);
        m_digests = new String[m_files.length];
    }

    /**
     * Constructor.
     * 
     * @param fileList 	List of files from which to read in the MiniSEED data.
     */
    public SeedSplitter (File[] fileList) {
        super();
        _construct(fileList);
    }

    /**
     * Constructor.
     * 
     * @param fileList 				List of files from which to read in the MiniSEED data.
     * @param externalProgressQueue Queue into which progress updates are pushed.
     */
    public SeedSplitter (File[] fileList, FallOffQueue<SeedSplitProgress> externalProgressQueue) {
        super();
        m_externalProgressQueue = externalProgressQueue;
        _construct(fileList);
    }

    /**
     * Sets a filter which determines whether a MiniSEED record should be 
     * passed on to the SeedSplitProcessor.
     * 
     * @param filter 	Filter pattern to be used for this property.
     * @param which		Property to which this filter pattern should be applied.
     * @throws InvalidFilterException If the supplied filter pattern is invalid.
     */
    public void setFilter(String filter, int which) 
        throws InvalidFilterException
    {
        Pattern pattern;
        Pattern pVerify = Pattern.compile("^[A-Za-z0-9*?.]+$");
        Matcher mVerify = pVerify.matcher(filter);
        if (!mVerify.matches()) {
            throw new InvalidFilterException("Invalid filter text: '" +filter+ "' ");
        }

        String pText = filter.replaceAll("[*]",".*");
        pText = pText.replaceAll("[?]",".?");

        try {
            pattern = Pattern.compile(pText); 
        } catch (PatternSyntaxException e) {
            throw new InvalidFilterException("Invalid filter text: '" +filter+ "'");
        }

        if ((which & NETWORK) > 0) {
            m_patternNetwork = pattern;
        } 
        else if ((which & STATION) > 0) {
            m_patternStation = pattern;
        } 
        else if ((which & LOCATION) > 0) {
            m_patternLocation = pattern;
        } 
        else if ((which & CHANNEL) > 0) {
            m_patternChannel = pattern;
        }

    }

    /**
     * Get the results after the SeedSplitter has finished processing all files.
     * 
     * @return The hash table containing all of the data post filtering and re-ordering.
     */
    public Hashtable<String,ArrayList<DataSet>> getTable()
    {
        return m_table;
    }

    public Hashtable<String,ArrayList<Integer>> getQualityTable()
    {
        return m_qualityTable;
    }
    public Hashtable<String,ArrayList<Blockette320>> getCalTable()
    {
        return m_calTable;
    }

    /**
     * Get the final progress status.
     * 
     * @return A SeedSplitProgress object describing the progress at the completion of processing.
     */
    public SeedSplitProgress getFinalProgress()
    {
        return m_lastProgress;
    }

    /**
     * Returns an array of Strings containing the SHA-1 sums of each file.
     * 
     * @return An array of Strings containing the SHA-1 sums of each file.
     */
    public String[] getDigests()
    {
        return m_digests;
    }

    /**
     * Overrides the doInBackground method of SwingWorker, launching and 
     * monitoring two threads which read the files and process MiniSEED Data.
     * 
     * @return  A hash table containing all of the data acquired from the file list.
     */
    @Override
    public Hashtable<String,ArrayList<DataSet>> doInBackground()
    {
        // Only turn this on for extremely verbose debugging:
        logger.setLevel(Level.OFF);

        SeedSplitProgress progress = null;
        int progressPercent = 0; // 0 - 100
        int lastPercent = 0;
        long totalBytes = 0;
        long progressBytes = 0;
        long stageBytes = 0;
        boolean finalFile = false;
        for (File file: m_files) {
            totalBytes += file.length();
        }

        SeedSplitProcessor processor = new SeedSplitProcessor(m_recordQueue, m_progressQueue);
        processor.setNetworkPattern(m_patternNetwork);
        processor.setStationPattern(m_patternStation);
        processor.setLocationPattern(m_patternLocation);
        processor.setChannelPattern(m_patternChannel);
        Thread processorThread = new Thread(processor);
        processorThread.start();
        for (int i = 0; i < m_files.length; i++) {
            if (i == (m_files.length - 1)) {
                finalFile = true;
            }
            File file = m_files[i];
        // MTH: SeedSplitProcessor hangs if seed filesize = 0 --> Handled in Scanner.java instead
        // MTH: Skip this file if size = 0:
        //  if (file.length() == 0) {
        //      continue;
        //  }
            DataInputStream inputStream;
            Thread inputThread = null;
            progressBytes = 0;
            try {
                inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
                SeedInputStream stream = new SeedInputStream(inputStream, m_recordQueue, finalFile); 
                inputThread = new Thread(stream);
                logger.debug("Processing file " + file.getName() + "...");
                inputThread.start();

                while ((progressBytes >= 0) && !this.isCancelled()) {
                    try {
                        progress = m_progressQueue.take();
                        if (progress.errorOccurred()) {
                            logger.debug(progress.getErrorMessage());
                            break;
                        } 
                        else if (progress.isFileDone()) {
                            break;
                        }
                        else if (progress.isComplete()) {
                            progressBytes = -1L;
                        } else {
                            progressBytes = progress.getByteCount();
                            lastPercent = progressPercent;
                            progressPercent = (int)((stageBytes + progressBytes) * 100L / totalBytes);
                            if (progressPercent > 99) {
                                progressPercent = 99;
                            }
                            // Only update our percentage if it has changed
                            if (progressPercent > lastPercent) {
                                this.setProgress(progressPercent);
                                logger.debug("Total Bytes:      " + totalBytes);
                                logger.debug("Progress Bytes:   " + progressBytes);
                                logger.debug("Progress Percent: " + progressPercent + "%");
                            }
                        }
                    } catch (InterruptedException e) {;}
                }
                m_digests[i] = stream.getDigestString();
            } catch (FileNotFoundException e) {
                logger.debug("File '" +file.getName()+ "' not found\n");
                // Should we do something more? Throw an exception?
            }
            m_table = processor.getTable();
//MTH:
            m_qualityTable = processor.getQualityTable();
            m_calTable     = processor.getCalTable();
            m_lastProgress = progress;
            if (progress.errorOccurred()) {
                m_table = null;
                return null;
            }
            if (this.isCancelled()) {
                m_table = null;
                return null;
            }
            if (inputThread != null) {
                try {
                    inputThread.join();
                } catch (InterruptedException e) {;}
            }
            if ((processorThread != null) && (progress.isComplete())) {
                try {
                    processorThread.join();
                } catch (InterruptedException e) {;}
            }
            logger.debug("Finished processing file " + file.getName() + "  " + progressPercent + "% complete");
            stageBytes += file.length();
        }
        logger.debug("All done. Setting progress to 100%");
        this.setProgress(100);
        return m_table;
    }
}

