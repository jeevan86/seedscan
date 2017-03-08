package asl.seedscan.database;

import asl.metadata.Channel;
import asl.metadata.Station;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class MetricDatabaseMock extends MetricDatabase {

  private boolean mockConnected;

  private HashMap<MetricValueIdentifier, Double> values = new HashMap<>();
  private HashMap<MetricValueIdentifier, ByteBuffer> digests = new HashMap<>();

  private Queue<DatabaseScan> newScans = new LinkedBlockingQueue<>();
  private List<DatabaseScan> takenScans = new LinkedList<>();
  private List<DatabaseScan> finishScans = new LinkedList<>();

  private int scanRequests = 0;
  private int errorsInserted = 0;

  public MetricDatabaseMock() {
    super(); //Call required because of extension.
    this.mockConnected = true;
  }

  public void setConnected(boolean isConnected) {
    this.mockConnected = isConnected;
  }

  @Override
  public boolean isConnected() {
    return mockConnected;
  }

  public void insertMockData(MetricValueIdentifier id, Double value, ByteBuffer digest){
    values.put(id, value);
    digests.put(id, digest);
  }

  public void insertMockDigest(MetricValueIdentifier id, ByteBuffer digest){
    digests.put(id, digest);
  }

  /**
   * Reserved channels that must be missing include 00-BH*
   */
  @Override
  public Double getMetricValue(LocalDate date, String metricName, Station station, Channel channel) {
    return values.get(new MetricValueIdentifier(date, metricName, station, channel));
  }

  /**
   * Currently getMetricValueDigest() is the only method called (from the
   * MetricData class)
   */
  @Override
  public ByteBuffer getMetricValueDigest(LocalDate date, String metricName, Station station, Channel channel) {
    return digests.get(new MetricValueIdentifier(date, metricName, station, channel));
  }



  @Override
  public synchronized DatabaseScan takeNextScan() {
    scanRequests++;
    if(newScans.isEmpty()){
      return null;
    }
    DatabaseScan scan =  newScans.poll();

    takenScans.add(scan);
    return scan;
  }

  public void offerNewScan(DatabaseScan scan){
    newScans.offer(scan);
  }

  @Override
  public synchronized void insertError(String message) {
    errorsInserted++;
  }

  public synchronized int getNumberErrors(){
    return errorsInserted;
  }

  public synchronized int getNumberOfScanRequests(){
    return scanRequests;
  }
}