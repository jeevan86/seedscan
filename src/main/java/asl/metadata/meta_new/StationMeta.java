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
package asl.metadata.meta_new;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Blockette;
import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.ChannelKey;
import asl.metadata.EpochData;
import asl.metadata.WrongBlocketteException;

/**
 * Our internal representation of a station's metadata Holds all channel
 * metadata for a single station for a specified day.
 *
 * @author Mike Hagerty hagertmb@bc.edu
 */
public class StationMeta implements java.io.Serializable {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.meta_new.StationMeta.class);

	/** The network code. */
	private String network = null;
	
	/** The name code. */
	private String name = null;
	
	/** The latitude. */
	private double latitude;
	
	/** The longitude. */
	private double longitude;
	
	/** The elevation. */
	private double elevation;
	
	/** The channels. */
	private Hashtable<ChannelKey, ChannelMeta> channels;
	
	/** The meta timestamp. */
	private Calendar metaTimestamp = null;
	
	/** The meta date. */
	private String metaDate = null;

	/** The blockette50. */
	private Blockette blockette50 = null;

	/**
	 * Instantiates a new station meta.
	 *
	 * @param blockette the blockette
	 * @param timestamp the timestamp
	 * @throws WrongBlocketteException if the passed blockette 
	 */
	public StationMeta(Blockette blockette, Calendar timestamp)
			throws WrongBlocketteException {
		if (blockette.getNumber() != 50) { // We're expecting a station
											// blockette (B050)
			throw new WrongBlocketteException();
		}
		this.network = blockette.getFieldValue(16, 0);
		this.name = blockette.getFieldValue(3, 0);
		this.latitude = Double.parseDouble(blockette.getFieldValue(4, 0));
		this.longitude = Double.parseDouble(blockette.getFieldValue(5, 0));
		this.elevation = Double.parseDouble(blockette.getFieldValue(6, 0));
		channels = new Hashtable<ChannelKey, ChannelMeta>();
		this.metaTimestamp = (Calendar) timestamp.clone();
		this.metaDate = (EpochData.epochToDateString(this.metaTimestamp));
		this.blockette50 = blockette;
	}

	/**
	 * Adds the channel to channels HashTable.
	 *
	 * @param chanKey the channel key
	 * @param channel the channel
	 */
	public void addChannel(ChannelKey chanKey, ChannelMeta channel) {
		channels.put(chanKey, channel);
	}

	/**
	 * Gets the station code.
	 *
	 * @return the station
	 */
	public String getStation() {
		return name;
	}

	/**
	 * Gets the network.
	 *
	 * @return the network
	 */
	public String getNetwork() {
		return network;
	}

	/**
	 * Gets the latitude.
	 *
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Gets the longitude.
	 *
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}
	
	/**
	 * Gets the elevation.
	 *
	 * @return the elevation
	 */
	public double getElevation() {
		return elevation;
	}

	/**
	 * Gets the number of channels.
	 *
	 * @return the number of channels
	 */
	public int getNumberOfChannels() {
		return channels.size();
	}

	/**
	 * Gets the timestamp.
	 *
	 * @return the timestamp
	 */
	public Calendar getTimestamp() {
		return (Calendar) metaTimestamp.clone();
	}

	/**
	 * Gets the date.
	 *
	 * @return the date
	 */
	public String getDate() {
		return metaDate;
	}

	/**
	 * Look for particular channelMeta (e.g., "00" "VHZ") in channels Hashtable.
	 * Return it if found, else return null
	 *
	 * @param chanKey the channel key
	 * @return the channel metadata
	 */
	public ChannelMeta getChannelMetadata(ChannelKey chanKey) {
		if (channels.containsKey(chanKey)) {
			return channels.get(chanKey);
		} else {
			return null;
		}
	}

	/**
	 * Gets the channel metadata
	 * @param channel the channel
	 * @return the channel metadata
	 */
	public ChannelMeta getChannelMetadata(Channel channel) {
		return getChannelMetadata(new ChannelKey(channel.getLocation(),
				channel.getChannel()));
	}

	/**
	 * Return the entire channels Hashtable.
	 *
	 * @return the channel hash table
	 */
	public Hashtable<ChannelKey, ChannelMeta> getChannelHashTable() {
		return channels;
	}

	/**
	 * Return ordered primary + secondary seismic channels that have matching
	 * band
	 * 
	 * e.g., if band = "LH" then return channels[0] = "00-LHZ" channels[1] =
	 * "00-LH1" -or- "00-LHN" channels[2] = "00-LH2" -or- "00-LHE" channels[3] =
	 * "10-LHZ" channels[4] = "10-LH1" -or- "10-LHN" channels[5] = "10-LH2" -or-
	 * "10-LHE"
	 *
	 * @param band the band
	 * @return the ZNE channel array
	 */
	public List<Channel> getZNEChannelArray(String band) {
		if (!Channel.validBandCode(band.substring(0, 1))
				|| !Channel.validInstrumentCode(band.substring(1, 2))) {
			return null;
		}
		TreeSet<ChannelKey> keys = new TreeSet<ChannelKey>();
		keys.addAll(channels.keySet());

		ArrayList<Channel> channelArrayList = new ArrayList<Channel>();

		String[] location = { "00", "10" };
		String[] chan = { band + "Z", band + "1", band + "2", band + "Z",
				band + "N", band + "E" };

		// Add found Primary + Secondary seismic channels to channel array in
		// order:
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 3; j++) {
				if (keys.contains(new ChannelKey(location[i], chan[j]))) {
					channelArrayList.add(new Channel(location[i], chan[j]));
				} else if (keys.contains(new ChannelKey(location[i],
						chan[j + 3]))) {
					channelArrayList.add(new Channel(location[i], chan[j + 3]));
				} else {
					logger.error(
							"Error: CAN'T find Channel=[{}-{}] OR Channel=[{}-{}] for date=[{}] in metadata",
							location[i], chan[j], location[i], chan[j + 3],
							this.getDate());
				}
			}
		}

		return channelArrayList;
	}

	/**
	 * Return ArrayList of all channels contained in this stationMeta that match
	 * the 2-char band e.g., if band = "LH" then return "00-LHZ", "00-LH1",
	 * "00-LH2", "00-LHN", "00-LHE", "10-LHZ", "10-LH1", "10-LH2", .. -or-
	 * "---LHZ", "---LH1", "---LH2", "---LHN", "---LHE",
	 *
	 * @param band the frequency band
	 * @return the channel array
	 */
	public List<Channel> getChannelArray(String band) {
		if (!Channel.validBandCode(band.substring(0, 1))
				|| !Channel.validInstrumentCode(band.substring(1, 2))) {
			return null;
		}
		TreeSet<ChannelKey> keys = new TreeSet<ChannelKey>();
		keys.addAll(channels.keySet());

		ArrayList<Channel> channelArrayList = new ArrayList<Channel>();

		for (ChannelKey channelKey : keys) {
			Channel channel = channelKey.toChannel();

			// if (channel.getChannel().contains(band) ){
			// We need to restrict ourselves to 3-char channels so we don't also
			// return
			// the derived channels (e.g., LHND, LHED)
			if (channel.getChannel().contains(band)
					&& channel.getChannel().length() == 3) {
				channelArrayList.add(channel);
			}
		}
		return channelArrayList;
	}
	
	/**
	 * Gets the channel array.
	 * Same as above but limit return channels to those containing the specified
	 * location as well as the specified band
	 * @see asl.metadata.meta_new.StationMeta#getChannelArray(String)
	 *
	 * @param location the location code
	 * @param band the frequency band
	 * @return the channel array
	 */
	public List<Channel> getChannelArray(String location, String band) {
		if (!Channel.validLocationCode(location)) {
			return null;
		}
		if (!Channel.validBandCode(band.substring(0, 1))
				|| !Channel.validInstrumentCode(band.substring(1, 2))) {
			return null;
		}
		TreeSet<ChannelKey> keys = new TreeSet<ChannelKey>();
		keys.addAll(channels.keySet());

		ArrayList<Channel> channelArrayList = new ArrayList<Channel>();

		for (ChannelKey channelKey : keys) {
			Channel channel = channelKey.toChannel();

			if (channel.getChannel().contains(band)
					&& channel.getLocation().equals(location)) {
				channelArrayList.add(channel);
			}
		}
		return channelArrayList;
	}
	
	/**
	 * Handle horizontal naming conventions (e.g., LH1,2 versus LHN,E)
	 * 
	 * "00" "LH" "1"
	 *
	 * @param location the location code EG 00
	 * @param band the frequency band EG LH
	 * @param comp the component EG N or 1
	 * @return the channel
	 */
	public Channel getChannel(String location, String band, String comp) {

		String name = null;

		if (comp.equals("1")) {
			name = band + "1";
			if (hasChannel(location, name)) { // Do we have LH1 ?
				return new Channel(location, name);
			} else {
				name = band + "N";
				if (hasChannel(location, name)) { // Do we have LHN ?
					return new Channel(location, name);
				}
			}
			return null;

		} else if (comp.equals("2")) {
			name = band + "2";
			if (hasChannel(location, name)) { // Do we have LH2 ?
				return new Channel(location, name);
			} else {
				name = band + "E";
				if (hasChannel(location, name)) { // Do we have LHE ?
					return new Channel(location, name);
				}
			}
			return null;

		} else {
			logger.warn(
					"== getChannel: Channel=[{}-{}{}] date=[{}] NOT FOUND\n",
					location, band, comp, this.getDate());
			return null;
		}
	}

	/**
	 * Return a (sorted) ArrayList of channels that are continuous
	 * (channelFlag="C?").
	 *
	 * @return the continuous channels
	 */
	public List<Channel> getContinuousChannels() {
		TreeSet<ChannelKey> keys = new TreeSet<ChannelKey>();
		keys.addAll(channels.keySet());

		ArrayList<Channel> channelArrayList = new ArrayList<Channel>();

		for (ChannelKey channelKey : keys) {
			Channel channel = channelKey.toChannel();
			String channelFlags = getChannelMetadata(channelKey).getChannelFlags();

			if (channelFlags.substring(0, 1).equals("C")
					|| channelFlags.equals("G") || channelFlags.equals("H")){
				channelArrayList.add(channel);
			}
		}
		return channelArrayList;
	}

	/**
	 * Checks for channel.
	 *
	 * @param channelKey the channel key
	 * @return true, if successful
	 */
	public boolean hasChannel(ChannelKey channelKey) {
		return channels.containsKey(channelKey);
	}

	/**
	 * Checks for channel.
	 *
	 * @param channel the channel
	 * @return true, if successful
	 */
	public boolean hasChannel(Channel channel) {
		return hasChannel(new ChannelKey(channel.getLocation(),
				channel.getChannel()));
	}

	/**
	 * Checks for channel.
	 *
	 * @param location the location
	 * @param name the name
	 * @return true, if the channel has metadata
	 */
	public boolean hasChannel(String location, String name) {
		return hasChannel(new ChannelKey(location, name));
	}

	/**
	 * Checks for channels.
	 *
	 * @param channelArray the channel array
	 * @return true, if all channels have metadata.
	 */
	public boolean hasChannels(ChannelArray channelArray) {
		for (Channel channel : channelArray.getChannels()) {
			if (!hasChannel(channel)) {
				return false;
			}
		}
		return true; // If we made it to here then it must've found all channels
	}

	/**
	 * Checks for channels.
	 *
	 * @param location the location
	 * @param chan1 the chan1
	 * @param chan2 the chan2
	 * @param chan3 the chan3
	 * @return true, if successful
	 */
	public boolean hasChannels(String location, String chan1, String chan2,
			String chan3) {
		if (hasChannel(location, chan1) && hasChannel(location, chan2)
				&& hasChannel(location, chan3)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks for channels.
	 *
	 * @param location the location
	 * @param band the band
	 * @return true, if successful
	 */
	public boolean hasChannels(String location, String band) {
		if (!Channel.validLocationCode(location)) {
			// return null;
			return false;
		}
		if (!Channel.validBandCode(band.substring(0, 1))
				|| !Channel.validInstrumentCode(band.substring(1, 2))) {
			// return null;
			return false;
		}
		// First try kcmp = "Z", "1", "2"
		ChannelArray chanArray = new ChannelArray(location, band + "Z", band
				+ "1", band + "2");
		if (hasChannels(chanArray)) {
			return true;
		}
		// Then try kcmp = "Z", "N", "E"
		chanArray = new ChannelArray(location, band + "Z", band + "N", band
				+ "E");
		if (hasChannels(chanArray)) {
			return true;
		}
		// If we're here then we didn't find either combo --> return false
		return false;
	}

	/**
	 * Not sure yet if we need this to drive addRotatedChannel below e.g., if we
	 * need to rotate 2 channels at one time
	 *
	 * @param location the location
	 * @param channelPrefix the channel prefix
	 */
	public void addRotatedChannelMeta(String location, String channelPrefix) {
		String northChannelName = channelPrefix + "ND";
		String eastChannelName = channelPrefix + "ED";
		addRotatedChannel(location, northChannelName);
		addRotatedChannel(location, eastChannelName);
	}

	/**
	 * If handed a derivedChannelName like "LHND" (or "LHED"), try to create it
	 * from LH1 (or LH2) channel metadata. If these aren't found, look for LHN
	 * (or LHE) instead.
	 *
	 * @param location the location
	 * @param derivedChannelName the derived channel name
	 */
	public void addRotatedChannel(String location, String derivedChannelName) {

		String origChannelName = null;
		double azimuth;

		boolean found = false;

		if (derivedChannelName.contains("ND")) { // Derived Orig
			origChannelName = derivedChannelName.replace("ND", "1"); // "LHND"
																		// -->
																		// "LH1"
			if (hasChannel(new Channel(location, origChannelName))) {
				found = true;
			} else { // try ...
				origChannelName = derivedChannelName.replace("ND", "N"); // "LHND"
																			// -->
																			// "LHN"
				if (hasChannel(new Channel(location, origChannelName))) {
					found = true;
				}
			}
			azimuth = 0.; // NORTH
		} else if (derivedChannelName.contains("ED")) { // Derived Orig
			origChannelName = derivedChannelName.replace("ED", "2"); // "LHED"
																		// -->
																		// "LH2"
			if (hasChannel(new Channel(location, origChannelName))) {
				found = true;
			} else { // try ...
				origChannelName = derivedChannelName.replace("ED", "E"); // "LHED"
																			// -->
																			// "LHE"
				if (hasChannel(new Channel(location, origChannelName))) {
					found = true;
				}
			}
			azimuth = 90.; // EAST
		} else {
			logger.error(
					"== addRotatedChannel: -- Don't know how to make channel=[{}] date=[{}]\n",
					derivedChannelName, this.getDate());
			return;
		}

		if (!found) {
			logger.error(
					"== addRotatedChannel: -- StationMeta doesn't contain horizontal channels needed to make channel=[{}] date=[{}]\n",
					derivedChannelName, this.getDate());
			return;
		}

		Channel origChannel = new Channel(location, origChannelName);
		Channel derivedChannel = new Channel(location, derivedChannelName);

		try {
			/*
			 * ChannelMeta.copy(channel) Deep copy the orig chanMeta to the
			 * derived chanMeta and set the derived chanMeta azimuth
			 */
			ChannelMeta derivedChannelMeta = (getChannelMetadata(origChannel))
					.copy(derivedChannel);
			derivedChannelMeta.setAzimuth(azimuth);
			this.addChannel(new ChannelKey(derivedChannel), derivedChannelMeta);
		} catch (RuntimeException e) {
			logger.error("RuntimeException:", e);
		}
	}

	/**
	 * Prints the Station metadata and channels to System.out.
	 */
	public void print() {
		System.out.print(this);
		ArrayList<ChannelKey> chanKeys = new ArrayList<ChannelKey>();
		chanKeys.addAll(channels.keySet());
		Collections.sort(chanKeys);
		for (ChannelKey chanKey : chanKeys) {
			channels.get(chanKey).print();
		}
	}

	/**
	 * Prints blockette 50 which is the station identifier blockette.
	 */
	public void printStationInfo() {

		blockette50.print();
	}

	/** 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return network + "_" + name;
	}

}
