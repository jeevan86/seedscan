package asl.seedscan.metrics;

import asl.util.Logging;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.plotmaker.PlotMaker2;
import asl.plotmaker.PlotMakerException;
import asl.plotmaker.Trace;
import asl.plotmaker.TraceException;
import asl.timeseries.CrossPower;
import asl.timeseries.TimeseriesUtils;

/**
 * NLNMDeviationMetric - Compute Difference (over specified range of periods =
 * powerband) between the power spectral density (psd) of a channel and the
 * NLNM.
 */
public class NLNMDeviationMetric extends PowerBandMetric {

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.NLNMDeviationMetric.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see asl.seedscan.metrics.Metric#getVersion()
	 */
	@Override
	public long getVersion() {
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see asl.seedscan.metrics.PowerBandMetric#getBaseName()
	 */
	@Override
	public String getBaseName() {
		return "NLNMDeviationMetric";
	}

	/**
	 * Instantiates a new NLNM deviation metric.
	 */
	public NLNMDeviationMetric() {
		super();
		addArgument("nlnm-modelfile");
		addArgument("nhnm-modelfile");
		addArgument("channel-restriction");
	}

	/** The Constant DEFAULT_NLNM_PATH. */
	private static final String DEFAULT_NLNM_PATH = "/noiseModels/NLNM.ascii";

	/** The Constant DEFAULT_NHNM_PATH. */
	private static final String DEFAULT_NHNM_PATH = "/noiseModels/NHNM.ascii";

	/** The new low noise model file. */
	private static URL NLNMFile;

	/** The new high noise model file. */
	private static URL NHNMFile;

	/** The new low noise model. */
	private static NoiseModel NLNM;

	/** The new high noise model. */
	private static NoiseModel NHNM;

	/** The plot maker. */
	private PlotMaker2 plotMaker = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see asl.seedscan.metrics.Metric#process()
	 */
	public void process() {
		logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

		String station = getStation();
		String day = getDay();
		String metric = getName();
		String bands = null;

		try {

			String nlnmPath = get("nlnm-modelfile");
			String nhnmPath = get("nhnm-modelfile");
			bands = get("channel-restriction");

			if (nlnmPath == null) {
				NLNMFile = this.getClass().getResource(DEFAULT_NLNM_PATH);
			} else {
				NLNMFile = new File(get("nlnm-modelfile")).toURI().toURL();
			}

			if (nhnmPath == null) {
				NHNMFile = this.getClass().getResource(DEFAULT_NHNM_PATH);
			} else {
				NHNMFile = new File(get("nhnm-modelfile")).toURI().toURL();
			}
			
			if(bands == null){
				bands = "LH,BH,HH";
			}

		}  catch (NoSuchFieldException | MalformedURLException e) {
			logger.error("station=[{}] day=[{}]: Failed to load a noise model file", station, day);
		}

		// Low noise model (NLNM) MUST exist or we can't compute the metric
		// (NHNM is optional)
		if (!getNLNM().isValid()) {
			logger.warn("Low Noise Model (NLNM) does NOT exist day=[{}] --> Skip Metric", day);
			return;
		}

		// Get all LH channels in metadata
		List<Channel> channels = stationMeta.getChannelArray(bands, true, true);

		if (channels == null || channels.size() == 0) {
			logger.warn("No channels found for station={} day={}", getStation(), day);
			return;
		}

		// Loop over channels, get metadata & data for channel and Calculate
		// Metric

		for (Channel channel : channels) {
			if (!metricData.hasChannelData(channel)) {
				logger.warn("No data found for channel:[{}] day:[{}] --> Skip metric", channel, day);
				continue;
			}
			PowerBand band = getPowerBand();
			double highFrequency = 1 / band.getLow();
			
			//Skip if channel not sampling above nyquist.
			if(stationMeta.getChannelMetadata(channel).getSampleRate() < (2 * highFrequency)){
				continue;
			}

			ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel), getForceUpdate());

			if (digest == null) { // means oldDigest == newDigest and we don't
									// need to recompute the metric
				logger.info("Digest unchanged station:[{}] channel:[{}] day:[{}] --> Skip metric", getStation(),
						channel, day);
				continue;
			}

			try { // computeMetric() MetricException handle
				double result = computeMetric(channel, station, day, metric);
				if (result != NO_RESULT) {
					metricResult.addResult(channel, result, digest);
				}
			} catch (MetricException e) {
				logger.error(Logging.prettyExceptionWithCause(e));
			}

		} // end foreach channel

