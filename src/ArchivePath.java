import java.io.File;

/**
 * 
 */
public class ArchivePath
{
    private GregorianCalendar timestamp;
    private Station station = null;
    private Channel channel = null;

    public SeedPath(GregorianCalendar timestamp, Station station)
    {
        this.timestamp = timestamp;
        this.station = station;
    }

    public SeedPath(GregorianCalendar timestamp, Channel channel)
    {
        this.timestamp = timestamp;
        this.station = channel.getStation();
        this.channel = channel;
    }

    public void setTimestamp(GregorianCalendar timestamp) {
        this.timestamp = timestamp;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public void setChannel(Channel channel) {
        this.station = channel.getStation();
        this.channel = channel;
    }

    public String makePath(String pattern)
    {
        int startIndex = 0;
        int  lastIndex = 0;
        if (station != null) {
            if (station.getNetwork() != null) {
                pattern = pattern.replace("${NETWORK}", station.getNetwork());
            }
            pattern = pattern.replace("${STATION}", station.getStation());
        }
        if (channel != null) {
            if (station.getLocation() != null) {
                pattern = pattern.replace("${LOCATION}", channel.getLocation());
            }
            pattern = pattern.replace("${CHANNEL}", channel.getChannel());
        }
        return String.format(pattern, timestamp);
    }
}
