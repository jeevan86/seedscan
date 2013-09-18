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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;

import asl.util.Hex;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 * 
 */
public abstract class MemberDigest
{
    private static final Logger logger = LoggerFactory.getLogger(asl.security.MemberDigest.class);

    private MessageDigest digest = null;
    private ByteBuffer raw = null;
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
        raw = ByteBuffer.wrap(digest.digest());
        str = Hex.byteArrayToHexString(raw.array());
    }

    public ByteBuffer getDigestBytes() {
        computeDigest();
        return raw;
    }

    public String getDigestString() {
        computeDigest();
        return str;
    }

    // Methods for adding member variables' data to the digest
    protected void addToDigest(byte[] data, int offset, int length) {
        digest.update(data, offset, length);
    }

    protected void addToDigest(byte[] data) {
        addToDigest(data, 0, data.length);
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

    public static ByteBuffer multiDigest(Collection<MemberDigest> digests) {
        ArrayList<ByteBuffer> buffers = new ArrayList<ByteBuffer>(digests.size());
        for (MemberDigest digest: digests) {
            buffers.add(digest.getDigestBytes());
        }
        return multiBuffer(buffers);
    }

    public static ByteBuffer multiBuffer(Collection<ByteBuffer> digests) {
        // If the digests collection is empty, we will end up returning null
        ByteBuffer last = null;
        ByteBuffer multi = null;

        for (ByteBuffer curr: digests) {
            int last_len, curr_len;
            byte[] last_array = null;
            byte[] curr_array = null;

            curr_array = curr.array();
            curr_len = curr_array.length;

            // If this is the first digest, only its contents should be included in
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
                    multi_array[i] = (byte)(last_array[i] ^ curr_array[i]);
                }
            }

            // The combination of all digests so far becomes the new value of last
            last = multi;
        }

        return last;
    }
}
