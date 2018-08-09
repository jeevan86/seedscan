package asl.seedscan.metrics;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.meta_new.ChannelMeta.ResponseUnits;
import asl.metadata.meta_new.ChannelMetaException;
import asl.seedscan.event.EventCMT;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;

public class EventCompareStrongMotion extends Metric {
	private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.EventCompareStrongMotion.class);

	private Hashtable<String, EventCMT> eventCMTs = null;

	private static final double PERIOD1 = 25;
	private static final double PERIOD2 = 20;
	private static final double PERIOD3 = 4;
	private static final double PERIOD4 = 2;

	private static final double FREQUENCY1 = 1. / PERIOD1;
	private static final double FREQUENCY2 = 1. / PERIOD2;
	private static final double FREQUENCY3 = 1. / PERIOD3;
	private static final double FREQUENCY4 = 1. / PERIOD4;
	
	public EventCompareStrongMotion() {
		super();
		addArgument("base-channel");
		addArgument("channel-restriction");
	}

	@Override
	public long getVersion() {
		return 2;
	}

	@Override
	public String getName() {
		return "EventCompareStrongMotion";
	}

	@Override
	public void process() {
		logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());
		eventCMTs = getEventTable();
		
		if (eventCMTs == null) {
			logger.info("No Event CMTs found for Day=[{}] --> Skip EventCompareStrongMotion Metric", getDay());
			return;
		}

		List<Channel> channels = stationMeta.getRotatableChannels();
		String[] basechannel;
		String basePreSplit = null;
		List<String> bands;
		String preSplitBands = null;
		try {
			basePreSplit = get("base-channel");

		} catch (NoSuchFieldException ignored) {
		}
		try {
			preSplitBands = get("channel-restriction");
		} catch (NoSuchFieldException ignored) {
		}
		
		if (basePreSplit == null) {
			basePreSplit = "20-LN";
			logger.info("No base channel for EventCompare Strong Motion using: " + basePreSplit);
		}
		if (preSplitBands == null) {
			preSplitBands = "LH";
			logger.info("No band restriction set for EventCompare Strong Motion using: " + preSplitBands);
		}

		bands = Arrays.asList(preSplitBands.split(","));
		basechannel = basePreSplit.split("-");

		// Check for basechannel actually existing.
		if (!weHaveChannels(basechannel[0], basechannel[1])) {
			logger.info("Base channel not present. Skipping day. " + basePreSplit);
			return;
		}

		for (Channel channel : channels) {
			String channelLoc = channel.toString().split("-")[0];
			String channelVal = channel.toString().split("-")[1];
			if (bands.contains(channelVal.substring(0, 2)) && !channelLoc.equals(basechannel[0])) {
				/*
				 * basechannel[1].substring(0,2) +channelVal.substring(2) => LN
				 * + ED, LN + Z, etc
				 */
				Channel baseChannel = new Channel(basechannel[0],
						basechannel[1].substring(0, 2) + channelVal.substring(2));
				Channel curChannel = new Channel(channelLoc, channelVal);

				ChannelArray channelArray = new ChannelArray(baseChannel, curChannel);

				//Rotate channels as needed.
				metricData.checkForRotatedChannels(channelArray);
				if (metricData.getNextMetricData() != null) {
					metricData.getNextMetricData().checkForRotatedChannels(channelArray);
				}
				
				ByteBuffer digest = metricData.valueDigestChanged(channelArray,
						createIdentifier(baseChannel, curChannel), getForceUpdate());
				if (digest == null)
					continue; // Skip since not out of date or missing.

				double srateX = metricData.getChannelData(baseChannel).get(0).getSampleRate();
				double srateY = metricData.getChannelData(curChannel).get(0).getSampleRate();

				double result = 0;
				boolean correlated = false;

				if (srateX == srateY) {
					int nEvents = 0;
					// Loop over Events for this day
					try { // getFilteredDisplacement
						SortedSet<String> eventKeys = new TreeSet<>(eventCMTs.keySet());
						for (String key : eventKeys) {

							EventCMT eventCMT = eventCMTs.get(key);

							/*
							 * Window the data from the Event (PDE) Origin. Use
							 * larger time window to do the instrument decons
							 * and trim it down later:
							 */

							long duration = 8000000L; // 8000 sec = 8000000
														// msecs
							/*
							 * Event origin epoch time in millisecs
							 */
							long eventStartTime = eventCMT.getTimeInMillis();
							long eventEndTime = eventStartTime + duration;

							/*
							 * Use P and S arrival times to trim the window down
							 * for comparison:
							 */
							double[] arrivalTimes = getEventArrivalTimes(eventCMT);
							if (arrivalTimes == null) {
								logger.info(
										"== {}: arrivalTimes==null for stn=[{}] day=[{}]: Distance to stn probably > 97-deg --> Don't compute metric\n",
										getName(), getStation(), getDay());
								continue;
							}

							// P - 120 sec
							int nstart = (int) (arrivalTimes[0] - 120.);
							// S + 60 sec
							int nend = (int) (arrivalTimes[1] + 60.);
							if (nstart < 0) {
								nstart = 0;
							}

							ResponseUnits units = ResponseUnits.DISPLACEMENT;

							double[] baseData = metricData.getFilteredDisplacement(units, baseChannel, eventStartTime,
									eventEndTime, FREQUENCY1, FREQUENCY2, FREQUENCY3, FREQUENCY4);
							double[] channelData = metricData.getFilteredDisplacement(units, curChannel, eventStartTime,
									eventEndTime, FREQUENCY1, FREQUENCY2, FREQUENCY3, FREQUENCY4);

							if(baseData == null || channelData == null){
								//Not enough data to compute skip this event
								continue;
							}
							double corrVal = getCorr(channelData, baseData, nstart, nend);
							if (Math.abs(corrVal) >= 0.85) {
								correlated = true;
								result += scaleFac(channelData, baseData, nstart, nend);
								nEvents++;
							}
						}
						if (correlated) {
							metricResult.addResult(curChannel, baseChannel, result / nEvents, digest);
						} else {
							logger.info("station=[{}] day=[{}]: Low correlation", getStation(), getDay());
						}

					} catch (ChannelMetaException e) {
						logger.error("ChannelMetaException:", e);
					} catch (MetricException e) {
						logger.error("MetricException:", e);
					}
				}
			}

		}
	}

	private double scaleFac(double[] data1, double[] data2, int n1, int n2) {
		// if n1 < n2 or nend < data.length ...
		double numerator = 0.;
		double denominator = 0.;
		for (int i = n1; i < n2; i++) {
			numerator += (data1[i] * data2[i]);
			denominator += (data1[i] * data1[i]);
		}
		if (denominator == 0.) {
			logger.error(
					"station=[{}] day=[{}]: scaleFac: denominator==0 --> Divide by 0 --> Expect result = Infinity!",
					getStation(), getDay());
		}

		double result = numerator / denominator;
		// If the result is too large cap it at 4.
		if (result >= 4.) {
			result = 4.;
		}
		if (result <= -4.) {
			result = -4.;
		}

		return result;
	}

	// NOTE: because n1, n2 are times in seconds, data needs to be at 1Hz sample rate for this to wk
	private double getCorr(double[] data1, double[] data2, int n1, int n2) {
		// This function computs the Pearson's correlation value for the two
		// time series
		if (n2 < n1) {
			logger.error("station=[{}] day=[{}]: calcDiff: n2 < n1 --> Bad window", getStation(), getDay());
			return NO_RESULT;
		}
		if (n2 >= data1.length || n2 >= data2.length) {
			logger.error(
					"station=[{}] day=[{}]: calcDiff: n2=[{}] > data1.length=[{}] and/or data2.length=[{}] --> Bad window",
					getStation(), getDay(), n2, data1.length, data2.length);
			return NO_RESULT;
		}

		// Calculate the mean of both data streams
		double data1mean = 0.;
		double data2mean = 0.;

		for (int i = n1; i < n2; i++) {
			data1mean += data1[i];
			data2mean += data2[i];

		}
		data1mean = data1mean / (double) data1.length;
		data2mean = data2mean / (double) data2.length;

		// Calculate the standard deviation of both data streams
		double std1 = 0.;
		double std2 = 0.;

		for (int i = n1; i < n2; i++) {
			std1 += (data1[i] - data1mean) * (data1[i] - data1mean);
			std2 += (data2[i] - data2mean) * (data2[i] - data2mean);

		}
		std1 = std1 / (double) data1.length;
		std2 = std2 / (double) data2.length;

		// Calculate the r correlation
		double r = 0.;
		for (int i = n1; i < n2; i++) {
			r += (data1[i] - data1mean) * (data2[i] - data2mean) / (std1 * std2);

		}
		r = r / (double) (data1.length - 1);

		return r;
	}

	private double[] getEventArrivalTimes(EventCMT eventCMT) {
		double evla = eventCMT.getLatitude();
		double evlo = eventCMT.getLongitude();
		double evdep = eventCMT.getDepth();
		double stla = stationMeta.getLatitude();
		double stlo = stationMeta.getLongitude();
		double gcarc = SphericalCoords.distance(evla, evlo, stla, stlo);
		double azim = SphericalCoords.azimuth(evla, evlo, stla, stlo);
		TauP_Time timeTool = null;
		try {
			timeTool = new TauP_Time("prem");
			timeTool.parsePhaseList("P,S");
			timeTool.setSourceDepth(evdep);
			timeTool.calculate(gcarc);
		} catch (TauModelException e) {
			logger.error(e.getMessage());
			return null; // Return null since arrival times are not
							// determinable.
		}

		List<Arrival> arrivals = timeTool.getArrivals();

		// We could screen by max distance (e.g., 90 deg for P direct)
		// or by counting arrivals (since you won't get a P arrival beyond about
		// 97 deg or so)
		if (arrivals.size() != 2) { // Either we don't have both P & S or we
									// don't have just P & S
			logger.info("Expected P and/or S arrival times not found [gcarc={}]", gcarc);
			return null;
		}

		double arrivalTimeP = 0.;
		if (arrivals.get(0).getName().equals("P")) {
			arrivalTimeP = arrivals.get(0).getTime();
		} else {
			logger.info("Expected P arrival time not found");
		}
		double arrivalTimeS = 0.;
		if (arrivals.get(1).getName().equals("S")) {
			arrivalTimeS = arrivals.get(1).getTime();
		} else {
			logger.info("Expected S arrival time not found");
		}

		logger.info(String.format(
				"Event:%s <evla,evlo> = <%.2f, %.2f> Station:%s <%.2f, %.2f> gcarc=%.2f azim=%.2f tP=%.3f tS=%.3f\n",
				eventCMT.getEventID(), evla, evlo, getStation(), stla, stlo, gcarc, azim, arrivalTimeP, arrivalTimeS));

		double[] arrivalTimes = new double[2];
		arrivalTimes[0] = arrivalTimeP;
		arrivalTimes[1] = arrivalTimeS;

		return arrivalTimes;
	}
}
