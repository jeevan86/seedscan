package asl.metadata;

/**
 * The Class BlocketteFieldIdentifierFormatException.
 * This exception is thrown when the field identifier is invalid.
 * 
 * @author Joel Edwards - USGS
 */
class BlocketteFieldIdentifierFormatException extends Exception {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new blockette field identifier format exception.
	 *
	 * @param message the specific message
	 * 
	 * @see asl.metadata.Blockette#addFieldData(String, String)
	 */
	BlocketteFieldIdentifierFormatException(String message) {
		super(message);
	}
}
