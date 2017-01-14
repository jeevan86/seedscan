package asl.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class NetworkKey. This is used to identify which volume contains station
 * data in {@link asl.metadata.MetaGenerator#volumes}
 * 
 * @author Mike Hagerty
 * @author James Holland
 */
class NetworkKey extends Key {

	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.NetworkKey.class);

	/**
	 * The Constant DATALESS_VOLUME_BLOCKETTE_NUMBER. Volume info blockettes are
	 * always blockette 10
	 */
	private static final int DATALESS_VOLUME_BLOCKETTE_NUMBER = 10;

	/** The network code e.g. IU, CU, IC. */
	public final String network;

	/**
	 * Instantiates a new network key based on volume info blockette
	 * 
	 * @param blockette
	 *            the volume info blockette, blockette 10
	 * @throws WrongBlocketteException
	 *             the blockette passed was not blockette 10
	 */
	NetworkKey(Blockette blockette) throws WrongBlocketteException {
		if (blockette.getNumber() != DATALESS_VOLUME_BLOCKETTE_NUMBER) {
			throw new WrongBlocketteException();
		}
		// TODO: The assumption that blockette 10 field 9 will always be either
		// XX or XX* is wrong.
		// This field is optional and can vary widely.
		// This varLabel.substring(0,2) is a hack to get it working after Iris
		// switched to XX.dataless
		String varLabel = blockette.getFieldValue(9, 0);
		network = varLabel.substring(0, 2);
		logger.info("Network key created for [{}]", network);
		if (network.equals(varLabel)) {
			logger.warn(
					"Volume label doesn't match determined network. Volume label = [{}]",
					varLabel);
		}
	}

	/**
	 * Instantiates a new network key based on a string passed.
	 * 
	 * @param network
	 *            the network code e.g. IU
	 */
	NetworkKey(String network) {
		this.network = network;
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
		NetworkKey other = (NetworkKey) obj;
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
	 * @see asl.metadata.Key#toString()
	 */
	public String toString() {
		return network;
	}
}
