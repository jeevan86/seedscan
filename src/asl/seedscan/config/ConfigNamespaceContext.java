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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class ConfigNamespaceContext
implements NamespaceContext
{
    private Hashtable<String, String> namespaces;
    private Hashtable<String, ArrayList<String>> prefixes;
    private String defaultURI = XMLConstants.NULL_NS_URI;

    public ConfigNamespaceContext()
    {
        namespaces = new Hashtable<String, String>();
        prefixes = new Hashtable<String, ArrayList<String>>();

        addPair("xml", XMLConstants.XML_NS_URI);
        addPair("cfg", "config.seedscan.asl");
        addPair("", "config.seedscan.asl");
    }

    private void addPair(String prefix, String namespace)
    {
        namespaces.put(prefix, namespace);
        if (!prefixes.containsKey(namespace)) {
            prefixes.put(namespace, new ArrayList<String>());
        }
        prefixes.get(namespace).add(prefix);
    }

    public String getNamespaceURI(String prefix)
    {
        if (prefix == null) {
            throw new IllegalArgumentException();
        } 
        if (namespaces.containsKey(prefix)) {
            return namespaces.get(prefix);
        }
        return defaultURI;
    }

    public String getPrefix(String uri)
    {
        if (uri == null) {
            throw new IllegalArgumentException();
        } 
        if (prefixes.containsKey(uri)) {
            if (prefixes.get(uri).size() > 0) {
                return prefixes.get(uri).get(0);
            }
        }
        return null;
    }

    public Iterator getPrefixes(String uri)
    {
        if (uri == null) {
            throw new IllegalArgumentException();
        } 
        if (prefixes.containsKey(uri)) {
            return prefixes.get(uri).iterator();
        }
        return null;
    }
}
