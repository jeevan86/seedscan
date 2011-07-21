/*
 * Copyright 2011, United States Geological Survey or
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

package asl.seedscan;

import java.util.List;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * 
 */
public class Config
{
	private static final Logger logger = Logger.getLogger("Config");
	
	private File configFile;
	private Document dom;
	
	public Config(File configFile)
	{
		this.configFile = configFile;
	}
	
	public void loadConfig()
	{
		try {
		dom = (new SAXBuilder()).build(configFile);
		} catch (JDOMException e) {
			logger.warning("Could not assemble DOM from config file '" +configFile+ "'");
		} catch (IOException e) {
			logger.warning("Could not read from config file '" +configFile+ "'");
		}
		parseConfig();
	}
	
	private void parseConfig()
	{
		List elements = dom.getContent();
		int numElements = elements.size();
		for (int i = 0; i < numElements; i++) {
			Element element = (Element)elements.get(i);
		}
	}
}