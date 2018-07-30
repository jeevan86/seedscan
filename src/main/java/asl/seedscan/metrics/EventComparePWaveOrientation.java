package asl.seedscan.metrics;

import asl.metadata.Channel;
import asl.seedscan.event.EventCMT;
import asl.timeseries.TimeseriesUtils;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sac.SacHeader;
import sac.SacTimeSeries;
import uk.me.berndporr.iirj.Butterworth;

public class EventComparePWaveOrientation extends Metric {

    private static final Logger logger =
            LoggerFactory.getLogger(asl.seedscan.metrics.EventComparePWaveOrientation.class);

    // length of window to take for p-wave data in seconds
    private static final int P_WAVE_WINDOW = 15;

    // range of degrees (arc length) over which data will be valid
    private static final int MIN_DEGREES = 20;
    private static final int MAX_DEGREES = 90;
    /**
     * Filter corner 0.05 Hz or 20 seconds period.
     */
    private static final double LOW_PASS_FILTER_CORNER = 0.05;
    /**
     * 50 second window for p-wave ringing offset data
     */
    private static final int P_WAVE_RINGING_OFFSET = 50;

    EventComparePWaveOrientation() {
        super();
        addArgument("base-channel");
        addArgument("channel-restriction");
    }

    @Override
    public long getVersion() {
        return 1;
    }

    @Override
    public String getName() {
        return "EventComparePWaveOrientation";
    }

