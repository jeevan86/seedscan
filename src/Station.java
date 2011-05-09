
public class Station
{
    private String network;
    private String station;

    public Station(String network, String station)
    {
        setNetwork(network);
        setStation(station);
    }

    public void setNetwork() {
        if (network != null) {
            if (network.length() > 2) {
                throw new RuntimeError("network name is too long");
            }
            this.network = network;
        }
    }

    public void setStation() {
        if (station == null) {
            throw new RuntimeError("station cannot be null");
        }
        if (station.length() < 1) {
            throw new RuntimeError("station name is too short");
        }
        if (station.length() > 5) {
            throw new RuntimeError("station name is too long");
        }
        this.station = station;
    }


    public String getNetwork() {
        return network;
    }

    public String getStation() {
        return station;
    }
}
