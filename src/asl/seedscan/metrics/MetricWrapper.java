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
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.MetricWrapper");

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

/**
 * if forceUpdate is set then we will compute this metric even if if
 *    the metric digests haven't changed
*/
    public void setForceUpdate(String forceUpdateString){
        if (forceUpdateString == null) 
            return;
        if (forceUpdateString.toLowerCase().equals("yes") || 
            forceUpdateString.toLowerCase().equals("true") ) {
                 arguments.setForceUpdate();
        }
    }

    public String get(String name)
    throws NoSuchFieldException
    {
        return arguments.get(name);
    }

    public Metric getNewInstance()
    {
        try {
            Metric metric = (Metric)metricClass.newInstance();
            Enumeration<String> names = arguments.names();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                metric.add(name, arguments.get(name));
            }
// MTH: Not sure why, but we have to reset this for this particular instance of the metric ??
            if (arguments.getForceUpdate())
                metric.setForceUpdate();
            return metric;
        } catch (InstantiationException ex) {
            String message = ex.getClass().getName() + " in MetricWrapper.getNewInstance(), should never happen!";
            logger.severe(message);
            throw new RuntimeException(message);
        } catch (IllegalAccessException ex) {
            String message = ex.getClass().getName() + " in MetricWrapper.getNewInstance(), should never happen!";
            logger.severe(message);
            throw new RuntimeException(message);
        } catch (NoSuchFieldException ex) {
            String message = ex.getClass().getName() + " in MetricWrapper.getNewInstance(), should never happen!";
            logger.severe(message);
            throw new RuntimeException(message);
        }
    }
}
