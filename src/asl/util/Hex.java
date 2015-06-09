package asl.util;

/**
 * 
 * @author Joel D. Edwards - USGS
 *
 */
public class Hex {

	public static String byteArrayToHexString(byte[] byteArray)
			throws IllegalArgumentException {
		try {
			return byteArrayToHexString(byteArray, false);
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	

	private static String byteArrayToHexString(byte[] byteArray,
			boolean upperCase) throws IllegalArgumentException {
		StringBuilder builder = new StringBuilder();
		for (byte b : byteArray) {
			try {
				builder.append((char) ((int) valueToHexChar(((b >> 4) & 0x0f),
						upperCase)));
				builder.append((char) ((int) valueToHexChar((b & 0x0f),
						upperCase)));
			} catch (IllegalArgumentException e) {
				throw e;
			}
		}
		return builder.toString();
	}

	

	

	private static Integer valueToHexChar(int b, boolean upper)
			throws IllegalArgumentException {
		char result;
		switch (b) {
		case 0:
			result = '0';
			break;
		case 1:
			result = '1';
			break;
		case 2:
			result = '2';
			break;
		case 3:
			result = '3';
			break;
		case 4:
			result = '4';
			break;
		case 5:
			result = '5';
			break;
		case 6:
			result = '6';
			break;
		case 7:
			result = '7';
			break;
		case 8:
			result = '8';
			break;
		case 9:
			result = '9';
			break;
		case 10:
			result = (upper ? 'A' : 'a');
			break;
		case 11:
			result = (upper ? 'B' : 'b');
			break;
		case 12:
			result = (upper ? 'C' : 'c');
			break;
		case 13:
			result = (upper ? 'D' : 'd');
			break;
		case 14:
			result = (upper ? 'E' : 'e');
			break;
		case 15:
			result = (upper ? 'F' : 'f');
			break;
		default:
			throw new IllegalArgumentException();
		}
		return (int) result;
	}
}
