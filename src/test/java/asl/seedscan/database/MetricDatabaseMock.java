package asl.seedscan.database;

import asl.metadata.Channel;
import asl.metadata.Station;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class MetricDatabaseMock extends MetricDatabase {

  private boolean mockConnected;

  private HashMap<MetricValueIdentifier, Double> values = new HashMap<>();
  private HashMap<MetricValueIdentifier, ByteBuffer> digests = new HashMap<>();

  private Queue<DatabaseScan> newScans = new LinkedBlockingQueue<>();
  private Map<UUID, DatabaseScan> takenScans = new HashMap<>();
  private Map<UUID, DatabaseScan> finishedScans = new HashMap<>();
  private List<DatabaseScan> childScans = new LinkedList<>();

  private int scanRequests = 0;
  private int errorsInserted = 0;
  private int messagesInserted = 0;
  private int numberOfInsertedChildScans = 0;

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

    takenScans.put(scan.scanID, scan);
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

  @Override
  public synchronized void insertScanMessage(UUID scanID, String network, String station, String location, String channel,
      String metric, String message) {
    messagesInserted++;
  }

  public void insertChildScan(UUID parentID, String network, String station, String location, String channel,
      String metric, LocalDate startDate, LocalDate endDate, int priority, boolean deleteExisting) {
    numberOfInsertedChildScans++;

    childScans.add(new DatabaseScan(
        null,
        parentID,
        metric,
        network,station, location, channel,
        startDate, endDate,
        priority,
        deleteExisting));
  }

  public synchronized int getNumberScanMessages(){
    return messagesInserted;
  }

  @Override
  public void finishScan(UUID pkScanID) {
    DatabaseScan scan = takenScans.get(pkScanID);
    takenScans.remove(pkScanID);
    finishedScans.put(scan.scanID, scan);

  }

  public int getNumberOfInsertedChildScans() {
    return numberOfInsertedChildScans;
  }
}