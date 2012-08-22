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
package asl.security;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import asl.util.Hex;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 * 
 */
public abstract class MemberDigest
{
    private static final Logger logger = Logger.getLogger("asl.seedsplitter.MemberDigest");

    private MessageDigest digest = null;
    private byte[] raw = null;
    private String str = null;

    /**
     * Constructor.
     */
    public MemberDigest()
    {
        this("MD5");
    }

    public MemberDigest(String algorithm)
    {
        try {
            digest = MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Could not initialize digest for the '" +algorithm+ "' algorithm");
        }
    }

    protected abstract void addDigestMembers();

    private synchronized void computeDigest() {
        digest.reset();
        addDigestMembers();
        raw = digest.digest();
        str = Hex.byteArrayToHexString(raw);
    }

    public byte[] getDigestBytes() {
        computeDigest();
        return raw;
    }

    public String getDigestString() {
        computeDigest();
        return str;
    }

 // Methods for adding member variables' data to the digest
    protected void addToDigest(byte[] data) {
        digest.update(data);
    }

    protected void addToDigest(Object data) {
        addToDigest(data.toString().getBytes());
    }

    protected void addToDigest(String data) {
        addToDigest(data.getBytes());
    }

    protected void addToDigest(ByteBuffer data) {
        addToDigest(data.array());
    }

    protected void addToDigest(Character data) {
        addToDigest(ByteBuffer.allocate(2).putChar(data));
    }

    protected void addToDigest(Short data) {
        addToDigest(ByteBuffer.allocate(2).putShort(data));
    }

    protected void addToDigest(Integer data) {
        addToDigest(ByteBuffer.allocate(4).putInt(data));
    }

    protected void addToDigest(Long data) {
        addToDigest(ByteBuffer.allocate(8).putLong(data));
    }

    protected void addToDigest(Float data) {
        addToDigest(ByteBuffer.allocate(4).putFloat(data));
    }

    protected void addToDigest(Double data) {
        addToDigest(ByteBuffer.allocate(8).putDouble(data));
    }

   public static String bytesToHex(byte[] b) {
      char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7',
                         '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
      StringBuffer buf = new StringBuffer();
      for (int j=0; j<b.length; j++) {
         buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
         buf.append(hexDigit[b[j] & 0x0f]);
      }
      return buf.toString();
   }


}
