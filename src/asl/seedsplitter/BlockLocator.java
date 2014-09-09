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

import java.util.ArrayList;

import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 * 
 *         The BlockLocator class takes a ArrayList of ArrayList<DataSet>
 *         objects and builds a list of contiguous data segments which are
 *         common across all of the ArrayList<DataSet> objects.
 */
public class BlockLocator extends
		SwingWorker<ArrayList<ContiguousBlock>, ContiguousBlock> {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedsplitter.BlockLocator.class);

	private ArrayList<ArrayList<DataSet>> m_dataLists = null;
	private ArrayList<ContiguousBlock> m_blockList = null;
	private long m_totalDataSets = 0;
	private long m_processedDataSets = 0;
	private int m_percentComplete = 0;
	private BlockLocateProgress m_lastProgress = null;
	/**
	 * Creates a new BlockLocator based on the supplied structure of DataSets.
	 * 
	 * @param dataLists
	 *            A structure of DataSets across which to locate contiguous
	 *            blocks.
	 */
	public BlockLocator(ArrayList<ArrayList<DataSet>> dataLists) {
		super();
		m_dataLists = dataLists;
		m_blockList = new ArrayList<ContiguousBlock>(16);
	}

	/**
	 * Returns a list of the contiguous blocks of data located within the
	 * supplied structure of DataSets.
	 * 
	 * @return An ArrayList of ContiguousBlock objects.
	 */
	public ArrayList<ContiguousBlock> getBlocks() {
		return m_blockList;
	}

	/**
	 * Returns the progress state once BlockLocator has stopped working, either
	 * due to completion of its task, or due to an error.
	 * 
	 * @return A BlockLocateProgress object describing the current progress
	 *         state.
	 */
	public BlockLocateProgress getFinalProgress() {
		return m_lastProgress;
	}

	/**
	 * Searches for contiguous blocks of data across all of the supplied
	 * ArrayList<DataSet> objects.
	 * 
	 * @return A ArrayList of ContiguousBlock objects.
	 */
	@Override
	public ArrayList<ContiguousBlock> doInBackground() {
		// The first ArrayList sets up the base ArrayList of ContiguousBlock
		// objects
		// Step through each of the remaining ArrayLists and build a new group
		// of
		// ContiguousBlock objects that contain a valid subset of the blocks
		// within the original ArrayList and the current data.
		ArrayList<ContiguousBlock> newBlockList = null;

		ArrayList<DataSet> dataList = null;
		for (int i = 0; i < m_dataLists.size(); i++) {
			dataList = m_dataLists.get(i);
			m_totalDataSets += (long) dataList.size();
			logger.debug("Data Set Count: " + dataList.size() + "("
					+ m_totalDataSets + ")");
		}

		m_blockList = _buildFirstList(m_dataLists.get(0));
		for (int i = 0; i < m_dataLists.size(); i++) {
			if (this.isCancelled()) {
				break;
			}
			try {
				newBlockList = _buildDependentList(m_dataLists.get(i),
						m_blockList);
				m_blockList = newBlockList;
			} catch (BlockIntervalMismatchException e) {
				double lastPercent = m_lastProgress.getProgressPercent();
				m_lastProgress = new BlockLocateProgress(lastPercent,
						BlockLocateError.INTERVAL_MISMATCH,
						"Interval (sample rate) does not match across channels.");
				logger.debug("Interval (sample rate) does not match across channels.");
				break;
			}
		}

		if (this.isCancelled()) {
			m_blockList = null;
		} else if (m_lastProgress.errorOccurred()) {
			m_blockList = null;
		} else {
			this.setProgress(100);
			m_lastProgress = new BlockLocateProgress(100.0, true);
		}

		return m_blockList;
	}

	/**
	 * Generates the initial list of contiguous data regions.
	 * 
	 * @param dataList
	 *            A list of DataSet objects containing the data from a channel.
	 * @return An ArrayList of ContiguousBlock objects.
	 */
	private ArrayList<ContiguousBlock> _buildFirstList(
			ArrayList<DataSet> dataList) {
		ArrayList<ContiguousBlock> resultList = new ArrayList<ContiguousBlock>();
		DataSet tempData = null;
		ContiguousBlock tempBlock = null;

		for (int i = 0; i < dataList.size(); i++) {
			if (this.isCancelled()) {
				resultList = null;
				break;
			}
			tempData = dataList.get(i);
			tempBlock = new ContiguousBlock(tempData.getStartTime(),
					tempData.getEndTime(), tempData.getInterval());
			resultList.add(tempBlock);
			this._updateProgress();
		}

		return resultList;
	}

	/**
	 * Updates the list of contiguous data blocks based on the data in an
	 * additional data list.
	 * 
	 * @param dataList
	 *            A new group of data which will be used to update the list of
	 *            contiguous data blocks.
	 * @param blockList
	 *            The previous list of contiguous data blocks.
	 * @return A new list of contiguous data blocks.
	 * @throws BlockIntervalMismatchException
	 *             If the sample rate of any of the DataSets does not match with
	 *             those of the ContiguousBlocks.
	 */
	private ArrayList<ContiguousBlock> _buildDependentList(
			ArrayList<DataSet> dataList, ArrayList<ContiguousBlock> blockList)
			throws BlockIntervalMismatchException {
		ArrayList<ContiguousBlock> resultList = new ArrayList<ContiguousBlock>();
		DataSet tempData = null;
		ContiguousBlock oldBlock = null;
		ContiguousBlock newBlock = null;
		long startTime = 0;
		long endTime = 0;

		for (int dataIndex = 0, blockIndex = 0; (dataIndex < dataList.size())
				&& (blockIndex < blockList.size());) {
			if (this.isCancelled()) {
				resultList = null;
				break;
			}

			tempData = dataList.get(dataIndex);
			oldBlock = blockList.get(blockIndex);

			if (tempData.getInterval() != oldBlock.getInterval()) {
				throw new BlockIntervalMismatchException();
			}

			if (tempData.getEndTime() <= oldBlock.getStartTime()) {
				dataIndex++;
			} else if (tempData.getStartTime() >= oldBlock.getEndTime()) {
				blockIndex++;
			} else {
				// Ensure the new block is a subset of the time within the old
				// block.
				if (tempData.getStartTime() < oldBlock.getStartTime()) {
					startTime = oldBlock.getStartTime();
				} else {
					startTime = tempData.getStartTime();
				}
				if (tempData.getEndTime() > oldBlock.getEndTime()) {
					endTime = oldBlock.getEndTime();
					blockIndex++;
				} else {
					endTime = tempData.getEndTime();
					dataIndex++;
				}
				newBlock = new ContiguousBlock(startTime, endTime,
						tempData.getInterval());
				resultList.add(newBlock);
			}
			this._updateProgress();
		}

		return resultList;
	}

	/**
	 * Tracks the BlockLocator's progress, updating the parent's progress as
	 * necessary.
	 */
	private void _updateProgress() {
		int lastPercent = m_percentComplete;
		m_processedDataSets++;

		if (m_totalDataSets == 0) {
			m_percentComplete = 99;
		} else {
			m_percentComplete = (int) (m_processedDataSets * 100L / m_totalDataSets);
			if (m_percentComplete > 99) {
				m_percentComplete = 99;
			}
		}
		if (lastPercent < m_percentComplete) {
			this.setProgress(m_percentComplete);
			m_lastProgress = new BlockLocateProgress(m_percentComplete);
			logger.debug("Total DataSets: " + m_totalDataSets);
			logger.debug("Progress:       " + m_percentComplete + "%");
		}
	}
}
