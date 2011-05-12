
public class Channel
{
    private Station station  = null;
    private String  location = "";
    private String  channel  = "";

    public Channel ()
    {
        setStation(station);
        setLocation(location);
        setChannel(channel);
    }

    public void setStation(Station station) {
        if (station == null) {
            throw new RuntimeException("station cannot be null");
        }
        this.station = station;
    }

    public void setLocation(String location) {
        if (location != null) {
            if (location.length() > 2) {
                throw new RuntimeException("location name is too long");
            }
            this.location = location;
        }
    }

    public void setChannel(String channel) {
        if (channel == null) {
            throw new RuntimeException("channel cannot be null");
        }
        if (channel.length() < 1) {
            throw new RuntimeException("channel name is too short");
        }
        if (channel.length() > 3) {
            throw new RuntimeException("channel name is too long");
        }
        this.channel = channel;
    }


    public Station getStation() {
        return station;
    }

    public String getLocation() {
        return location;
    }

    public String getChannel() {
        return channel;
    }
}
