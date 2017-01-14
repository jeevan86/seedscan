package asl.seedscan;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.MetaGenerator;
import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.event.EventCMT;
import asl.seedscan.event.EventLoader;
import asl.seedscan.metrics.Metric;
import asl.seedscan.metrics.MetricData;
import asl.seedscan.metrics.MetricResult;
import asl.seedscan.metrics.MetricWrapper;
import asl.seedscan.scanner.DataLoader;
import asl.timeseries.CrossPower;
import asl.timeseries.CrossPowerKey;
import sac.SacTimeSeries;

class Scanner implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.Scanner.class);
	private Station station;
	private MetricDatabase database;
	private Scan scan;
	private MetaGenerator metaGenerator;





	Scanner(MetricDatabase database, Station station, Scan scan, MetaGenerator metaGenerator) {
		this.database = database;
		this.station = station;
		this.metaGenerator = metaGenerator;
		this.scan = scan;
	}

	public void run() {
		scan();
	}

	private void scan() {
		logger.debug("Enter scan(): Thread id=[{}]", Thread.currentThread().getId());

		LocalDate currentDate = null;
		
		MetricData currentMetricData = null;
		MetricData nextMetricData = null;

		// Look for cfg:start_date first:
		if (scan.getStartDate() >= 1970001 && scan.getStartDate() < 2114001) {
			currentDate = LocalDate.ofYearDay(scan.getStartDate() / 1000, scan.getStartDate() % 1000);
		} else if (scan.getStartDate() != 0 // Catch if no startDate is set
				&& (scan.getStartDate() < 1970001 || scan.getStartDate() > 2114001)) {
			logger.error(
					"Start Date=[{}] is invalid. Either it must be inbetween 1970001 and 2114001 OR 0 to use start_day.",
					scan.getStartDate());
			return; // Can't scan an invalid date so get out of here.
		}

		// timestamp is now set to current time - (24 hours x StartDay). What we
		// really want is to set it
		// to the start (hh:mm=00:00) of the first day we want to scan

		// CMT Event loader - use to load events for each day
		EventLoader eventLoader = new EventLoader(scan.getEventsDir());

		// Loop over days to scan, from most recent (currentDay=startDay) to
		// oldest (currentDay=startDay - daysToScan - 1)
		// e.g.,
		// i currentDay nextDay
		// -----------------------
		// 0 072 073
		// 1 071 072
		// 2 070 071
		// :
		// daysToScan - 1

		for (int i = 0; i < scan.getDaysToScan(); i++) {
			if (i != 0) {
				currentDate.minusDays(1);
			}
			LocalDate nextDayTimestamp = currentDate.plusDays(1);

			logger.debug("Scan Station={} Day={} Thread id=[{}]", station,
					currentDate.format(DateTimeFormatter.ISO_ORDINAL_DATE), Thread.currentThread().getId());

			// [1] Get all the channel metadata for this station, for this day
			// StationMeta stnMeta = metaGen.getStationMeta(station, timestamp);
			StationMeta stnMeta = metaGenerator.getStationMeta(station, currentDate.atStartOfDay());
			if (stnMeta == null) { // No Metadata found for this station + this
				// day --> skip day
				logger.warn("== Scanner: No Metadata found for Station:{}_{} + Day:{} --> Skipping",
						station.getNetwork(), station.getStation(), currentDate.format(DateTimeFormatter.ISO_ORDINAL_DATE));
				continue;
			}

			if (i == 0) {
				stnMeta.printStationInfo();
			}

			logger.info("Scan Station={} Day={}", station, currentDate.format(DateTimeFormatter.ISO_ORDINAL_DATE));

			Hashtable<String, EventCMT> eventCMTs = eventLoader.getDayEvents(currentDate);
			Hashtable<String, Hashtable<String, SacTimeSeries>> eventSynthetics = null;

			if (eventCMTs != null) {
				eventSynthetics = eventLoader.getDaySynthetics(currentDate, station);
			} else {
				// System.out.format("== Scanner: NO CMTs FOUND for this
				// day\n");
			}

			// [2] Read in all the seed files for this station, for this day &
			// for the next day
			// If this isn't the first day of the scan then simply copy current
			// into next so we
			// don't have to reread all of the seed files in

			if (i == 0) {
				nextMetricData = DataLoader.getMetricData(nextDayTimestamp, station, null);
			} else {
				// Need to null out ref to next day before passing
				// currentMetricData to avoid chaining refs
				if (currentMetricData != null) {
					currentMetricData.setNextMetricDataToNull();
					nextMetricData = currentMetricData;
				}
			}
			currentMetricData = null;
			currentMetricData = DataLoader.getMetricData(currentDate, station, null);

			if (currentMetricData != null) {
				// This doesn't mean nextMetricData isn't null!
				currentMetricData.setNextMetricData(nextMetricData);
			}

			// [3] Loop over Metrics to compute, for this station, for this day
			Hashtable<CrossPowerKey, CrossPower> crossPowerMap = null;

			try { // wrapper.getNewInstance()
				for (MetricWrapper wrapper : scan.getMetrics()) {
					Metric metric = wrapper.getNewInstance();
					metric.setBaseOutputDir(scan.getPlotsDir());

					if (currentMetricData == null) {
						metric.setData(new MetricData(this.database, stnMeta));
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

					// This is a little convoluted: calibration.getResult()
					// returns a MetricResult, which may contain many values
					// in a Hashtable<String,String> = map.
					// MetricResult.getResult(id) returns value = String

					MetricResult results = metric.getMetricResult();
					if (results == null) {
					} else {
						for (String id : results.getIdSortedSet()) {
							double value = results.getResult(id);
							/* @formatter:off */
							logger.info(String.format("%s [%7s] [%s] %15s:%6.2f", results.getMetricName(),
									results.getStation(), results.getDate().format(DateTimeFormatter.ISO_ORDINAL_DATE), id, value));

							if (Double.isNaN(value)) {
								logger.error(String.format("%s [%s] [%s] %s: ERROR: metric value = [ NaN ] !!\n",
										results.getMetricName(), results.getStation(),
										results.getDate().format(DateTimeFormatter.ISO_ORDINAL_DATE), id));
							}
							if (Double.isInfinite(value)) {
								logger.error(String.format("%s [%s] [%s] %s: ERROR: metric value = [ Infinity ] !!\n",
										results.getMetricName(), results.getStation(),
										results.getDate().format(DateTimeFormatter.ISO_ORDINAL_DATE), id));
							}
							/* @formatter:on */
						}
						if (database.isConnected()) {
							database.insertMetricData(results);
						} else {
							logger.warn("Injector *IS NOT* connected --> Don't inject");
						}
					}
				} // end loop over metrics
			} catch (InstantiationException e) {
				logger.error("Scanner InstantationException:", e);
			} catch (IllegalAccessException e) {
				logger.error("Scanner IllegalAccessException:", e);
			} catch (NoSuchFieldException e) {
				logger.error("Scanner NoSuchFieldException:", e);
			} catch (IllegalArgumentException e) {
				logger.error("Scanner IllegalArgumentException:", e);
			}
		} // end loop over day to scan
	} // end scan()

	
}
