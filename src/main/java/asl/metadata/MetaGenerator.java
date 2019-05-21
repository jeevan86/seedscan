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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.StationMeta;

/**
 * MetaGenerator - Holds metadata for all networks x stations x channels x
 * epochs Currently reads metadata in from network dataless seed files
 *
 * @author Mike Hagerty hagertmb@bc.edu
 *
 */
public class MetaGenerator {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.MetaGenerator.class);

	/**
	 * Each datalessDir/XX.dataless file is read into a separate SeedVolume
	 * keyed by network (e.g., XX)
	 */
	protected Hashtable<StationKey, SeedVolume> volumes = null;

	/**
	 * Private class meant to enable mock test class to inherit from this without running other function
	 */
	protected MetaGenerator() {
        volumes = new Hashtable<>();
	}

	/**
	 * Look in datalessDir for all files of form XX.dataless
	 * where XX = network {II, IU, NE, etc.}
	 *
	 * @param datalessDir	path to dataless seed files, read from config.xml
	 * @param networkSubset the network subset to parse
	 */
	public MetaGenerator(String datalessDir, List<String> networkSubset) {
		volumes = new Hashtable<>();

		File dir = new File(datalessDir);
		if (!dir.exists()) {
			logger.error("Path '" + dir + "' does not exist.");
			System.exit(0);
		} else if (!dir.isDirectory()) {
			logger.error("Path '" + dir + "' is not a directory.");
			System.exit(0);
		}

		// this will try to create a list of all paths that define dataless based on network data

		for (String networkName : networkSubset) {
			List<Path> allMetadataFiles = new ArrayList<>();
			try (DirectoryStream<Path> stream = Files
					.newDirectoryStream(dir.toPath(), networkName + ".*.dataless")) {
				for (Path entry : stream) {
					allMetadataFiles.add(entry);
				}
			} catch (DirectoryIteratorException e) {
				logger.error("Error in iterating through directory for network " + networkName, e);
				System.exit(0);
			} catch (IOException e) {
				logger.error("Triggered IOException while enumerating files for network " + networkName, e);
				System.exit(0);
			}

			List<String> files = new ArrayList<>(allMetadataFiles.size());
			for (Path metadataPath : allMetadataFiles) {
				files.add(metadataPath.toString());
			}

			if (files.size() == 0) {
				logger.error("== No dataless files exist!");
				System.exit(0);
			}
			ArrayList<String> strings = new ArrayList<>();

			// we expect dataless structures to be associated over entire network
			// so we'll read in the strings from
			for (String datalessFile : files) {

				String filename = datalessFile.substring(
						datalessFile.lastIndexOf("/")+1, datalessFile.indexOf("."));

				String stationName = filename.split(".")[1];
				System.out.println("READING IN: " + networkName + "," + stationName);

				System.out.format(
						"== MetaGenerator: rdseed -f [datalessFile=%s]\n",
						datalessFile);
				ProcessBuilder pb = new ProcessBuilder("rdseed", "-s", "-f",
						datalessFile);

				try {
					Process process = pb.start();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(process.getInputStream()));
					String line = null;
					while ((line = reader.readLine()) != null) {
						strings.add(line);
					}
					process.waitFor();
				}
				// Need to catch both IOException and InterruptedException
				catch (IOException e) {
					// System.out.println("Error: IOException Description: " +
					// e.getMessage());
					logger.error("IOException:", e);
				} catch (InterruptedException e) {
					// System.out.println("Error: InterruptedException Description: "
					// + e.getMessage());
					logger.error("InterruptedException:", e);
				}


			SeedVolume volume = null;
			try {
				volume = buildVolumesFromStringData(strings, networkName, stationName);
			} catch (Exception e) {
				logger.error("== processing dataless volume for network=[{}]", networkName);
			}

			if (volume == null) {
				logger.error("== processing dataless volume==null! for network=[{}]", networkName);
				System.exit(0);
			} else {
				addVolume(volume);
			}

			} // end loop over the per-station dataless files for a given network

		} // end loop over network name codes
	}

	SeedVolume buildVolumesFromStringData(List<String> strings, String networkName, String stationName) throws DatalessParseException {
		Dataless dataless = new Dataless(strings, networkName, stationName);
		dataless.processVolume();
		return dataless.getVolume();
	}

	protected void addVolume(SeedVolume volume) {
		StationKey stationKey = volume.getStationKey();
		if (volumes.containsKey(stationKey)) {
			logger.error("== Attempting to load volume networkKey=[{}] --> Already loaded!",
					stationKey);
		} else {
			volumes.put(stationKey, volume);
		}
	}

	/**
	 * Return a list of all stations contained in all volumes
	 */
	public List<Station> getStationList() {
		if (volumes == null) {
			return null;
		}
		ArrayList<Station> allStations = new ArrayList<>();
		for (StationKey key : volumes.keySet()) {
			SeedVolume volume = volumes.get(key);
			List<Station> stations = volume.getStationList();
			allStations.addAll(stations);
		}
		return allStations;
	}

	/**
	 * Return a list of all stations matching parameters.
	 *
	 * @param networks network restrictions. Can be null
	 * @param stations station restrictions. Can be null
	 * @return the station list
	 */
	public List<Station> getStationList(String[] networks, String[] stations) {

		logger.info("Generating list of stations for: {}  | {}", networks, stations);
		if (volumes == null) {
			return null;
		}

		List<Station> allStations = new ArrayList<>();

		if (networks != null && stations != null) {
			for (String network : networks) {
				for (String station : stations) {
					if (volumes.containsKey(new StationKey(network, station))) {
						allStations.add(new Station(network, station));
					}
				}
			}
		} else if (networks != null) {
			// networks is not null so stations must be null based on previous conditional
			// we'll check this by iterating through keys and finding if they match the networks in list
			Set<String> networkSet = new HashSet<>(Arrays.asList(networks)); // speeds up lookup
			for (StationKey stationKey : volumes.keySet()) {
				if (networkSet.contains(stationKey.getNetwork())) {
					allStations.add(new Station(stationKey.getNetwork(), stationKey.getName()));
				}
			}
		} else if (stations != null) {
			// networks must be null based on previous conditional
			Set<String> stationSet = new HashSet<>(Arrays.asList(stations));
			for (StationKey stationKey : volumes.keySet()) {
				if (stationSet.contains(stationKey.getName())) {
					allStations.add(new Station(stationKey.getNetwork(), stationKey.getName()));
				}
			}
		} else {
			allStations = getStationList();
		}

		return allStations;
	}

	/**
	 * loadDataless() reads in the entire dataless seed file (all stations)
	 * getStationData returns the metadata for a single station for all epochs
	 * It is called by
	 * {@link asl.metadata.MetaGenerator#getStationMeta(Station, LocalDateTime)}
	 * below.
	 *
	 * @param station
	 *            the station
	 * @return the station data - this can be null if seed files are
	 *         malformatted
	 */
	private StationData getStationData(Station station) {
		SeedVolume volume = volumes.get(new StationKey(station));
		if (volume == null) {
			logger.error(
					"== getStationData() - Volume==null for Station=[{}]  Check the volume label in Blockette 10 Field 9. Must be formatted like IU* to work.\n",
					station);
			return null; //No volume so nothing can be returned.
		}
		StationData stationData = volume.getStation(new StationKey(station));
		if (stationData == null) {
			logger.error("stationData is null ==> This COULD be caused by incorrect network code INSIDE seedfile ...");
		}
		return stationData;
	}

	/**
	 * getStationMeta Calls getStationData to get the metadata for all epochs
	 * for this station, Then scans through the epochs to find and return the
	 * requested epoch metadata.
	 *
	 * @param station The station for which metadata is requested
	 * @param timestamp The (epoch) timestamp for which metadata is requested
	 *
	 *            ChannelData - Contains all Blockettes for a particular
	 *            channel, for ALL epochs EpochData - Constains all Blockettes
	 *            for a particular channel, for the REQUESTED epoch only.
	 *            ChannelMeta - Our (minimal) internal format of the channel
	 *            response. Contains the first 3 (0, 1, 2) response stages for
	 *            the REQUESTED epoch only. ChannelMeta.setDayBreak() = true if
	 *            we detect a change in metadata on the requested timestamp day.
	 */

	public StationMeta getStationMeta(Station station, LocalDateTime timestamp){

		StationData stationData = getStationData(station);
		//This can happen if the file DATALESS.IW_LKWY.seed doesn't match
		if (stationData == null) {
			logger.error("== getStationMeta request:\t\t[{}]\t[{}]\tNOT FOUND!",
					station, timestamp.format(DateTimeFormatter.ISO_ORDINAL_DATE));
			return null; // the name INSIDE the dataless (= US_LKWY) ... so the
							// keys don't match
		}
		// Scan stationData for the correct station blockette (050) for this
		// timestamp - return null if it isn't found
		Blockette blockette = stationData.getBlockette(timestamp);

		if (blockette == null) {
			logger.error("== getStationMeta request:\t\t[{}]\t[{}]\tNOT FOUND!",
					station, timestamp.format(DateTimeFormatter.ISO_ORDINAL_DATE));
			return null;
		} else { // Uncomment to print out a Blockette050 each time
					// getStationMeta is called
					// blockette.print();
			logger.info("== MetaGenerator getStationMeta request:\t\t[{}]\t[{}]",
					station, EpochData.epochToDateString(timestamp));
		}

		StationMeta stationMeta = null;
		try {
			stationMeta = new StationMeta(blockette, timestamp);
		} catch (WrongBlocketteException e) {
			logger.error("Could not create new StationMeta(blockette) !!");
			System.exit(0); //TODO: Fix this System.exit(0) This shouldn't happen.
		}

		// Get this StationData's ChannelKeys and sort:
		Hashtable<ChannelKey, ChannelData> channels = stationData.getChannels();
		TreeSet<ChannelKey> keys = new TreeSet<>(channels.keySet());
		for (ChannelKey key : keys) {
            // System.out.println("==Channel:"+key );
            ChannelData channel = channels.get(key);
            // ChannelMeta channelMeta = new ChannelMeta(key,timestamp);
            ChannelMeta channelMeta = new ChannelMeta(key, timestamp,
                    station);

            // See if this channel contains the requested epoch time and if
            // so return the key
            // (=Epoch Start timestamp)
            // channel.printEpochs();
            LocalDateTime epochTimestamp = channel.containsEpoch(timestamp);
            if (epochTimestamp != null) {
                EpochData epochData = channel.getEpoch(epochTimestamp);

                // If the epoch is closed, check that the end time is at
                // least 24 hours later than the requested time
                if (epochData.getEndTime() != null) {
                    if (epochData.getEndTime().compareTo(timestamp.plusDays(1)) < 0){
                        // set channelMeta.dayBreak = true
                        channelMeta.setDayBreak();
                    }
                }
                channelMeta.processEpochData(epochData);
                stationMeta.addChannel(key, channelMeta);
            }
        }

		return stationMeta;
	}
}
