package seed;

import org.junit.Test;

public class Steim1Test {

	/**
	 * This test is a copy from the Main() method that was found in Steim1
	 * @throws Exception
	 */
	@Test
	public final void testDecodeByteArrayIntBooleanInt() throws Exception {
		byte[] b = new byte[64];
		@SuppressWarnings("unused")
		int[] temp;

		for (int i = 0; i < 64; i++) {
			b[i] = 0x00;
		}
		b[0] = 0x01;
		b[1] = (byte) 0xb0;
		b[2] = (byte) 0xff;
		b[3] = (byte) 0xff;

		b[4] = 0;
		b[5] = 0;
		b[6] = 0;
		b[7] = 0;

		b[8] = 0;
		b[9] = 0;
		b[10] = 0;
		b[11] = 0;

		b[12] = 1;
		b[13] = 2;
		b[14] = 3;
		b[15] = 0;

		b[16] = 1;
		b[17] = 1;
		b[18] = 0;
		b[19] = 0;

		b[20] = 0;
		b[21] = 1;
		b[22] = 0;
		b[23] = 0;
		try {
			temp = Steim1.decode(b, 17, false);
		} catch (SteimException e) {
			throw e;
		}	
		}

}
