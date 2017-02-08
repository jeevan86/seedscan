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

public abstract class PowerBandMetric extends Metric {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.metrics.PowerBandMetric.class);

	public PowerBandMetric() {
		super();
		addArgument("lower-limit");
		addArgument("upper-limit");
	}

	public final PowerBand getPowerBand() {
		PowerBand band = null;
		try {
			band = new PowerBand(Double.parseDouble(get("lower-limit")),
					Double.parseDouble(get("upper-limit")));
		} catch (NoSuchFieldException ex) {
			logger.error("NoSuchFieldException:", ex);
		}
		return band;
	}

	protected abstract String getBaseName();

	public final String getName() {
		PowerBand band = getPowerBand();
		// This gives a runtime error: I think it will left-justify by default
		// anyway ...
		// return getBaseName() + String.format("-%0.6f-%0.6f", band.getLow(),
		// band.getHigh());
		return getBaseName()
				+ String.format(":%s-%s", smartNumberFormat(band.getLow(), 6),
						smartNumberFormat(band.getHigh(), 6));
	}

	private static String smartNumberFormat(double value, int precision) {
		String formatted;
		formatted = String.format(String.format("%%.%df", precision), value);
		int clipCount = 0;
		for (int i = formatted.length() - 1; i >= 0; i--) {
			char c = formatted.charAt(i);
			if (c == '0') {
				clipCount++;
			} else {
				if (c == '.') {
					clipCount++;
				}
				break;
			}
		}
		if (clipCount > 0) {
			formatted = formatted.substring(0, formatted.length() - clipCount);
		}
		return formatted;
	}

	protected static boolean checkPowerBand(double lowPeriod,
			double highPeriod, double Tmin, double Tmax) {

		if (lowPeriod >= highPeriod) {
			logger.warn(String
					.format("checkPowerBand: Requested band [%f - %f] has lowPeriod >= highPeriod\n",
							lowPeriod, highPeriod));
			return false;
		}
		// Make sure that we only compare to Noise Model within the range of
		// useable periods/frequencies for this channel
		if (lowPeriod < Tmin || highPeriod > Tmax) {

			logger.warn("checkPowerBand: Requested band [{} - {}] lies outside channel's Useable band [{} - {}]",
							lowPeriod, highPeriod, Tmin, Tmax);
			return false;
		}

		return true;
	}
}