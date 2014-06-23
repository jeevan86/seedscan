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
package asl.seedscan.event;

import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventCMT {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.event.EventCMT.class);

	private final String eventID;
	private final double eventLat;
	private final double eventLon;
	private final double eventDep;
	private final double eventMw;
	private final Calendar eventCal;

	public static class Builder {
		// Required params
		private final String eventID;

		// Optional params
		private Calendar eventCal;
		private double eventLat = -999.;
		private double eventLon = -999.;
		private double eventDep = -999.;
		private double eventMw = -999.;

		public Builder(String eventID) {
			this.eventID = eventID;
		}

		public Builder calendar(Calendar val) {
			eventCal = (Calendar) val.clone();
			return this;
		}

		public Builder latitude(double val) {
			eventLat = val;
			return this;
		}

		public Builder longitude(double val) {
			eventLon = val;
			return this;
		}

		public Builder depth(double val) {
			eventDep = val;
			return this;
		}

		public Builder mw(double val) {
			eventMw = val;
			return this;
		}

		public EventCMT build() {
			return new EventCMT(this);
		}
	}

	// constructor
	private EventCMT(Builder builder) {
		eventID = builder.eventID;
		eventCal = builder.eventCal;
		eventLat = builder.eventLat;
		eventLon = builder.eventLon;
		eventDep = builder.eventDep;
		eventMw = builder.eventMw;
	}

	public String getEventID() {
		return eventID;
	}

	public Calendar getCalendar() {
		return (Calendar) eventCal.clone();
	}

	public long getTimeInMillis() {
		return eventCal.getTimeInMillis();
	}

	public double getLatitude() {
		return eventLat;
	}

	public double getLongitude() {
		return eventLon;
	}

	public double getDepth() {
		return eventDep;
	}

	public String toString() {
		return new String(
				String.format(
						"== EventCMT: eventID=[%s] %d/%02d/%02d (%03d) %02d:%02d:%02d.%03d",
						eventID, eventCal.get(Calendar.YEAR),
						eventCal.get(Calendar.MONTH) + 1,
						eventCal.get(Calendar.DAY_OF_MONTH),
						eventCal.get(Calendar.DAY_OF_YEAR),
						eventCal.get(Calendar.HOUR_OF_DAY),
						eventCal.get(Calendar.MINUTE),
						eventCal.get(Calendar.SECOND),
						eventCal.get(Calendar.MILLISECOND)));
	}

	public void printCMT() {
		System.out
				.format("== EventCMT: eventID=[%s] %d/%02d/%02d (%03d) %02d:%02d:%02d.%03d\n",
						eventID, eventCal.get(Calendar.YEAR),
						eventCal.get(Calendar.MONTH) + 1,
						eventCal.get(Calendar.DAY_OF_MONTH),
						eventCal.get(Calendar.DAY_OF_YEAR),
						eventCal.get(Calendar.HOUR_OF_DAY),
						eventCal.get(Calendar.MINUTE),
						eventCal.get(Calendar.SECOND),
						eventCal.get(Calendar.MILLISECOND));
	}

}
