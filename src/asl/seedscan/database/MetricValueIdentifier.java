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
}
