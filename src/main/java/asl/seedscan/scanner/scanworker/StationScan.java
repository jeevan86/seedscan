package asl.seedscan.scanner.scanworker;

import asl.metadata.ChannelKey;
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
import asl.seedscan.metrics.PulseDetectionMetric;
import asl.seedscan.metrics.PulseDetectionMetric.PulseDetectionData;
import asl.seedscan.scanner.DataLoader;
import asl.seedscan.scanner.ScanManager;
import asl.timeseries.CrossPower;
import asl.timeseries.CrossPowerKey;
import asl.util.Logging;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sac.SacTimeSeries;

/**
 * The Class Scan.
 */
public class StationScan extends ScanWorker {

  private static final Logger logger = LoggerFactory
      .getLogger(asl.seedscan.scanner.scanworker.StationScan.class);

  final Station station;
  final DatabaseScan databaseScan;

  final LocalDate currentDate;

  Hashtable<String, EventCMT> eventCMTs;
  Hashtable<String, Hashtable<String, SacTimeSeries>> eventSynthetics;

  private StationMeta currentMetadata;
  /**
   * Data for the day being scanned.
   */
  MetricData currentMetricData;
  /**
   * Data for the day that chronologically follows day being scanned.
   */
  MetricData nextMetricData;
  MetricData previousMetricData;


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

  /**
   * Load the scan data independently of the run method. This allows better testing.
   */
  void loadScanData(){
    // CMT Event loader - use to load events for each day
    EventLoader eventLoader = new EventLoader(Global.getEventsDir());

    // Get all the channel metadata for this station, for this day
    currentMetadata = manager.metaGenerator
        .getStationMeta(station, currentDate.atStartOfDay());

    if (databaseScan.location != null && databaseScan.location.length() > 0) {
      // expect that the channel filter is a list of comma-delineated locations
      // in the event that only one location is specified this should still work
      String[] locations = databaseScan.location.split(",");
      currentMetadata.addToWhitelist(databaseScan.location);
    }

    boolean eventNearStartOfDay = false;
    eventCMTs = eventLoader.getDayEvents(currentDate);
    if (eventCMTs != null) {
      eventSynthetics = eventLoader.getDaySynthetics(currentDate, station);

      for( String key : eventCMTs.keySet()){
        if (eventCMTs.get(key).getCalendar().get(Calendar.HOUR_OF_DAY) < 4 ){
          eventNearStartOfDay = true;
        }
      }
    }

    // May have been passed from previous day
    if (currentMetricData == null) {
      currentMetricData = DataLoader.getMetricData(currentDate, station, manager);
    }
    nextMetricData = DataLoader.getMetricData(currentDate.plusDays(1), station, manager);


    if (currentMetricData != null) {
      // This doesn't mean nextMetricData isn't null!
      currentMetricData.setNextMetricData(nextMetricData);
      if (eventNearStartOfDay){
        // This doesn't use previously loaded data. It reloads metricdata for the day.
        previousMetricData = DataLoader.getMetricData(currentDate.minusDays(1), station, manager);
        currentMetricData.setPreviousMetricData(previousMetricData);
      }
    }
  }

  @Override
  public void run() {
    try {
      logger.debug("Scan Station={} Day={} Thread id=[{}]", station,
          currentDate.format(DateTimeFormatter.ISO_ORDINAL_DATE), Thread.currentThread().getId());

      loadScanData();

      // No Metadata found for this station-day --> skip day
      if (currentMetadata == null) {
        logger.info("== Scanner: No Metadata found for Station:{}_{} for Day:{} --> Skipping",
            station.getNetwork(), station.getStation(),
            currentDate.format(DateTimeFormatter.ISO_ORDINAL_DATE));
      } else {
        currentMetadata.printStationInfo();

        // Loop over Metrics to compute, for this station, for this day
        Hashtable<CrossPowerKey, CrossPower> crossPowerMap = null;
        Map<ChannelKey, PulseDetectionData> pulseDetectionMap = null;

				/*
         * TODO: The contents of this for loop should be extracted out into a task and run in the pool.
				 * Skipping adding tests for it now.
				 */
        for (MetricWrapper wrapper : Global.getMetrics()) {
          Metric metric = wrapper.getNewInstance();
          metric.setBaseOutputDir(Global.getPlotsDir());

          if (currentMetricData == null) {
            metric.setData(new MetricData(manager.database, currentMetadata));
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
          // Do the same for pulseDetectionMap, if this is a pulse detection metric
          if (metric instanceof PulseDetectionMetric && pulseDetectionMap != null) {
            ((PulseDetectionMetric) metric).setPulseDetectionData(pulseDetectionMap);
          }

          metric.process();
          // Save the current crossPowerMap for the next metric:
          crossPowerMap = metric.getCrossPowerMap();
          // And similar for pulse detection as necessary
          if (metric instanceof PulseDetectionMetric) {
            pulseDetectionMap = ((PulseDetectionMetric) metric).getPulseDetectionData();
          }

          MetricResult results = metric.getMetricResult();
          if (results != null) {
            if (manager.database.isConnected()) {
              manager.database.insertMetricData(results);
            }
          }
        } // end loop over metrics
      }
      // Insert Next Day task
      if (currentDate.plusDays(1).compareTo(databaseScan.endDate) <= 0) {
        manager.addTask(
            new StationScan(this.manager, this.databaseScan, currentDate.plusDays(1),
                this.nextMetricData));
      } else {
        // We have finished this station
        manager.database.finishScan(databaseScan.scanID);
      }

    } catch (Exception e) {
      String message = "Scan Date: " + this.currentDate + "\n" + Logging.prettyExceptionWithCause(e);
      logger.error(message);
      manager.database
          .insertScanMessage(databaseScan.parentScanID, station.getNetwork(), station.getStation(),
              null, null, null, message);
    } finally {
      // Cleanup

      // Release the previous day since we are done with it.
      if (currentMetricData != null) {
        if (currentMetricData.getPreviousMetricData() != null) {
          currentMetricData.getPreviousMetricData().setNextMetricDataToNull();
        }
        currentMetricData.setPreviousMetricDataToNull();
      }

      currentMetricData = null;
      nextMetricData = null;
      previousMetricData = null;
    }
  }

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