    @Override
    public void process() {
        logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

        // a bunch of this is copy-pasted from eventCompareSynthetic since it's the same thing
        Hashtable<String, EventCMT> eventCMTs = getEventTable();
        if (eventCMTs == null) {
            logger.info(
                    String.format("No Event CMTs found for Day=[%s] --> Skip EventComparePOrientation Metric",
                            getDay()));
            return;
        }

        List<Channel> channels = stationMeta.getRotatableChannels();

        // get pairs of ED, ND data and then do the rotation with those
        String[] basechannel;
        String basePreSplit = null;

        try {
            basePreSplit = get("base-channel");
        } catch (NoSuchFieldException ignored) {
        }

        if (basePreSplit == null) {
            basePreSplit = "XX-LX";
            logger.info("No base channel for Event Compare P Orientation using: " + basePreSplit);
        }
        basechannel = basePreSplit.split("-");

        String preSplitBands = null;
        try {
            preSplitBands = get("channel-restriction");
        } catch (NoSuchFieldException ignored) {
        }
        if (preSplitBands == null) {
            preSplitBands = "LH";
            logger.info("No band restriction set for Event Compare P Orientation using: "
                    + preSplitBands);
        }
        List<String> bands;
        bands = Arrays.asList(preSplitBands.split(","));

        // get lat and long to make sure data is within a reasonable range
        double stationLatitude = stationMeta.getLatitude();
        double stationLongitude = stationMeta.getLongitude();

        // map used for quick access to paired channel data
        // we only do calculations once both N/E channel pairs (or ND/ED)
        // exist in the map
        Map<String, Channel> chNameMap = new HashMap<>();
        for (Channel curChannel : channels) {

            String name = curChannel.toString();
            chNameMap.put(name, curChannel);
            String channelVal = name.split("-")[1];
            if (!bands.contains(channelVal.substring(0, 2))) {
                // current channel not part of valid band options, skip it
                continue;
            }

            int lastCharIdx = channelVal.length() - 1;
            char last = channelVal.charAt(lastCharIdx);
            if (last == 'Z' || last == 'E') {
                // assume vertical sensor component requires no orientation
                // assume east sensor will be referenced when north is read in
                continue;
            } else if (last == 'D') {
                if (channelVal.charAt(lastCharIdx - 1) == 'E') {
                    // deal with, say, LHED (east-derived) when we read in LHND (north-derived)
                    continue;
                }
            }

            String pairName, vertName;
            try {
                pairName = getPairedChannelNameString(name);
                vertName = getVerticalChannelNameString(name);
            } catch (MetricException e) {
                logger.error("Error in format of channel name (expected E, N, or Z channel code.): " + name);
                continue;
            }

            if (!chNameMap.keySet().contains(pairName)) {
                logger.warn("Could not find data for station with name " + pairName);
                continue;
            }

            // this is the function that produces rotated data given the current chanel
            ByteBuffer digest = metricData.valueDigestChanged(curChannel, createIdentifier(curChannel),
                    getForceUpdate());

            Channel pairChannel = chNameMap.get(pairName);
            Channel vertChannel = chNameMap.get(vertName);
            System.out.println(vertName);
            double sampleRateN = stationMeta.getChannelMetadata(curChannel).getSampleRate();
            double sampleRateE = stationMeta.getChannelMetadata(pairChannel).getSampleRate();
            double sampleRateV = stationMeta.getChannelMetadata(vertChannel).getSampleRate();
            // now curChannel, pairChannel the two channels to get the orientation of
            // double azi = stationMeta.getChannelMetadata(curChannel).getAzimuth();

            // now we have the angle for which to rotate data

            // now to get the synthetics data
            SortedSet<String> eventKeys = new TreeSet<>(eventCMTs.keySet());
            for (String key : eventKeys) {

                EventCMT eventMeta = eventCMTs.get(key);
                double eventLatitude = eventMeta.getLatitude();
                double eventLongitude = eventMeta.getLongitude();
                // derived data has azimuth of 0 -- can use this w/ station coords to get azimuth relative to event
                double azimuth = SphericalCoords.azimuth(stationLatitude, stationLongitude, eventLatitude, eventLongitude);
                azimuth = (360 + azimuth) % 360;
                // SphericalCoords.azimuth(eventLatitude, eventLongitude, stationLatitude, stationLongitude);
                System.out.println(azimuth);
                double angleBetween = SphericalCoords.distance(eventLatitude, eventLongitude, stationLatitude, stationLongitude);
                if (angleBetween < MIN_DEGREES || angleBetween > MAX_DEGREES) {
                    logger.info("== {}: Arc length ({}) to key=[{}] out of range for this station\n",
                            getName(), angleBetween, key);
                    continue;
                }

                Hashtable<String, SacTimeSeries> synthetics = getEventSynthetics(key);
                if (synthetics == null) {
                    logger.warn("== {}: No synthetics found for key=[{}] for this station\n", getName(), key);
                    continue;
                }

                SacTimeSeries sacSynthetics;
                String fileKey = getStn() + "." + basechannel[0] + "." + basechannel[1].substring(0, 2)
                        + channelVal.substring(2, 3) + ".modes.sac.proc";
                // e.g. "ANMO.XX.LXZ.modes.sac.proc"
                if (synthetics.containsKey(fileKey)) {
                    sacSynthetics = synthetics.get(fileKey);
                } else {
                    logger.warn("Did not find sac synthetic=[{}] in Hashtable", fileKey);
                    continue; // Try next event
                }

                // get start time of p-wave, then take data 100 secs before that
                SacHeader header = sacSynthetics.getHeader();
                long stationDataStartTime = getSacStartTimeInMillis(header);
                try {
                    long pTravelTime = getPArrivalTime(eventMeta);
                    stationDataStartTime += pTravelTime;
                } catch (ArrivalTimeException ignore) {
                    // error was already logged in getPArrivalTime
                    continue;
                }

                // ending of p-wave is this length of time afterward
                // add 100 seconds on each end to compensate for potential filter ringing later
                // (after filtering we will trim the ringing from each side)
                long stationEventEndTime = stationDataStartTime + (P_WAVE_WINDOW + P_WAVE_RINGING_OFFSET) * 1000;
                // set window start back by 100 seconds (units in ms here) plus 50s ring-compensation offset
                stationDataStartTime -= (100 + P_WAVE_RINGING_OFFSET) * 1000; // (100 + X) sec * 1000 ms/sec

                double[] northData = metricData.getWindowedData(curChannel, stationDataStartTime,
                        stationEventEndTime);
                double[] eastData = metricData.getWindowedData(pairChannel, stationDataStartTime,
                        stationEventEndTime);
                double[] vertData = metricData.getWindowedData(vertChannel, stationDataStartTime,
                        stationEventEndTime);
                boolean dataMissing = false;
                if (null == northData) {
                    logger.error("== {}: {} attempt to get north-facing data returned nothing",
                            getName(), getStation());
                    dataMissing = true;
                }
                // separate conditionals for each to log if one or both pieces of data not gettable
                if (null == eastData) {
                    logger.error("== {}: {} attempt to get east-facing data returned nothing",
                            getName(), getStation());
                    dataMissing = true;
                }
                if (dataMissing) {
                    continue;
                }
                if (northData.length != eastData.length) {
                    logger.error("== {}: {} datasets of north & east not the same length!!",
                            getName(), getStation());
                    continue;
                }

                // filter, trim, remove mean
                northData = preprocess(northData, sampleRateN);
                eastData = preprocess(eastData, sampleRateE);
                vertData = preprocess(vertData, sampleRateV);

                normalize(northData, eastData);
                normalize(vertData, vertData);

                // evaluate signal-to-noise ratio of data (RMS)
                // noise is first 15 seconds of data (i.e., before p-wave arrival)
                int noiseLength = getSamplesInTimePeriod(P_WAVE_WINDOW, sampleRateN);
                // signal is last 15 seconds of data (i.e., after p-wave arrival);
                int signalOffset = northData.length - noiseLength;

                double snr = getSignalToNoiseRatioOfRMS(northData, eastData, signalOffset, noiseLength);
                final double SIGNAL_CUTOFF = 5.;
                if (snr < SIGNAL_CUTOFF) {
                    logger.warn("== {}: Signal to noise ratio under 5 -- ({}, {}): {}", getName(),
                            name, pairName, snr);
                    continue;
                }

                double linearity = calculateLinearity(northData, eastData, signalOffset);
                if (linearity < 0.95) {
                    logger.warn("== {}: Skipping; data linearity less than .95 -- [({} - {}) - {}]",
                            getName(), name, pairName, linearity);
                    continue;
                }

                double backAzimuth = calculateBackAzimuth(northData, eastData, azimuth, signalOffset);
                int signumN = (int) Math.signum(northData[signalOffset + 5]);
                int signumE = (int) Math.signum(eastData[signalOffset + 5]);
                int signumZ = (int) Math.signum(vertData[signalOffset + 5]);
                backAzimuth = correctBackAzimuthQuadrant(backAzimuth, signumN, signumE, signumZ);

                double angleDifference = (azimuth - backAzimuth) % 360;
                // add warning before publishing result if it's inconsistent with expected
                if (Math.abs(angleDifference) > 5) {
                    logger.warn("== {}: Difference btwn calc. and est. azimuth > 5 degrees -- "
                            + "[({} - {}) - ({} (calc) vs. {} (exp))]", getName(), name, pairName, backAzimuth, azimuth);
                }
                // now, populate the results from this data
                metricResult.addResult(curChannel, angleDifference, digest);
            }
        }
    }

