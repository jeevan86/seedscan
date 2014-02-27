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
package asl.seedscan.metrics;

import java.awt.Color;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.nio.ByteBuffer;

import java.awt.BasicStroke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.util.Hex;
import asl.util.PlotMaker;
import asl.util.PlotMaker2;
import asl.util.Trace;

import timeutils.Timeseries;

/**
 * DeadChannelMetric - Computes Difference (over 4-8 second period) between the
 * 		       the power spectral density (psd) of a channel and the NLNM
 *		       if this value is at or below a 5dB threshold the channel is dead
 */

public class DeadChannelMetric
extends PowerBandMetric
{
	private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.DeadChannelMetric.class);

	@Override public long getVersion()
	{
		return 1;
	}

	@Override public String getBaseName()
	{
		return "DeadChannelMetric";
	}

	public void process()
	{
			String station;
			String day;
			Calendar date;	
			station = getStation();
			day = getDay();
			logger.info("-Enter- [ Station {} ] [ Day {} ]", station, day);
			
			// Low noise model (NLNM) MUST exist or we can't compute the metric
			System.out.format("DeadChannelMetric station/date: %s/%s", station, day);
	}
}

