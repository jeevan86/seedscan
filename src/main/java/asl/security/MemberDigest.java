package asl.security;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The Class MemberDigest.
 *
 * @author Joel D. Edwards - USGS
 * @author James Holland - USGS
 */
public abstract class MemberDigest implements Serializable {

	/** The Serial Version UID. */
	private static final long serialVersionUID = 1L;

	/** The digest. */
	private transient MessageDigest digest = null;

	/** The raw digest, this is inserted into the database. */
	private transient ByteBuffer raw = null;

	/**
	 * Default Constructor. Uses MD5 as its hashing algorithm.
	 * 
	 * @throws RuntimeException
	 *             if MD5 digest type could not be used.
	 */
	public MemberDigest() throws RuntimeException {
		this("MD5");
	}

	/**
	 * Instantiates a new member digest.
	 *
	 * @param algorithm
	 *            the algorithm
	 * @throws RuntimeException
	 *             if the passed algorithm could not be used.
	 */
	public MemberDigest(String algorithm) throws RuntimeException {
		setAlgorithm(algorithm);
	}

	/**
	 * Adds the digest members.
	 */
	protected abstract void addDigestMembers();

	/**
	 * Compute digest. Calls the abstract {@link #addDigestMembers()};
	 */
	private synchronized void computeDigest() {
		digest.reset();
		addDigestMembers();
		raw = ByteBuffer.wrap(digest.digest());
	}

	/**
	 * Gets the digest bytes. Calls {@link #computeDigest()} which in turn calls
	 * the abstracted {@link #addDigestMembers()}
	 *
	 * @return the digest ByteBuffer {@link #raw}
	 */
	public ByteBuffer getDigestBytes() {
		computeDigest();
		return raw;
	}

	/**
	 * Adds a byte[] with offset to digest.
	 *
	 * @param data
	 *            the data
	 * @param offset
	 *            the offset
	 * @param length
	 *            the length
	 */
	// Methods for adding member variables' data to the digest
	private void addToDigest(byte[] data, int offset, int length) {
		digest.update(data, offset, length);
	}

	/**
	 * Adds a byte[] to digest.
	 *
	 * @param data
	 *            the data
	 */
	private void addToDigest(byte[] data) {
		addToDigest(data, 0, data.length);
	}

	/**
	 * Adds a String to digest.
	 *
	 * @param data
	 *            the data
	 */
	protected void addToDigest(String data) {
		addToDigest(data.getBytes());
	}

	/**
	 * Adds a ByteBuffer to digest.
	 *
	 * @param data
	 *            the data
	 */
	private void addToDigest(ByteBuffer data) {
		addToDigest(data.array());
	}

	/**
	 * Adds a Character to digest.
	 *
	 * @param data
	 *            the data
	 */
	protected void addToDigest(Character data) {
		addToDigest(ByteBuffer.allocate(2).putChar(data));
	}

	/**
	 * Adds an Integer to digest.
	 *
	 * @param data
	 *            the data
	 */
	protected void addToDigest(Integer data) {
		addToDigest(ByteBuffer.allocate(4).putInt(data));
	}

	/**
	 * Adds a Long to digest.
	 *
	 * @param data
	 *            the data
	 */
	protected void addToDigest(Long data) {
		addToDigest(ByteBuffer.allocate(8).putLong(data));
	}

	/**
	 * Adds a Double to digest.
	 *
	 * @param data
	 *            the data
	 */
	protected void addToDigest(Double data) {
		addToDigest(ByteBuffer.allocate(8).putDouble(data));
	}

	/**
	 * Combines multiple MemberDigests into a ByteBuffer
	 *
	 * @param digests
	 *            the MemberDigest collection
	 * @return the combined ByteBuffer
	 */
	public static ByteBuffer multiDigest(Collection<MemberDigest> digests) {
		ArrayList<ByteBuffer> buffers = new ArrayList<ByteBuffer>(digests.size());
		for (MemberDigest digest : digests) {
			buffers.add(digest.getDigestBytes());
		}
		return multiBuffer(buffers);
	}

	/**
	 * Combines multiple ByteBuffers into a single ByteBuffer
	 *
	 * @param digests
	 *            the ByteBuffer Collection
	 * @return the combined ByteBuffer
	 */
	public static ByteBuffer multiBuffer(Collection<ByteBuffer> digests) {
		// If the digests collection is empty, we will end up returning null
		ByteBuffer last = null;
		ByteBuffer multi = null;

		for (ByteBuffer curr : digests) {
			int last_len, curr_len;
			byte[] last_array = null;
			byte[] curr_array = null;

			curr_array = curr.array();
			curr_len = curr_array.length;

			// If this is the first digest, only its contents should be included
			// in
			// the multi-digest ByteBuffer
			if (last == null) {
				last_len = 0;
			} else {
				last_array = last.array();
				last_len = last_array.length;
			}

			int max_len = Math.max(last_len, curr_len);
			// skip zero-length digests
			if (max_len == 0)
				continue;

			multi = ByteBuffer.allocate(max_len);
			byte[] multi_array = multi.array();

			for (int i = 0; i < max_len; i++) {
				// No more bytes in last, just add byte from curr
				if (i >= last_len) {
					multi_array[i] = curr_array[i];
				}
				// No more bytes in curr, just add byte from last
				else if (i >= curr_len) {
					multi_array[i] = last_array[i];
				}
				// Bytes in both curr and last, combine them with XOR
				else {
					multi_array[i] = (byte) (last_array[i] ^ curr_array[i]);
				}
			}

			// The combination of all digests so far becomes the new value of
			// last
			last = multi;
		}

		return last;
	}

	/**
	 * Sets the algorithm. This should only be called when initializing.
	 *
	 * @param algorithm
	 *            the new algorithm
	 * @throws RuntimeException
	 *             if the given algorithm causes a
	 *             {@linkplain java.security.NoSuchAlgorithmException}
	 */
	private void setAlgorithm(String algorithm) throws RuntimeException {
		try {
			digest = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException ex) {
			String message = String
					.format("Could not initialize digest for the '" + algorithm + "' algorithm:" + ex.getMessage());
			throw new RuntimeException(message);
		}
	}

	/**
	 * Write default object, then add a String with the used algorithm.
	 *
	 * @param out
	 *            the out
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject(new String(this.digest.getAlgorithm()));
	}

	/**
	 * Read object and initialize the digest with the found algorithm.
	 *
	 * @param in
	 *            the ObjectInputStream containing the object and algorithm
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		String algorithm = (String) in.readObject();
		setAlgorithm(algorithm);
	}
}