    static String getVerticalChannelNameString(String name) {
        char[] chNameArray = name.toCharArray();
        int lastCharIdx = chNameArray.length - 1;
        char lastChar = chNameArray[lastCharIdx];
        if (lastChar == 'D') {
            --lastCharIdx;
            lastChar = chNameArray[lastCharIdx];
        }
        char pairChar = 'Z';
        chNameArray[lastCharIdx] = pairChar;
        // don't need vertical-derived channel
        return new String(Arrays.copyOfRange(chNameArray, 0, lastCharIdx+1));
    }

    private double[] preprocess(double[] data, double sampleRate) {
        // first, we low-pass filter the data
        // filter corner at 0.05Hz (20 s interval)
        // and use a 4 poles in the filter
        data = lowPassFilter(data, sampleRate, LOW_PASS_FILTER_CORNER);

        // assume there are filter artifacts in first 50 seconds' worth of data
        int afterRinging = getSamplesInTimePeriod(P_WAVE_RINGING_OFFSET, sampleRate);
        data = Arrays.copyOfRange(data, afterRinging, data.length - afterRinging);

        // detrend operations are done in-place
        TimeseriesUtils.demean(data);

        return data;

    }

    private void normalize(double[] northData, double[] eastData) {
        // now normalize -- both sets of data by the largest abs value in either
        double maxNorth = northData[0];
        double maxEast = eastData[0];
        // get min and max values for scaling to (-1, 1)
        for (int i = 0; i < northData.length; ++i) {
            double testNorth = Math.abs(northData[i]);
            double testEast = Math.abs(eastData[i]);
            maxNorth = Math.max(maxNorth, testNorth);
            maxEast = Math.max(maxEast, testEast);
        }
        // now scale data by maximum value of all components
        double scaleFactor = Math.max(maxNorth, maxEast);
        for (int i = 0; i < northData.length; ++i) {
            northData[i] /= scaleFactor;
            eastData[i] /= scaleFactor;
        }
    }

