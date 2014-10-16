/*
 * Copyright 2011, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */
package asl.seedscan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.MetaServer;
import asl.metadata.Station;
import asl.seedscan.database.MetricInjector;
import asl.seedscan.database.MetricReader;

public class ScanManager {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.ScanManager.class);

	private Scan scan = null;

	public ScanManager(MetricReader reader, MetricInjector injector,
			List<Station> stationList, Scan scan, MetaServer metaServer)

	{
		this.scan = scan;

		int threadCount = Runtime.getRuntime().availableProcessors();
		// We don't want to overload the computer. There are also injector
		// threads and the main thread.
		if (threadCount > 20) {
			threadCount = 20;
			//threadCount = 1;
		}

		logger.info("Number of Threads to Use = [{}]", threadCount);

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		List<Callable<Object>> tasks = new ArrayList<Callable<Object>>(
				stationList.size());
		for (Station station : stationList) {
			if (passesFilter(station)) {
				logger.debug("Add station={} to the task queue", station);
				tasks.add(Executors.callable(new Scanner(reader, injector,
						station, scan, metaServer)));
			} else {
				logger.debug("station={} Did NOT pass filter for scan={}",
						station, scan.getName());
			}
		}
		try {
			executor.invokeAll(tasks); // It will wait here until scanner
										// threads finish.
			executor.shutdown();
			executor.awaitTermination(300, TimeUnit.SECONDS); // This lets any injector/reader threads finish
																// before we return.
		} catch (InterruptedException e) {
			logger.warn("Scan Manager executor service interrupted:", e);
		} catch (NullPointerException e) {
			logger.warn("Scan Manager executor service returned null:", e);
		} catch (RejectedExecutionException e) {
			logger.warn("Scan Manager executor service cannot be scheduled:", e);
		}

		logger.info("ALL SCANNER THREADS HAVE FINISHED");
	}

	private boolean passesFilter(Station station) {
		if (scan.getNetworks() != null) {
			if (!scan.getNetworks().filter(station.getNetwork())) {
				return false;
			}
		}
		if (scan.getStations() != null) {
			if (!scan.getStations().filter(station.getStation())) {
				return false;
			}
		}
		return true;
	}

}
