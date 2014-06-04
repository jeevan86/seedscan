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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PassKey {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.security.Encrypted.class);

	private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA1";

	private int keySize = 16;

	private byte[] key;
	private byte[] salt;

	// constructor(s)
	// generate a random salt (for setting new passwords)
	public PassKey(String password, int keySize) {
		this.salt = new byte[8];
		SecureRandom srand = new SecureRandom();
		srand.nextBytes(this.salt);
		_init(password, keySize);
	}

	// use an existing salt (for working with existing passwords)
	public PassKey(String password, int keySize, byte[] salt) {
		this.salt = salt;
		_init(password, keySize);
	}

	private void _init(String password, int keySize) {
		this.keySize = keySize;

		// Derive the key from the password and salt
		try {
			SecretKeyFactory factory = SecretKeyFactory
					.getInstance(HASH_ALGORITHM);
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 8192,
					keySize * 8);
			SecretKey tmp = factory.generateSecret(spec);
			key = tmp.getEncoded();
		} catch (NoSuchAlgorithmException e) {
			// throw new
			// RuntimeException("Password hashing algorithm '"+HASH_ALGORITHM+"' not present.");
			String message = new String(
					"PassKey RuntimeException: Password hashing algorithm '"
							+ HASH_ALGORITHM + "' not present.");
			logger.error(message, e);
		} catch (InvalidKeySpecException e) {
			// throw new
			// RuntimeException("PBEKeySpec for algorithm '"+HASH_ALGORITHM+"' not present.");
			String message = new String(
					"PassKey RuntimeException: PBEKeySpec for algorithm '"
							+ HASH_ALGORITHM + "' not present.");
			logger.error(message, e);
		}
	}

	public byte[] getKey() {
		return Arrays.copyOf(key, key.length);
	}

	public byte[] getSalt() {
		return Arrays.copyOf(salt, salt.length);
	}
}
