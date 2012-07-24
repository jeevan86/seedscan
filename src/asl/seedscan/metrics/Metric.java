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
package asl.seedscan.metrics;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;

public abstract class Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.Metric");

    private Hashtable<String, String> arguments;

    protected MetricData data = null;
    protected MetricResult result = null;

    public Metric()
    {
        arguments = new Hashtable<String, String>();
    }

    public void setData(MetricData data)
    {
        this.data = data;
    }

    public MetricResult getResult()
    {
        return result;
    }

    public abstract void process();
    public abstract String getName();

// Dynamic argumemnt managment
    protected final void addArgument(String name)
    {
        arguments.put(name, "");
    }

    public final void add(String name, String value)
    throws NoSuchFieldException
    {
        if (!arguments.containsKey(name)) {
            throw new NoSuchFieldException("Argument '" +name+ "' is not recognized.");
        }
        arguments.put(name, value);
    }

    public final String get(String name)
    throws NoSuchFieldException
    {
        if (!arguments.containsKey(name)) {
            throw new NoSuchFieldException("Argument '" +name+ "' is not recognized.");
        }
        String result = arguments.get(name);
        if ((result == null) || (result.equals(""))) {
            result = null;
        }
        return result;
    }

    public final Enumeration<String> names()
    {
        return arguments.keys();
    }
}
