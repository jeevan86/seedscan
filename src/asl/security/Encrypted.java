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

import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Encrypted
{
    private static final Logger logger = Logger.getLogger("asl.security.Encrypted");

	private static final String CHAR_ENCODING = "UTF-8";
	private static final String DIGEST_ALGORITHM = "SHA-256";
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final String CRYPT_ALGORITHM = "AES";
	//private static final String CRYPT_TRANSFORM = "AES/CBC/NoPadding";
	private static final String CRYPT_TRANSFORM = "AES/CBC/PKCS5Paadding";
	private static final int BLOCK_SIZE = 16;
	private static final int KEY_SIZE = 16;
	private static final int SHA_SIZE = 16;

    private byte[] key = null;
    private byte[] iv = null;
    private byte[] cipherText = null;
    private byte[] hmac = null;

 // constructor(s)
    public Encrypted(byte[] key)
    throws NegativeArraySizeException,
           NullPointerException
    {
        this.key = Arrays.copyOf(key, key.length);
    }

    public Encrypted(byte[] iv,
                     byte[] cipherText,
                     byte[] hmac)
    throws NegativeArraySizeException,
           NullPointerException
    {
        this.iv = Arrays.copyOf(iv, iv.length);
        this.cipherText = Arrays.copyOf(cipherText, cipherText.length);
        this.hmac = Arrays.copyOf(hmac, hmac.length);
    }

 // ready
    private boolean readyForEncrypt()
    {
        return key == null ? false : true;
    }

    private boolean readyForDecrypt()
    {
        return (key  == null) ? false :
               (iv   == null) ? false :
               (hmac == null) ? false : 
               (cipherText == null) ? false : true;
    }

 // cryptographic routines
    protected boolean encrypt(String plainText)
    throws EncryptionException
    {
        boolean success = false;
        if (readyForEncrypt()) {
            try {
                // create a new IV
                iv = new byte[KEY_SIZE];
                SecureRandom srand = new SecureRandom();
                srand.nextBytes(iv);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                // perpare the key
                SecretKeySpec keySpec = new SecretKeySpec(key, CRYPT_ALGORITHM); 
                // create the cipher
                Cipher cipher = Cipher.getInstance(CRYPT_TRANSFORM);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
                AlgorithmParameters params = cipher.getParameters();
                // generate the HMAC
                hmac = Mac.getInstance(HMAC_ALGORITHM).doFinal(plainText.getBytes(CHAR_ENCODING));
                // perform the decryption
                cipherText = cipher.doFinal(plainText.getBytes(CHAR_ENCODING));
                success = true;
            } catch (BadPaddingException e) {
                throw new EncryptionException(e.toString());
            } catch (IllegalBlockSizeException e) {
                throw new EncryptionException(e.toString());
            } catch (InvalidAlgorithmParameterException e) {
                throw new EncryptionException(e.toString());
            } catch (InvalidKeyException e) {
                throw new EncryptionException(e.toString());
            } catch (NoSuchPaddingException e) {
                throw new EncryptionException(e.toString());
            } catch (NoSuchAlgorithmException e) {
                throw new EncryptionException(e.toString());
            } catch (UnsupportedEncodingException e) {
                throw new EncryptionException(e.toString());
            }
        }
        return success;
    }

    protected String decrypt()
    throws EncryptionException
    {
        String plainText = null;
        if (readyForDecrypt()) {
            try {
                // prepare the IV
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                // perpare the key
                SecretKeySpec keySpec = new SecretKeySpec(key, CRYPT_ALGORITHM); 
                // create the cipher
                Cipher cipher = Cipher.getInstance(CRYPT_TRANSFORM);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
                // perform the decryption
                byte[] clearText = cipher.doFinal(cipherText);
                // check the HMAC before returning the plainText
                if (Arrays.equals(hmac, Mac.getInstance(HMAC_ALGORITHM).doFinal(clearText))) {
                    plainText = new String(clearText, CHAR_ENCODING);
                }
            } catch (BadPaddingException e) {
                throw new EncryptionException(e.toString());
            } catch (IllegalBlockSizeException e) {
                throw new EncryptionException(e.toString());
            } catch (InvalidAlgorithmParameterException e) {
                throw new EncryptionException(e.toString());
            } catch (InvalidKeyException e) {
                throw new EncryptionException(e.toString());
            } catch (NoSuchPaddingException e) {
                throw new EncryptionException(e.toString());
            } catch (NoSuchAlgorithmException e) {
                throw new EncryptionException(e.toString());
            } catch (UnsupportedEncodingException e) {
                throw new EncryptionException(e.toString());
            }
        }
        return plainText;
    }

 // iv
    public void setIV(byte[] iv)
    {
        this.iv = Arrays.copyOf(iv, iv.length);
    }

    public byte[] getIV()
    {
        return Arrays.copyOf(iv, iv.length);
    }

 // key
    public void setKey(byte[] key)
    {
        this.key = Arrays.copyOf(key, key.length);
    }

    public byte[] getKey()
    {
        return Arrays.copyOf(key, key.length);
    }

 // cipher text
    public void setCiphertext(byte[] cipherText)
    {
        this.cipherText = Arrays.copyOf(cipherText, cipherText.length);
    }

    public byte[] getCiphertext()
    {
        return Arrays.copyOf(cipherText, cipherText.length);
    }

 // hmac
    public void setHMAC(byte[] hmac)
    {
        this.hmac = Arrays.copyOf(hmac, hmac.length);
    }

    public byte[] getHMAC()
    {
        return Arrays.copyOf(hmac, hmac.length);
    }
}
