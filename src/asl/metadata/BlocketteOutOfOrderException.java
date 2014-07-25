package asl.metadata;

/**
 * The Class BlocketteOutOfOrderException.
 * This is thrown when a blockette comes before its descriptive blockette e.g. any blockette before blockette 10 is processed.
 */
public class BlocketteOutOfOrderException extends Exception {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new blockette out of order exception.
	 */
	public BlocketteOutOfOrderException() {
		super();
	}

	/**
	 * Instantiates a new blockette out of order exception with a message.
	 *
	 * @param message the exception message
	 */
	public BlocketteOutOfOrderException(String message) {
		super(message);
	}
}
