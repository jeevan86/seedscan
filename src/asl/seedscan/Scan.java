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
package asl.seedscan;

import java.util.ArrayList;

import asl.util.Filter; 
import asl.seedscan.metrics.MetricWrapper; 

public class Scan
{
    private String pathPattern;
    private int startDay;
    private int daysToScan;
    private ArrayList<MetricWrapper> metrics;

    private Filter networks = null;
    private Filter stations = null;
    private Filter locations = null;
    private Filter channels = null;

    public Scan()
    {
        metrics = new ArrayList<MetricWrapper>();
    }

    // path pattern
    public void setPathPattern(String pathPattern)
    {
        this.pathPattern = pathPattern;
    }

    public String getPathPattern()
    {
        return pathPattern;
    }

    // metrics
    public void addMetric(MetricWrapper metric)
    {
        metrics.add(metric);
    }

    public MetricWrapper getMetric(int index)
        throws IndexOutOfBoundsException
        {
            return metrics.get(index);
        }

    public ArrayList<MetricWrapper> getMetrics()
    {
        return metrics;
    }

    public boolean removeMetric(MetricWrapper metric)
    {
        return metrics.remove(metric);
    }

    public MetricWrapper removeMetric(int index)
        throws IndexOutOfBoundsException
        {
            return metrics.remove(index);
        }

    public void clearMetrics()
    {
        metrics.clear();
    }

    // start depth
    public void setStartDay(int startDay)
    {
        this.startDay = startDay;
    }

    public int getStartDay()
    {
        return startDay;
    }

    // scan depth
    public void setDaysToScan(int daysToScan)
    {
        this.daysToScan = daysToScan;
    }

    public int getDaysToScan()
    {
        return daysToScan;
    }

    // network filter
    public void setNetworks(Filter networks)
    {
        this.networks = networks;
    }

    public Filter getNetworks()
    {
        return networks;
    }

    // station filter
    public void setStations(Filter stations)
    {
        this.stations = stations;
    }

    public Filter getStations()
    {
        return stations;
    }

    // location filter
    public void setLocations(Filter locations)
    {
        this.locations = locations;
    }

    public Filter getLocations()
    {
        return locations;
    }

    // channel filter
    public void setChannels(Filter channels)
    {
        this.channels = channels;
    }

    public Filter getChannels()
    {
        return channels;
    }
}
