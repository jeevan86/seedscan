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
public class NetworkKey extends Key {

	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.NetworkKey.class);

	/**
	 * The Constant DATALESS_VOLUME_BLOCKETTE_NUMBER. Volume info blockettes are
	 * always blockette 10
	 */
	public static final int DATALESS_VOLUME_BLOCKETTE_NUMBER = 10;

	/** The network code e.g. IU, CU, IC. */
	private String network = null;

	/**
	 * Instantiates a new network key based on volume info blockette
	 * 
	 * @param blockette
	 *            the volume info blockette, blockette 10
	 * @throws WrongBlocketteException
	 *             the blockette passed was not blockette 10
	 */
	public NetworkKey(Blockette blockette) throws WrongBlocketteException {
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
		if (network != varLabel) {
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
	public NetworkKey(String network) {
		this.network = network;
	}

	/**
	 * Gets the network code
	 * 
	 * @return the network code
	 */
	public String getNetwork() {
		return network;
	}

	/**
	 * @see asl.metadata.Key#toString()
	 */
	public String toString() {
		return network;
	}
}
