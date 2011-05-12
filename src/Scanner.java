import java.io.File;
import java.io.IOException;
import java.lang.Runnable;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

public class Scanner
implements Runnable
{
    public static final Logger logger = Logger.getLogger("Scanner");
    public long dayMilliseconds = 1000 * 60 * 60 * 24;

    public Station station;
    public StationDatabase database;
    public String pathPattern;
    public int scanDepth  = 2;
    public int startDepth = 1;

    public Scanner(StationDatabase database, Station station, String pathPattern) {
        this.station  = station;
        this.database = database;
        this.pathPattern = pathPattern;
    }

    public void setScanDepth(int scanDepth) {
        this.scanDepth = scanDepth;
    }

    public void run() {
        scan();
    }

    public void scan()
    {
        GregorianCalendar timestamp = new GregorianCalendar();
        if (startDepth > 0) {
            timestamp.setTimeInMillis(timestamp.getTimeInMillis() - (startDepth * dayMilliseconds));
        }

        for (int i=0; i < scanDepth; i++) {
            if (i != 0) {
                timestamp.setTimeInMillis(timestamp.getTimeInMillis() - dayMilliseconds);
            }
            ArchivePath pathEngine = new ArchivePath(timestamp, station);
            String path = pathEngine.makePath(pathPattern);
            File dir = new File(path);
            if (!dir.exists()) {
                logger.info("Path '" +dir+ "' does not exist.");
                continue;
            }
            else if (!dir.isDirectory()) {
                logger.info("Path '" +dir+ "' is not a directory.");
                continue;
            }

            File[] files = dir.listFiles();
            int seedCount = 0;
            for (File file: files) {
                if (file.getName().endsWith(".seed")) {
                    seedCount++;
                    logger.fine("Processing file '" +file.getPath()+ "'.");
                }
            }
            logger.info(dir.getPath() + " contains " +seedCount+ " MiniSEED files.");
        }
    }
}
