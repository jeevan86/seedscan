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

public class PowerBand
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.CoherencePBM.class);

    private double low;
    private double high;

    public PowerBand(double low, double high)
    {
        this.low = low;
        this.high = high;
    }

    public double getLow()
    {
        return low;
    }

    public double getHigh()
    {
        return high;
    }
}

