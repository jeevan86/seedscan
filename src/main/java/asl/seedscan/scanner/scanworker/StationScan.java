package asl.seedscan.scanner.scanworker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;
import asl.seedscan.Global;
import asl.seedscan.database.DatabaseScan;
import asl.seedscan.event.EventCMT;
import asl.seedscan.event.EventLoader;
import asl.seedscan.metrics.Metric;
import asl.seedscan.metrics.MetricData;
import asl.seedscan.metrics.MetricResult;
import asl.seedscan.metrics.MetricWrapper;
import asl.seedscan.scanner.DataLoader;
import asl.seedscan.scanner.ScanManager;
import asl.timeseries.CrossPower;
import asl.timeseries.CrossPowerKey;
import asl.util.Logging;
import sac.SacTimeSeries;

/**
 * The Class Scan.
 */
public class StationScan extends ScanWorker {

  private static final Logger logger = LoggerFactory
      .getLogger(asl.seedscan.scanner.scanworker.StationScan.class);

  private final Station station;
  private final DatabaseScan databaseScan;

  private final LocalDate currentDate;

  /**
   * Data for the day being scanned.
   */
  private MetricData currentMetricData;
  /**
   * Data for the day that chronologically follows day being scanned.
   */
  private MetricData nextMetricData;


  /**
   * Start the first day of the scan
   *
   * @param manager The managing object
   * @param databaseScan The correct database for results.
   */
  StationScan(ScanManager manager, DatabaseScan databaseScan) {
    super(manager);
    this.station = new Station(databaseScan.network, databaseScan.station);
    this.databaseScan = databaseScan;
    this.currentDate = databaseScan.startDate;
  }

  /**
   * Scan a specified day with provided metricData
   *
   * @param manager The managing object
   * @param databaseScan The correct database for results.
   * @param date The day to scan
   * @param metricData The days preloaded MetricData. Can be null
   */
  StationScan(ScanManager manager, DatabaseScan databaseScan, LocalDate date,
      MetricData metricData) {
    super(manager);
    this.station = new Station(databaseScan.network, databaseScan.station);
    this.databaseScan = databaseScan;
    this.currentDate = date;
    this.currentMetricData = metricData;
  }


  /*
   * (non-Javadoc)
   *
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    try {
      logger.debug("Scan Station={} Day={} Thread id=[{}]", station,
          currentDate.format(DateTimeFormatter.ISO_ORDINAL_DATE), Thread.currentThread().getId());

      // CMT Event loader - use to load events for each day
      EventLoader eventLoader = new EventLoader(Global.getEventsDir());

      LocalDate nextDayTimestamp = currentDate.plusDays(1);

      // Get all the channel metadata for this station, for this day
      StationMeta stnMeta = manager.metaGenerator
          .getStationMeta(station, currentDate.atStartOfDay());

      Hashtable<String, Hashtable<String, SacTimeSeries>> eventSynthetics = null;

      Hashtable<String, EventCMT> eventCMTs = eventLoader.getDayEvents(currentDate);
      if (eventCMTs != null) {
        eventSynthetics = eventLoader.getDaySynthetics(currentDate, station);
      }

      // May have been passed from previous day
      if (currentMetricData == null) {
        currentMetricData = DataLoader.getMetricData(currentDate, station, manager);
      }
      nextMetricData = DataLoader.getMetricData(nextDayTimestamp, station, manager);

      if (currentMetricData != null) {
        // This doesn't mean nextMetricData isn't null!
        currentMetricData.setNextMetricData(nextMetricData);
      }

      // No Metadata found for this station-day --> skip day
      if (stnMeta == null) {
        logger.info("== Scanner: No Metadata found for Station:{}_{} for Day:{} --> Skipping",
            station.getNetwork(), station.getStation(),
            currentDate.format(DateTimeFormatter.ISO_ORDINAL_DATE));
      } else {
        stnMeta.printStationInfo();

        // Loop over Metrics to compute, for this station, for this day
        Hashtable<CrossPowerKey, CrossPower> crossPowerMap = null;

				/*
         * TODO: The contents of this for loop should be extracted out into a task and run in the pool.
				 * Skipping adding tests for it now.
				 */
        for (MetricWrapper wrapper : Global.getMetrics()) {
          Metric metric = wrapper.getNewInstance();
          metric.setBaseOutputDir(Global.getPlotsDir());

          if (currentMetricData == null) {
            metric.setData(new MetricData(manager.database, stnMeta));
          } else {
            metric.setData(currentMetricData);
          }
          if (eventCMTs != null) {
            metric.setEventTable(eventCMTs);
            if (eventSynthetics != null) {
              metric.setEventSynthetics(eventSynthetics);
            }
          }

          // Hand off the crossPowerMap from metric to metric,
          // adding to it each time
          if (crossPowerMap != null) {
            metric.setCrossPowerMap(crossPowerMap);
          }
          metric.process();
          // Save the current crossPowerMap for the next metric:
          crossPowerMap = metric.getCrossPowerMap();

          MetricResult results = metric.getMetricResult();
          if (results != null) {
            if (manager.database.isConnected()) {
              manager.database.insertMetricData(results);
            }
          }
        } // end loop over metrics
      }
      // Insert Next Day task
      if (nextDayTimestamp.compareTo(databaseScan.endDate) <= 0) {
        manager.addTask(
            new StationScan(this.manager, this.databaseScan, nextDayTimestamp,
                this.nextMetricData));
      } else {
        // We have finished this station
        manager.database.finishScan(databaseScan.scanID);
      }

    } catch (Exception e) {
      String message = Logging.exceptionToString(e);
      logger.error(message);
      manager.database
          .insertScanMessage(databaseScan.parentScanID, station.getNetwork(), station.getStation(),
              null, null, null, message);
    } finally {
      // Cleanup
      currentMetricData = null;
      nextMetricData = null;
    }
  }

  /* (non-Javadoc)
   * @see asl.seedscan.worker.Worker#getPriorityBase()
   */
  @Override
  public Integer getBasePriority() {
    //Average StationScan priority.
    return 45;
  }

  public Long getFinePriority() {
    //We want to prioritize dates when comparing station scans
    return this.currentDate.toEpochDay();
  }

}
