package asl.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;

/**
 * The Class MemberDigestTest. This class has a inner class that extends
 * MemberDigest for testing purposes.
 */
public class MemberDigestTest {

	/**
	 * The Class GenericDigest.
	 */
	private class GenericDigest extends MemberDigest {

		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = 1L;

		/** The test integer. */
		private Integer testInteger;

		/** The test long. */
		private Long testLong;

		/** The test double. */
		private Double testDouble;

		/** The test char. */
		private Character testChar;

		/**
		 * Instantiates a new generic digest.
		 *
		 * @param b
		 *            the testInteger
		 * @param c
		 *            the testLong
		 * @param d
		 *            the testDouble
		 * @param e
		 *            the testChar
		 */
		GenericDigest(Integer b, Long c, Double d, Character e) {
			super();
			testInteger = b;
			testLong = c;
			testDouble = d;
			testChar = e;
		}

		/**
		 * Instantiates a new generic digest.
		 *
		 * @param algorithm
		 *            the algorithm
		 * @param b
		 *            the testInteger
		 * @param c
		 *            the testLong
		 * @param d
		 *            the testDouble
		 * @param e
		 *            the testChar
		 */
		GenericDigest(String algorithm, Integer b, Long c, Double d, Character e) {
			super(algorithm);
			testInteger = b;
			testLong = c;
			testDouble = d;
			testChar = e;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see asl.security.MemberDigest#addDigestMembers()
		 */
		@Override
		protected void addDigestMembers() {
			addToDigest(testInteger);
			addToDigest(testLong);
			addToDigest(testDouble);
			addToDigest(testChar);
		}
	}

	/** The digest1. */
	private MemberDigest digest1;

	/** The digest2. */
	private MemberDigest digest2;

	/** The digest3. */
	private MemberDigest digest3;

	/**
	 * Test add digest members method. Only fails if an exception is thrown.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public final void testAddDigestMembers() throws Exception {
		digest2 = new GenericDigest(new String("SHA-256"), new Integer(-100), new Long(4221372026214775806L),
				new Double(123456.1), new Character('q'));
		digest2.addDigestMembers();
	}

	/**
	 * Tests differences in adding characters
	 * 
	 * Two identical MemberDigests are created and one different. Equality and
	 * inequality tests are performed.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testAddToDigestCharacter() throws Exception {
		digest1 = new GenericDigest(new Integer(54654), new Long(78901342789L), new Double(0.0000001),
				new Character('j'));
		digest2 = new GenericDigest(new Integer(54654), new Long(78901342789L), new Double(0.0000001),
				new Character('k'));
		digest3 = new GenericDigest(new Integer(54654), new Long(78901342789L), new Double(0.0000001),
				new Character('k'));

		ByteBuffer bytes1 = digest1.getDigestBytes();
		String hex1 = DatatypeConverter.printHexBinary(bytes1.array());

		ByteBuffer bytes2 = digest2.getDigestBytes();
		String hex2 = DatatypeConverter.printHexBinary(bytes2.array());

		ByteBuffer bytes3 = digest3.getDigestBytes();
		String hex3 = DatatypeConverter.printHexBinary(bytes3.array());

		/* Different */
		assertNotEquals(hex1, hex2);
		/* Same */
		assertEquals(hex2, hex3);
	}

