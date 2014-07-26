/*
 * Copyright 2011, United States Geological Survey or
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

import java.util.GregorianCalendar;

import asl.metadata.Channel;
import asl.metadata.Station;

/**
 * 
 */
public class ArchivePath {

	private GregorianCalendar timestamp;
	private Station station = null;
	private Channel channel = null;

	public ArchivePath(Station station) {
		this.station = station;
	}

	public ArchivePath(GregorianCalendar timestamp, Station station) {
		this.timestamp = timestamp;
		this.station = station;
	}

	public ArchivePath(GregorianCalendar timestamp, Channel channel) {
		this.timestamp = timestamp;
		this.station = channel.getStation();
		this.channel = channel;
	}

	public void setTimestamp(GregorianCalendar timestamp) {
		this.timestamp = timestamp;
	}

	public void setStation(Station station) {
		this.station = station;
	}

	public void setChannel(Channel channel) {
		this.station = channel.getStation();
		this.channel = channel;
	}

	public String makePath(String pattern) {
		if (station != null) {
			if (station.getNetwork() != null) {
				pattern = pattern.replace("${NETWORK}", station.getNetwork());
			}
			pattern = pattern.replace("${STATION}", station.getStation());
		}
		if (channel != null) {
			if (channel.getLocation() != null) {
				pattern = pattern.replace("${LOCATION}", channel.getLocation());
			}
			pattern = pattern.replace("${CHANNEL}", channel.getChannel());
		}
		pattern = pattern.replace("${YEAR}", String.format("%1$tY", timestamp));
		pattern = pattern
				.replace("${MONTH}", String.format("%1$tm", timestamp));
		pattern = pattern.replace("${DAY}", String.format("%1$td", timestamp));
		pattern = pattern.replace("${JDAY}", String.format("%1$tj", timestamp));
		pattern = pattern.replace("${HOUR}", String.format("%1$tH", timestamp));
		pattern = pattern.replace("${MINUTE}",
				String.format("%1$tM", timestamp));
		pattern = pattern.replace("${SECOND}",
				String.format("%1$tS", timestamp));

		return pattern;
	}
}
