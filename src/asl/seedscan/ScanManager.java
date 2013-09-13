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

import org.apache.log4j.Logger;

import asl.metadata.*;

public class ScanManager
implements Runnable
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.ScanManager");

    private Scan scan = null;

    public ScanManager(Scan scan)
    {
        this.scan = scan;
    }

    public void run()
    {
        // TODO: Manage a thread pool of Scanners

        /*
        for (Station station: stations) {
            Scanner scanner = new Scanner(database, station, scan);
            scanner.scan();
        }
        */
    }
}