		// If we didn't add any channel-panels below, then plotMaker should
		// still be null
		if (getMakePlots() && (plotMaker != null)) {
			BasicStroke stroke = new BasicStroke(4.0f);
			for (int iPanel = 0; iPanel < 3; iPanel++) {
				try {
					plotMaker.addTraceToPanel(
							new Trace(getNLNM().getPeriods(), getNLNM().getPowers(), "NLNM", Color.black, stroke),
							iPanel);
					if (getNHNM().isValid()) {
						plotMaker.addTraceToPanel(
								new Trace(getNHNM().getPeriods(), getNHNM().getPowers(), "NHNM", Color.black, stroke),
								iPanel);
					}
				} catch (PlotMakerException e) {
					logger.error("PlotMakerException:", e);
				} catch (TraceException e) {
					logger.error("TraceException:", e);
				}
			}
			// outputs/2012160.IU_ANMO.nlnm-dev.png
			final String pngName = String.format("%s.%s.png", getOutputDir(), "nlnm-dev");
			plotMaker.writePlot(pngName);
		}
	} // end process()

	/**
	 * Compute NLNM Deviation metric.
	 *
	 * @param channel
	 *            the channel
	 * @param station
	 *            the station
	 * @param day
	 *            the day
	 * @param metric
	 *            the metric
	 * @return the double
	 * @throws MetricException
	 *             the metric exception
	 */
	private double computeMetric(Channel channel, String station, String day, String metric) throws MetricException {

		// Compute/Get the 1-sided psd[f] using Peterson's algorithm (24 hrs, 13
		// segments, etc.)

		CrossPower crossPower = getCrossPower(channel, channel);
		double[] psd = crossPower.getSpectrum();
		double df = crossPower.getSpectrumDeltaF();

		// nf = number of positive frequencies + DC (nf = nfft/2 + 1, [f: 0, df,
		// 2df, ...,nfft/2*df] )
		int nf = psd.length;
		double freq[] = new double[nf];

		// Fill freq array & Convert spectrum to dB
		for (int k = 0; k < nf; k++) {
			freq[k] = (double) k * df;
			psd[k] = 10. * Math.log10(psd[k]);
		}

		// Convert psd[f] to psd[T]
		// Reverse freq[] --> per[] where per[0]=shortest T and
		// per[nf-2]=longest T:

		double[] per = new double[nf];
		double[] psdPer = new double[nf];
		// per[nf-1] = 1/freq[0] = 1/0 = inf --> set manually:
		per[nf - 1] = 0;
		for (int k = 0; k < nf - 1; k++) {
			per[k] = 1. / freq[nf - k - 1];
			psdPer[k] = psd[nf - k - 1];
		}
		double Tmin = per[0]; // Should be = 1/fNyq = 2/fs = 0.1 for fs=20Hz
		double Tmax = per[nf - 2]; // Should be = 1/df = Ndt

		// Timeseries.timeoutXY(per, psdPer, outFile);

		// Interpolate the smoothed psd to the periods of the NLNM Model:
		double psdInterp[] = TimeseriesUtils.interpolate(per, psdPer, getNLNM().getPeriods());

		// outFile = channel.toString() + ".psd.Fsmooth.T.Interp";
		// Timeseries.timeoutXY(NLNMPeriods, psdInterp, outFile);

		PowerBand band = getPowerBand();
		double lowPeriod = band.getLow();
		double highPeriod = band.getHigh();

		if (!checkPowerBand(lowPeriod, highPeriod, Tmin, Tmax)) {
			logger.error("powerBand station=[{}] day=[{}]: Skipping channel:{}", station, day, channel);
			return NO_RESULT;
		}

		// Compute deviation from NLNM within the requested period band:
		double deviation = 0;
		int nPeriods = 0;
		for (int k = 0; k < getNLNM().getPeriods().length; k++) {
			if (getNLNM().getPeriods()[k] > highPeriod) {
				break;
			} else if (getNLNM().getPeriods()[k] >= lowPeriod) {
				double difference = psdInterp[k] - getNLNM().getPowers()[k];
				// deviation += Math.sqrt( Math.pow(difference, 2) );
				deviation += difference;
				nPeriods++;
			}
		}

		if (nPeriods == 0) {
			throw new MetricException(String.format(
					"station=[%s] day=[%s]: Requested band [%f - %f sec] contains NO periods within NLNM\n", station,
					day, lowPeriod, highPeriod));
		}
		deviation = deviation / (double) nPeriods;

		if (getMakePlots()) {
			try {
				makePlots(channel, day, getNLNM().getPeriods(), psdInterp);
			} catch (MetricException | TraceException | PlotMakerException e) {
				logger.error(Logging.prettyExceptionWithCause(e));
			}
		}

		return deviation;
	} // end computeMetric()

	/**
	 * Make plots.
	 *
	 * @param channel
	 *            the channel
	 * @param day
	 *            the day
	 * @param xdata
	 *            the xdata
	 * @param ydata
	 *            the ydata
	 * @throws MetricException
	 *             the metric exception
	 * @throws PlotMakerException
	 *             the plot maker exception
	 * @throws TraceException
	 *             the trace exception
	 */
	private void makePlots(Channel channel, String day, double xdata[], double ydata[])
			throws MetricException, PlotMakerException, TraceException {
		if (xdata.length != ydata.length) {
			throw new MetricException(String.format("day=%s makePlots(): xdata.len=%d != ydata.len=%d", day, xdata.length, ydata.length));
		}
		if (plotMaker == null) {
			String date = String.format("%04d%03d", metricResult.getDate().getYear(),
					metricResult.getDate().getDayOfYear());
			final String plotTitle = String.format("[ Date: %s ] [ Station: %s ] NLNM-Deviation", date, getStation());
			plotMaker = new PlotMaker2(plotTitle);
			plotMaker.initialize3Panels("LHZ", "LH1/LHN", "LH2/LHE");
		}
		int iPanel;
		Color color = Color.black;
		BasicStroke stroke = new BasicStroke(2.0f);

		switch (channel.getChannel()) {
			case "LHZ":
				iPanel = 0;
				break;
			case "LH1":
			case "LHN":
				iPanel = 1;
				break;
			case "LH2":
			case "LHE":
				iPanel = 2;
				break;
			default:
				return;
		}

		if (channel.getLocation().equals("00")) {
			color = Color.green;
		} else if (channel.getLocation().equals("10")) {
			color = Color.red;
		}
		plotMaker.addTraceToPanel(new Trace(xdata, ydata, channel.toString(), color, stroke), iPanel);
	}

	/**
	 * Gets the new high NoiseModel. <br>
	 * If the NHNM is not already initialized it calls {@link #initNHNM()}
	 *
	 * @return the new high NoiseModel
	 */
	static synchronized NoiseModel getNHNM() {
		if (NHNM == null)
			initNHNM();
		return NHNM;
	}

	/**
	 * Initiates the new high NoiseModel.
	 */
	private synchronized static void initNHNM() {
		if (NHNM == null) {
			NHNM = new NoiseModel();
			try {
				readNoiseModel(NHNMFile, NHNM);
			} catch (MetricException e) {
				logger.error(Logging.prettyExceptionWithCause(e));
			}
		}
	}

	/**
	 * Gets the new low NoiseModel. <br>
	 * If the NLNM is not already initialized it calls {@link #initNLNM()}
	 *
	 * @return the new low NoiseModel
	 */
	static synchronized NoiseModel getNLNM() {
		if (NLNM == null)
			initNLNM();
		return NLNM;
	}

	/**
	 * Initiates the new low NoiseModel.
	 */
	private synchronized static void initNLNM() {
		if (NLNM == null) {
			NLNM = new NoiseModel();
			try {
				readNoiseModel(NLNMFile, NLNM);
			} catch (MetricException e) {
				logger.error(Logging.prettyExceptionWithCause(e));
			}
		}
	}

	/**
	 * Read in Peterson's NewLow(or High)NoiseModel from file specified in
	 * config.xml or a default file found in the jar.
	 *
	 * @param fileURL
	 *            the file url
	 * @param noiseModel
	 *            the NoiseModel
	 * @throws MetricException
	 *             if the noise model file is malformed, not exactly 2 values
	 *             per line.
	 */
	private synchronized static void readNoiseModel(URL fileURL, NoiseModel noiseModel) throws MetricException {
		logger.info("Read in Noise Model from file=[{}]", fileURL.toString());

		// Temporary ArrayList(s) to read in unknown number of (x,y) pairs:
		ArrayList<Double> tmpPers = new ArrayList<>();
		ArrayList<Double> tmpPows = new ArrayList<>();
		BufferedReader br = null;
		try {
			String line;
			br = new BufferedReader(new InputStreamReader(fileURL.openStream()));
			while ((line = br.readLine()) != null) {
				String[] args = line.trim().split("\\s+");
				if (args.length != 2) {
					throw new MetricException("==Error reading NLNM: got " + args.length + " args on one line!");
				}
				tmpPers.add(Double.valueOf(args[0].trim()));
				tmpPows.add(Double.valueOf(args[1].trim()));
			}
		} catch (IOException e) {
			logger.error("IOException:", e);
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				logger.error("IOException:", ex);
			}
		}
		Double[] modelPeriods = tmpPers.toArray(new Double[] {});
		Double[] modelPowers = tmpPows.toArray(new Double[] {});

		noiseModel.periods = new double[modelPeriods.length];
		noiseModel.powers = new double[modelPowers.length];

		for (int i = 0; i < modelPeriods.length; i++) {
			noiseModel.periods[i] = modelPeriods[i];
			noiseModel.powers[i] = modelPowers[i];
		}

		noiseModel.valid = true;
	}

	/**
	 * The Class NoiseModel.
	 */
	static class NoiseModel {

		/** The periods. */
		private double[] periods = null;

		/** The powers. */
		private double[] powers = null;

		/**
		 * True if valid, False if not. Set in
		 * {@link asl.seedscan.metrics.NLNMDeviationMetric#readNoiseModel(URL, NoiseModel)}
		 */
		private boolean valid = false;

		/**
		 * Gets the periods.
		 *
		 * @return the periods
		 */
		public double[] getPeriods() {
			return periods;
		}

		/**
		 * Gets the powers.
		 *
		 * @return the powers
		 */
		public double[] getPowers() {
			return powers;
		}

		/**
		 * Checks if the NoiseModel is valid.
		 *
		 * @return true, if is valid
		 */
		public boolean isValid() {
			return valid;
		}
	}
}
