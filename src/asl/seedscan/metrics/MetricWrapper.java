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

public class MetricWrapper
{
    private Metric arguments;
    private Class metricClass;

    public MetricWrapper(Class metricClass)
    throws  IllegalAccessException,
            InstantiationException
    {
        this.metricClass = metricClass;
        arguments = (Metric)metricClass.newInstance();
    }

    public void add(String name, String value)
    throws NoSuchFieldException
    {
        arguments.add(name, value);
    }

    public String get(String name)
    throws NoSuchFieldException
    {
        return arguments.get(name);
    }

    public Metric getNewInstance()
    throws  IllegalAccessException,
            InstantiationException,
            NoSuchFieldException
    {
        Metric metric = (Metric)metricClass.newInstance();
        Enumeration<String> names = arguments.names();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            metric.add(name, arguments.get(name));
        }
        return metric;
    }
}
