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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import seed.Blockette320;

/**
 * @author Joel D. Edwards
 * 
 *         The SeedSplitter class reads MiniSEED records from a list of files,
 *         filters out records that don't match the filters (if supplied),
 *         de-duplicates the data, orders it based on date, and breaks it up
 *         into DataSets based on continuity and station/channel info.
 */
public class SeedSplitter extends
		SwingWorker<Hashtable<String, ArrayList<DataSet>>, SeedSplitProgress> {
	private static final Logger datalogger = LoggerFactory.getLogger("DataLog");
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedsplitter.SeedSplitter.class);

	// Consider changing the T,V types
	// T may be alright, but V should be some sort of progress indicator
	// along the lines of (file # out of total, byte count out of total, percent
	// complete)
	private File[] m_files;
	private Hashtable<String, ArrayList<DataSet>> m_table;
	private LinkedBlockingQueue<ByteBlock> m_recordQueue;
	private SeedSplitProgress m_lastProgress = null;

	private Pattern m_patternNetwork = null;
	private Pattern m_patternStation = null;
	private Pattern m_patternLocation = null;
	private Pattern m_patternChannel = null;

	// MTH
	private Hashtable<String, ArrayList<Integer>> m_qualityTable;
	private Hashtable<String, ArrayList<Blockette320>> m_calTable;

	/**
	 * Hidden initializer which is called by all constructors.
	 * 
	 * @param fileList
	 *            List of files from which to read in the MiniSEED data.
	 */
	private void _construct(File[] fileList) {
		m_files = fileList;
		m_table = null;

		m_recordQueue = new LinkedBlockingQueue<>(1024);
		// m_dataQueue = new LinkedBlockingQueue<DataSet>(64);

	}

	/**
	 * Constructor.
	 * 
	 * @param fileList
	 *            List of files from which to read in the MiniSEED data.
	 */
	public SeedSplitter(File[] fileList) {
		super();
		_construct(fileList);
	}

	/**
	 * Get the results after the SeedSplitter has finished processing all files.
	 * 
	 * @return The hash table containing all of the data post filtering and
	 *         re-ordering.
	 */
	public Hashtable<String, ArrayList<DataSet>> getTable() {
		return m_table;
	}

	public Hashtable<String, ArrayList<Integer>> getQualityTable() {
		return m_qualityTable;
	}

	public Hashtable<String, ArrayList<Blockette320>> getCalTable() {
		return m_calTable;
	}

	/**
	 * Get the final progress status.
	 * 
	 * @return A SeedSplitProgress object describing the progress at the
	 *         completion of processing.
	 */
	public SeedSplitProgress getFinalProgress() {
		return m_lastProgress;
	}

	/**
	 * Overrides the doInBackground method of SwingWorker, launching and
	 * monitoring two threads which read the files and process MiniSEED Data.
	 * 
	 * @return A hash table containing all of the data acquired from the file
	 *         list.
	 */
	@Override
	public Hashtable<String, ArrayList<DataSet>> doInBackground() {
		int progressPercent = 0; // 0 - 100
		int lastPercent = 0;
		long totalBytes = 0;
		long progressBytes = 0;
		long stageBytes = 0;
		boolean finalFile = false;
		for (File file : m_files) {
			totalBytes += file.length();
		}

		// Initialize new logging for data errors
		/*
		 * String errorLogFile = appender.getFile(); String[] logsplit =
		 * errorLogFile.split("ERROR"); String dataLogFile = logsplit[0] +
		 * "DATA" + logsplit[1]; appender.setFile(dataLogFile);
		 * appender.activateOptions();
		 */

		SeedSplitProcessor processor = new SeedSplitProcessor(m_recordQueue);
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
			// MTH: SeedSplitProcessor hangs if seed filesize = 0 --> Handled in
			// Scanner.java instead
			// MTH: Skip this file if size = 0:
			// if (file.length() == 0) {
			// continue;
			// }
			DataInputStream inputStream;
			Thread inputThread = null;
			try {
				inputStream = new DataInputStream(new BufferedInputStream(
						new FileInputStream(file)));
				SeedInputStream stream = new SeedInputStream(inputStream,
						m_recordQueue, finalFile);
				inputThread = new Thread(stream);
				logger.debug("Processing file " + file.getName() + "...");
				inputThread.start();
			} catch (FileNotFoundException e) {
				// logger.debug("File '" +file.getName()+ "' not found\n");
				String message = "FileNotFoundException: File '"
						+ file.getName() + "' not found\n";
				datalogger.error(message, e);
				// Should we do something more? Throw an exception?
			}
			m_table = processor.getTable();
			// MTH:
			m_qualityTable = processor.getQualityTable();
			m_calTable = processor.getCalTable();
			if (this.isCancelled()) {
				m_table = null;
				return null;
			}
			if (inputThread != null) {
				try {
					inputThread.join();
				} catch (InterruptedException e) {
					datalogger.error("InterruptedException:", e);
				}
			}
			if (finalFile) {
				try {
					processorThread.join();
				} catch (InterruptedException e) {
					datalogger.error("InterruptedException:", e);
				}
			}
			logger.debug("Finished processing file " + file.getName() + "  "
					+ progressPercent + "% complete");
			stageBytes += file.length();
		}
		logger.debug("All done. Setting progress to 100%");
		this.setProgress(100);
		return m_table;
	}
}
