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
import java.lang.management.RuntimeMXBean;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockFile
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.LockFile.class);

    private File        file;
    private FileChannel channel;
    private FileLock    lock;

    public LockFile (File file)
    {
        this.file = file;
    }

    public LockFile (String file)
    {
        this.file = new File(file);
        this.file.setReadable(true, false);
        this.file.setWritable(true, false);
    }

    public boolean acquire() {
        boolean success = false;
        try {
            channel = new RandomAccessFile(file, "rw").getChannel();
            lock = channel.tryLock(0, Long.MAX_VALUE, false /*Shared*/);
            if (lock != null) {
                success = true;
                channel.truncate(0);
                String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
                String newLine = System.getProperty("line.seperator");
                channel.write(ByteBuffer.wrap((pid + newLine).getBytes()));
            }
        } catch (FileNotFoundException e) {
        	logger.error("LockFile FileNotFoundException:", e);
        } catch (IOException e) {
        	logger.error("LockFile IOException:", e);
        }
        return success;
    }

    public void release() throws IOException {
        if (this.hasLock()) {
            channel.truncate(0);
            lock.release();
            lock = null;
        }
    }

    public boolean hasLock() {
        boolean locked = false;
        if ((lock != null) && (lock.isValid())) {
            locked = true;
        }
        return locked;
    }
}
