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
package asl.seedscan.metrics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import timeutils.Timeseries;
import asl.metadata.Channel;
import asl.metadata.Station;
import asl.seedscan.ArchivePath;
import asl.util.PlotMaker2;
import asl.util.PlotMakerException;
import asl.util.Trace;
import asl.util.TraceException;

public class StationDeviationMetric extends PowerBandMetric {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.metrics.StationDeviationMetric.class);

	@Override
	public long getVersion() {
		return 1;
	}

	@Override
	public String getBaseName() {
		return "StationDeviationMetric";
	}

	private double[] ModelPeriods;
	private double[] ModelPowers;
	private String ModelDir;

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
		ModelDir = pathEngine.makePath(pathPattern);

		// Get all LH channels in metadata
		List<Channel> channels = stationMeta.getChannelArray("LH");

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
				logger.warn(
						"Digest unchanged station:[{}] channel:[{}] day:[{}] --> Skip metric",
						getStation(), channel, getDay());
				continue;
			}

			try { // computeMetric() handle
				double result = computeMetric(channel);
				if (result == NO_RESULT) {
					// Metric computation failed --> do nothing
				} else {
					metricResult.addResult(channel, result, digest);
				}
			} catch (MetricException e) {
				logger.error("MetricException:", e);
			}
		}// end foreach channel

		if (getMakePlots() && (plotMaker != null)) { // If no station model
			// files were found
			// plotMaker still ==
			// null
			BasicStroke stroke = new BasicStroke(4.0f);
			for (int iPanel = 0; iPanel < 3; iPanel++) {
				try {
					plotMaker.addTraceToPanel(new Trace(ModelPeriods,
							ModelPowers, "StnModel", Color.black, stroke),
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

	private double computeMetric(Channel channel) throws MetricException {

		// Read in specific noise model for this station+channel //
		// ../ANMO.00.LH1.90
		String modelFileName = stationMeta.getStation() + "."
				+ channel.getLocation() + "." + channel.getChannel() + ".90";
		try {
			if (!readModel(modelFileName)) {
				logger.warn(String
						.format("ModelFile=%s not found for requested channel:%s day:%s --> Skipping\n",
								modelFileName, channel.getChannel(), getDay()));
				return NO_RESULT;
			}
		} catch (MetricException e) {
			logger.error("MetricException:", e);
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
		double psdInterp[] = Timeseries.interpolate(per, psdPer, ModelPeriods);

		PowerBand band = getPowerBand();
		double lowPeriod = band.getLow();
		double highPeriod = band.getHigh();

		if (!checkPowerBand(lowPeriod, highPeriod, Tmin, Tmax)) {
			logger.warn(String.format("powerBand Error: Skipping channel:%s day:%s\n",
					channel, getDay()));
			return NO_RESULT;
		}

		// Compute deviation from The Model within the requested period band:
		double deviation = 0;
		int nPeriods = 0;
		for (int k = 0; k < ModelPeriods.length; k++) {
			if (ModelPeriods[k] > highPeriod) {
				break;
			} else if (ModelPeriods[k] >= lowPeriod) {
				double difference = psdInterp[k] - ModelPowers[k];
				// deviation += Math.sqrt( Math.pow(difference, 2) );
				deviation += difference;
				nPeriods++;
			}
		}

		if (nPeriods == 0) {
			StringBuilder message = new StringBuilder();
			message.append(String
					.format("%s: Requested band [%f - %f] contains NO periods within station model\n",
							getDay(), lowPeriod, highPeriod));
			throw new MetricException(message.toString());
		}

		deviation = deviation / (double) nPeriods;

		if (getMakePlots()) {
			try {
				makePlots(channel, ModelPeriods, psdInterp);
			} catch (MetricException e) {
				logger.error("MetricException:", e);
			} catch (PlotMakerException e) {
				logger.error("PlotMakerException:", e);
			} catch (TraceException e) {
				logger.error("TraceException:", e);
			}
		}

		return deviation;
	} // end computeMetric()

	private void makePlots(Channel channel, double xdata[], double ydata[])
			throws MetricException, PlotMakerException, TraceException {
		if (xdata.length != ydata.length) {
			StringBuilder message = new StringBuilder();
			message.append(String.format(
					"makePlots() %s: xdata.len=%d != ydata.len=%d",
					getDay(), xdata.length, ydata.length));
			throw new MetricException(message.toString());
		}
		if (plotMaker == null) {
			String date = String.format("%04d%03d",
					metricResult.getDate().get(Calendar.YEAR), metricResult
					.getDate().get(Calendar.DAY_OF_YEAR));
			final String plotTitle = String.format(
					"[ Date: %s ] [ Station: %s ] Station-Deviation", date,
					getStation());
			plotMaker = new PlotMaker2(plotTitle);
			plotMaker.initialize3Panels("LHZ", "LH1/LHN", "LH2/LHE");
		}
		int iPanel;
		Color color = Color.black;
		BasicStroke stroke = new BasicStroke(2.0f);

		if (channel.getChannel().equals("LHZ")) {
			iPanel = 0;
		} else if (channel.getChannel().equals("LH1")
				|| channel.getChannel().equals("LHN")) {
			iPanel = 1;
		} else if (channel.getChannel().equals("LH2")
				|| channel.getChannel().equals("LHE")) {
			iPanel = 2;
		} else { // ??
			StringBuilder message = new StringBuilder();
			message.append(String.format(
					"makePlots() %s: Don't know how to plot channel=%s",
					getDay(), channel));
			throw new MetricException(message.toString());
		}

		if (channel.getLocation().equals("00")) {
			color = Color.green;
		} else if (channel.getLocation().equals("10")) {
			color = Color.red;
		} else { // ??
		}

		try {
			plotMaker.addTraceToPanel(
					new Trace(xdata, ydata, channel.toString(), color, stroke),
					iPanel);
		} catch (PlotMakerException e) {
			throw e;
		} catch (TraceException e) {
			throw e;
		}
	}

	private boolean readModel(String fName) throws MetricException {

		// ../stationmodel/IU_ANMO/ANMO.00.LHZ.90
		String fileName = ModelDir + fName;

		// First see if the file exists
		if (!(new File(fileName).exists())) {
			// System.out.format("=== %s: ModelFile=%s does NOT exist!\n",
			// getName(), fileName);
			StringBuilder message = new StringBuilder();
			message.append(String.format(
					"== ModelFile=%s does NOT exist!\n", fileName));
			logger.warn(message.toString());
			return false;
		}
		// Temp ArrayList(s) to read in unknown number of (x,y) pairs:
		ArrayList<Double> tmpPers = new ArrayList<Double>();
		ArrayList<Double> tmpPows = new ArrayList<Double>();
		BufferedReader br = null;
		try {
			String line;
			br = new BufferedReader(new FileReader(fileName));
			while ((line = br.readLine()) != null) {
				String[] args = line.trim().split("\\s+");
				// MTH: This is hard-wired for Adam's station model files which
				// have 7 columns:
				if (args.length != 7) {
					String message = "== reading Station Model File: got "
							+ args.length + " args on one line!";
					throw new MetricException(message.toString());
				}
				try {
					tmpPers.add(Double.valueOf(args[0].trim()).doubleValue());
					tmpPows.add(Double.valueOf(args[2].trim()).doubleValue());
				} catch (NumberFormatException e) {
					StringBuilder message = new StringBuilder();
					message.append(String
							.format("== %s: Error reading modelFile=[%s]: \n",
									getDay(), fName));
					logger.error(message.toString(), e);
					return false;
				}
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
		Double[] tmpPeriods = tmpPers.toArray(new Double[] {});
		Double[] tmpPowers = tmpPows.toArray(new Double[] {});

		ModelPeriods = new double[tmpPeriods.length];
		ModelPowers = new double[tmpPowers.length];

		for (int i = 0; i < tmpPeriods.length; i++) {
			ModelPeriods[i] = tmpPeriods[i];
			ModelPowers[i] = tmpPowers[i];
		}

		return true;
	} // end readModel
} // end class
