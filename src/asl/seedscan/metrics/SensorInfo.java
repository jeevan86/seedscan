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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SensorInfo
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.SensorInfo.class);

    private String sensorName=null;
    private double Tmin;
    private double Tmax;
    private double Tnorm;

    // constructor
    public SensorInfo(String sensorName, double Tmin, double Tmax, double Tnorm)
    {
        this.sensorName  = sensorName;
        this.Tmin  = Tmin;
        this.Tmax  = Tmax;
        this.Tnorm = Tnorm;
    }
    public String getSensorName(){
        return sensorName;
    }
    public double getMinPeriod(){
        return Tmin;
    }
    public double getMaxPeriod(){
        return Tmax;
    }
    public double getNormPeriod(){
        return Tnorm;
    }

}

