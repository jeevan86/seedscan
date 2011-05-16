import java.io.File;
import java.util.Formatter;
import java.util.GregorianCalendar;

/**
 * 
 */
public class ArchivePath
{
    private GregorianCalendar timestamp;
    private Station station = null;
    private Channel channel = null;

    public ArchivePath(GregorianCalendar timestamp, Station station)
    {
        this.timestamp = timestamp;
        this.station = station;
    }

    public ArchivePath(GregorianCalendar timestamp, Channel channel)
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
            if (channel.getLocation() != null) {
                pattern = pattern.replace("${LOCATION}", channel.getLocation());
            }
            pattern = pattern.replace("${CHANNEL}", channel.getChannel());
        }
        pattern = pattern.replace("${YEAR}",   String.format("%$1tY"));
        pattern = pattern.replace("${MONTH}",  String.format("%$1tm"));
        pattern = pattern.replace("${DAY}",    String.format("%$1td"));
        pattern = pattern.replace("${JDAY}",   String.format("%$1tj"));
        pattern = pattern.replace("${HOUR}",   String.format("%$1tH"));
        pattern = pattern.replace("${MINUTE}", String.format("%$1tM"));
        pattern = pattern.replace("${SECOND}", String.format("%$1tS"));

        return pattern;
    }
}
