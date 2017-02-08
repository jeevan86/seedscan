package asl.metadata;

/**
 * The Class StationKey.
 * This is basically a copy of {@link asl.metadata.Station}
 * This is used to tie stations in {@link asl.metadata.SeedVolume#stations}
 * 
 * @author James Holland - USGS
 * @author Joel Edwards - USGS
 * @author Mike Hagerty
 */
class StationKey extends Key implements Comparable<StationKey> {
	
	/** The Constant STATION_EPOCH_BLOCKETTE_NUMBER. Blockette 50*/
	private static final int STATION_EPOCH_BLOCKETTE_NUMBER = 50;

	/** The network code. */
	private final String network;
	
	/** The station name. */
	private final String name;

	/**
	 * Instantiates a new station key based on the station epoch blockette, blockette 50.
	 *
	 * @param blockette the blockette
	 * @throws WrongBlocketteException the wrong blockette exception
	 */
	StationKey(Blockette blockette) throws WrongBlocketteException {
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
	StationKey(Station station) {
		this.network = station.getNetwork();
		this.name = station.getStation();
	}
	
	StationKey(String network, String station){
		this.network = network;
		this.name = station;
	}


	/**
	 * Gets the network code.
	 *
	 * @return the network code
	 */
	public String getNetwork() {
		return network;
	}

	/**
	 * Gets the station name.
	 *
	 * @return the station name
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see asl.metadata.Key#toString()
	 * 
	 * @return [network code]_[station name]
	 */
	public String toString() {
		return network + "_" + name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((network == null) ? 0 : network.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StationKey other = (StationKey) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (network == null) {
			if (other.network != null)
				return false;
		} else if (!network.equals(other.network))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(StationKey stnKey) {
		String thisCombo = getNetwork() + getName();
		String thatCombo = stnKey.getNetwork() + stnKey.getName();
		return thisCombo.compareTo(thatCombo);
	}
}
