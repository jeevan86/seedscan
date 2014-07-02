
package asl.seedscan.metrics;

import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sac.SacTimeSeries;
import timeutils.PSD;
import asl.metadata.Channel;
import asl.metadata.EpochData;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.ChannelMeta.ResponseUnits;
import asl.metadata.meta_new.ChannelMetaException;
import asl.metadata.meta_new.StationMeta;
import asl.seedscan.database.MetricValueIdentifier;
import asl.seedscan.event.EventCMT;
import freq.Cmplx;

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
		arguments = new Hashtable<String, String>();
		crossPowerMap = new Hashtable<CrossPowerKey, CrossPower>();

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
	 * @param channelA
	 * @param channelB
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
	 * @param channelA
	 * @param channelB
	 * @return the cross power
	 */
	protected CrossPower getCrossPower(Channel channelA, Channel channelB) {
		CrossPowerKey key = new CrossPowerKey(channelA, channelB);
		CrossPower crossPower;

		if (crossPowerMap.containsKey(key)) {
			crossPower = crossPowerMap.get(key);
		} else {
			double[] psd = null;
			double[] df = new double[1]; // Dummy array to get params out of
			// computePSD()
			for (int i = 0; i < df.length; i++)
				df[i] = 0;
			try {
				psd = computePSD(channelA, channelB, df);
			} catch (MetricPSDException e) {
				logger.error("MetricPSDException:", e);
			} catch (ChannelMetaException e) {
				logger.error("ChannelMetaException:", e);
			}
			crossPower = new CrossPower(psd, df[0]);
			crossPowerMap.put(key, crossPower);
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
		// e.g., "outputs/2012/2012160/2012160.IU_ANMO"
		return new String(String.format("%s/%4s/%4s%3s/%4s%3s.%s",
				getBaseOutputDir(), getYear(), getYear(), getDOY(), getYear(),
				getDOY(), getStation()));
	}

	/**
	 * Gets the day.
	 *
	 * @return the date string of stationMeta's timestamp.
	 */
	public String getDay() { // returns yyyy:ddd:hh:mm
		return (EpochData.epochToDateString(stationMeta.getTimestamp()));
	}

	/**
	 * Gets the date.
	 *
	 * @return the stationMeta timestamp.
	 */
	public Calendar getDate() {
		// returns Calendar date from StationMeta
		return stationMeta.getTimestamp();
	}

	/**
	 * Gets the day of year.
	 *
	 * @return the day of year
	 */
	public String getDOY() {
		String[] dateArray = getDay().split(":");
		return dateArray[1];
	}

	/**
	 * Gets the year.
	 *
	 * @return the year
	 */
	public String getYear() {
		String[] dateArray = getDay().split(":");
		return dateArray[0];
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
		if (!stationMeta.hasChannels(location, band)) {
			return false;
		}
		if (!metricData.hasChannels(location, band)) {
			return false;
		}
		return true;
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
			StringBuilder message = new StringBuilder();
			message.append(String.format("Argument '" + name
					+ "' is not recognized.\n"));
			throw new NoSuchFieldException(message.toString());
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
			StringBuilder message = new StringBuilder();
			message.append(String.format("Argument '" + name
					+ "' is not recognized.\n"));
			throw new NoSuchFieldException(message.toString());
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

	/**
	 * computePSD - Done here so that it can be passed from metric to metric,
	 * rather than re-computing it for each metric that needs it
	 * 
	 * Use Peterson's algorithm (24 hrs = 13 segments with 75% overlap, etc.)
	 * 
	 * @param channelX            - X-channel used for power-spectral-density computation
	 * @param channelY            - Y-channel used for power-spectral-density computation
	 * @param params            [] - Dummy array used to pass df (frequency spacing) back up
	 * @return psd[f] - Contains smoothed crosspower-spectral density computed
	 *         for nf = nfft/2 + 1 frequencies (+ve freqs + DC + Nyq)
	 * @throws ChannelMetaException the channel metadata exception
	 * @throws MetricPSDException the metric psd exception
	 */
	private final double[] computePSD(Channel channelX, Channel channelY,
			double[] params) throws ChannelMetaException, MetricPSDException {
		int ndata = 0;
		double srate = 0; // srate = sample frequency, e.g., 20Hz

		// This would give us 2 channels with the SAME number of (overlapping)
		// points, but
		// they might not represent a complete day (e.g., could be a single
		// block of data in the middle of the day)
		// double[][] channelOverlap = metricData.getChannelOverlap(channelX,
		// channelY);
		// double[] chanXData = channelOverlap[0];
		// double[] chanYData = channelOverlap[1];

		// Instead, getPaddedDayData() gives us a complete (zero padded if
		// necessary) array of data for 1 day:
		double[] chanXData = metricData.getPaddedDayData(channelX);
		double[] chanYData = metricData.getPaddedDayData(channelY);

		double srateX = metricData.getChannelData(channelX).get(0)
				.getSampleRate();
		double srateY = metricData.getChannelData(channelY).get(0)
				.getSampleRate();
		ChannelMeta chanMetaX = stationMeta.getChanMeta(channelX);
		ChannelMeta chanMetaY = stationMeta.getChanMeta(channelY);

		if (srateX != srateY) {
			StringBuilder message = new StringBuilder();
			message.append(String.format("computePSD(): srateX (=" + srateX
					+ ") != srateY (=" + srateY + ")\n"));
			throw new MetricPSDException(message.toString());
		}
		srate = srateX;
		ndata = chanXData.length;

		if (srate == 0)
			throw new MetricPSDException("Got srate=0");

		double dt = 1. / srate;

		PSD psdRaw = new PSD(chanXData, chanYData, dt);
		Cmplx[] spec = psdRaw.getSpectrum();
		double[] freq = psdRaw.getFreq();
		double df = psdRaw.getDeltaF();
		int nf = freq.length;

		params[0] = df;

		// Get the instrument response for Acceleration and remove it from the
		// PSD
		try {
			Cmplx[] instrumentResponseX = chanMetaX.getResponse(freq,
					ResponseUnits.ACCELERATION);
			Cmplx[] instrumentResponseY = chanMetaY.getResponse(freq,
					ResponseUnits.ACCELERATION);

			Cmplx[] responseMagC = new Cmplx[nf];

			double[] psd = new double[nf]; // Will hold the 1-sided PSD
			// magnitude
			psd[0] = 0;

			// We're computing the squared magnitude as we did with the FFT
			// above
			// Start from k=1 to skip DC (k=0) where the response=0

			for (int k = 1; k < nf; k++) {
				responseMagC[k] = Cmplx.mul(instrumentResponseX[k],
						instrumentResponseY[k].conjg());
				if (responseMagC[k].mag() == 0) {
					StringBuilder message = new StringBuilder();
					message.append(String
							.format("responseMagC[k]=0 --> divide by zero!\n"));
					throw new MetricPSDException(message.toString());
				} else { // Divide out (squared)instrument response & Convert to
					// dB:
					spec[k] = Cmplx.div(spec[k], responseMagC[k]);
					psd[k] = spec[k].mag();
				}
			}

			return psd;
		} catch (ChannelMetaException e) {
			throw e;
		}
	} // end computePSD
} // end class
