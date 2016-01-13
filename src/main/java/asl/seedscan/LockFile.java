/*
 * Copyright 2011, United States Geological Survey or
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

package asl.seedscan;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class LockFile.
 */
public class LockFile {
	
	/** The Constant logger. */
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.LockFile.class);

	/** The file. */
	private File file;
	
	/** The FileChannel closed in release(). */
	private FileChannel channel;
	
	/** The lock. */
	private FileLock lock;

	/**
	 * Instantiates a new lock file.
	 *
	 * @param file the file
	 */
	public LockFile(File file) {
		this.file = file;
	}

	/**
	 * Instantiates a new lock file.
	 *
	 * @param file name of our lock file.
	 */
	public LockFile(String file) {
		this.file = new File(file);
		this.file.setReadable(true, false);
		this.file.setWritable(true, false);
	}

	/**
	 * Acquire a lock on the file.
	 *
	 * @return true, if a lock was acquired
	 */
	@SuppressWarnings("resource") //RandomAccessFile is closed when channel is closed in release()
	public boolean acquire() {
		boolean success = false;
		try {
			channel = new RandomAccessFile(file, "rw").getChannel();
			lock = channel.tryLock(0, Long.MAX_VALUE, false /* Shared */);
			if (lock != null) {
				success = true;
				channel.truncate(0);
				String pid = ManagementFactory.getRuntimeMXBean().getName()
						.split("@")[0];
				String newLine = System.getProperty("line.seperator");
				channel.write(ByteBuffer.wrap((pid + newLine).getBytes()));
			}
		} catch (FileNotFoundException e) {
			logger.error("FileNotFoundException:", e);
			if(channel != null)
			{
				try {
					channel.close();
				} catch (IOException ignored) {
				}
			}
		} catch (IOException e) {
			logger.error("IOException:", e);
			if(channel != null)
			{
				try {
					channel.close();
				} catch (IOException ignored) {
				}
			}
		}
		return success;
	}

	/**
	 * Release the lock, truncate the lock file and close out the streams.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void release() throws IOException {
		if (this.hasLock()) {
			channel.truncate(0);
			lock.release();
			lock = null;
			channel.close();
		}
	}

	/**
	 * Checks for lock.
	 *
	 * @return true, if there is a valid lock.
	 */
	public boolean hasLock() {
		boolean locked = false;
		if ((lock != null) && (lock.isValid())) {
			locked = true;
		}
		return locked;
	}
}
