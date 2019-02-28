package asl.seedsplitter;

import asl.util.Time;
import edu.iris.dmc.seedcodec.SteimException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import seed.BlockSizeException;
import seed.Blockette320;
import seed.IllegalSeednameException;
import seed.MiniSeed;

/**
 * @author Joel D. Edwards
 * 
 *         The SeedSplitProcessor receives MiniSEED records via a Queue, and
 *         splits them up by channel into trees. All of the channel trees are
 *         stored in the hash table. Each tree is an ordered group of DataSet
 *         objects, each containing a contiguous block of data outside of the
 *         time range of any other DataSet in the same tree.
 */
@SuppressWarnings("cast")
public class SeedSplitProcessor implements Runnable {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedsplitter.SeedSplitProcessor.class);

	private LinkedBlockingQueue<ByteBlock> m_queue;
	private boolean m_running;
	private Hashtable<String, TreeSet<DataSet>> m_trees = null;
	private Hashtable<String, ArrayList<DataSet>> m_table = null;

	// MTH:
	private Hashtable<String, ArrayList<Integer>> m_qualityTable = new Hashtable<>();
	private Hashtable<String, ArrayList<Blockette320>> m_calTable = new Hashtable<>();

	private Pattern m_patternNetwork = null;
	private Pattern m_patternStation = null;
	private Pattern m_patternLocation = null;
	private Pattern m_patternChannel = null;

	/**
	 * Constructor.
	 * 
	 * @param queue
	 *            The queue from which MiniSEED records are received.
	 */
	public SeedSplitProcessor(LinkedBlockingQueue<ByteBlock> queue) {
		_construct(queue, new Hashtable<>());
	}

	/**
	 * Hidden initializer called by every constructor.
	 * 
	 * @param queue
	 *            The queue from which MiniSEED records are received.
	 * @param table
	 *            An initial hash table to which new data should be added.
	 */
	private void _construct(LinkedBlockingQueue<ByteBlock> queue,
			Hashtable<String, ArrayList<DataSet>> table) {
		m_queue = queue;
		m_running = false;
		m_table = table;
		m_trees = new Hashtable<>();

	}

	/**
	 * Filter pattern for the MiniSEED record's Network field.
	 * 
	 * @param pattern
	 *            Filter pattern for the MiniSEED record's Network field.
	 */
	public void setNetworkPattern(Pattern pattern) {
		m_patternNetwork = pattern;
	}

	/**
	 * Filter pattern for the MiniSEED record's Station field.
	 * 
	 * @param pattern
	 *            Filter pattern for the MiniSEED record's Station field.
	 */
	public void setStationPattern(Pattern pattern) {
		m_patternStation = pattern;
	}

	/**
	 * Filter pattern for the MiniSEED record's Location field.
	 * 
	 * @param pattern
	 *            Filter pattern for the MiniSEED record's Location field.
	 */
	public void setLocationPattern(Pattern pattern) {
		m_patternLocation = pattern;
	}

	/**
	 * Filter pattern for the MiniSEED record's Channel field.
	 * 
	 * @param pattern
	 *            Filter pattern for the MiniSEED record's Channel field.
	 */
	public void setChannelPattern(Pattern pattern) {
		m_patternChannel = pattern;
	}

	/**
	 * Returns the populated hash table.
	 * 
	 * @return The populated hash table.
	 */
	public Hashtable<String, ArrayList<DataSet>> getTable() {
		return m_table;
	}

	public Hashtable<String, ArrayList<Integer>> getQualityTable() {
		return m_qualityTable;
	}

	public Hashtable<String, ArrayList<Blockette320>> getCalTable() {
		return m_calTable;
	}

	private volatile int lastSequenceNumber = 0;

	/**
	 * Pulls {@link ByteBlock}s from the queue and converts the contained SEED
	 * records into one or more {@link DataSet} object.
	 */
	@Override
	public void run() {

		ByteBlock block = null;
		MiniSeed record = null;
		DataSet tempData = null;

		String network = null;
		String station = null;
		String location = null;
		String channel = null;
		double sampleRate = 0.0;
		long interval = 0;

		long startTime = 0;

		byte[] recordBytes = null;
		int[] samples = null;

		String seedstring = null;
		// total number of bytes that have been received from the queue
		long byteTotal = 0;
		SeedSplitProgress progress = null;
		String key = null;
		TreeSet<DataSet> tree = null;
		Hashtable<String, DataSet> temps = new Hashtable<>();
		Hashtable<String, Integer> recordCounts = new Hashtable<>();

		Matcher matcher = null;

		int kept = 0;
		int discarded = 0;

		m_running = true;
		while (m_running) {
			progress: {
				try {
					block = m_queue.take();
					// even if we don't end up using this data, it counts toward
					// our progress
					byteTotal += block.getLength();
					byteTotal += block.getSkippedBytes();
					progress = new SeedSplitProgress(byteTotal);
					recordBytes = block.getData();
					if (block.isLast()) {
						m_running = false;
					} else if (block.isEnd()) {
						progress.setFileDone(true);
					} else if (MiniSeed.crackIsHeartBeat(recordBytes)) {
						logger.debug("Found HEARTBEAT record!");
					} else { // MTH
						seedstring = MiniSeed.crackSeedname(recordBytes);
						network = seedstring.substring(0, 2).trim();
						if (m_patternNetwork != null) {
							matcher = m_patternNetwork.matcher(network);
							if (!matcher.matches()) {
								discarded++;
								break progress;
							}
						}
						station = seedstring.substring(2, 7).trim();
						if (m_patternStation != null) {
							matcher = m_patternStation.matcher(station);
							if (!matcher.matches()) {
								discarded++;
								break progress;
							}
						}
						location = seedstring.substring(10, 12).trim();
						if (m_patternLocation != null) {
							matcher = m_patternLocation.matcher(location);
							if (!matcher.matches()) {
								discarded++;
								break progress;
							}
						}
						channel = seedstring.substring(7, 10).trim();
						if (m_patternChannel != null) {
							matcher = m_patternChannel.matcher(channel);
							if (!matcher.matches()) {
								discarded++;
								break progress;
							}
						}

						// Set the default location codes
						if (location.equals("--") || location.equals("")
								|| location == null) {
							logger.debug("miniseed channel=[{}] location=[{}] was changed to [00]",
											channel, location);
							location = "00";
						}
						if (location.equals("HR")) {
							logger.debug("miniseed channel=[{}] location=[{}] was changed to [10]",
											channel, location);
							location = "10";
						}

						sampleRate = MiniSeed.crackRate(recordBytes);
						try {
							interval = DataSet.sampleRateToInterval(sampleRate);
						} catch (IllegalSampleRateException e) {
							MiniSeed ms = new MiniSeed(recordBytes);
							logger.error("Illegal Sample Rate: sequence #{}, rate = {}", ms.getSequence(), sampleRate);
							discarded++;
							break progress;
						}
						kept++;
						logger.debug(String.format("%s_%s %s-%s", network,
								station, location, channel));
						key = String.format("%s_%s %s-%s (%.1f Hz)", network,
								station, location, channel, sampleRate);

						if (!recordCounts.containsKey(key)) {
							recordCounts.put(key, 1);
						} else {
							recordCounts.put(key, recordCounts.get(key) + 1);
						}

						int year = MiniSeed.crackYear(recordBytes);
						int doy = MiniSeed.crackDOY(recordBytes);

						/*See SEED manual Chapter 3*/
						int[] btime = MiniSeed.crackTime(recordBytes);

						LocalDateTime dateTime = Time.btimeToLocalDateTime(year, doy, btime[0], btime[1], btime[2], btime[3]);

						startTime = Time.calculateEpochMicroSeconds(dateTime);

						tempData = temps.getOrDefault(key, null);

						if (!m_trees.containsKey(key)) {
							tree = new TreeSet<>();
							m_trees.put(key, tree);
						} else {
							tree = m_trees.get(key);
						}

						// Allow for a fudge factor of 1 millisecond if sample
						// rate is less than 100 Hz.
						//
						// Is this a good idea, or would it be better to simply
						// report a gap so the user is aware of the jump?
						//
						// long intervalAdjustment = (interval > 10000 ? 1000 :
						// 0);
						long intervalAdjustment = interval / 1;

						boolean replaceDataSet = false;
						// Temporarily disabled fudge factor

						if ((tempData == null)
								|| ((startTime - tempData.getEndTime()) > (interval + intervalAdjustment))) {
							// if ((tempData == null) || ((startTime -
							// tempData.getEndTime()) > interval)) {
							// (VIM-HACK) }
							replaceDataSet = true;
						} else {
							MiniSeed ms = new MiniSeed(recordBytes);
							if ((startTime - tempData.getEndTime()) < (interval - intervalAdjustment)) {
								// if ((startTime - tempData.getEndTime()) <
								// interval) {
								// (VIM-HACK) }
								replaceDataSet = true;
								logger
										.error("Found data overlap <{}] - [{}> sequence #{}.!\n",
														DataSet.timestampToString(tempData
																.getEndTime()),
														DataSet.timestampToString(startTime),
														ms.getSequence());
								if (ms.getSequence() <= lastSequenceNumber) {
									logger
											.error("Out of sequence last={} current={}",
															lastSequenceNumber,
															ms.getSequence());
								}
								// throw new SeedRecordOverlapException();
							}
						}
						try {
							if (replaceDataSet) {
								if (tempData != null) {
									tree.add(tempData);
									logger.debug("Adding DataSet to TreeSet.");
									logger.debug("  Range: {} - {} ({} data points {CHECK: {}})",
													DataSet.timestampToString(tempData
															.getStartTime()),
													DataSet.timestampToString(tempData
															.getEndTime()),
													((tempData.getEndTime() - tempData
															.getStartTime())
															/ tempData
																	.getInterval() + 1),
													tempData.getLength());
									tempData = null;
									temps.remove(key);
								}
								logger.debug("Creating new DataSet");
								tempData = new DataSet();
								tempData.setNetwork(network);
								tempData.setStation(station);
								tempData.setLocation(location);
								tempData.setChannel(channel);
								tempData.setStartTime(startTime);
								try {
									tempData.setSampleRate(sampleRate);
								} catch (RuntimeException e) {
									MiniSeed ms = new MiniSeed(recordBytes);
									logger.error(String.format(
											"Invalid Start Time: sequence #%d",
											ms.getSequence()), e.getMessage());
									tempData = null;
									break progress;
								} catch (IllegalSampleRateException e) {
									MiniSeed ms = new MiniSeed(recordBytes);
									logger
											.error(String
													.format("Invalid Sample Rate: sequence #%d, rate = %f",
															ms.getSequence(),
															ms.getRate()), e.getMessage());
									tempData = null;
									break progress;
								}
								temps.put(key, tempData);
							} // replaceDataSet
						} catch (RuntimeException e) {
							logger.error("RuntimeException:", e.getMessage());
						}

						record = new MiniSeed(recordBytes);

						samples = record.decomp();

						// blockettes = record.getBlockettes();
						lastSequenceNumber = record.getSequence();
						tempData.extend(samples, 0, samples.length);

						// MTH: Get timing quality from the current miniseed
						// block and store it for this key
						int quality = record.getTimingQuality();

						ArrayList<Integer> qualityArray = null;
						if (m_qualityTable.get(key) == null) {
							qualityArray = new ArrayList<>();
							m_qualityTable.put(key, qualityArray);
						} else {
							qualityArray = m_qualityTable.get(key);
						}
						if (quality >= 0) { // getTimingQuality() return -1
							// if no B1001 block found
							qualityArray.add(quality);
						}

						// MTH: Get calibration block from the current
						// miniseed block and store it for this key
						// byteBuf320 = 64-byte Blockette320 as per SEED
						// Manual HOWEVER, D. Ketcham's
						// MiniSeed.getBlockette320()
						// only returns 32-bytes ?? --> modified to return
						// 64-bytes
						byte[] byteBuf320 = record.getBlockette320();
						if (byteBuf320 != null) {
							// System.out.format("== SeedSplitProcessor: Blockette320 found for key=%s kept=[%d] discarded=[%d]\n",
							// key, kept, discarded);
							Blockette320 blockette320 = new Blockette320(
									byteBuf320);
							// System.out.format("== blockette320: epoch secs=[%d]\n",
							// blockette320.getCalibrationEpoch() );


							ArrayList<Blockette320> calBlock = null;
							if (m_calTable.get(key) == null) {
								calBlock = new ArrayList<>();
								m_calTable.put(key, calBlock);
							} else {
								calBlock = m_calTable.get(key);
							}
							calBlock.add(blockette320);
						}
					}

				} catch (SteimException e) {
					logger.error("SteimException:", e.getMessage());
				} catch (BlockSizeException e) {
					logger.error("BlockSizeException:", e.getMessage());
				} catch (InterruptedException e) {
					logger.error("InterruptedException:", e.getMessage());
				} catch (IllegalSeednameException e) {
					logger.error("IllegalSeednameException:", e.getMessage());
				}
			}
		}
		for (String tempKey : temps.keySet()) {
			tempData = null;
			tree = null;
			if (temps.containsKey(tempKey)) {
				tempData = temps.get(tempKey);
			}
			if (m_trees.containsKey(tempKey)) {
				tree = m_trees.get(tempKey);
			}
			if ((tempData != null) && (tree != null)) {
				tree.add(tempData);
				tree.size();
				
				logger.debug("Adding DataSet to TreeSet.");
				logger.debug(String.format(
						"  Range: %s - %s (%d data points {CHECK: %d})",
						DataSet.timestampToString(tempData.getStartTime()),
						DataSet.timestampToString(tempData.getEndTime()),
						((tempData.getEndTime() - tempData.getStartTime())
								/ tempData.getInterval() + 1),
						tempData.getLength()));
				tempData = null;
			}
		}

		// The following block loops through the contents of the tree in order
		// to allow the user to visually inspect gaps. The block is only to
		// ensure the variables go out of scope.
		// MTH: need to convert this block from java.util.logging --> to log4j
		/*
		 * 
		 * if (logger.getLevel() <= Level.FINE) { TreeSet<DataSet> temp_tree =
		 * null; logger.fine("===== TREE SET ELEMENTS ====="); Iterator iter =
		 * temp_tree.iterator(); DataSet data = null; DataSet last = null; while
		 * (iter.hasNext()) { data = (DataSet)iter.next(); if (last != null) {
		 * long gap = data.getStartTime() - last.getEndTime(); long points = gap
		 * / data.getInterval(); logger.fine("      gap: " + gap +
		 * " microseconds (" + points + " data point" + ((points == 1) ? "" :
		 * "s") + ")"); } logger.fine("    DataSet: " +
		 * DataSet.timestampToString(data.getStartTime()) + " to " +
		 * DataSet.timestampToString(data.getEndTime()) + " (" +
		 * data.getLength() + " data points)"); last = data; }
		 * logger.fine("============================="); } //
		 */

		Iterator<DataSet> iter;
		DataSet currDataSet;
		DataSet lastDataSet;
		for (String chanKey : m_trees.keySet()) {
			tree = m_trees.get(chanKey);
			ArrayList<DataSet> list = new ArrayList<>(tree.size());
			if (!tree.isEmpty()) {
				logger.debug("Processing " + tree.size()
						+ " tree elements for '" + chanKey + "'");
				iter = tree.iterator();
				currDataSet = null;
				lastDataSet = iter.next();

				while (iter.hasNext()) {
					currDataSet = iter.next();

					try {
						logger.debug("Merging DataSets...");
						currDataSet.mergeInto(lastDataSet);
						logger.debug("Done.");
					} catch (SequenceIntervalMismatchException e) {
						throw new RuntimeException(
								"Interval Mismatch. This should never happen!");
					} catch (SequenceMergeRangeException e) {
						list.add(lastDataSet);
						lastDataSet = currDataSet;
					} catch (BlockSizeMismatchException e) {
						logger
								.error("BlockSizeMismatchException: BlockPool.addBlock() Impossible situation!",
										e.getMessage());
					}
				}
				list.add(lastDataSet);
				m_table.put(chanKey, list);
			} else {
				logger.debug("Empty tree for '" + chanKey + "'");
			}
		}

		logger.debug("<SeedSplitProcessor Thread> Yeah, we're done.");
		logger.debug("Kept " + kept + " records");
		logger.debug("Discarded " + discarded + " records");
		for (String countKey : recordCounts.keySet()) {
			logger.debug("  " + countKey + ": " + recordCounts.get(key)
					+ " records");
		}
	}

}
