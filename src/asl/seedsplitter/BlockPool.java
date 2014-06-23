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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 * 
 *         Keeps a pool of integer blocks of uniform size into which existing
 *         blocks can be injected in order to minimize the need for new
 *         allocations.
 */
public class BlockPool {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedsplitter.BlockPool.class);

	private ArrayList<int[]> m_pool;
	private int m_blockSize = 0;

	/**
	 * Constructor.
	 * 
	 * @param blockSize
	 *            The size of each block within the pool.
	 */
	public BlockPool(int blockSize) {
		m_blockSize = blockSize;
		m_pool = new ArrayList<int[]>(8);
	}

	/**
	 * Returns the block size.
	 * 
	 * @return An integer value representing the block size.
	 */
	public int getBlockSize() {
		return m_blockSize;
	}

	/**
	 * Adds a new block
	 * 
	 * @param block
	 *            The block to inject into the pool.
	 * @throws BlockSizeMismatchException
	 *             If the size of the added block does not match this
	 *             BlockPool's block size.
	 */
	public void addBlock(int[] block) throws BlockSizeMismatchException {
		if (block.length != m_blockSize) {
			throw new BlockSizeMismatchException(
					"BlockSizeMismatchException: block.length != m_blockSize");
		}
		m_pool.add(block);
	}

	/**
	 * Returns a block from the pool if it contains any blocks, otherwise a new
	 * block is allocated.
	 * 
	 * @return A new block either from the pool, or freshly allocated if the
	 *         pool is empty.
	 */
	public synchronized int[] getNewBlock() {
		int[] block = null;
		if (m_pool.size() > 0) {
			block = m_pool.remove(0);
		} else {
			block = new int[m_blockSize];
		}
		return block;
	}
}
