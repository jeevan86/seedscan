
package asl.seedscan.metrics;

import java.util.Enumeration;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.metadata.EpochData;
import asl.metadata.meta_new.ChannelMetaException;
import asl.metadata.meta_new.StationMeta;
import asl.seedscan.database.MetricValueIdentifier;
import asl.seedscan.event.EventCMT;
import asl.timeseries.CrossPower;
import asl.timeseries.CrossPowerKey;
import sac.SacTimeSeries;

/**
 * The basic class that all metrics extend.
 * @author Mike Hagerty
 * @author Joel Edwards - USGS
 * @author James Holland - USGS
 * @author Alejandro Gonzales - Honeywell
 */
public abstract class Metric {
	
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.metrics.Metric.class);

	/** The arguments. */
	private Hashtable<String, String> arguments;
	
	/** The cross power map. */
	private Hashtable<CrossPowerKey, CrossPower> crossPowerMap;

	/** Determines if metric will be forced to recompute and update the database.*/
	private boolean forceUpdate = false;
	
	/** Determines if plots will be created. */
	private boolean makePlots = false;
	
	/** The output directory for plots. */
	private String outputDir = null;

	/** Dummy value when no results is computed. */
	protected final double NO_RESULT = -999.999;

	/** The station metadata. */
	protected StationMeta stationMeta = null;
	
	/** The metric data. */
	protected MetricData metricData = null;
	
	/** The metric result. */
	protected MetricResult metricResult = null;

	/** The event table. */
	private Hashtable<String, EventCMT> eventTable = null;
	
	/** The event synthetics. */
	private Hashtable<String, Hashtable<String, SacTimeSeries>> eventSynthetics = null;

	/**
	 * Instantiates a new metric.
	 */
	public Metric() {
		arguments = new Hashtable<>();
		crossPowerMap = new Hashtable<>();

		// MTH: 03-18-13: Added to allow these optional arguments to each
		// cfg:metric in config.xml
		addArgument("makeplots");
		addArgument("forceupdate");
	}

	/**
	 * Sets the data.
	 *
	 * @param metricData the new data
	 */
	public void setData(MetricData metricData) {
		this.metricData = metricData;
		stationMeta = metricData.getMetaData();
		metricResult = new MetricResult(stationMeta, getName());
	}

	/**
	 * Gets the version.
	 * This function appears to not be used.
	 *
	 * @return the version
	 */
	public abstract long getVersion();

	/**
	 * Gets the name.
	 *
	 * @return the metric name
	 */
	public abstract String getName();

	/**
	 * Process.
	 */
	public abstract void process();

	/**
	 * Creates the identifier.
	 *
	 * @param channel the channel
	 * @return the metric value identifier
	 */
	public MetricValueIdentifier createIdentifier(Channel channel) {
		return new MetricValueIdentifier(metricResult.getDate(),
				metricResult.getMetricName(), metricResult.getStation(),
				channel);
	}

	/**
	 * Creates the identifier for multiple channels.
	 *
	 * @param channelA first channel
	 * @param channelB second channel
	 * @return the metric value identifier
	 */
	public MetricValueIdentifier createIdentifier(Channel channelA,
			Channel channelB) {
		return createIdentifier(MetricResult.createChannel(MetricResult
				.createResultId(channelA, channelB)));
	}

	/**
	 * Gets the cross power map.
	 *
	 * @return the cross power map
	 */
	public Hashtable<CrossPowerKey, CrossPower> getCrossPowerMap() {
		return crossPowerMap;
	}

	/**
	 * Sets the cross power map.
	 *
	 * @param crossPowerMap the cross power map
	 */
	public void setCrossPowerMap(
			Hashtable<CrossPowerKey, CrossPower> crossPowerMap) {
		this.crossPowerMap = crossPowerMap;
	}

	/**
	 * Gets the cross power.
	 *
	 * @param channelA first channel
	 * @param channelB second channel
	 * @return the cross power
	 * @throws MetricException when CrossPower cannot be created.
	 */
	protected CrossPower getCrossPower(Channel channelA, Channel channelB) throws MetricException {
		CrossPowerKey key = new CrossPowerKey(channelA, channelB);
		CrossPower crossPower = null;

		if (crossPowerMap.containsKey(key)) {
			crossPower = crossPowerMap.get(key);
		} else {
			try {
				crossPower = new CrossPower(channelA, channelB, metricData);
				crossPowerMap.put(key, crossPower);
			} catch (MetricPSDException | ChannelMetaException e) {
				throw new MetricException("Unable to create CrossPower", e);
			}
		}
		return crossPower;
	}

	/* (non-javadoc) run crosspower over a pre-selected range of data, i.e., 3 hours before W phase
	 * for noise analysis in w phase quality metric
	 */
	protected CrossPower getCrossPower(Channel channelA, Channel channelB,
			double[] aData, double[] bData) throws MetricException {
		CrossPowerKey key = new CrossPowerKey(channelA, channelB);
		CrossPower crossPower = null;

		if (crossPowerMap.containsKey(key)) {
			crossPower = crossPowerMap.get(key);
		} else {
			try {
				crossPower = new CrossPower(channelA, channelB, metricData, aData, bData);
				crossPowerMap.put(key, crossPower);
			} catch (MetricPSDException | ChannelMetaException e) {
				throw new MetricException("Unable to create CrossPower", e);
			}
		}
		return crossPower;
	}

	/**
	 * Gets the event synthetics.
	 *
	 * @param eventIdString the event id string
	 * @return the event synthetics
	 */
	public Hashtable<String, SacTimeSeries> getEventSynthetics(
			String eventIdString) {
		if (eventSynthetics.containsKey(eventIdString)) {
			return eventSynthetics.get(eventIdString);
		} else {
			logger.warn(String
					.format("== getEventSynthetics - Synthetics not found for eventIdString=[%s]\n",
							eventIdString));
			return null;
		}
	}

	/**
	 * Gets the event table.
	 *
	 * @return the event table
	 */
	public Hashtable<String, EventCMT> getEventTable() {
		return eventTable;
	}

	/**
	 * Sets the event synthetics.
	 *
	 * @param eventSynthetics the event synthetics
	 */
	public void setEventSynthetics(
			Hashtable<String, Hashtable<String, SacTimeSeries>> eventSynthetics) {
		this.eventSynthetics = eventSynthetics;
	}

	/**
	 * Sets the event table.
	 *
	 * @param events the events
	 */
	public void setEventTable(Hashtable<String, EventCMT> events) {
		eventTable = events;
	}

	/**
	 * Gets the metric result.
	 *
	 * @return the metric result
	 */
	public MetricResult getMetricResult() {
		return metricResult;
	}

	/**
	 * Sets the base output dir.
	 *
	 * @param outputDir the new base output dir
	 */
	public void setBaseOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	/**
	 * Gets the base output dir.
	 *
	 * @return the outputDir private field.
	 */
	public String getBaseOutputDir() {
		return outputDir;
	}

	/**
	 * Gets the output dir.
	 *
	 * @return the output dir structure with dates. e.g., "outputs/2012/2012160/2012160.IU_ANMO"
	 */
	public String getOutputDir() {
		String year = String.valueOf(stationMeta.getTimestamp().getYear());
		String doy = String.valueOf(stationMeta.getTimestamp().getDayOfYear());
		// e.g., "outputs/2012/2012160/2012160.IU_ANMO"
		return String.format("%s/%4s/%4s%3s/%4s%3s.%s",
				getBaseOutputDir(), year, year, doy, year,
				doy, getStation());
	}

	/**
	 * Gets the day.
	 *
	 * @return the date string of stationMeta's timestamp.
	 */
	public String getDay() { // returns yyyy:ddd:hh:mm
		return EpochData.epochToDateString(stationMeta.getTimestamp());
	}

	/**
	 * Gets the station name.
	 *
	 * @return the station name e.g., will return "ANMO"
	 */
	public String getStn() // e.g., will return "ANMO"
	{
		return stationMeta.getStation();
	}

	/**
	 * Gets the network and station name.
	 *
	 * @return the network and station name e.g., will return "IU_ANMO"
	 */
	public String getStation() // e.g., will return "IU_ANMO"
	{
		return metricResult.getStation().toString();
	}

	/**
	 * Sets the force update to true.
	 */
	private void setForceUpdate() {
		this.forceUpdate = true;
	}

	/**
	 * Gets the force update.
	 *
	 * @return the force update field
	 */
	public boolean getForceUpdate() {
		return forceUpdate;
	}

	/**
	 * Sets the make plots to true.
	 */
	private void setMakePlots() {
		this.makePlots = true;
	}

	/**
	 * Gets the make plots.
	 *
	 * @return the make plots field
	 */
	public boolean getMakePlots() {
		return makePlots;
	}

	/**
	 * Check if both station metadata and metric data have the channels and power bands.
	 *
	 * @param location the location
	 * @param band the power band
	 * @return true, if both stationMeta and metric data have channels and band.
	 */
	public boolean weHaveChannels(String location, String band) {
		return stationMeta.hasChannels(location, band) && metricData.hasChannels(location, band);
	}

	/**
	 * Dynamic argument managment
	 * Adds the argument to the arguments hashtable with empty string as value.
	 *
	 * @param name the name of the argument being added to the hashtable.
	 */
	protected final void addArgument(String name) {
		arguments.put(name, "");
	}

	/**
	 * Sets the value of the argument in the arguments hashtable.
	 *
	 * @param name argument name/key
	 * @param value argument's new value
	 * @throws NoSuchFieldException the field was never added to arguments.
	 */
	public final void add(String name, String value)
			throws NoSuchFieldException {
		if (!arguments.containsKey(name)) {
			throw new NoSuchFieldException("Argument '" + name + "' is not recognized.");
		}
		arguments.put(name, value);

		if (name.equals("forceupdate")) {
			if (value.toLowerCase().equals("true")
					|| value.toLowerCase().equals("yes")) {
				setForceUpdate();
			}
		} else if (name.equals("makeplots")) {
			if (value.toLowerCase().equals("true")
					|| value.toLowerCase().equals("yes")) {
				setMakePlots();
			}
		}
	}

	/**
	 * get(key) returns value if key is found in Hashtable arguments returns
	 * null if key is found but value is not set (value="") throws
	 * NoSuchFieldException if key is not found.
	 *
	 * @param name the argument's name/key
	 * @return the value of the hashtable
	 * @throws NoSuchFieldException the field was never added to arguments.
	 */
	public final String get(String name) throws NoSuchFieldException {
		if (!arguments.containsKey(name)) {
			throw new NoSuchFieldException("Argument '" + name + "' is not recognized.");
		}
		String argumentValue = arguments.get(name);
		if ((argumentValue == null) || (argumentValue.equals(""))) {
			argumentValue = null;
		}
		return argumentValue;
	}

	/**
	 * Returns list of keys in arguments hashtable.
	 *
	 * @return the list of keys in arguments
	 */
	public final Enumeration<String> names() {
		return arguments.keys();
	}
} // end class
