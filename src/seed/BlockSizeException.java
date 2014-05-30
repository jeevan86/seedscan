package seed;

/**
 * A type of exception specific to problems encountered with
 * data offsets being larger than data block sizes
 * @author agonzales
 *
 */

public class BlockSizeException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public BlockSizeException() {
		super();
	}
	
	public BlockSizeException(String s) {
		super(s);
	}
}
