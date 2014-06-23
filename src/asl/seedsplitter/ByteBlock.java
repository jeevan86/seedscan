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

/**
 * @author Joel Edwards <jdedwards@usgs.gov>
 * 
 *         The ByteBlock class wraps blocks of data read from a stream.
 */
public class ByteBlock {
	private byte[] m_data = null;
	private int m_skippedBytes = 0;
	private boolean m_fileEnd = false;
	private boolean m_lastBlock = false;

	/**
	 * Constructor.
	 * 
	 * @param data
	 *            The raw data.
	 * @param length
	 *            Length of the raw data.
	 */
	public ByteBlock(byte[] data, int length) {
		init(data, length, 0, false, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *            The raw data.
	 * @param length
	 *            Length of the raw data.
	 * @param skipped
	 *            Number of bytes skipped when reading data.
	 */
	public ByteBlock(byte[] data, int length, int skipped) {
		init(data, length, skipped, false, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *            The raw data.
	 * @param length
	 *            Length of the raw data.
	 * @param fileEnd
	 *            End of the file has been reached.
	 * @param isLast
	 *            This is the last data in a stream.
	 */
	public ByteBlock(byte[] data, int length, boolean fileEnd, boolean isLast) {
		init(data, length, 0, fileEnd, isLast);
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *            The raw data.
	 * @param length
	 *            Length of the raw data.
	 * @param skipped
	 *            Number of bytes skipped when reading data.
	 * @param fileEnd
	 *            End of the file has been reached.
	 * @param isLast
	 *            This is the last data in a stream.
	 */
	public ByteBlock(byte[] data, int length, int skipped, boolean fileEnd,
			boolean isLast) {
		init(data, length, skipped, fileEnd, isLast);
	}

	/**
	 * Hidden initializer called by all constructors.
	 * 
	 * @param data
	 *            The raw data.
	 * @param length
	 *            Length of the raw data.
	 * @param skipped
	 *            Number of bytes skipped when reading data.
	 * @param fileEnd
	 *            End of the file has been reached.
	 * @param isLast
	 *            This is the last data in a stream.
	 */
	private void init(byte[] data, int length, int skipped, boolean fileEnd,
			boolean isLast) {
		if (length > 0) {
			m_data = new byte[length];
			System.arraycopy(data, 0, m_data, 0, length);
		}
		m_lastBlock = isLast;
		m_fileEnd = fileEnd;
		m_skippedBytes = skipped;
	}

	/**
	 * Returns the data buffer.
	 * 
	 * @return The data buffer.
	 */
	public byte[] getData() {
		return m_data;
	}

	/**
	 * Returns the length of the buffer.
	 * 
	 * @return The length of the buffer.
	 */
	public int getLength() {
		return (m_data == null) ? 0 : m_data.length;

	}

	/**
	 * Returns the number of bytes skipped while reading.
	 * 
	 * @return The number of bytes skipped while reading.
	 */
	public int getSkippedBytes() {
		return m_skippedBytes;
	}

	/**
	 * Indicates whether this is the last block in the sequence.
	 * 
	 * @return True if this is the last block in the sequence; otherwise false.
	 */
	public boolean isLast() {
		return m_lastBlock;
	}

	/**
	 * Indicates whether the end of the file has been reached.
	 * 
	 * @return True if the end of the file has been reached; otherwise false.
	 */
	public boolean isEnd() {
		return m_fileEnd;
	}

	/**
	 * Set last block state.
	 * 
	 * @param last
	 *            Is this the last block.
	 */
	public void setLast(boolean last) {
		m_lastBlock = last;
	}

	/**
	 * Set file end reached state.
	 * 
	 * @param end
	 *            Has the end of the file been reached.
	 */
	public void setEnd(boolean end) {
		m_fileEnd = end;
	}

	/**
	 * Clear last block state.
	 */
	public void clearLast() {
		m_lastBlock = false;
	}
}
