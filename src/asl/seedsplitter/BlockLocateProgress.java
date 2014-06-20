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

import asl.worker.Progress;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 * 
 *         Progress feedback class for BlockLocator.
 */
public class BlockLocateProgress implements Progress {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedsplitter.BlockLocateProgress.class);

	private BlockLocateError m_errorType = BlockLocateError.NONE;
	private String m_errorMessage;

	private double m_percentComplete;
	private boolean m_complete;

	/**
	 * Creates a new instance of BlockLocateProgress.
	 * 
	 * @param percentComplete
	 *            The percentage of the total number of blocks which have been
	 *            processed.
	 */
	public BlockLocateProgress(double percentComplete) {
		_init(percentComplete, false, BlockLocateError.NONE, null);
	}

	/**
	 * Creates a new instance of BlockLocateProgress.
	 * 
	 * @param percentComplete
	 *            The percentage of the total number of blocks which have been
	 *            processed.
	 * @param complete
	 *            A boolean value signifying whether the location operation is
	 *            complete.
	 */
	public BlockLocateProgress(double percentComplete, boolean complete) {
		_init(percentComplete, complete, BlockLocateError.NONE, null);
	}

	/**
	 * Creates a new instance of BlockLocateProgress.
	 * 
	 * @param percentComplete
	 *            The percentage of the total number of blocks which have been
	 *            processed.
	 * @param errorType
	 *            The type of error (if any) which occurred.
	 * @param errorMessage
	 *            A message providing greater detail about the error, if one
	 *            occurred.
	 */
	public BlockLocateProgress(double percentComplete,
			BlockLocateError errorType, String errorMessage) {
		_init(percentComplete, false, errorType, errorMessage);
	}

	/**
	 * Hidden initializer called by every constructor.
	 * 
	 * @param percentComplete
	 *            The percentage of the total number of blocks which have been
	 *            processed.
	 * @param complete
	 *            A boolean value signifying whether the location operation is
	 *            complete.
	 * @param errorType
	 *            The type of error (if any) which occurred.
	 * @param errorMessage
	 *            A message providing greater detail about the error, if one
	 *            occurred.
	 */
	private void _init(double percentComplete, boolean complete,
			BlockLocateError errorType, String errorMessage) {
		switch (errorType) {
		case NONE:
			m_errorMessage = null;
			break;
		case INTERVAL_MISMATCH:
			m_errorMessage = errorType
					+ ": "
					+ ((errorMessage == null) ? "No Error Information"
							: errorMessage);
			break;
		}
		m_errorType = errorType;
		m_complete = complete;
		m_percentComplete = percentComplete;
	}

	/**
	 * Informs the caller whether the location operation is complete.
	 * 
	 * @return A boolean value: true if the location operation is complete,
	 *         otherwise false.
	 */
	public boolean isComplete() {
		return m_complete;
	}

	/**
	 * Informs the caller whether an error occurred.
	 * 
	 * @return A boolean value: true if an error occurred, otherwise false.
	 */
	public boolean errorOccurred() {
		return m_errorType != BlockLocateError.NONE;
	}

	/**
	 * Reports what type of error (if any) occurred.
	 * 
	 * @return A value of the BlockLocateError enumerated type.
	 */
	public BlockLocateError getErrorType() {
		return m_errorType;
	}

	/**
	 * Returns the error message if one exists.
	 * 
	 * @return A String value describing the error if one occurred, otherwise
	 *         null.
	 */
	public String getErrorMessage() {
		return m_errorMessage;
	}

	/**
	 * Returns the percentage of the total number of blocks which have been
	 * processed.
	 * 
	 * @return A double value between 0.0 and 1.0 representing the percentage of
	 *         the total number of blocks which have been processed.
	 */
	public double getProgressPercent() {
		return m_percentComplete;
	}
}
