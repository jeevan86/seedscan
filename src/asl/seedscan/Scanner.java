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

import java.io.File;
import java.io.IOException;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.concurrent.BlockingQueue;

import asl.concurrent.FallOffQueue;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.SeedSplitProgress;
import asl.seedsplitter.SeedSplitter;
import asl.seedscan.scan.Scan;

public class Scanner
    implements Runnable
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.Scanner");
    public long dayMilliseconds = 1000 * 60 * 60 * 24;

    public Station station;
    public StationDatabase database;
    public Scan scan;

    private FallOffQueue<SeedSplitProgress> progressQueue;

    public Scanner(StationDatabase database, Station station, Scan scan)
    {
        this.station  = station;
        this.database = database;
        this.scan = scan;
        this.progressQueue = new FallOffQueue<SeedSplitProgress>(8);
    }

    public void run() {
        scan();
    }

    public void scan()
    {
        GregorianCalendar timestamp = new GregorianCalendar();
        if (scan.getStartDepth() > 0) {
            timestamp.setTimeInMillis(timestamp.getTimeInMillis() - (scan.getStartDepth() * dayMilliseconds));
        }

        for (int i=0; i < scan.getScanDepth(); i++) {
            if (i != 0) {
                timestamp.setTimeInMillis(timestamp.getTimeInMillis() - dayMilliseconds);
            }
            ArchivePath pathEngine = new ArchivePath(timestamp, station);
            String path = pathEngine.makePath(scan.getPathPattern());
            File dir = new File(path);
            if (!dir.exists()) {
                logger.info("Path '" +dir+ "' does not exist.");
                continue;
            }
            else if (!dir.isDirectory()) {
                logger.info("Path '" +dir+ "' is not a directory.");
                continue;
            }

            File[] files = dir.listFiles();
            int seedCount = files.length;

            Hashtable<String,ArrayList<DataSet>> table = null;
            logger.info(dir.getPath() + " contains " +seedCount+ " files.");
            progressQueue.clear();
            SeedSplitter splitter = new SeedSplitter(files, progressQueue);
            table = splitter.doInBackground();

            // TODO: List Contents

            //*
            for (File file: files) {
                if (file.getName().endsWith(".seed")) {
                    seedCount++;
                    logger.fine("Processing file '" +file.getPath()+ "'.");
                }
            }
            // */
        }
    }
}
