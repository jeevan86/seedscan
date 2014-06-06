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

import java.io.BufferedInputStream;
import java.io.Console;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;

import org.xml.sax.SAXException;

import asl.seedscan.config.ConfigT;

public class ConfigParser
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.ConfigParser.class);

    private ConfigT config = null;
    private Schema  schema = null;

    public ConfigParser(Collection<File> schemaFiles)
    {
    	schema = makeSchema(schemaFiles);
    }

    private Schema makeSchema(Collection<File> files)
    {
        Schema schema = null;
        StreamSource[] sources = new StreamSource[files.size()];

        int i = 0;
        for (File file: files) {
            sources[i] = new StreamSource(file);
            i++;
        }

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            schema = factory.newSchema(sources);
        } catch (SAXException ex) {
            String message = "SAXException: Could not generate schema from supplied files:";
            logger.error(message, ex);
        }

        return schema;
    }

    public ConfigT parseConfig(File configFile)
    {
        ConfigT cfg = null;

        try {
            JAXBContext context = JAXBContext.newInstance("asl.seedscan.config");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            unmarshaller.setSchema(schema);
            InputStream stream = new BufferedInputStream(
                                 new DataInputStream(
                                 new FileInputStream(configFile)));
            JAXBElement<ConfigT> cfgRoot = (JAXBElement<ConfigT>)unmarshaller.unmarshal(stream);
            cfg = cfgRoot.getValue();
        } catch (FileNotFoundException ex) {
            String message = "FileNotFoundException: Could not locate config file:";
            logger.error(message, ex);
            
        } catch (JAXBException ex) {
            String message = "JAXBException: Could not unmarshal config file:";
            logger.error(message, ex);
        }

        return cfg;
    }

}
