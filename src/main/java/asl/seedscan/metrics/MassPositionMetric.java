package asl.seedscan.metrics;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.PolynomialStage;
import asl.metadata.meta_new.ResponseStage;
import asl.seedsplitter.DataSet;

/**
 * The Class MassPositionMetric.
 * 
 * @author James Holland - USGS (jholland@usgs.gov)
 * @author Mike Hagerty
 * @author Alejandro Gonzalez - Honeywell
 * @author Joel Edwards - USGS
 */
public class MassPositionMetric extends Metric {

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.metrics.MassPositionMetric.class);
	
	MassPositionMetric(){
		super();
		addArgument("channel-restriction");
	}

	/**
	 * Computes the actual mass position metric.
	 * 
	 * @param channel
	 *            the channel
	 * @param station
	 *            the station
	 * @param day
	 *            the day
	 * @return the computed mass position value.
	 * @throws MetricException
	 *             thrown when a polynomial response is not formed correctly
	 * @throws UnsupportedEncodingException
	 *             thrown when the response is not a polynomial response
	 */
	private double computeMetric(Channel channel, String station, String day)
			throws MetricException, UnsupportedEncodingException {

		ChannelMeta chanMeta = this.stationMeta.getChannelMetadata(channel);
		List<DataSet> datasets = this.metricData.getChannelData(channel);

		double a0 = 0;
		double a1 = 0;
		double upperBound = 0;
		double lowerBound = 0;

		// Get Stage 1, make sure it is a Polynomial Stage (MacLaurin) and get
		// Coefficients
		// will add RuntimeException() to logger.error('msg', e)
		ResponseStage stage = chanMeta.getStage(1);
		if (!(stage instanceof PolynomialStage)) {
			StringBuilder message = new StringBuilder();
			message.append(String
					.format("station=[%s] channel=[%s] day=[%s]: Stage 1 is NOT a PolynomialStage--> Skip metric",
							station, channel.toString(), day));
			throw new UnsupportedEncodingException(message.toString());
		}
		PolynomialStage polyStage = (PolynomialStage) stage;
		double[] coefficients = polyStage.getRealPolynomialCoefficients();
		lowerBound = polyStage.getLowerApproximationBound();
		upperBound = polyStage.getUpperApproximationBound();

		// We're expecting a MacLaurin Polynomial with 2 coefficients (a0, a1)
		// to represent mass position
		if (coefficients.length != 2) {
			StringBuilder message = new StringBuilder();
			message.append(String
					.format("station=[%s] channel=[%s] day=[%s]: We're expecting 2 coefficients for this PolynomialStage!--> Skip metric",
							station, channel.toString(), day));
			throw new MetricException(message.toString());
		} else {
			a0 = coefficients[0];
			a1 = coefficients[1];
		}
		// Make sure we have enough ingredients to calculate something useful
		if (a0 == 0 && a1 == 0 || lowerBound == 0 && upperBound == 0) {
			StringBuilder message = new StringBuilder();
			message.append(String
					.format("station=[%s] channel=[%s] day=[%s]: We don't have enough information to compute mass position!--> Skip metric",
							station, channel.toString(), day));
			throw new MetricException(message.toString());
		}

		double massPosition = 0;
		int ndata = 0;

		for (DataSet dataset : datasets) {
			int intArray[] = dataset.getSeries();
			for (int j = 0; j < intArray.length; j++) {
				massPosition += Math.pow((a0 + intArray[j] * a1), 2);
			}
			ndata += dataset.getLength();
		} // end for each dataset

		massPosition = Math.sqrt(massPosition / ndata);

		double massRange = (upperBound - lowerBound) / 2;
		double massCenter = lowerBound + massRange;

		return 100. * Math.abs(massPosition - massCenter) / massRange;
	}

	/**
	 * @see asl.seedscan.metrics.Metric#getName()
	 */
	@Override
	public String getName() {
		return "MassPositionMetric";
	}

	/**
	 * @see asl.seedscan.metrics.Metric#getVersion()
	 */
	@Override
	public long getVersion() {
		return 1;
	}

	/**
	 * @see asl.seedscan.metrics.Metric#process()
	 */
	@Override
	public void process() {
		logger.info("-Enter- [ Station {} ] [ Day {} ]", this.getStation(),
				this.getDay());

		String station = this.getStation();
		String day = this.getDay();
		String bands = null;

		try {
			bands = get("channel-restriction");

		} catch (Exception ignored) {
		}
		
		if(bands == null){
			bands = "VM";
		}

		// Get all VM? channels in metadata to use for loop
		List<Channel> channels = this.stationMeta.getChannelArray(bands, false, true);

		// Loop over channels, get metadata & data for channel and Calculate
		// Metric
		for (Channel channel : channels) {
			if (!this.metricData.hasChannelData(channel)) {
				logger.warn(
						"No data found for channel:[{}] day:[{}] --> Skip metric",
						channel, day);
				continue;
			}

			ByteBuffer digest = this.metricData.valueDigestChanged(channel,
					this.createIdentifier(channel), this.getForceUpdate());

			if (digest == null) { // means oldDigest == newDigest and we don't
				// need to recompute the metric
				logger.info(
						"Digest unchanged station:[{}] channel:[{}] day:[{}] --> Skip metric",
						this.getStation(), channel, day);
				continue;
			}

			try {
				double result = this.computeMetric(channel, station, day);

				this.metricResult.addResult(channel, result, digest);
			} catch (MetricException e) {
				logger.error(e.getMessage());
			} catch (UnsupportedEncodingException e) {
				logger.warn(e.getMessage());
			}

		}
	}
}
