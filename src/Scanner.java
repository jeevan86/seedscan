import java.lang.Runnable;
import java.util.logging.Logger;

public class Scanner
implements Runnable
{
    public static final Logger logger = Logger.getLogger("Scanner");

    public Station station;
    public StationDatabase database;
    public String pathPattern;
    public int scanDepth = 2;

    public Scanner(StationDatabase database, Station station, String pathPattern) {
        this.station  = station;
        this.database = database;
        this.pathPattern = pathPattern
    }

    public void setScanDepth(int scanDepth) {
        this.scanDepth = scanDepth;
    }

    public void run() {
        scan()
    }

    public void scan()
    {
        GregorianCalendar timestamp = new GregorianCalendar();

        for (int i=0; i < scanDepth; i++) {
            timestamp.setTimeInMillis(timestamp.getTimeInMillis() - dayMilliseconds)
            ArchivePath path = new ArchivePath(timestamp, station.getNetwork(), station.getStation(), "", "");
            String path = ArchivePath.makePath(tr1PathPattern);
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
            for (File file: files) {
                ;
            }
        }
    }
}
