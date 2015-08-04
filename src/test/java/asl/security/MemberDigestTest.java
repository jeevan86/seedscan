package asl.security;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import asl.seedsplitter.Sequence;
import asl.security.MemberDigest;

public class MemberDigestTest {
	private MemberDigest digest1;
	private MemberDigest digest2;

	@Before
	public void setUp() throws Exception {
		System.out.println("Test1");
		digest1 = new Sequence();
		
		
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testMemberDigest() throws Exception {
		digest2 = new Sequence();
	}

	@Test
	public final void testMemberDigestString() throws Exception {
		System.out.println("Test2");
		if(false)
			fail("not yet implemented");
	}

	@Test
	public final void testAddDigestMembers() throws Exception {
		System.out.println("Test3");
		//fail("not yet implemented");
	}

	@Test
	public final void testGetDigestBytes() throws Exception {
		System.out.println("Test4");
		//fail("not yet implemented");
	}

	@Test
	public final void testAddToDigestString() throws Exception {
		//fail("not yet implemented");
	}

	@Test
	public final void testAddToDigestCharacter() throws Exception {
		//fail("not yet implemented");
	}

	@Test
	public final void testAddToDigestInteger() throws Exception {
		//fail("not yet implemented");
	}

	@Test
	public final void testAddToDigestLong() throws Exception {
		//fail("not yet implemented");
	}

	@Test
	public final void testAddToDigestDouble() throws Exception {
		//fail("not yet implemented");
	}

	@Test
	public final void testMultiDigest() throws Exception {
		//fail("not yet implemented");
	}

	@Test
	public final void testMultiBuffer() throws Exception {
		//fail("not yet implemented");
	}

}
