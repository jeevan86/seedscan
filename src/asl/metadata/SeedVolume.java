package asl.metadata;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class SeedVolume.
 * 
 * @author Joel Edwards - USGS
 * @author Mike Hagerty
 */
public class SeedVolume {

	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.SeedVolume.class);

	/** The volume info blockette: blockette 10. */
	private Blockette volumeInfo = null;

	/** The network key. This is based on info blockette field 9: volume label */
	private NetworkKey networkKey = null;

	/** The station locators. */
	private ArrayList<Blockette> stationLocators;

	/** The stations. */
	private Hashtable<StationKey, StationData> stations;

	/**
	 * Instantiates a new seed volume.
	 */
	public SeedVolume() {
		stations = new Hashtable<StationKey, StationData>();
		stationLocators = new ArrayList<Blockette>();
	}

	/**
	 * Instantiates a new seed volume.
	 * 
	 * @param volumeInfo
	 *            the volume info
	 */
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

	/**
	 * Adds a station to the station hashtable
	 * 
	 * @param key
	 *            the station key
	 * @param data
	 *            the station metadata
	 */
	public void addStation(StationKey key, StationData data) {
		stations.put(key, data);
	}

	/**
	 * Checks for station in the station hashtable
	 * 
	 * @param key
	 *            the key
	 * @return true, if station hashtable has stationkey
	 */
	public boolean hasStation(StationKey key) {
		return stations.containsKey(key);
	}

	/**
	 * Gets the station metadata.
	 * 
	 * @param key
	 *            the key
	 * @return the station
	 */
	public StationData getStation(StationKey key) {
		return stations.get(key);
	}

	// volume info
	/**
	 * Sets the volume info.
	 * 
	 * @param volumeInfo
	 *            the new volume info
	 */
	public void setVolumeInfo(Blockette volumeInfo) {
		this.volumeInfo = volumeInfo;
	}

	/**
	 * Gets the volume info.
	 * 
	 * @return the volume info
	 */
	public Blockette getVolumeInfo() {
		return this.volumeInfo;
	}

	/**
	 * Gets the network key.
	 * 
	 * @return the network key
	 */
	public NetworkKey getNetworkKey() {
		return this.networkKey;
	}

	/**
	 * station locators (list of stations in seed volume)
	 * 
	 * @param stationLocator
	 *            the station locator
	 */
	public void addStationLocator(Blockette stationLocator) {
		stationLocators.add(stationLocator);
	}

	/**
	 * Gets the station locators.
	 * 
	 * @return the station locators
	 */
	public ArrayList<Blockette> getStationLocators() {
		return stationLocators;
	}

	/**
	 * Prints the stations.
	 * 
	 * @deprecated It is no longer used.
	 */
	public void printStations() {
		TreeSet<StationKey> keys = new TreeSet<StationKey>();
		keys.addAll(stations.keySet());
		for (StationKey key : keys) {
			System.out.format("     == SeedVolume: Stn = [%s]\n", key);
		}
	}

	/**
	 * Gets the station list.
	 * 
	 * @return the station list
	 */
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
