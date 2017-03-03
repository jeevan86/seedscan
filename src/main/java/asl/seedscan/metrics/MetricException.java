package asl.seedscan.metrics;

/**
 * The Class MetricException.
 * 
 * This is a generic exception used when a metric needs an exception.
 */
class MetricException extends Exception {

	/** The Constant serial version UID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new metric exception.
	 * 
	 * @param message
	 *            the exception message.
	 */
	MetricException(String message) {
		super(message);
	}

	/**
	 * Instantiates metric exception with a cause.
	 * Used when multiple exceptions cause exception in a single method.
	 * 
	 * @param message Error message
	 * @param cause Causing Throwable
	 */
	public MetricException(String message, Throwable cause) {
		super(message, cause);
	}
}
