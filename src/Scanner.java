import java.lang.Runnable;

public class Scanner
implements Runnable
{
    public Station station;
    public StationDatabase database;

    public Scanner(Station station, StationDatabase database) {
        this.station  = station;
        this.database = database;
    }

    public void run() {
        scan()
    }

    public void scan()
    {
        for (int i=0; i < scanDepth; i++) {
            timestamp.setTimeInMillis(timestamp.getTimeInMillis() - dayMilliseconds)
            ArchivePath path = new ArchivePath(timestamp, station.getNetwork(), station.getStation(), "", "");
            String path = ArchivePath.makePath(tr1PathPattern);
            File dir = new File(path);
            if (!dir.isDirectory()) {
                // LOG this
                continue;
            }

            File[] files = listFiles();
            for (File file: files) {
            }
        }
    }
}
