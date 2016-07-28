package asl.timeseries;

/**
 * The Class TimeseriesException.
 * 
 * This is a generic exception used in the asl.timeseries package.
 */
public class TimeseriesException extends Exception {

	/** The Constant serial version UID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new timeseries exception.
	 */
	TimeseriesException() {
		super();
	}

	/**
	 * Instantiates a new timeseries exception.
	 * 
	 * @param message
	 *            the exception message.
	 */
	TimeseriesException(String message) {
		super(message);
	}
}
