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

public class TextPassword implements Password {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.security.TextPassword.class);

	private String password = null;

	public TextPassword(String password) {
		this.password = password;
	}

	public boolean setPassword(String password) {
		this.password = password;
		return (password == null) ? false : true;
	}

	public String getPassword() {
		return password;
	}

	public String toString() {
		return new String("TextPassword: *** [" + password.length() + "]");
	}
}
