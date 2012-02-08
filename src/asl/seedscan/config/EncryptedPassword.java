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

package asl.seedscan.config;

import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedPassword
implements Password
{
    private static final Logger logger = Logger.getLogger("EncryptedPassword");

    private String cipherText = null;
    private IvParameterSpec iv = null;
    private SecretKeySpec key = null;

    public EncryptedPassword(IvParameterSpec iv,
                             SecretKeySpec key)
    {
        this.iv = iv;
        this.key = key;
    }

    private String encryptPassword(String password)
    {
        String cipherText = "";
        // TODO: Encrypt text
        return cipherText;
    }

    private String decryptPassword(String cipherText)
    {
        String password = "";
        // TODO: Decrypt text
        return password;
    }

    private Mac generateHMAC(String password)
    {
        Mac hmac = null;
        // TODO: generate password HMAC
        return hmac;

    }

    public void setCiphertext(String cipherText)
    {
        this.cipherText = cipherText;
    }

    public void setPassword(String password)
    {
        encryptPassword(password);
    }

    public String getPassword()
    {
        if (cipherText == null) {
            return null;
        }
        return decryptPassword(cipherText);
    }

    public String getCiphertext()
    {
        return cipherText;
    }
}