    private double calculateLinearity(double[] northData, double[] eastData, int signalOffset) {
        double sumNN = 0., sumEN = 0., sumEE = 0.;
        for (int i = signalOffset; i < northData.length; ++i) {
            sumNN += northData[i] * northData[i];
            sumEE += eastData[i] * eastData[i];
            sumEN += eastData[i] * northData[i];
        }
        RealMatrix mat = new BlockRealMatrix(new double[][]{{sumNN, sumEN}, {sumEN, sumEE}});
        EigenDecomposition eigenDecomposition = new EigenDecomposition(mat);
        RealMatrix eigenValues = eigenDecomposition.getD();
        return (eigenValues.getEntry(0, 0) / eigenValues.getTrace()) -
                (eigenValues.getEntry(1, 1) / eigenValues.getTrace());
    }

    private double getSignalToNoiseRatioOfRMS(double[] northData, double[] eastData, int signalOffset, int noiseLength) {
        double signalNorth = 0., noiseNorth = 0., signalEast = 0., noiseEast = 0.;
        for (int i = 0; i < noiseLength; ++i) {
            signalNorth += Math.pow(northData[signalOffset + i], 2);
            noiseNorth += Math.pow(northData[i], 2);
            signalEast += Math.pow(eastData[signalOffset + i], 2);
            noiseEast += Math.pow(eastData[i], 2);
        }
        signalNorth /= noiseLength;
        signalEast /= noiseLength;
        noiseNorth /= noiseLength;
        noiseEast /= noiseLength;

        signalNorth = Math.sqrt(signalNorth);
        signalEast = Math.sqrt(signalEast);
        noiseNorth = Math.sqrt(noiseNorth);
        noiseEast = Math.sqrt(noiseEast);

        return (signalNorth + signalEast) / (noiseNorth + noiseEast);
    }

    private double calculateBackAzimuth(double[] north, double[] east, double evtAzimuth, int signalOffset) {

        // we don'fintt care a but the intercept, only the slope
        SimpleRegression slopeCalculation = new SimpleRegression(false);
        for (int i = signalOffset; i < north.length; ++i) {
            slopeCalculation.addData(east[i], north[i]);
        }
        double backAzimuth = Math.atan(1. / slopeCalculation.getSlope());
        backAzimuth = 360 + Math.toDegrees(backAzimuth);

        return backAzimuth;

    }

    double correctBackAzimuthQuadrant(double azimuth, int signumN, int signumE, int signumZ) {
        double correctedAzimuth = ((azimuth % 360) + 360) % 360;
        double minValue = 0;
        double maxValue = 360;
        if (signumN == signumE && signumN != signumZ) {
            maxValue = 90;
        } else if (signumN == signumZ && signumN != signumE) {
            minValue = 90;
            maxValue = 180;
        } else if (signumN == signumZ) {
            // signum E must be in phase to reach this point
            minValue = 180;
            maxValue = 270;
        } else {
            minValue = 270;
        }
        while (correctedAzimuth < minValue) {
            correctedAzimuth += 90;
        }
        while (correctedAzimuth > maxValue) {
            correctedAzimuth -= 90;
        }
        return correctedAzimuth % 360;
    }

