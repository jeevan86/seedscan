package asl.seedscan.database;

import asl.metadata.Channel;
import asl.metadata.Station;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.HashMap;
import javax.xml.bind.DatatypeConverter;

public class MetricDatabaseMock extends MetricDatabase {

  private boolean mockConnected;

  private HashMap<MetricValueIdentifier, Double> values = new HashMap<>();
  HashMap<MetricValueIdentifier, ByteBuffer> digests = new HashMap<>();

  public MetricDatabaseMock() {
    super(); //Call required because of extension.
    this.mockConnected = true;
  }

  public void setConnected(boolean isConnected) {
    this.mockConnected = isConnected;
  }

  public void insertMockData(MetricValueIdentifier id, Double value, ByteBuffer digest){
    values.put(id, value);
    digests.put(id, digest);
  }

  public void insertMockDigest(MetricValueIdentifier id, ByteBuffer digest){
    digests.put(id, digest);
  }

  @Override
  public boolean isConnected() {
    return mockConnected;
  }

  /**
   * Reserved channels that must be missing include 00-BH*
   */
  public Double getMetricValue(LocalDate date, String metricName, Station station, Channel channel) {
    return values.get(new MetricValueIdentifier(date, metricName, station, channel));
  }

  /**
   * Currently getMetricValueDigest() is the only method called (from the
   * MetricData class)
   */
  public ByteBuffer getMetricValueDigest(LocalDate date, String metricName, Station station, Channel channel) {
    return digests.get(new MetricValueIdentifier(date, metricName, station, channel));
  }
}