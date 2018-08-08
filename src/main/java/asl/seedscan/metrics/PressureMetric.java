package asl.seedscan.metrics;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.ResponseStage;
import asl.metadata.meta_new.PolynomialStage;
import asl.seedsplitter.DataSet;

/**
 * Pressure metric for atmospheric sensors
 */
public class PressureMetric extends Metric {

    private static final double CONSTANT_FACTOR = 2.25577E-5;
    private static final double CONSTANT_EXPONENT = 5.25588;

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.PressureMetric.class);

    public PressureMetric() {
        super();
        addArgument("channel-restriction");
    }

    @Override
    public long getVersion() {
        return 1;
    }

    @Override
    public String getName() {
        return "PressureMetric";
    }

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
            bands = "LD";
        }

        // Get all VM? channels in metadata to use for loop
        List<Channel> channels = this.stationMeta.getChannelArray(bands, false, true);
        for (Channel channel : channels) {
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

    /**
     * Computes the elevation-corrected RMS of the given channel's pressure metric.
     *
     * @param channel
     *            the channel
     * @param station
     *            the station
     * @param day
     *            the day
     * @return the computed corrected pressure value in atm (expected to be 1).
     * @throws MetricException
     *             thrown when a polynomial response is not formed correctly
     * @throws UnsupportedEncodingException
     *             thrown when the response is not a polynomial response
     */
    private double computeMetric(Channel channel, String station, String day)
            throws MetricException, UnsupportedEncodingException {

        ChannelMeta chanMeta = this.stationMeta.getChannelMetadata(channel);
        List<DataSet> datasets = this.metricData.getChannelData(channel);

        double elevation = chanMeta.getElevation();

        // Get Stage 0, make sure it is a Polynomial Stage (MacLaurin) and get
        // Coefficients
        // will add RuntimeException() to logger.error('msg', e)
        ResponseStage stage = chanMeta.getStage(0);
        if (!(stage instanceof PolynomialStage)) {
            throw new UnsupportedEncodingException(String
                    .format("station=[%s] channel=[%s] day=[%s]: Stage 0 is NOT a PolynomialStage--> Skip metric",
                            station, channel.toString(), day));
        }
        PolynomialStage polyStage = (PolynomialStage) stage;
        double[] coefficients = polyStage.getRealPolynomialCoefficients();

        // Make sure we have enough ingredients to calculate something useful (i.e., not everything is zero)
        boolean hasAtLeastOneNonzeroCoefficient = false;
        for (double coeff : coefficients) {
            hasAtLeastOneNonzeroCoefficient |= (coeff > 0);
        }
        if (!hasAtLeastOneNonzeroCoefficient) {
            throw new MetricException(String
                    .format("station=[%s] channel=[%s] day=[%s]: We don't have enough information to compute pressure!--> Skip metric",
                            station, channel.toString(), day));
        }

        double rmsValue = 0;
        int pointCount = 0;

        for (DataSet dataset : datasets) {
            int intArray[] = dataset.getSeries();
            for (int dataPoint : intArray) {
                double polynomialAccumulator = 0.; // a0 + a1 * x + a2 * x^2, etc.
                // we expect there to be only a0 and a1 but won't enforce this as a constraint
                for (int k = 0; k < coefficients.length; ++k) {
                    polynomialAccumulator += coefficients[k] * Math.pow(dataPoint, k);
                }
                rmsValue += Math.pow(polynomialAccumulator, 2);
            }
            pointCount += dataset.getLength();
        } // end for each dataset

        rmsValue = Math.sqrt(rmsValue / pointCount);
        // correct for elevation and then convert to atmospheres (assume resp in Pascals)
        return (rmsValue / Math.pow(1. - (CONSTANT_FACTOR * elevation), CONSTANT_EXPONENT)) / 101325.;
    }
}
