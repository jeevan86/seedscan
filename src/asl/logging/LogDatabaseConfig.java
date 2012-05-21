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
package asl.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import asl.security.Password;

public class LogDatabaseConfig
extends LogConfig
{
    private static final Logger logger = Logger.getLogger("asl.logging.LogDatabaseConfig");

    private String uri = null;
    private String username = null;
    private Password password = null;

 // constructor(s)
    public LogDatabaseConfig()
    {
        super();
    }

 // ready
    public boolean isReady()
    {
        return (uri      == null) ? false :
               (username == null) ? false :
               (password == null) ? false : true;
    }

 // URI
    public void setURI(String uri)
    {
        this.uri = uri;
    }

    public String getURI()
    {
        return uri;
    }

 // username
    public void setUsername(String username)
    {
        logger.config("Username: "+username);
        this.username = username;
    }

    public String getUsername()
    {
        return username;
    }

 // password
    public void setPassword(Password password)
    {
        logger.config("Password: " + password);
        this.password = password;
    }

    public String getPassword()
    {
        return password.getPassword();
    }
}

