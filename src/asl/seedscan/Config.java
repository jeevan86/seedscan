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

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.input.SAXHandler;
import org.xml.sax.SAXException;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

/**
 * 
 */
public class Config
{
    private static final Logger logger = Logger.getLogger("Config");

    private SAXParserFactory saxFactory = null;
    private SchemaFactory schemaFactory = null;

    private SAXParser   parser      = null;
    private SAXHandler  handler     = null;
    private Schema      schema      = null;
    private Validator   validator   = null;
    private Document    dom         = null;
    private XPath       xpath       = null;
    private boolean     validate    = false;

    private boolean     ready       = false;

    private Hashtable<String,Object> configuration = null;

    public Config()
    {
        _construct(null);
    }

    public Config(File schemaFile)
    {
        _construct(schemaFile);
    }

    private void _construct(File schemaFile)
    {
        configuration = new Hashtable<String,Object>(32, (float)0.75);
        saxFactory = SAXParserFactory.newInstance();
        handler = new SAXHandler();

        if (schemaFile != null) {
            schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                schema = schemaFactory.newSchema(schemaFile);
            } catch (SAXException e) {
                logger.severe("Could not read validation file '" +schemaFile+ "'.\n  Details: " +e);
                throw new RuntimeException("Could not read validation file.");
            }
            saxFactory.setSchema(schema);
            saxFactory.setNamespaceAware(true);
            saxFactory.setValidating(true);
            validate = true;
        }
        else {
            saxFactory.setValidating(false);
            validate = false;
        }

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
        Validator validator = schema.newValidator();
        try {
            validator.validate(new StreamSource(configFile));
            logger.info("Configuration file passed validation.");
        } catch (SAXException e) {
            logger.severe("Configuration file did not pass validation.\n Details: " +e);
            throw new RuntimeException("Configuration file failed validation.");
        } catch (IOException e) {
            logger.severe("Failed to read configuration from file '" +configFile+ "'.\n Details: " +e);
            throw new RuntimeException("Could not read configuration file.");
        }

        try {
            parser.parse(configFile, handler);
        } catch (SAXException e) {
            logger.severe("Could not assemble DOM from config file '" +configFile+ "'.\n Details: " +e);
            throw new RuntimeException("Could not assemble configuration from file.");
        } catch (IOException e) {
            logger.severe("Could not read config file '" +configFile+ "'.\n Details:" +e);
            throw new RuntimeException("Could not read configuration file.");
        }

        dom = handler.getDocument();
        xpath = XPathFactory.newInstance().newXPath();

        try {
            parseConfig();
        } catch (XPathExpressionException e) {
            logger.severe("XPath expression error!\n Details: " +e);
            e.printStackTrace();
            throw new RuntimeException("XPath expression error.");
        }
        ready = true;
    }

    private void parseConfig()
      throws javax.xml.xpath.XPathExpressionException
    {
     // Parse Lock File Config
        System.err.printf("Parsing lockfile.\n");
        configuration.put("lockfile",   xpath.evaluate("/seedscan/lockfile/text()", dom));

     // Parse Log Config
        System.err.printf("Parsing log.\n");
        configuration.put("log-level",      xpath.evaluate("/seedscan/log/level/text()", dom));
        configuration.put("log-directory",  xpath.evaluate("/seedscan/log/directory/text()", dom));
        configuration.put("log-prefix",     xpath.evaluate("/seedscan/log/prefix/text()", dom));
        configuration.put("log-postfix",    xpath.evaluate("/seedscan/log/postfix/text()", dom));

     // Parse Database Config
        System.err.printf("Parsing database.\n");
        configuration.put("database-url",       xpath.evaluate("/seedscan/database/url/text()", dom));
        configuration.put("database-username",  xpath.evaluate("/seedscan/database/username/text()", dom));
        configuration.put("database-password",  xpath.evaluate("/seedscan/database/password/text()", dom));

     // Parse Scans
        System.err.printf("Parsing scans.\n");
        int id;
        String key;
        Object scan;
        NodeList scans = (NodeList)xpath.evaluate("/seedscan/scans/scan", dom, XPathConstants.NODESET);
        if ((scans == null) || (scans.getLength() < 1)) {
            logger.warning("No scans in configuration.");
            return;
        }
        int scanCount = scans.getLength();
        for (int j=0; j < scanCount; j++) {
            scan = scans.item(j);
            id = Integer.parseInt(xpath.evaluate("./@id", scan));
            key = "scan-" + id;
            configuration.put(key+ "-path", xpath.evaluate("./path/text()", scan));
            configuration.put(key+ "-start_depth", xpath.evaluate("./start_depth/text()", scan));
            configuration.put(key+ "-scan_depth",  xpath.evaluate("./scan_depth/text()", scan));

            NodeList ops = (NodeList)xpath.evaluate("./operations/operation", scan, XPathConstants.NODESET);
            int opCount = ops.getLength();
            if ((ops == null) || (ops.getLength() < 1)) {
                logger.warning("No operations found in scan " +id+".");
                continue;
            }
            for (int i=1; i <= opCount; i++) {
                configuration.put(key+ "-op-" +i,  xpath.evaluate("./*[0]/name()", scan));
            }
        }
    }

}
