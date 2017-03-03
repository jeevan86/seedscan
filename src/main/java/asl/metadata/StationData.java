package asl.metadata;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StationData {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.StationData.class);

	private static final int STATION_EPOCH_BLOCKETTE_NUMBER = 50;
	private static final int STATION_COMMENT_BLOCKETTE_NUMBER = 51;

	private Hashtable<LocalDateTime, Blockette> comments;
	private Hashtable<LocalDateTime, Blockette> epochs;
	private Hashtable<ChannelKey, ChannelData> channels;
	private String network = null;
	private String name = null;

	// Constructor(s)
	public StationData(String network, String name) {
		comments = new Hashtable<>();
		epochs = new Hashtable<>();
		channels = new Hashtable<>();
		this.name = name;
		this.network = network;
	}

	// identifiers
	public String getNetwork() {
		return network;
	}

	public String getName() {
		return name;
	}

	// comments
	public LocalDateTime addComment(Blockette blockette)
			throws TimestampFormatException, WrongBlocketteException,
			MissingBlocketteDataException {
		if (blockette.getNumber() != STATION_COMMENT_BLOCKETTE_NUMBER) {
			throw new WrongBlocketteException();
		}
		// Epoch epochNew = new Epoch(blockette);
		String timestampString = blockette.getFieldValue(3, 0);
		if (timestampString == null) {
			throw new MissingBlocketteDataException();
		}
		LocalDateTime timestamp = BlocketteTimestamp
                .parseTimestamp(timestampString);
		comments.put(timestamp, blockette);
		return timestamp;
	}

	// epochs
	public LocalDateTime addEpoch(Blockette blockette)
			throws TimestampFormatException, WrongBlocketteException,
			MissingBlocketteDataException {
		if (blockette.getNumber() != STATION_EPOCH_BLOCKETTE_NUMBER) {
			throw new WrongBlocketteException();
		}

		String timestampString = blockette.getFieldValue(13, 0);
		if (timestampString == null) {
			throw new MissingBlocketteDataException();
		}
		
		LocalDateTime timestamp = BlocketteTimestamp
				.parseTimestamp(timestampString);
		epochs.put(timestamp, blockette);
		return timestamp;
	}

	public boolean hasEpoch(LocalDateTime timestamp) {
		return epochs.containsKey(timestamp);
	}

	public Blockette getEpoch(LocalDateTime timestamp) {
		return epochs.get(timestamp);
	}

	/**
	 * Epoch index ----------- ONLY THIS EPOCH! 0 newest startTimestamp - newest
	 * endTimestamp (may be "null") 1 ... - ... 2 ... - ... . ... - ... n-1
	 * oldest startTimestamp - oldest endTimestamp
	 **/
	// public boolean containsEpoch(Calendar epochTime)

	// Return the correct Blockette 050 for the requested epochTime
	// Return null if epochTime not contained
	public Blockette getBlockette(LocalDateTime epochTime) {
		ArrayList<LocalDateTime> epochtimes = new ArrayList<>();
		epochtimes.addAll(epochs.keySet());
		Collections.sort(epochtimes);
		Collections.reverse(epochtimes);
		int nEpochs = epochtimes.size();

		LocalDateTime startTimeStamp = null;
		LocalDateTime endTimeStamp = null;

		// Loop through Blockettes (B050) and pick out epoch end dates
		for (int i = 0; i < nEpochs; i++) {
			endTimeStamp = null;
			startTimeStamp = epochtimes.get(i);
			Blockette blockette = epochs.get(startTimeStamp);
			String timestampString = blockette.getFieldValue(14, 0);
			if (!timestampString.equals("(null)")) {
				try {
					endTimeStamp = BlocketteTimestamp
							.parseTimestamp(timestampString);
				} catch (TimestampFormatException e) {
					logger.error("StationData.printEpochs() [{}-{}] Error converting timestampString={}",
							network, name, timestampString);
				}
			}
			if (endTimeStamp == null) { // This Epoch is open
				if (epochTime.compareTo(startTimeStamp) >= 0) {
					return blockette;
				}
			} // This Epoch is closed
			else if (epochTime.compareTo(startTimeStamp) >= 0
					&& epochTime.compareTo(endTimeStamp) <= 0) {
				return blockette;
			}
		} // for
		return null; // If we made it to here than we are returning
						// blockette==null
	}

	// Loop through all station (=Blockette 050) epochs and print summary

	public void printEpochs() {
		TreeSet<LocalDateTime> epochtimes = new TreeSet<>();
		epochtimes.addAll(epochs.keySet());

		for (LocalDateTime timestamp : epochtimes) {
			String startDate = EpochData.epochToDateString(timestamp);

			Blockette blockette = epochs.get(timestamp);
			String timestampString = blockette.getFieldValue(14, 0);
			String endDate = null;
			if (!timestampString.equals("(null)")) {
				try {
					LocalDateTime endtimestamp = BlocketteTimestamp
							.parseTimestamp(timestampString);
					endDate = EpochData.epochToDateString(endtimestamp);
				} catch (TimestampFormatException e) {
					logger.error("printEpochs: Error converting timestampString={}",
							timestampString);
					logger.error(e.getLocalizedMessage());
				}
			}
			logger.info("==StationData Epoch: {} - {}", startDate, endDate);
		}
	}

	// Sort channels and print out
	public void printChannels() {
		TreeSet<ChannelKey> keys = new TreeSet<>();
		keys.addAll(channels.keySet());

		for (ChannelKey key : keys) {
			System.out.println("==Channel:" + key);
			ChannelData channel = channels.get(key);
			channel.printEpochs();
		}
	}

	// channels
	public void addChannel(ChannelKey key, ChannelData data) {
		channels.put(key, data);
	}

	public boolean hasChannel(ChannelKey key) {
		return channels.containsKey(key);
	}

	public ChannelData getChannel(ChannelKey key) {
		return channels.get(key);
	}

	public Hashtable<ChannelKey, ChannelData> getChannels() {
		return channels;
	}

}
