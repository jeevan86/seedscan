/*
 * Copyright 2012, United States Geological Survey or
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
package asl.seedsplitter;

import org.apache.log4j.Logger;

/**
 * @author  Joel D. Edwards <jdedwards@usgs.gov>
 *
 * A simple class containing start and end times for contiguous data blocks.
 */
public class ContiguousBlock {
    private static final Logger logger = Logger.getLogger("asl.seedsplitter.ContiguousBlock");

    private long m_startTime;
    private long m_endTime;
    private long m_interval;
    
    /**
     * Constructor.
     * 
     * @param startTime	The start time of this time series.
     * @param endTime 	The end time of this time series.
     * @param interval	The interval (1/sample-rate) of this time series.
     */
    public ContiguousBlock(long startTime, long endTime, long interval) {
        m_startTime = startTime;
        m_endTime = endTime;
        m_interval = interval;
    }

    /**
     * Returns the number of microseconds between the start and end time.
     * 
     * @return The number of microseconds between the start and end time.
     */
    public long getRange() {
        return m_endTime - m_startTime;
    }

    /**
     * Returns the start time.
     * 
     * @return The start time.
     */
    public long getStartTime() {
        return m_startTime;
    }

    /**
     * Returns the end time.
     * 
     * @return The end time.
     */
    public long getEndTime() {
        return m_endTime;
    }

    /**
     * Returns the interval.
     * 
     * @return The interval.
     */
    public long getInterval() {
        return m_interval;
    }
}

