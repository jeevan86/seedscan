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

import java.util.logging.Logger;

public abstract class PowerBandMetric
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.CoherencePBM");

    public PowerBandMetric()
    {
        super();
        addArgument("lower-limit");
        addArgument("upper-limit");
    }

    public final PowerBand getPowerBand()
    {
        PowerBand band = null;
        try {
            band = new PowerBand(Double.parseDouble(get("lower-limit")), Double.parseDouble(get("upper-limit")));
        } catch (NoSuchFieldException ex) {
            ;
        }
        return band;
    }

    protected abstract String getBaseName();

    public final String getName()
    {
        PowerBand band = getPowerBand();
        //This gives a runtime error: I think it will left-justify by default anyway ...
        //return getBaseName() + String.format("-%0.6f-%0.6f", band.getLow(), band.getHigh());
        return getBaseName() + String.format(":%.6f-%.6f", band.getLow(), band.getHigh());
    }
}

