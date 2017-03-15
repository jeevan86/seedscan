package asl.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Creates a lock based on a given file name.
 * It creates an empty file that can be locked and released.
 * Designed to prevent multiple applications from starting simultaneously.
 */
public class LockFile {

  /**
   * The file.
   */
  private File file;

  /**
   * The FileChannel closed in release().
   */
  private FileChannel channel;

  /**
   * The file lock that is maintains the lock state.
   */
  private FileLock lock;

  /**
   * Instantiates a new lock file.
   *
   * @param file name of our lock file.
   */
  public LockFile(String file) {
    this.file = new File(file);
  }

  /**
   * Acquire a lock on the file.
   *
   * @return true, if a lock was acquired
   */
  @SuppressWarnings("resource") //RandomAccessFile is closed when channel is closed in release()
  public boolean acquire() {
    try {
      channel = new RandomAccessFile(file, "rw").getChannel();
      lock = channel.tryLock();
    } catch (IOException e) {
      if (channel != null) {
        try {
          channel.close();
        } catch (IOException ignored) {
        }
      }
    }
    return hasLock();
  }

  /**
   * Release the lock, truncate the lock file and close out the streams.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void release() throws IOException {
    if (this.hasLock()) {
      lock.release();
      lock = null;
      channel.close();
      file.delete();
    }
  }

  /**
   * Checks for lock.
   *
   * @return true, if there is a valid lock.
   */
  private boolean hasLock() {
    return (lock != null) && (lock.isValid());
  }
}
