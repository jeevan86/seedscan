package asl.seedscan.metrics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.plotmaker.PlotMaker2;
import asl.plotmaker.PlotMakerException;
import asl.plotmaker.Trace;
import asl.plotmaker.TraceException;
import asl.timeseries.CrossPower;

public class CoherencePBM extends PowerBandMetric {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.metrics.CoherencePBM.class);

	@Override
	public long getVersion() {
		return 1;
	}

	@Override
	public String getBaseName() {
		return "CoherencePBM";
	}

	private PlotMaker2 plotMaker = null;

	public CoherencePBM()
	{
		super();
		addArgument("base-channel");
	}
	
	public void process() {
		logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

		String station = getStation();
		String day = getDay();
		String metric = getName();
		// completeCompute tracks if all results are computed this affects
		// plotting.
		boolean completeCompute = true;

		List<Channel> channels = stationMeta.getRotatableChannels();
		String[] basechannel;
		String basePreSplit = null;
		try {
			basePreSplit = get("base-channel");
		} catch (NoSuchFieldException ignored) {
		}
		if(basePreSplit == null){
			basePreSplit = "00-LH";
			logger.info("No base channel for Coherence using: " + basePreSplit);
		}
		basechannel = basePreSplit.split("-");

		for(Channel channel : channels)
		{	
			String channelLoc = channel.toString().split("-")[0];
			String channelVal = channel.toString().split("-")[1];
			String regex = "^"+basechannel[1]+".*";
			if(channelVal.matches(regex) && !channelLoc.equals(basechannel[0]) && weHaveChannels(basechannel[0], basechannel[1]) && weHaveChannels(channelLoc, basechannel[1]))
			{
				Channel channelX = new Channel(basechannel[0], channelVal);
				Channel channelY = new Channel(channelLoc, channelVal);
				
				ChannelArray channelArray = new ChannelArray(channelX, channelY);

				ByteBuffer digest = metricData.valueDigestChanged(channelArray,
						createIdentifier(channelX, channelY), getForceUpdate());

				double srateX = metricData.getChannelData(channelX).get(0).getSampleRate();
				double srateY = metricData.getChannelData(channelY).get(0).getSampleRate();
				
				if(srateX == srateY)
				{
					if (digest == null) { // means oldDigest == newDigest and we don't
						// need to recompute the metric
						logger.info(
								"Digest unchanged station:[{}] day:[{}] channelX=[{}] channelY=[{}]--> Skip metric",
								getStation(), getDay(), channelX, channelY);
						completeCompute = false;
						continue;
					}
	
					try { // computeMetric (MetricException)
						double result = computeMetric(channelX, channelY, station, day,
								metric);
						if (result != NO_RESULT) {
							metricResult.addResult(channelX, channelY, result, digest);
						}
					} catch (MetricException e) {
						logger.error("MetricException:", e);
					} catch (PlotMakerException e) {
						logger.error("PlotMakerException:", e);
					} catch (TraceException e) {
						logger.error("TraceException:", e);
					}
	
				}
				else
				{
					logger.info("computePSD(): srateX = {} != srateY = {}", srateX, srateY);
					completeCompute = false; 
					continue; 
				}
	
				if (getMakePlots() && completeCompute) {
					final String pngName = String.format("%s.%s.png", getOutputDir(),
							"coher");
					plotMaker.writePlot(pngName);
				}
			}
		}
		
	} // end process()

	private double computeMetric(Channel channelX, Channel channelY,
			String station, String day, String metric) throws MetricException,
			PlotMakerException, TraceException {
		// Compute/Get the 1-sided psd[f] using Peterson's algorithm (24 hrs, 13
		// segments, etc.)

		CrossPower crossPower = getCrossPower(channelX, channelX);
		double[] Gxx = crossPower.getSpectrum();
		double dfX = crossPower.getSpectrumDeltaF();

		crossPower = getCrossPower(channelY, channelY);
		double[] Gyy = crossPower.getSpectrum();
		double dfY = crossPower.getSpectrumDeltaF();

		crossPower = getCrossPower(channelX, channelY);
		double[] Gxy = crossPower.getSpectrum();

		if (dfX != dfY) { // Oops - spectra have different frequency sampling!
			throw new MetricException(String
					.format("station=[%s] channelX[%s] channelY=[%s] day=[%s]: dfX != dfY --> Can't continue\n",
							station, channelX, channelY, day));
		}

		if (Gxx.length != Gyy.length || Gxx.length != Gxy.length) { // Something's
			// wrong ...
			throw new MetricException(String
					.format("station=[%s] channelX=[%s] channelY=[%s] day=[%s]: Gxx.length != Gyy.length --> Can't continue\n",
							station, channelX, channelY, day));
		}
		// nf = number of positive frequencies + DC (nf = nfft/2 + 1, [f: 0, df,
		// 2df, ...,nfft/2*df] )
		int nf = Gxx.length;
		double freq[] = new double[nf];
		double gamma[] = new double[nf];

		// Compute gamma[f] and fill freq array
		for (int k = 0; k < nf; k++) {
			freq[k] = (double) k * dfX;
			gamma[k] = (Gxy[k] * Gxy[k]) / (Gxx[k] * Gyy[k]);
			gamma[k] = Math.sqrt(gamma[k]);
		}
		gamma[0] = 0;
		// Timeseries.timeoutXY(freq, gamma, "Gamma");
		// Timeseries.timeoutXY(freq, Gxx, "Gxx");
		// Timeseries.timeoutXY(freq, Gyy, "Gyy");
		// Timeseries.timeoutXY(freq, Gxy, "Gxy");

		// Convert gamma[f] to gamma[T]
		// Reverse freq[] --> per[] where per[0]=shortest T and
		// per[nf-2]=longest T:

		double[] per = new double[nf];
		double[] gammaPer = new double[nf];

		// per[nf-1] = 1/freq[0] = 1/0 = inf --> set manually:
		per[nf - 1] = 0;
		for (int k = 0; k < nf - 1; k++) {
			per[k] = 1. / freq[nf - k - 1];
			gammaPer[k] = gamma[nf - k - 1];
		}
		double Tmin = per[0]; // Should be = 1/fNyq = 2/fs = 0.1 for fs=20Hz
		double Tmax = per[nf - 2]; // Should be = 1/df = Ndt

		PowerBand band = getPowerBand();
		double lowPeriod = band.getLow();
		double highPeriod = band.getHigh();

		if (!checkPowerBand(lowPeriod, highPeriod, Tmin, Tmax)) {
			logger.info(
					"{} powerBand Error: Skipping channel:{} day:{}",
					getName(), channelX, getDay());
			return NO_RESULT;
		}

		// Compute average Coherence within the requested period band:
		double averageValue = 0;
		int nPeriods = 0;
		for (int k = 0; k < per.length; k++) {
			if (per[k] > highPeriod) {
				break;
			} else if (per[k] >= lowPeriod) {
				averageValue += gammaPer[k];
				nPeriods++;
			}
		}

		if (nPeriods == 0) {
			throw new MetricException(String
					.format("station=[%s] channelX=[%s] channelY=[%s] day=[%s]: Requested band [%f - %f] contains NO periods --> divide by zero!\n",
							station, channelX, channelY, day, lowPeriod,
							highPeriod));
		}
		averageValue /= (double) nPeriods;

		if (getMakePlots()) { // Output files like 2012160.IU_ANMO.00-LHZ.png =
			// psd

			if (plotMaker == null) {
				String plotTitle = String.format("%04d%03d [ %s ] Coherence",
						metricResult.getDate().getYear(), metricResult
								.getDate().getDayOfYear(),
						metricResult.getStation());
				plotMaker = new PlotMaker2(plotTitle);
				plotMaker.initialize3Panels("LHZ", "LHND", "LHED");
			}
			int iPanel = 0;
			Color color = Color.red;

			BasicStroke stroke = new BasicStroke(2.0f);

			switch (channelX.getChannel()) {
				case "LHZ":
					iPanel = 0;
					break;
				case "LHND":
					iPanel = 1;
					break;
				case "LHED":
					iPanel = 2;
					break;
			}

			String channelLabel = MetricResult.createResultId(channelX,
					channelY);
			plotMaker.addTraceToPanel(new Trace(per, gammaPer,
						channelLabel, color, stroke), iPanel);
		}

		return averageValue;
	} // end computeMetric()
} // end class
