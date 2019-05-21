package asl.metadata;

import java.time.LocalDateTime;

/* 
 * Follwing is a depiction of the structure generated as a result of 
 * parsing the output of `evalresp -s`
 * 
 *
 * SeedVolume
 *  |
 *   - volumeInfo (Blockette)
 *  |
 *   - stationLocators (ArrayList<Blockette>)
 *  |
 *   - stations (Hashtable<String, StationData>)
 *      |
 *       - 'NN_SSSS' (String)
 *          :
 *         data (StationData)
 *          |
 *           - comments (Hashtable<Calendar, Blockette>)
 *              |
 *               - timestamp (Calendar)
 *                  :
 *                 comment (Blockette)
 *          | 
 *           - epochs (Hashtable<Calendar, Blockette>)
 *              |
 *               - timestamp (Calendar)
 *                  :
 *                 epoch (Blockette)
 *          | 
 *           - channels (Hashtable<String, ChannelData>)
 *              |
 *               - 'LL-CCC' (String)
 *                  :
 *                 data (ChannelData)
 *                      |
 *                       - comments (Hashtable<Calendar, Blockette>)
 *                          |
 *                           - timestamp (Calendar)
 *                              :
 *                             comment (Blockette)
 *                      |
 *                       - epochs (Hashtable<Calendar, EpochData>)
 *                          |
 *                           - timestamp (Calendar)
 *                              :
 *                             epoch (EpochData)
 *                              |
 *                               - format (Blockette)
 *                              |
 *                               - info (Blockette)
 *                              |
 *                               - misc (ArrayList<Blockette>)
 *                              |
 *                               - stages (Hashtable<Integer, StageData>)
 *                                  |
 *                                   - stageIndex (Integer)
 *                                      :
 *                                     data (StageData)
 *
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Dataless {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.Dataless.class);

	private SeedVolume volume;
	private Collection<String> rawDataless;
	private ArrayList<Blockette> blockettes;
	private String networkName;
	private String stationName;
	private double percent;
	private double lastPercent;
	private double count;
	private double total;
	
	Dataless(Collection<String> rawDataless, String networkName, String stationName) {
		this.rawDataless = rawDataless;
		this.networkName = networkName;
		this.stationName = stationName;
	}

	void processVolume() throws DatalessParseException {
		boolean failed = true;
		try {
			parse();
			assemble();
			failed = false;
		} catch (BlocketteFieldIdentifierFormatException exception) {
			logger.warn("Malformed blocketted field identifier.", exception);
		} catch (BlocketteOutOfOrderException exception) {
			logger.warn("Out of order blockette.", exception);
		} catch (DuplicateBlocketteException exception) {
			logger.warn("Unexpected duplicate blockette.", exception);
		} catch (MissingBlocketteDataException exception) {
			logger.warn("Blockette is missing requird data.", exception);
		} catch (TimestampFormatException exception) {
			logger.warn("Invalid timestamp format.", exception);
		} catch (WrongBlocketteException exception) {
			logger.warn("Wrong blockettte.", exception);
		}

		if (failed) {
			throw new DatalessParseException();
		}
	}

	private void parse() throws BlocketteFieldIdentifierFormatException {
		if (rawDataless == null) {
			return;
		}

		blockettes = new ArrayList<>();
		Hashtable<Integer, Blockette> blocketteMap = new Hashtable<>();

		total = rawDataless.size();
		count = 0.0;
		percent = 0.0;
		lastPercent = 0.0;
		for (String line : rawDataless) {
			count++;
			percent = Math.floor(count / total * 100.0);
			if (percent > lastPercent) {
				lastPercent = percent;
			}

			line = line.trim();
			if (line.equals("")) {
				continue;
			}
			if (line.startsWith("#")) {
				continue;
			}
			if (!line.startsWith("B")) {
				continue;
			}

			String[] lineParts = line.split("\\s", 2);
			String word = lineParts[0]; // e.g., word = "B010F13-18"
			String lineData = lineParts[1]; // e.g., lineData =
											// "  SEED Format version: ..."

			int blocketteNumber = Integer.parseInt(word.substring(1, 4));
			String fieldIdentifier = word.substring(5, word.length());

			Blockette blockette;
			// If the blockette does not exists, or the attempt to add field
			// data
			// reported that this should be part of a new blockette, we create
			// a new blockette, and add this data to it instead.

			if ((!blocketteMap.containsKey(blocketteNumber))
                    || (!blocketteMap.get(blocketteNumber).addFieldData(
                            fieldIdentifier, lineData))) {
                blockette = new Blockette(blocketteNumber);
                blocketteMap.put(blocketteNumber, blockette);
                blockettes.add(blockette);
                blockette.addFieldData(fieldIdentifier, lineData);
                // System.out.format("  Dataless.parse(): new blockette number=%d fieldIdentifier=%s\n",
                // blocketteNumber, fieldIdentifier );
            }
		}
	}

	private void assemble() throws
			BlocketteOutOfOrderException, 
			DuplicateBlocketteException, MissingBlocketteDataException,
			TimestampFormatException, WrongBlocketteException {
		if (blockettes == null) {
			return;
		}

		total = blockettes.size();
		count = 0.0;
		percent = 0.0;
		lastPercent = 0.0;
		StationData station = null;
		ChannelData channel = null;
		EpochData epoch = null;

		for (Blockette blockette : blockettes) {
			count++;
			percent = Math.floor(count / total * 100.0);
			if (percent > lastPercent) {
				lastPercent = percent;
			}

			int blocketteNumber = blockette.getNumber();

			switch (blocketteNumber) {
			case 10:
				// MTH: As it stands, a Dataless object is only expecting to
				// create
				// a *single* Volume corresponding to a single Network dataless
				// This could be modified so that we close out a previously
				// created
				// SeedVolume and create a new one when we encounter a Blockette
				// B010.
				// Then a List<SeedVolume> could be handed back via getVolumes()
				// ...
				if (volume != null) {
					throw new DuplicateBlocketteException();
				}
				volume = new SeedVolume(blockette, networkName, stationName);
				break;
			case 11:
				if (volume == null) {
					throw new BlocketteOutOfOrderException();
				}
				volume.addStationLocator(blockette);
				break;
			case 50:
				if (volume == null) {
					throw new BlocketteOutOfOrderException();
				}
				StationKey stationKey = new StationKey(blockette);

				if (!volume.hasStation(stationKey)) {
                    station = new StationData(stationKey.getNetwork(),
                            stationKey.getName());
                    volume.addStation(stationKey, station);
                } else {
                    station = volume.getStation(stationKey);
                }

				station.addEpoch(blockette);
				break;
			case 51:
				if (station == null) {
					throw new BlocketteOutOfOrderException();
				}

				station.addComment(blockette);

				break;
			case 52:
				if (station == null) {
					throw new BlocketteOutOfOrderException();
				}
				ChannelKey channelKey = null;

				channelKey = new ChannelKey(blockette);

				if (!station.hasChannel(channelKey)) {
					channel = new ChannelData(channelKey);
					station.addChannel(channelKey, channel);
				} else {
					channel = station.getChannel(channelKey);
				}
				LocalDateTime epochKey = channel.addEpoch(blockette);
				epoch = channel.getEpoch(epochKey);

				break;
			case 30:
				if (epoch == null) {
					throw new BlocketteOutOfOrderException();
				}
				epoch.setFormat(blockette);
				break;
			case 53:
			case 54:
			case 55:
			case 56:
			case 57:
			case 58:
			case 61:
			case 62:
				if (epoch == null) {
					throw new BlocketteOutOfOrderException();
				}
				/*
				 * MTH: I see the following output from rdseed -s: B053F04 Stage
				 * Sequence Number: 1 B058F03 Stage Sequence Number: 1 B054F04
				 * Stage Sequence Number: 2 B057F03 Stage Sequence Number: 2
				 * B058F03 Stage Sequence Number: 2 B054F04 Stage Sequence
				 * Number: 3
				 */
				int stageKey;
				if (blocketteNumber == 57 || blocketteNumber == 58) {
					stageKey = Integer.parseInt(blockette.getFieldValue(3, 0));
				} else if (blocketteNumber == 61) {
					stageKey = Integer.parseInt(blockette.getFieldValue(3, 0));
				} else {
					stageKey = Integer.parseInt(blockette.getFieldValue(4, 0));
				}

				if (!epoch.hasStage(stageKey)) {
					epoch.addStage(stageKey, new StageData(stageKey));
				}
				StageData stage = epoch.getStage(stageKey);
				stage.addBlockette(blockette);
				break;
			default:
				if (epoch == null) {
					throw new BlocketteOutOfOrderException();
				}
				epoch.addMiscBlockette(blockette);
				break;
			}
		}
	}

	public SeedVolume getVolume() {
		return volume;
	}
}
