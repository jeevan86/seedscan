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
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.input.SAXHandler;

/**
 * 
 */
public class Config
{
    private static final Logger logger = Logger.getLogger("Config");

    private SAXParserFactory saxFactory = null;
    private SchemaFactory schemaFactory = null;

    private SAXParser  parser     = null;
    private SAXHandler handler    = null;
    private Schema     schema     = null;
    private Validator  validator  = null;
    private Document   dom        = null;
    private boolean    ready      = false;

    public Config()
    {
        saxFactory = SAXParserFactory.newInstance();
        handler = new SAXHandler();
        saxFactory.setValidating(false);
        try {
            parser = saxFactory.newSAXParser();
        } catch (ParserConfigurationException e) {
            logger.severe("Invalid configuration for SAX parser.\n  Details: " +e);
            throw new RuntimeException("Invalid configuration for SAX parser.");
        } catch (SAXException e) {
            logger.severe("Could not assemble SAX parser.\n  Details: " +e);
            throw new RuntimeException("Could not assemble SAX parser.");
        }
    }

    public Config(File schemaFile)
    {
        saxFactory = SAXParserFactory.newInstance();
        schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        handler = new SAXHandler();

        try {
            schema = schemaFactory.newSchema(schemaFile);
        } catch (SAXException e) {
            logger.severe("Could not read validation file '" +schemaFile+ "'.\n  Details: " +e);
            throw new RuntimeException("Could not read validation file.");
        }
        saxFactory.setSchema(schema);
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(true);

        try {
            parser = saxFactory.newSAXParser();
        } catch (ParserConfigurationException e) {
            logger.severe("Invalid configuration for SAX parser.\n  Details: " +e);
            throw new RuntimeException("Invalid configuration for SAX parser.");
        } catch (SAXException e) {
            logger.severe("Could not assemble SAX parser.\n  Details: " +e);
            throw new RuntimeException("Could not assemble SAX parser.");
        }
    }

    public void loadConfig(File configFile)
    {
        try {
            parser.parse(configFile, handler);
            dom = handler.getDocument();
        } catch (SAXException e) {
            logger.severe("Could not assemble DOM from config file '" +configFile+ "'.\n  Details: " +e);
            return;
        } catch (IOException e) {
            logger.severe("Could not read config file '" +configFile+ "'");
            return;
        }
        parseConfig();
        ready = true;
    }

    private void parseConfig()
    {
        Element core = (Element)dom.getContent().get(0);
        System.out.println("" + core);
        List elements = core.getContent();
        int numElements = elements.size();
        System.out.println("" + elements);
        for (int i = 0; i < numElements; i++) {
            Object obj = elements.get(i);
            if (!(obj instanceof Element)) {
                logger.info("Skipping Text");
                continue;
            }
            Element element = (Element)obj;
            String name = element.getName();
            logger.info("Parsing element '" +name+ "'");
            if (name.equals("lock_file")) {
                parseLock(element);
            } 
            else if (name.equals("log")) {
                parseLog(element);
            }
            else if (name.equals("database")) {
                parseDatabase(element);
            }
            else if (name.equals("scan")) {
                parseScans(element);
            }
        }
    }

    private void parseLock(Element el)
    {
    }

    private void parseLog(Element el)
    {
    }

    private void parseDatabase(Element el)
    {
    }

    private void parseScans(Element el)
    {
    }

}