	/**
	 * Tests differences in adding Doubles
	 * 
	 * Two identical MemberDigests are created and one different. Equality and
	 * inequality tests are performed.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testAddToDigestDouble() throws Exception {
		digest1 = new GenericDigest(new Integer(0), new Long(-1000000000), new Double(2512.124), new Character('z'));
		digest2 = new GenericDigest(new Integer(0), new Long(-1000000000), new Double(-2512.124), new Character('z'));
		digest3 = new GenericDigest(new Integer(0), new Long(-1000000000), new Double(-2512.124), new Character('z'));

		ByteBuffer bytes1 = digest1.getDigestBytes();
		String hex1 = DatatypeConverter.printHexBinary(bytes1.array());

		ByteBuffer bytes2 = digest2.getDigestBytes();
		String hex2 = DatatypeConverter.printHexBinary(bytes2.array());

		ByteBuffer bytes3 = digest3.getDigestBytes();
		String hex3 = DatatypeConverter.printHexBinary(bytes3.array());

		/* Different */
		assertNotEquals(hex1, hex2);
		/* Same */
		assertEquals(hex2, hex3);
	}

	/**
	 * Tests differences in adding Integers
	 * 
	 * Two identical MemberDigests are created and one different. Equality and
	 * inequality tests are performed.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testAddToDigestInteger() throws Exception {
		digest1 = new GenericDigest(new Integer(64000), new Long(9454565754L), new Double(121231.000124),
				new Character('@'));
		digest2 = new GenericDigest(new Integer(64), new Long(9454565754L), new Double(121231.000124),
				new Character('@'));
		digest3 = new GenericDigest(new Integer(64), new Long(9454565754L), new Double(121231.000124),
				new Character('@'));

		ByteBuffer bytes1 = digest1.getDigestBytes();
		String hex1 = DatatypeConverter.printHexBinary(bytes1.array());

		ByteBuffer bytes2 = digest2.getDigestBytes();
		String hex2 = DatatypeConverter.printHexBinary(bytes2.array());

		ByteBuffer bytes3 = digest3.getDigestBytes();
		String hex3 = DatatypeConverter.printHexBinary(bytes3.array());

		/* Different */
		assertNotEquals(hex1, hex2);
		/* Same */
		assertEquals(hex2, hex3);
	}

	/**
	 * Tests differences in adding Longs
	 * 
	 * Two identical MemberDigests are created and one different. Equality and
	 * inequality tests are performed.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testAddToDigestLong() throws Exception {
		digest1 = new GenericDigest(new Integer(52465), new Long(-163245646544L), new Double(32512.124),
				new Character('1'));
		digest2 = new GenericDigest(new Integer(52465), new Long(0L), new Double(32512.124), new Character('1'));
		digest3 = new GenericDigest(new Integer(52465), new Long(0L), new Double(32512.124), new Character('1'));

		ByteBuffer bytes1 = digest1.getDigestBytes();
		String hex1 = DatatypeConverter.printHexBinary(bytes1.array());

		ByteBuffer bytes2 = digest2.getDigestBytes();
		String hex2 = DatatypeConverter.printHexBinary(bytes2.array());

		ByteBuffer bytes3 = digest3.getDigestBytes();
		String hex3 = DatatypeConverter.printHexBinary(bytes3.array());

		/* Different */
		assertNotEquals(hex1, hex2);
		/* Same */
		assertEquals(hex2, hex3);
	}

	/**
	 * Test getDigestBytes method. A generated digest is compared to a
	 * pre-calculated Hex string.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public final void testGetDigestBytes() throws Exception {
		digest1 = new GenericDigest(new Integer(12), new Long(9221372026854775806L), new Double(57.21),
				new Character('e'));
		String expected = new String("CC8BDBC838EC258DEC6273F66FDE3449");
		ByteBuffer bytes = digest1.getDigestBytes();
		String hex = DatatypeConverter.printHexBinary(bytes.array());
		assertEquals(expected, hex);
	}

	/**
	 * Test member digest Construction.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public final void testMemberDigest() throws Exception {
		digest1 = new GenericDigest(new Integer(12), new Long(9221372026854775806L), new Double(-10000.21),
				new Character('e'));
	}

	/**
	 * Test member digest custom algorithm construction using SHA-1.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public final void testMemberDigestString() throws Exception {
		digest2 = new GenericDigest(new String("SHA-1"), new Integer(12), new Long(9221372026854775806L),
				new Double(57.21), new Character('e'));
	}

	/**
	 * Test multi buffer.
	 * 
	 * Creates 3 digests and merges them with multiBuffer. The output is
	 * compared to a pre generated hash.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public final void testMultiBuffer() throws Exception {
		ArrayList<ByteBuffer> digests = new ArrayList<ByteBuffer>();
		digest1 = new GenericDigest(new Integer(0), new Long(9221372026154775806L), new Double(5.0000001),
				new Character('d'));
		digest2 = new GenericDigest(new Integer(52465), new Long(0L), new Double(32512.124), new Character('1'));
		digest3 = new GenericDigest(new Integer(-1234), new Long(12L), new Double(4561.321), new Character('L'));

		digests.add(digest1.getDigestBytes());
		digests.add(digest2.getDigestBytes());
		digests.add(digest3.getDigestBytes());

		ByteBuffer bytes = MemberDigest.multiBuffer(digests);
		String expected = new String("4927E27583FE47AFC5C5BAB83442E627");

		String hex = DatatypeConverter.printHexBinary(bytes.array());
		assertEquals(expected, hex);
	}
}
