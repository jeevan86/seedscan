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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Station;
import asl.metadata.MetaServer;
import asl.seedscan.database.*;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;

public class ScanManager
//implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.ScanManager.class);

    private Scan scan = null;
    private ConcurrentLinkedQueue<Runnable> taskQueue = null;
    private boolean running = true;

    public ScanManager(MetricReader reader, MetricInjector injector, List<Station> stationList, Scan scan, MetaServer metaServer)

    {
        this.scan = scan;
        taskQueue = new ConcurrentLinkedQueue<Runnable>(); // Create the task queue
        for (Station station : stationList) {
            if (passesFilter(station)) {
                logger.debug("Add station={} to the task queue", station);
                logger.info("Add station={} to the task queue", station);
                taskQueue.add( new Scanner(reader, injector, station, scan, metaServer) );
            }
            else {
                logger.debug("station={} Did NOT pass filter for scan={}", station, scan.getName());
            }
        }

        //int threadCount = Runtime.getRuntime().availableProcessors();
        int threadCount = 1;
        logger.info("Number of Threads to Use = [{}]", threadCount);

        WorkerThread[] workers = new WorkerThread[threadCount];
        running = true;
        //threadsCompleted = 0;
        for (int i=0; i<threadCount; i++) {
            workers[i] = new WorkerThread();
            try {
                workers[i].setPriority( Thread.currentThread().getPriority() - 1);
            }
            catch (Exception e) {
                logger.error("Caught exception:", e);
            }
            logger.info("Start thread:[{}]", i);
            workers[i].start();
        }
    }

    private boolean passesFilter(Station station) {
        if (scan.getNetworks() != null){
            if (!scan.getNetworks().filter(station.getNetwork())) {
                return false;
            }
        }
        if (scan.getStations() != null){
            if (!scan.getStations().filter(station.getStation())) {
                return false;
            }
        }
        return true;
    }

    private class WorkerThread extends Thread {
        public void run() {
            try {
                while (running) {
                    Runnable task = taskQueue.poll(); // Get a task from the queue
                    if (task == null) {
                        break;  // queue is empty
                    }
                    task.run(); // execute the task
                }
            }
            finally {
            }
        }
    }


}
