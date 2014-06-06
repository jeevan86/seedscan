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

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptedPassword
extends Encrypted
implements Password
{
	private static final Logger logger = LoggerFactory.getLogger(asl.security.EncryptedPassword.class);
 // constructor(s)
    public EncryptedPassword(byte[] key)
    {
        super(key);
    }

    public EncryptedPassword(byte[] iv,
                             byte[] cipherText,
                             byte[] hmac)
    {
        super(iv, cipherText, hmac);
    }

 // password (implements methods from interface Password)
    public boolean setPassword(String password)
    {
        boolean result;
        try {
            result = encrypt(password);
        } catch (EncryptionException e) {
        	logger.error("EncryptionException:", e);
            result = false;
        }
        return result;
    }

    public String getPassword()
    {
        String passwordString;
        try {
            passwordString = decrypt();
        } catch (EncryptionException e) {
        	logger.error("EncryptionException:", e);
            passwordString = null;
        }
        return passwordString;
    }

    public String toString()
    {
        byte[] hmac = getHMAC();
        String hmacHex = (hmac == null) ? "NULL" : DatatypeConverter.printHexBinary(hmac);
        return new String("EncryptedPassword: hmac["+hmacHex+"]");
    }
}
