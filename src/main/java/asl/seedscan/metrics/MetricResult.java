package asl.seedscan.metrics;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;

public class MetricResult {
	private String metricName;
	private LocalDate date;
	private Station station;
	private Hashtable<String, Double> valueMap;
	private Hashtable<String, ByteBuffer> digestMap;

	public MetricResult(StationMeta stationInfo, String metricName) {
		this.metricName = metricName;
		this.date = stationInfo.getTimestamp().toLocalDate();
		this.station = new Station(stationInfo.getNetwork(),
				stationInfo.getStation());
		this.valueMap = new Hashtable<>();
		this.digestMap = new Hashtable<>();
	}

	public String getMetricName() {
		return metricName;
	}

	public LocalDate getDate() {
		return date;
	}

	public Station getStation() {
		return station;
	}

	public void addResult(Channel channel, Double value, ByteBuffer digest) {
		addResult(createResultId(channel), value, digest);
	}

	public void addResult(Channel channelA, Channel channelB, Double value,
			ByteBuffer digest) {
		addResult(createResultId(channelA, channelB), value, digest);
	}

	public void addResult(String id, Double value, ByteBuffer digest) {
		valueMap.put(id, value);
		digestMap.put(id, digest);
	}

	public Double getResult(String id) {
		return valueMap.get(id);
	}

	public ByteBuffer getDigest(String id) {
		return digestMap.get(id);
	}

	public Enumeration<String> getIds() {
		return valueMap.keys();
	}

	public Set<String> getIdSet() {
		return valueMap.keySet();
	}

	public SortedSet<String> getIdSortedSet() {
		return new TreeSet<>(valueMap.keySet());
	}

	// Static methods
	public static String createResultId(Channel channel) {
		return String.format("%s,%s", channel.getLocation(),
				channel.getChannel());
	}

	public static String createResultId(Channel channelA, Channel channelB) {
		return String.format("%s-%s,%s-%s", channelA.getLocation(),
				channelB.getLocation(), channelA.getChannel(),
				channelB.getChannel());
	}

	public static Channel createChannel(String id) {
		Channel channel = null;
		if (id.length() > 15) {
			/*
			 * This is likely to be coming from a CalibrationMetric so handle
			 * differently id = "{ "channelId":"00-LHZ", "band": { ..."
			 */
			String[] f1 = id.split(":"); // f1="00-LHZ","band"
			String[] f2 = f1[1].split(","); // f2="00-LHZ"
			String[] f3 = f2[0].split("-"); // f3[0]="00" f3[1]="LHZ"
			channel = new Channel(f3[0], f3[1]);
		} else {
			String[] parts = id.split(",");
			if (parts.length == 2) {
				channel = new Channel(parts[0], parts[1]);
			}
		}
		return channel;
	}
}