    /**
     * @param secs       seconds
     * @param sampleRate sample rate in hertz
     * @return number samples in a given time period.
     */
    static int getSamplesInTimePeriod(int secs, double sampleRate) {
        // samples = time (s) * sampleRate (samp/s)
        return (int) Math.ceil(secs * sampleRate);
    }

    static String getPairedChannelNameString(String channelName) throws MetricException {
        char[] chNameArray = channelName.toCharArray();
        int lastCharIdx = chNameArray.length - 1;
        char lastChar = chNameArray[lastCharIdx];
        if (lastChar == 'D') {
            --lastCharIdx;
            lastChar = chNameArray[lastCharIdx];
        }
        char pairChar = getPairChar(lastChar);
        chNameArray[lastCharIdx] = pairChar;
        return new String(chNameArray);
    }

    private static char getPairChar(char orient) throws MetricException {
        // get N,E pairs (horizontal data) for orientation calculation
        switch (orient) {
            case 'N':
                return 'E';
            case 'E':
                return 'N';
            default:
                throw new MetricException("Error with channel orientation label format");
        }
    }

    private long getPArrivalTime(EventCMT eventCMT) throws ArrivalTimeException {

        double eventLatitude = eventCMT.getLatitude();
        double eventLongitude = eventCMT.getLongitude();
        double eventDepth = eventCMT.getDepth();
        double stationLatitude = stationMeta.getLatitude();
        double stationLongitude = stationMeta.getLongitude();
        double greatCircleArc = SphericalCoords.distance(eventLatitude, eventLongitude, stationLatitude, stationLongitude);
        TauP_Time timeTool;
        try {
            timeTool = new TauP_Time("prem");
            timeTool.parsePhaseList("P");
            timeTool.setSourceDepth(eventDepth);
            timeTool.calculate(greatCircleArc);
        } catch (TauModelException e) {
            //Arrival times are not determinable.
            logger.error(e.getMessage());
            throw new ArrivalTimeException(e.getMessage());
        }

        List<Arrival> arrivals = timeTool.getArrivals();

        double arrivalTimeP;
        if (arrivals.get(0).getName().equals("P")) {
            arrivalTimeP = arrivals.get(0).getTime();
        } else {
            logger.info("Got an arrival, but it was not a P-wave");
            throw new ArrivalTimeException("Arrival time found was not a P-wave");
        }

        logger.info(
                "Event:{} <eventLatitude,eventLongitude> = <{}, {}> Station:{} <{}, {}> greatCircleArc={} tP={}",
                eventCMT.getEventID(), eventLatitude, eventLongitude, getStation(), stationLatitude, stationLongitude, greatCircleArc, arrivalTimeP);

        return ((long) arrivalTimeP) * 1000; // get the arrival time in ms
    }

    /**
     * Gets the sac start time in millis.
     *
     * @param hdr the sac header
     * @return the sac start time in millis
     */
    private static long getSacStartTimeInMillis(SacHeader hdr) {
        GregorianCalendar gcal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        gcal.set(Calendar.YEAR, hdr.getNzyear());
        gcal.set(Calendar.DAY_OF_YEAR, hdr.getNzjday());
        gcal.set(Calendar.HOUR_OF_DAY, hdr.getNzhour());
        gcal.set(Calendar.MINUTE, hdr.getNzmin());
        gcal.set(Calendar.SECOND, hdr.getNzsec());
        gcal.set(Calendar.MILLISECOND, hdr.getNzmsec());

        return gcal.getTimeInMillis();
    }

    private class ArrivalTimeException extends Exception {
        private static final long serialVersionUID = 6851116640460104395L;

        ArrivalTimeException(String message) {
            super(message);
        }

    }

    private static double[] lowPassFilter(double[] toFilt, double sps, double corner) {
        Butterworth cascadeFilter = new Butterworth();
        cascadeFilter.lowPass(4, sps, corner);

        double[] filtered = new double[toFilt.length];
        for (int i = 0; i < toFilt.length; ++i) {
            filtered[i] = cascadeFilter.filter(toFilt[i]);
        }

        return filtered;
    }

}
