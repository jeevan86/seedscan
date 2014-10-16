
package asl.metadata;

/**
 * The Class StationKey.
 * This is used to tie stations in {@link asl.metadata.SeedVolume.stations}
 * 
 * @author Joel Edwards
 * @author Mike Hagerty
 */
public class StationKey extends Key implements Comparable<StationKey> {
	
	/** The Constant STATION_EPOCH_BLOCKETTE_NUMBER. Blockette 50*/
	public static final int STATION_EPOCH_BLOCKETTE_NUMBER = 50;

	/** The network code. */
	private String network = null;
	
	/** The station name. */
	private String name = null;

	/**
	 * Instantiates a new station key based on the station epoch blockette, blockette 50.
	 *
	 * @param blockette the blockette
	 * @throws WrongBlocketteException the wrong blockette exception
	 */
	public StationKey(Blockette blockette) throws WrongBlocketteException {
		if (blockette.getNumber() != STATION_EPOCH_BLOCKETTE_NUMBER) {
			throw new WrongBlocketteException();
		}
		network = blockette.getFieldValue(16, 0);
		name = blockette.getFieldValue(3, 0);
	}

	/**
	 * Instantiates a new station key based on a station object.
	 *
	 * @param station the station
	 */
	public StationKey(Station station) {
		this.network = station.getNetwork();
		this.name = station.getStation();
	}


	/**
	 * Gets the network code.
	 *
	 * @return the network code
	 */
	public String getNetwork() {
		return new String(network);
	}

	/**
	 * Gets the station name.
	 *
	 * @return the station name
	 */
	public String getName() {
		return new String(name);
	}

	/**
	 * @see asl.metadata.Key#toString()
	 * 
	 * @return [network code]_[station name]
	 */
	public String toString() {
		return new String(network + "_" + name);
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(StationKey stnKey) {
		String thisCombo = getNetwork() + getName();
		String thatCombo = stnKey.getNetwork() + stnKey.getName();
		return thisCombo.compareTo(thatCombo);
	}

}
