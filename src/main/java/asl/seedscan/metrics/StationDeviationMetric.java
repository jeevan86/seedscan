package asl.seedscan.metrics;

import asl.util.Logging;
import asl.utils.NumericUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.plotmaker.PlotMaker2;
import asl.plotmaker.PlotMakerException;
import asl.plotmaker.Trace;
import asl.plotmaker.TraceException;
import asl.seedscan.ArchivePath;
import asl.timeseries.CrossPower;

public class StationDeviationMetric extends PowerBandMetric {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.metrics.StationDeviationMetric.class);

	@Override
	public long getVersion() {
		return 2;
	}

	@Override
	public String getBaseName() {
		return "StationDeviationMetric";
	}

	private double[] modelPeriods;
	private double[] modelPowers;
	private String modelDirectory;

	private PlotMaker2 plotMaker = null;

	public StationDeviationMetric() {
		super();
		addArgument("modelpath");
	}

	public void process() {
		logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

		// Get the path to the station models that was read in from config.xml
		// <cfg:argument
		// cfg:name="modelpath">/Users/mth/mth/Projects/xs0/stationmodel/${NETWORK}_${STATION}/</cfg:argument>
		String pathPattern = null;
		try {
			pathPattern = get("modelpath");
		} catch (NoSuchFieldException ex) {
			logger.warn("Station Model Path ('modelpath') was not specified!");
			return; // Without the modelpath we can't compute the metric -->
			// return
		}
		ArchivePath pathEngine = new ArchivePath(new Station(
				stationMeta.getNetwork(), stationMeta.getStation()));
		modelDirectory = pathEngine.makePath(pathPattern);

		List<Channel> channels = stationMeta.getContinuousChannels();

		if (channels == null || channels.size() == 0) {
			System.out.format(
					"== %s: No LH? channels found for station=[%s] day=[%s]\n",
					getName(), getStation(), getDay());
			return;
		}

		// Loop over channels, get metadata & data for channel and Calculate
		// Metric

		for (Channel channel : channels) {
			if (!metricData.hasChannelData(channel)) {
				// logger.warn("No data found for channel[{}] --> Skip metric",
				// channel);
				continue;
			}

			ByteBuffer digest = metricData.valueDigestChanged(channel,
					createIdentifier(channel), getForceUpdate());

			if (digest == null) { // means oldDigest == newDigest and we don't
				// need to recompute the metric
				logger.info(
						"Digest unchanged station:[{}] channel:[{}] day:[{}] --> Skip metric",
						getStation(), channel, getDay());
				continue;
			}

			try { // computeMetric() handle
				double result = computeMetric(channel);
				if (result != NO_RESULT) {
					metricResult.addResult(channel, result, digest);
				}
			} catch (MetricException e) {
				logger.error(Logging.prettyExceptionWithCause(e));
			}
		}// end foreach channel

		if (getMakePlots() && (plotMaker != null)) { // If no station model
			// files were found
			// plotMaker still ==
			// null
			BasicStroke stroke = new BasicStroke(4.0f);
			for (int iPanel = 0; iPanel < 3; iPanel++) {
				try {
					plotMaker.addTraceToPanel(new Trace(modelPeriods,
							modelPowers, "StnModel", Color.black, stroke),
							iPanel);
				} catch (PlotMakerException e) {
					logger.error("PlotMakerException:", e);
				} catch (TraceException e) {
					logger.error("TraceException:", e);
				}
			}
			final String pngName = String.format("%s.%s.png", getOutputDir(),
					"stn-dev");
			plotMaker.writePlot(pngName);
		}
	} // end process()

	@Override
	public String getSimpleDescription() {
		return "Compares PSD frequency bands for this station to a station-specific noise model";
	}

	@Override
	public String getLongDescription() {
		return "Like the NLNM and ALNM deviation models, this metric takes the PSD of a full day's "
				+ "data and compares the named frequency range (period, in s) to a model of the station's "
				+ "noise characteristic. Unlike those metrics, this model is specific to the station.";
	}

	private double computeMetric(Channel channel) throws MetricException {

		// Read in specific noise model for this station+channel //
		// ../IU.ANMO.00.LH1.csv
		String modelFileName = stationMeta.getNetwork() + "." + stationMeta.getStation() + "."
				+ channel.getLocation() + "." + channel.getChannel() + ".csv";
		try {
			if (!readModel(modelFileName)) {
				logger.warn(String
						.format("ModelFile=%s not found for requested channel:%s day:%s --> Skipping\n",
								modelFileName, channel.getChannel(), getDay()));
				return NO_RESULT;
			}
		} catch (MetricException e) {
			logger.error(Logging.prettyExceptionWithCause(e));
		}

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

		// Interpolate the smoothed psd to the periods of the Station/Channel
		// Noise Model:
		double psdInterp[] = NumericUtils.interpolate(per, psdPer, modelPeriods);

		PowerBand band = getPowerBand();
		double lowPeriod = band.getLow();
		double highPeriod = band.getHigh();

		if (!checkPowerBand(lowPeriod, highPeriod, Tmin, Tmax)) {
			logger.warn(String.format(
					"powerBand Error: Skipping channel:%s day:%s\n", channel,
					getDay()));
			return NO_RESULT;
		}

		// Compute deviation from The Model within the requested period band:
		double deviation = 0;
		int nPeriods = 0;
		for (int k = 0; k < modelPeriods.length; k++) {
			if (modelPeriods[k] > highPeriod) {
				break;
			} else if (modelPeriods[k] >= lowPeriod) {
				double difference = psdInterp[k] - modelPowers[k];
				// deviation += Math.sqrt( Math.pow(difference, 2) );
				deviation += difference;
				nPeriods++;
			}
		}

		if (nPeriods == 0) {
			throw new MetricException(String
					.format("%s: Requested band [%f - %f] contains NO periods within station model\n",
							getDay(), lowPeriod, highPeriod));
		}

		deviation = deviation / (double) nPeriods;

		if (getMakePlots()) {
			try {
				makePlots(channel, modelPeriods, psdInterp);
			} catch (MetricException | TraceException | PlotMakerException e) {
				logger.error(Logging.prettyExceptionWithCause(e));
			}
		}

		return deviation;
	} // end computeMetric()

	private void makePlots(Channel channel, double xdata[], double ydata[])
			throws MetricException, PlotMakerException, TraceException {
		if (xdata.length != ydata.length) {
			throw new MetricException(String.format(
					"makePlots() %s: xdata.len=%d != ydata.len=%d", getDay(),
					xdata.length, ydata.length));
		}
		if (plotMaker == null) {
			String date = String.format("%04d%03d",
					metricResult.getDate().getYear(), metricResult
							.getDate().getDayOfYear());
			final String plotTitle = String.format(
					"[ Date: %s ] [ Station: %s ] Station-Deviation", date,
					getStation());
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
				throw new MetricException(String.format(
						"makePlots() %s: Don't know how to plot channel=%s",
						getDay(), channel));
		}

		if (channel.getLocation().equals("00")) {
			color = Color.green;
		} else if (channel.getLocation().equals("10")) {
			color = Color.red;
		}

		plotMaker.addTraceToPanel(
				new Trace(xdata, ydata, channel.toString(), color, stroke),
				iPanel);

	}

	private boolean readModel(String fName) throws MetricException {

		// ../stationmodel/IU_ANMO/IU.ANMO.00.LHZ.csv
		String fileName = modelDirectory + fName;

		// First see if the file exists
		if (!(new File(fileName).exists())) {
			logger.warn("== ModelFile={} does NOT exist!",
					fileName);
			return false;
		}

		// This uses the following lines to get the old format for station models
		// which had percent as the first value and powers (mean) as second value
		// Temp linkedlist(s) to read in unknown number of (x,y) pairs with constant-time inserts:
		List<Double> tmpPers = new LinkedList<>();
		List<Double> tmpPows = new LinkedList<>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
			String line = br.readLine();
			String[] args = line.trim().split("\\s+");
			if (args.length != 5 && args.length != 7) {
				throw new MetricException("== reading Station Model File: got "
						+ args.length + " args on one line!");

			}
			// if this first line is a format description header skip it
			try {
				Double.valueOf(args[0].trim());
			} catch (NumberFormatException e) {
				// skip to the next line, this first one has no value
				line = br.readLine();
			}

			do {
				args = line.trim().split(",\\s+");
				// hard-wired for new format has only 5 columns (percent, mean, median, 10th, 90th)
				if (args.length == 5) {
					try {
						tmpPers.add(Double.valueOf(args[0].trim()));
						tmpPows.add(Double.valueOf(args[1].trim()));
					} catch (NumberFormatException e) {
						logger.error(String.format(
								"== %s: Error reading modelFile=[%s]: \n",
								getDay(), fName), e);
						return false;
					}
				}

				else {
					throw new MetricException("== reading Station Model File: got "
							+ args.length + " args on one line!");
				}
			} while((line = br.readLine()) != null);

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

		modelPeriods = new double[tmpPers.size()];
		modelPowers = new double[tmpPers.size()];

		for (int i = 0; i < tmpPers.size(); i++) {
			modelPeriods[i] = tmpPers.get(i);
			modelPowers[i] = tmpPows.get(i);
		}

		return true;
	} // end readModel
} // end class
