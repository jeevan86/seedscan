package asl.testutils;

import asl.metadata.MetaGenerator;
import asl.metadata.MetaGeneratorMock;
import asl.seedscan.database.MetricDatabaseMock;
import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;
import asl.seedscan.metrics.MetricData;
import asl.seedsplitter.DataSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.*;
import java.util.concurrent.*;

import asl.seedsplitter.SeedSplitter;
import seed.Blockette320;

/**
 * ResourceManager is used for loading and saving serialized objects. It may be
 * used for loading basic resources as well.
 *
 * @author James Holland - USGS
 */
public abstract class ResourceManager { // NO_UCD (test only)

  /**
   * Stores a copy of the resources that is shared between tests. Tests that
   * use shared objects, must not modify the object in a destructive manner.
   */
  private static HashMap<String, Object> resources = new HashMap<>();

  private static MetaGenerator sharedMetaGenerator;

  /**
   * Compress using GZIPOutputStream and write to file specific in fileName.
   *
   * @param object object to be serialized
   * @param fileName file to save object into
   * @throws IOException if any errors occur the caller is expected to handle them.
   */
  public static void compressAndSaveObject(Object object, String fileName) throws IOException {
    FileOutputStream fos = null;
    GZIPOutputStream gzos = null;
    ObjectOutputStream oos = null;

    try {
      fos = new FileOutputStream(fileName);
      gzos = new GZIPOutputStream(fos);
      oos = new ObjectOutputStream(gzos);
      oos.writeObject(object);
    } finally { // This is still executed despite return statement.
      if (oos != null) {
        oos.close();
      } else if (gzos != null) {
        gzos.close(); //Try next level up
      } else if (fos != null) {
        fos.close();
      }
    }
  }

  /**
   * Loads a resource based on the passed name.
   *
   * Synchronized to prevent multiple tests from simulaneously loading the same resources.
   *
   * @param fileName filename to try loading
   * @param trashableCopy returns a copy that is not shared with any other class. Any test using
   * shared objects must not destruct the object
   * @return decompressed object
   * @throws IOException If the file cannot be loaded.
   * @throws ClassNotFoundException If the file cannot be deserialized
   */
  public synchronized static Object loadCompressedObject(String fileName, boolean trashableCopy)
      throws IOException, ClassNotFoundException {
    if (!trashableCopy && resources.containsKey(fileName)) {
      return resources.get(fileName);
    }
    Object object = null;
    GZIPInputStream gzis = null;
    ObjectInputStream ois = null;
    try {
      gzis = new GZIPInputStream(ResourceManager.class.getResourceAsStream(fileName));
      ois = new ObjectInputStream(gzis);
      object = ois.readObject();

      if (!trashableCopy) {
        resources.put(fileName, object);
      }
      return object;

    } finally { // This is still executed despite return statement.
      if (ois != null) {
        ois.close();
      } else if (gzis != null) {
        gzis.close(); //Try next level up.
      }
    }

  }

  /**
   * Returns the path of a resource directory. This assume the passed
   * parameter is a directory.
   *
   * @param directory the resource directory we want to get the path for
   * @return the path as a string with ending /
   */
  public static String getDirectoryPath(String directory) {
    return ResourceManager.class.getResource(directory).getPath() + "/";
  }

  public static synchronized MetaGenerator loadMetaGenerator() throws Exception {
    Dependent.assumeRDSeed();
    if (sharedMetaGenerator == null) {
      sharedMetaGenerator = new MetaGenerator(
          ResourceManager.getDirectoryPath("/metadata"), null);
    }

    return sharedMetaGenerator;
  }

  /**
   * Return a MetricData object for the station + timestamp based on some sort of test data or other input
   *
   * @param timeSeriesDataLocation folder to find seed data relevant to metric under test
   * @param metadataLocation file of ascii data from rdseed for relevant metadata loading
   * @param date The date to load
   * @param station Station to load
   * @return complete MetricData object for station day.
   */
  public static MetricData getMetricData(String timeSeriesDataLocation, String metadataLocation,
                                         LocalDate date, Station station) {
    // TODO: may need additional data for how to load in metadata objects (i.,e., locations)
    // or to construct new metadata objects for specifically test data
    MetricDatabaseMock mockDB = new MetricDatabaseMock();
    MetaGeneratorMock mockMetadata = new MetaGeneratorMock(metadataLocation);
    StationMeta stationMeta = mockMetadata.getStationMeta(station, date.atStartOfDay());

    File dir = new File(getDirectoryPath(timeSeriesDataLocation));
    File [] files = dir.listFiles((dir1, name) -> name.endsWith(".seed"));

    Hashtable<String, ArrayList<DataSet>> dataTable;
    Hashtable<String, ArrayList<Integer>> qualityTable;
    Hashtable<String, ArrayList<Blockette320>> calibrationTable;

    int timeout = 900;
    SplitterObject splitObj = null;
    try {
      splitObj = executeSplitter(files, timeout, date);
      SeedSplitter splitter = splitObj.splitter;
      dataTable = splitObj.table;
      qualityTable = splitter.getQualityTable();
      calibrationTable = splitter.getCalTable();


      return new MetricData(mockDB, dataTable, qualityTable, stationMeta, calibrationTable);
    } catch (TimeoutException | ExecutionException | InterruptedException e) {
      e.printStackTrace();
      return null;
    }

  }

  /**
   * SeedSplitter function: processing times greater than 3 min. will move to
   * the next day
   */
  private static SplitterObject executeSplitter(File[] files, int timeout, LocalDate timestamp)
          throws TimeoutException, ExecutionException, InterruptedException {
    Hashtable<String, ArrayList<DataSet>> table = null;
    SeedSplitter splitter = new SeedSplitter(files);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<Hashtable<String, ArrayList<DataSet>>> future = executor.submit(new Task(splitter));

    try {
      table = future.get(timeout, TimeUnit.SECONDS);
    } catch (TimeoutException | ExecutionException | InterruptedException e) {
      future.cancel(true);
      throw e;
    }
    executor.shutdown();
    executor.awaitTermination(300, TimeUnit.SECONDS);

    return new SplitterObject(splitter, table);
  }

  // Class to assign seedplitter object and seedsplitter table
  private static class SplitterObject {

    private SeedSplitter splitter;
    private Hashtable<String, ArrayList<DataSet>> table;

    private SplitterObject(SeedSplitter splitter, Hashtable<String, ArrayList<DataSet>> table) {
      this.splitter = splitter;
      this.table = table;
    }
  }

  // Class to run Future task (seedplitter.doInBackground())
  private static class Task implements Callable<Hashtable<String, ArrayList<DataSet>>> {

    private SeedSplitter splitter;

    private Task(SeedSplitter splitter) {
      this.splitter = splitter;
    }

    public Hashtable<String, ArrayList<DataSet>> call() throws Exception {
      Hashtable<String, ArrayList<DataSet>> table = splitter.doInBackground();
      return table;
    }
  }
}
