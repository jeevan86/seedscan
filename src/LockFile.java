import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class LockFile
{
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
            ;
        } catch (IOException e) {
            ;
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
