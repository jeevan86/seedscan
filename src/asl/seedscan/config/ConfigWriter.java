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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.SAXException;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

/**
 * 
 */
public class ConfigWriter
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.config.ConfigWriter");

    DocumentBuilderFactory  domFactory    = null;
    private SchemaFactory   schemaFactory = null;
    private DocumentBuilder builder       = null;

    private Schema    schema    = null;
    private Validator validator = null;
    private Document  doc       = null;
    private XPath     xpath     = null;

    private boolean validate = false;
    private boolean ready    = false;

    private Configuration configuration = null;

    public ConfigWriter()
    {
        _construct(null);
    }

    public ConfigWriter(File schemaFile)
    {
        _construct(schemaFile);
    }

    public void setConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }

    private void _construct(File schemaFile)
    {
        configuration = new Configuration();
        xpath         = XPathFactory.newInstance().newXPath();
        domFactory    = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);

        if (schemaFile != null) {
            schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                schema = schemaFactory.newSchema(schemaFile);
            } catch (SAXException e) {
                logger.severe("Could not read validation file '" +schemaFile+ "'.\n  Details: " +e);
                throw new RuntimeException("Could not read validation file.");
            }
            validate = true;
        }

        try {
            builder = domFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.severe("Invalid configuration for SAX parser.\n  Details: " +e);
            throw new RuntimeException("Invalid configuration for SAX parser.");
        }
    }

    public void saveConfiguration(File configFile)
    {
        try {
            populateConfig();
        } catch (XPathExpressionException e) {
            logger.severe("XPath expression error!\n Details: " +e);
            e.printStackTrace();
            throw new RuntimeException("XPath expression error.");
        }

        if (validate) {
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
        }

        try {
            doc = builder.parse(configFile);
            logger.info("Configuration file parsed.");
        } catch (SAXException e) {
            logger.severe("Could not assemble DOM from config file '" +configFile+ "'.\n Details: " +e);
            throw new RuntimeException("Could not assemble configuration from file.");
        } catch (IOException e) {
            logger.severe("Could not read config file '" +configFile+ "'.\n Details:" +e);
            throw new RuntimeException("Could not read configuration file.");
        }

        ready = true;
    }

    private void populateConfig()
      throws javax.xml.xpath.XPathExpressionException
    {
        //TODO: complete
        ;
    }

}
