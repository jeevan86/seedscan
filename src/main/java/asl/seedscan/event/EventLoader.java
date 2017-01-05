package asl.seedscan.event;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Station;
import sac.SacTimeSeries;

// TODO: Auto-generated Javadoc
/**
 * The Class EventLoader.
 */
public class EventLoader {

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.event.EventLoader.class);

	/** The events directory. */
	private static String eventsDirectory = null;

	/** True if the events directory has been loaded. */
	private static boolean eventsDirectoryLoaded = false;

	/** The events directory valid. */
	private static boolean eventsDirectoryValid = false;

	/**
	 * The CMT table. This table is two nested Hashtables. The keys are as
	 * below: Hashtable<"C201510260909A", Hashtable<"SDCO.XX.LXN.modes.sac" ,
	 * EventCMT>>
	 */
	private static Hashtable<String, Hashtable<String, EventCMT>> cmtTree = null;

	/**
	 * Instantiates a new event loader. This class is instantiated once per
	 * scanner thread. This is why it is mostly static. It could probably be
	 * converted to all static.
	 *
	 * @param directoryPath
	 *            the directory path
	 */
	public EventLoader(String directoryPath) {
		loadEventsDirectory(directoryPath);
	}

	/**
	 * This is used for testing purposes only.
	 * 
	 * @return the events Directory
	 */
	public static String getEventsDirectory() { // NO_UCD (test only)
		return eventsDirectory;
	}

	/**
	 * We only want 1 (Scanner) thread at a time in here! And it should only
	 * need to be entered one time for a given Scan.
	 *
	 * @param directoryPath
	 *            path to events
	 */
	synchronized private static void loadEventsDirectory(String directoryPath) {

		if (eventsDirectoryLoaded) {
			logger.info(String.format("eventsDir already initialized to:%s [valid=%s]", eventsDirectory,
					eventsDirectoryValid));
			return;
		}

		eventsDirectoryLoaded = true;

		if (directoryPath == null) {
			logger.warn("eventsDir was NOT set in config.xml: <cfg:events_dir> --> Don't Compute Event Metrics");
			return;
		} else if (!(new File(directoryPath)).exists()) {
			logger.warn(String.format("eventsDir=%s does NOT exist --> Skip Event Metrics", directoryPath));
			return;
		} else if (!(new File(directoryPath)).isDirectory()) {
			logger.error(String.format("eventsDir=%s is NOT a directory --> Skip Event Metrics", directoryPath));
			return;
		} else {
			logger.info(String.format("eventsDir=%s DOES exist --> Compute Event Metrics if asked", directoryPath));
			eventsDirectory = directoryPath;
			eventsDirectoryValid = true;
			return;
		}

	}

	/**
	 * Make key.
	 *
	 * @param date
	 *            the timestamp
	 * @return the string
	 */
	private final String makeKey(LocalDate date) {
		return date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
	}

	/**
	 * Gets the day synthetics. This method is dependent on getDayEvents() being
	 * called first. If not will always return null.
	 *
	 * @param timestamp
	 *            the timestamp
	 * @param station
	 *            the station
	 * @return the day synthetics
	 */
	public Hashtable<String, Hashtable<String, SacTimeSeries>> getDaySynthetics(LocalDate timestamp,
			final Station station) {

		final String key = makeKey(timestamp);

		if (cmtTree == null)
			return null; // No events loaded

		if (!cmtTree.containsKey(key))
			return null;// No events loaded for this day

		Hashtable<String, EventCMT> dayCMTs = cmtTree.get(key);
		if (dayCMTs == null)
			return null; // Not sure why this would happen

		FilenameFilter sacFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File file = new File(dir + "/" + name);
				// if (name.startsWith(station.getStation()) &&
				// name.endsWith(".sac") && (file.length() != 0) ) {
				if (name.startsWith(station.getStation()) && name.contains(".sac") && (file.length() != 0)) {
					return true;
				} else {
					return false;
				}
			}
		};

		Hashtable<String, Hashtable<String, SacTimeSeries>> allEventSynthetics = null;

		String year = key.substring(0, 4);
		String yearDir = eventsDirectory + "/" + year;

		SortedSet<String> keys = new TreeSet<String>(dayCMTs.keySet());
		for (String idString : keys) {
			// System.out.format("== getDaySynthetics: Got EventCMT
			// idString=[%s] --> [%s]\n",idString,
			// dayCMTs.get(idString) );
			File eventDir = new File(yearDir + "/" + idString);

			if (!eventDir.exists()) {
				logger.warn(String.format("getDaySynthetics: eventDir=[%s] does NOT EXIST!", eventDir));
			}

			File[] sacFiles = eventDir.listFiles(sacFilter);
			Hashtable<String, SacTimeSeries> eventSynthetics = null;
			
			if(sacFiles == null) continue; //Continue to next key

			for (File sacFile : sacFiles) {
				logger.info(String.format("Found sacFile=%s [%s]", sacFile, sacFile.getName()));
				SacTimeSeries sac = new SacTimeSeries();
				try {
					sac.read(sacFile);
				} catch (IOException e) {
					//File didn't read correctly, try next file.
					logger.error("Exception:", e);
					continue;
				}
				if (eventSynthetics == null) {
					eventSynthetics = new Hashtable<String, SacTimeSeries>();
				}
				eventSynthetics.put(sacFile.getName(), sac); // e.g.,
																// key="HRV.XX.LXZ.modes.sac.proc"
			}

			if (allEventSynthetics == null) {
				allEventSynthetics = new Hashtable<String, Hashtable<String, SacTimeSeries>>();
			}

			if (eventSynthetics != null) { // Add this event synthetics IF we
											// found the sacFiles
				allEventSynthetics.put(idString, eventSynthetics);
			}
		}
		// return eventSynthetics;
		return allEventSynthetics;
	}

	/**
	 * Gets the day events.
	 *
	 * @param timestamp
	 *            the timestamp
	 * @return the day events formatted as Hashtable<"C201510260909A", EventCMT>
	 */
	synchronized public Hashtable<String, EventCMT> getDayEvents(LocalDate timestamp) {

		final String key = makeKey(timestamp);

		logger.debug("getDayEvents: Request events for key=[{}]", key);

		if (!eventsDirectoryValid) {
			logger.error("getDayEvents: eventsDirectory is NOT valid --> return null");
			return null;
		}

		if (cmtTree != null) {
			if (cmtTree.containsKey(key)) {
				// System.out.format("== EventLoader.getDayEvents: key=[%s]
				// FOUND --> Return the events\n",
				// key);
				return cmtTree.get(key);
			}
		} else {
			cmtTree = new Hashtable<String, Hashtable<String, EventCMT>>();
		}

		// System.out.format("== EventLoader.getDayEvents: key=[%s] NOT FOUND
		// --> Try to load it\n",
		// key);
		Hashtable<String, EventCMT> dayCMTs = loadDayCMTs(key);

		if (dayCMTs != null) {
			cmtTree.put(key, dayCMTs);
		}

		return dayCMTs;
	}

	/**
	 * Load day's cmts.
	 *
	 * @param yyyymmdd
	 *            a date in string format
	 * @return the day events formatted as Hashtable<"C201510260909A", EventCMT>
	 */
	private Hashtable<String, EventCMT> loadDayCMTs(final String yyyymmdd) {

		Hashtable<String, EventCMT> dayCMTs = null;

		File[] events = null;

		String yyyy = yyyymmdd.substring(0, 4);

		File yearDir = new File(eventsDirectory + "/" + yyyy); // e.g.,
																// ../xs0/events/2012

		// File filter to catch dir names like "C201204112255A"
		FilenameFilter eventFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File file = new File(dir + "/" + name);
				if (name.contains(yyyymmdd) && file.isDirectory()) {
					return true;
				} else {
					return false;
				}
			}
		};

		// Check that yearDir exists and is a Directory:

		if (!yearDir.exists()) {
			logger.warn(String.format("loadDayCMTs: eventsDir=%s does NOT exist --> Skip Event Metrics", yearDir));
			return null;
		} else if (!yearDir.isDirectory()) {
			logger.error(String.format("loadDayCMTs: eventsDir=%s is NOT a Directory --> Skip Event Metrics", yearDir));
			return null;
		} else { // yearDir was found --> Scan for matching events
			logger.info(String.format("loadDayCMTs: getEventData: FOUND eventsDir=%s", yearDir));
			events = yearDir.listFiles(eventFilter);
			if (events == null) {
				logger.warn(String.format("No Matching events found for [yyyymodd=%s] " + "in eventsDir=%s", yyyymmdd,
						yearDir));
				return null;
			}
			// Loop over event dirs for this day and scan in CMT info, etc

			for (File event : events) { // Loop over event "file" (really a
										// directory - e.g.,
										// ../2012/C201204122255A/)
				logger.info(String.format("Found matching event dir=[%s]", event));
				File cmtFile = new File(event + "/" + "currCMTmineos");
				if (!cmtFile.exists()) {
					logger.error(String.format("Did NOT find cmtFile=currCMTmineos in dir=[%s]", event));
					continue;
				} else {
					BufferedReader br = null;
					try { // to read cmtFile

						br = new BufferedReader(new FileReader(cmtFile));
						String line = br.readLine();

						if (line == null) {
							logger.error(String.format("cmtFile=currCMTmineos in dir=[%s] is EMPTY", event));
							continue;
						} else {
							String[] args = line.trim().split("\\s+");
							if (args.length < 9) {
								logger.error(String.format("cmtFile=currCMTmineos in dir=[%s] is INVALID", event));
								continue;
							}

							// C201204112255A 2012 102 22 55 10.80 18.1500
							// -102.9600 21.3000 1.0 5.2000 1.204e26 7.9 -7.49
							// -0.41 7.7 -4.18 2.99 1.0e25 0 0 0 0 0 0

							try {
								String idString = args[0];
								int year = Integer.valueOf(args[1].trim());
								int dayOfYear = Integer.valueOf(args[2].trim());
								int hh = Integer.valueOf(args[3].trim());
								int mm = Integer.valueOf(args[4].trim());
								double xsec = Double.valueOf(args[5].trim());
								double lat = Double.valueOf(args[6].trim());
								double lon = Double.valueOf(args[7].trim());
								double dep = Double.valueOf(args[8].trim());

								int sec = (int) xsec;
								double foo = 1000 * (xsec - sec);
								int msec = (int) foo;

								GregorianCalendar gcal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
								gcal.set(Calendar.YEAR, year);
								gcal.set(Calendar.DAY_OF_YEAR, dayOfYear);
								gcal.set(Calendar.HOUR_OF_DAY, hh);
								gcal.set(Calendar.MINUTE, mm);
								gcal.set(Calendar.SECOND, sec);
								gcal.set(Calendar.MILLISECOND, msec);

								EventCMT eventCMT = new EventCMT.Builder(idString).calendar(gcal).latitude(lat)
										.longitude(lon).depth(dep).build();
								// eventCMT.printCMT();
								// Add EventCMT to this Day's event CMTs:
								if (dayCMTs == null) {
									dayCMTs = new Hashtable<String, EventCMT>();
								}
								dayCMTs.put(idString, eventCMT);
							} catch (NumberFormatException e) {
								StringBuilder message = new StringBuilder();
								message.append(String.format(
										"Caught NumberFormatException while trying to read cmtFile=[%s]\n", cmtFile));
								logger.error(message.toString(), e);
							}
						} // else line != null

					} // end try to read cmtFile
					catch (IOException e) {
						StringBuilder message = new StringBuilder();
						message.append(
								String.format("Caught IOException while trying to read cmtFile=[%s]\n", cmtFile));
						logger.error(message.toString(), e);
					} finally {
						try {
							if (br != null)
								br.close();
						} catch (IOException ex) {
							logger.error("IOException:", ex);
						}
					}

				} // else cmtFile exists

			} // for event

		} // else yearDir was found

		return dayCMTs;

	}

}
