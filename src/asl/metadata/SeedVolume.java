package asl.metadata;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeedVolume {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.SeedVolume.class);

	private Blockette volumeInfo = null;
	private NetworkKey networkKey = null;

	private ArrayList<Blockette> stationLocators;
	private Hashtable<StationKey, StationData> stations;

	// constructor(s)
	public SeedVolume() {
		stations = new Hashtable<StationKey, StationData>();
		stationLocators = new ArrayList<Blockette>();
	}

	public SeedVolume(Blockette volumeInfo) {
		this.volumeInfo = volumeInfo;
		try {
			this.networkKey = new NetworkKey(volumeInfo);
		} catch (WrongBlocketteException e) {
			logger.error("== WrongBlocketteException:", e);
		}
		stations = new Hashtable<StationKey, StationData>();
		stationLocators = new ArrayList<Blockette>();
	}

	// stations
	public void addStation(StationKey key, StationData data) {
		stations.put(key, data);
	}

	public boolean hasStation(StationKey key) {
		return stations.containsKey(key);
	}

	public StationData getStation(StationKey key) {
		return stations.get(key);
	}

	// volume info
	public void setVolumeInfo(Blockette volumeInfo) {
		this.volumeInfo = volumeInfo;
	}

	public Blockette getVolumeInfo() {
		return this.volumeInfo;
	}

	public NetworkKey getNetworkKey() {
		return this.networkKey;
	}

	// station locators (list of stations in seed volume)
	public void addStationLocator(Blockette stationLocator) {
		stationLocators.add(stationLocator);
	}

	public ArrayList<Blockette> getStationLocators() {
		return stationLocators;
	}

	public void printStations() {
		TreeSet<StationKey> keys = new TreeSet<StationKey>();
		keys.addAll(stations.keySet());
		for (StationKey key : keys) {
			System.out.format("     == SeedVolume: Stn = [%s]\n", key);
		}
	}

	public List<Station> getStationList() {
		ArrayList<Station> stns = new ArrayList<Station>();
		TreeSet<StationKey> keys = new TreeSet<StationKey>();
		keys.addAll(stations.keySet());

		for (StationKey key : keys) {
			stns.add(new Station(key.getNetwork(), key.getName()));
		}
		return stns;
	}

}
