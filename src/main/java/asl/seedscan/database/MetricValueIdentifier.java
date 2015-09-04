package asl.seedscan.database;

import java.util.Calendar;

import asl.metadata.Channel;
import asl.metadata.Station;

public class MetricValueIdentifier {
	private Calendar date;
	private String metricName;
	private Station station;
	private Channel channel;

	public MetricValueIdentifier(Calendar date, String metricName,
			Station station, Channel channel) {
		this.date = date;
		this.metricName = metricName;
		this.station = station;
		this.channel = channel;
	}

	public Calendar getDate() {
		return date;
	}

	public String getMetricName() {
		return metricName;
	}

	public Station getStation() {
		return station;
	}

	public Channel getChannel() {
		return channel;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((channel == null) ? 0 : channel.hashCode());
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((metricName == null) ? 0 : metricName.hashCode());
		result = prime * result + ((station == null) ? 0 : station.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetricValueIdentifier other = (MetricValueIdentifier) obj;
		if (channel == null) {
			if (other.channel != null)
				return false;
		} else if (!channel.equals(other.channel))
			return false;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (metricName == null) {
			if (other.metricName != null)
				return false;
		} else if (!metricName.equals(other.metricName))
			return false;
		if (station == null) {
			if (other.station != null)
				return false;
		} else if (!station.equals(other.station))
			return false;
		return true;
	}
}
