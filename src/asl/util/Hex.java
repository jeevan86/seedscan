/*
 * Copyright 2012, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */
package asl.util;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hex
{
	private static final Logger logger = LoggerFactory.getLogger(asl.util.Hex.class);
    public static String byteArrayToHexString(byte[] byteArray)
    throws IllegalArgumentException
    {
        return byteArrayToHexString(byteArray, false);
    }

    public static String byteBufferToHexString(ByteBuffer byteBuffer, boolean upperCase)
    throws IllegalArgumentException
    {
        return byteArrayToHexString(byteBuffer.array());
    }

    public static String byteArrayToHexString(byte[] byteArray, boolean upperCase)
    throws IllegalArgumentException
    {
        StringBuilder builder = new StringBuilder();
        for (byte b: byteArray) {
            builder.append((char)((int) valueToHexChar(((b >> 4) & 0x0f), upperCase)));
            builder.append((char)((int) valueToHexChar((b & 0x0f), upperCase)));
        }
        return builder.toString();
    }

    public static ByteBuffer hexStringToByteBuffer(String hexString)
    throws IllegalArgumentException
    {
        return ByteBuffer.wrap(hexStringToByteArray(hexString));
    }

    public static byte[] hexStringToByteArray(String hexString)
    throws IllegalArgumentException
    {
        char[] chars = hexString.toCharArray();
        int byteCount = chars.length / 2 + chars.length % 2;
        byte[] bytes = new byte[byteCount];
        for (int i=0; i < chars.length; i++) {
            bytes[i/2] |= hexCharToValue(chars[i]) << (4 * ((i+1) % 2)) & 0xff;
        }
        return bytes;
    }

    public static int valueToHexChar(int b)
    throws IllegalArgumentException
    {
        return valueToHexChar(b, false);
    }

    public static Integer valueToHexChar(int b, boolean upper)
    throws IllegalArgumentException
    {
        char result;
        switch (b) {
            case  0 : result = '0'; break;
            case  1 : result = '1'; break;
            case  2 : result = '2'; break;
            case  3 : result = '3'; break;
            case  4 : result = '4'; break;
            case  5 : result = '5'; break;
            case  6 : result = '6'; break;
            case  7 : result = '7'; break;
            case  8 : result = '8'; break;
            case  9 : result = '9'; break;
            case 10 : result = (upper ? 'A' : 'a'); break;
            case 11 : result = (upper ? 'B' : 'b'); break;
            case 12 : result = (upper ? 'C' : 'c'); break;
            case 13 : result = (upper ? 'D' : 'd'); break;
            case 14 : result = (upper ? 'E' : 'e'); break;
            case 15 : result = (upper ? 'F' : 'f'); break;
            default : 
            	//throw new IllegalArgumentException();
            	IllegalArgumentException e = new IllegalArgumentException();
            	logger.error("Hex IllegalArgumentException:", e);
            	return null;
        }
        return (int) result;
    }

    private static Integer hexCharToValue(int c)
    {
        int result;
        switch (c) {
            case '0' : result = 0; break;
            case '1' : result = 1; break;
            case '2' : result = 2; break;
            case '3' : result = 3; break;
            case '4' : result = 4; break;
            case '5' : result = 5; break;
            case '6' : result = 6; break;
            case '7' : result = 7; break;
            case '8' : result = 8; break;
            case '9' : result = 9; break;
            case 'A' :
            case 'a' : result = 10; break;
            case 'B' :
            case 'b' : result = 11; break;
            case 'C' :
            case 'c' : result = 12; break;
            case 'D' :
            case 'd' : result = 13; break;
            case 'E' :
            case 'e' : result = 14; break;
            case 'F' :
            case 'f' : result = 15; break;
            default  : 
            	//throw new IllegalArgumentException();
            	IllegalArgumentException e = new IllegalArgumentException();
            	logger.error("Hex IllegalArgumentException:", e);
            	return null;
        }
        return result;
    }
}
