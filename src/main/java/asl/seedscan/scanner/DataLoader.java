package asl.seedscan.scanner;

import java.io.File;
import java.io.FilenameFilter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;
import asl.seedscan.ArchivePath;
import asl.seedscan.Global;
import asl.seedscan.metrics.MetricData;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.SeedSplitter;
import seed.Blockette320;

public abstract class DataLoader {
	
	private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.scanner.DataLoader.class);
	
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
			Hashtable<String, ArrayList<DataSet>> table = null;
			table = splitter.doInBackground();
			return table;
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

	
	/**
	 * Return a MetricData object for the station + timestamp
	 */
	public static MetricData getMetricData(LocalDate date, Station station, ScanManager manager) {

		StationMeta stationMeta = manager.metaGenerator.getStationMeta(station, date.atStartOfDay());
		if (stationMeta == null) {
			return null;
		}

		ArchivePath pathEngine = new ArchivePath(date.atStartOfDay(), station);
		String path = pathEngine.makePath(Global.CONFIG.getPath());
		File dir = new File(path);
		File[] files = null;
		boolean dataExists = true;

		/*
		  MTH: There are some non-seed files (e.g., data_avail.txt) included in
		  files[]. For some reason the file netday.index causes the splitter to
		  hang. Either restrict the file list to .seed files (as I do below)
		  -or- Debug splitter so it drops non-seed/miniseed files.
		 */
		FilenameFilter textFilter = (dir1, name) -> {
      String lowercaseName = name.toLowerCase();
      File file = new File(dir1 + "/" + name);
      return lowercaseName.endsWith(".seed") && (file.length() > 0);
    };

		if (!dir.exists()) {
			logger.info("Path '" + dir + "' does not exist.");
			dataExists = false;
		} else if (!dir.isDirectory()) {
			logger.info("Path '" + dir + "' is not a directory.");
			dataExists = false;
		} else { // The dir exists --> See if we have any useful seed files in
					// it:

			files = dir.listFiles(textFilter);
			if (files == null) {
				dataExists = false;
			} else if (files.length == 0) {
				dataExists = false;
			}
		}

		if (!dataExists) {
			return null;
		}

		logger.info(dir.getPath() + " contains " + files.length + " files.");

		// execute SeedSplitter process (180 sec timer will be issued)
		try {
			int timeout = 180;
			SplitterObject splitObj = executeSplitter(files, timeout, date);
			SeedSplitter splitter = splitObj.splitter;
			Hashtable<String, ArrayList<DataSet>> table = splitObj.table;

			Hashtable<String, ArrayList<Integer>> qualityTable = null;
			qualityTable = splitter.getQualityTable();

			Hashtable<String, ArrayList<Blockette320>> calibrationTable = null;
			calibrationTable = splitter.getCalTable();

			return new MetricData(manager.database, table, qualityTable, stationMeta, calibrationTable);
		} catch (TimeoutException e) {
			logger.error("== TimeoutException: Skipping to next day for [{}]:[{}]\n", station,
					date.format(DateTimeFormatter.ISO_ORDINAL_DATE));
			return null;
		} catch (ExecutionException e) {
			logger.error("== ExecutionException: Skipping to next day for [{}]:[{}]\n", station,
					date.format(DateTimeFormatter.ISO_ORDINAL_DATE));
			return null;
		} catch (InterruptedException e) {
			logger.error("== InterruptedException: Skipping to next day for [{}]:[{}]\n", station,
					date.format(DateTimeFormatter.ISO_ORDINAL_DATE));
			return null;
		}
	}
}
