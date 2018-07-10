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
package asl.metadata;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpochData {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.EpochData.class);

	private Blockette format = null;
	private Blockette info = null;
	private ArrayList<Blockette> misc;
	private Hashtable<Integer, StageData> stages;

	// MTH:
	private LocalDateTime startTimestamp = null;
	private LocalDateTime endTimestamp = null;
	private double dip;
	private double azimuth;
	private double depth;
	private double elevation;
	private double sampleRate;
	private String instrumentType;
	private String channelFlags;

	public static String epochToDateString(LocalDateTime date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:DDD");
		if (date != null) {
			return date.format(formatter);
		} else {
			return "(null)";
		}
	}

	// Constructors
	public EpochData(Blockette info) {
		this.info = info;
		misc = new ArrayList<>();
		stages = new Hashtable<>();
		String startDateString = info.getFieldValue(22, 0);
		String endDateString = info.getFieldValue(23, 0);
		if (!startDateString.equals("(null)")) {
			try {
				startTimestamp = BlocketteTimestamp
						.parseTimestamp(startDateString);
			} catch (TimestampFormatException e) {
				logger.error("TimestampFormatException:", e);
			}
		}
		if (!endDateString.equals("(null)")) {
			try {
				endTimestamp = BlocketteTimestamp.parseTimestamp(endDateString);
			} catch (TimestampFormatException e) {
				logger.error("TimestampFormatException:", e);
			}
		}

		this.elevation = Double.parseDouble(info.getFieldValue(12, 0));
		this.depth = Double.parseDouble(info.getFieldValue(13, 0));
		this.azimuth = Double.parseDouble(info.getFieldValue(14, 0));
		this.dip = Double.parseDouble(info.getFieldValue(15, 0));
		this.sampleRate = Double.parseDouble(info.getFieldValue(18, 0));
		this.instrumentType = info.getFieldValue(6, 0);
		this.channelFlags = info.getFieldValue(21, 0);

		/*
		  MTH: This is what Blockette B052 looks like: B052F04 Channel: HNZ
		  B052F03 Location: 20 B052F05 Subchannel: 0 B052F06 Instrument lookup:
		  12 Kinemetrics FBA ES-T EpiSensor Accelerometer B052F07 Comment:
		  (null) B052F08 Signal units lookup: 7 M/S**2 - Acceleration in Meters
		  Per Second Per Second B052F09 Calibration units lookup: 0 No
		  Abbreviation Referenced B052F10 Latitude: 34.945913 B052F11
		  Longitude: -106.457295 B052F12 Elevation: 1816.000000 B052F13 Local
		  depth: 0.000000 B052F14 Azimuth: 0.000000 B052F15 Dip: 0.000000
		  B052F16 Format lookup: 2 Format Information Follows B052F17 Log2 of
		  Data record length: 9 B052F18 Sample rate: 100 B052F19 Clock
		  tolerance: 0 B052F21 Channel flags: CG B052F22 Start date: 2011,109
		  B052F23 End date: (null) B052F24 Update flag: N ... B030F03 Format
		  Name: Steim2 Integer Compression Format B030F05 Data family: 50
		 */
	}

	public EpochData(Blockette format, Blockette info) {
		this.format = format;
		this.info = info;
		misc = new ArrayList<>();
		stages = new Hashtable<>();
	}

	// Info
	public void setInfo(Blockette info) {
		this.info = info;
	}

	public Blockette getInfo() {
		return info;
	}

	// Format
	public void setFormat(Blockette format) {
		this.format = format;
	}

	public Blockette getFormat() {
		return format;
	}

	// Misc Blockettes
	public void addMiscBlockette(Blockette blockette) {
		misc.add(blockette);
	}

	public ArrayList<Blockette> getMiscBlockettes() {
		return misc;
	}

	// Stages
	public void addStage(Integer stageID, StageData data) {
		stages.put(stageID, data);
	}

	public boolean hasStage(Integer stageID) {
		return stages.containsKey(stageID);
	}

	public StageData getStage(Integer stageID) {
		return stages.get(stageID);
	}

	public Hashtable<Integer, StageData> getStages() {
		return stages;
	}

	public int getNumberOfStages() {
		// ArrayList<Integer> stageNumbers = new ArrayList<Integer>();
		// stageNumbers.addAll(stages.keySet());
		// Collections.sort(stageNumbers);
		return stages.size();
	}

	public LocalDateTime getStartTime() {
		return startTimestamp;
	}

	public LocalDateTime getEndTime() {
		return endTimestamp;
	}

	public double getDip() {
		return dip;
	}

	public double getDepth() {
		return depth;
	}

	public double getAzimuth() {
		return azimuth;
	}

	public double getSampleRate() {
		return sampleRate;
	}

	public String getInstrumentType() {
		return instrumentType;
	}

	public String getChannelFlags() {
		return channelFlags;
	}

	public double getElevation() { return elevation; }
}
