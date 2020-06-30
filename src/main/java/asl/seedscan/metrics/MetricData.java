package asl.seedscan.metrics;

import static asl.utils.FFTResult.cosineTaper;
import static asl.utils.FFTResult.singleSidedFFT;
import static asl.utils.NumericUtils.demeanInPlace;
import static asl.utils.NumericUtils.detrend;
import static asl.utils.TimeSeriesUtils.concatAll;

import asl.timeseries.PreprocessingUtils;
import asl.util.Logging;
import asl.utils.FFTResult;
import asl.utils.FilterUtils;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.ChannelException;
import asl.metadata.Station;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.ChannelMeta.ResponseUnits;
import asl.metadata.meta_new.ChannelMetaException;
import asl.metadata.meta_new.StationMeta;
import asl.security.MemberDigest;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.database.MetricValueIdentifier;
import asl.seedsplitter.BlockLocator;
import asl.seedsplitter.ContiguousBlock;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.IllegalSampleRateException;
import asl.seedsplitter.SequenceRangeException;
import asl.timeseries.TimeseriesException;
import asl.util.Time;
import seed.Blockette320;

/**
 * The Class MetricData. This class can be serialized for implementation in unit
 * tests.
 */
public class MetricData implements Serializable {

  /**
   * The Constant serialVersionUID.
   */
  private static final long serialVersionUID = 3L;

  /**
   * The Constant logger.
   */
  private static final Logger logger = LoggerFactory
      .getLogger(asl.seedscan.metrics.MetricData.class);

  /**
   * The data.
   */
  private Hashtable<String, ArrayList<DataSet>> data;

  /**
   * The quality data.
   */
  private Hashtable<String, ArrayList<Integer>> qualityData;

  /**
   * The random calibration.
   */
  private Hashtable<String, ArrayList<Blockette320>> randomCal;

  /**
   * The metadata.
   */
  private StationMeta metadata;

  /**
   * The metric reader.
   */
  private transient MetricDatabase metricReader;

  /**
   * Used exclusively in unit testing to plugin a reader after importing data from file
   *
   * @param metricReader the metricReader to set
   */
  protected void setMetricReader(MetricDatabase metricReader) { // NO_UCD (test only)
    this.metricReader = metricReader;
  }

  /**
   * The next metric data.
   */
  private transient MetricData nextMetricData;

  /**
   * Gets the next metric data.
   *
   * @return the next metric data
   */
  public MetricData getNextMetricData() {
    return nextMetricData;
  }

  /**
   * Sets the next metric data to null.
   */
  public void setNextMetricDataToNull() {
    this.nextMetricData = null;
  }

  /**
   * Instantiates a new metric data.
   *
   * @param metricReader the metric reader
   * @param data the data
   * @param qualityData the quality data
   * @param metadata the metadata
   * @param randomCal the random cal
   */
  public MetricData(MetricDatabase metricReader, Hashtable<String, ArrayList<DataSet>> data,
      Hashtable<String, ArrayList<Integer>> qualityData, StationMeta metadata,
      Hashtable<String, ArrayList<Blockette320>> randomCal) {
    this.metricReader = metricReader;
    this.data = data;
    this.qualityData = qualityData;
    this.randomCal = randomCal;
    this.metadata = metadata;
  }

  /**
   * Instantiates a new metric data.
   *
   * @param metricReader the metric reader
   * @param metadata the metadata
   */
  public MetricData(MetricDatabase metricReader, StationMeta metadata) {
    this.metadata = metadata;
    this.metricReader = metricReader;
  }

  /**
   * Gets the metadata.
   *
   * @return the metadata
   */
  public StationMeta getMetaData() {
    return metadata;
  }

  /**
   * Metadata setter for use in some test cases
   * @param metadata new metadata to load in for verification purposes
   */
  public void setMetadata(StationMeta metadata) {
    this.metadata = metadata;
  }

  /**
   * Checks for channels.
   * Only Z, 1, 2, N, E channels are checked.
   *
   * Channels such as VMU or LDO will return false.
   *
   * @param location the location code "00" or "35"
   * @param band the band in the form "LH" or "VH
   * @return true, if channel data exists, false if it does not.
   */
  boolean hasChannels(String location, String band) {
		/*
		  Not sure why this is here: if (!Channel.validLocationCode(location))
		  { return false; } if (!Channel.validBandCode(band.substring(0,1)) ||
		  !Channel.validInstrumentCode(band.substring(1,2)) ) { return false; }
		 */
    // First try kcmp = "Z", "1", "2"
    ChannelArray chanArray = new ChannelArray(location, band + "Z", band + "1", band + "2");
    if (hasChannelArrayData(chanArray)) {
      return true;
    }
    // Then try kcmp = "Z", "N", "E"
    chanArray = new ChannelArray(location, band + "Z", band + "N", band + "E");
    if (hasChannelArrayData(chanArray)) {
      return true;
    }
    // If we're here then we didn't find either combo --> return false
    return false;
  }

  /**
   * Checks for channel array data.
   *
   * @param channelArray the channel array
   * @return true, if successful
   */
  private boolean hasChannelArrayData(ChannelArray channelArray) {
    for (Channel channel : channelArray.getChannels()) {
      if (!hasChannelData(channel)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks for channel data.
   *
   * @param channel the channel
   * @return true, if successful
   */
  boolean hasChannelData(Channel channel) {
    return hasChannelData(channel.getLocation(), channel.getChannel());
  }

  /**
   * Checks for channel data.
   *
   * @param location the location
   * @param name the name
   * @return true, if successful
   */
  private boolean hasChannelData(String location, String name) {
    if (data == null) {
      return false;
    }
    String locationName = location + "-" + name;
    Set<String> keys = data.keySet();
    for (String key : keys) { // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
      if (key.contains(locationName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks for channel data. Same as above but only checks keys against
   * channel name (doesn't look at location)
   *
   * @param name the name
   * @return true, if successful
   */
  private boolean hasChannelData(String name) {
    if (data == null) {
      return false;
    }
    String locationName = "-" + name;
    Set<String> keys = data.keySet();
    for (String key : keys) { // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
      if (key.contains(locationName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the metric value.
   *
   * @param date the date
   * @param metricName the metric name
   * @param station the station
   * @param channel the channel
   * @return Double = metric value for given channel, station and date
   */
  Double getMetricValue(LocalDate date, String metricName, Station station, Channel channel) {
    // Retrieve metric value from Database
    if (metricReader.isConnected()) {
      return metricReader.getMetricValue(date, metricName, station, channel);
    } else {
      logger.warn("getMetricValue: Metric Reader is not connected");
      return null;
    }
  }

  /**
   * Gets the channel data.
   *
   * @param location the location
   * @param name the name
   * @return {@code ArrayList<DataSet>} = All DataSets for a given channel (e.g., "00-BHZ")
   */
  private ArrayList<DataSet> getChannelData(String location, String name) {
    String locationName = location + "-" + name;
    Set<String> keys = data.keySet();
    for (String key : keys) {
      // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
      if (key.contains(locationName)) {
        return data.get(key);
      }
    }
    return null;
  }

  /**
   * Gets the channel data.
   *
   * @param channel the channel
   * @return the channel data
   */
  public ArrayList<DataSet> getChannelData(Channel channel) {
    return getChannelData(channel.getLocation(), channel.getChannel());
  }

  /**
   * Note we don't rely on the metadata to contain any info about calibration
   * channels. We simply look for the presence of random calibration
   * blockettes (320's) for the IU stations, or miniseed channels like "LC0"
   * or "LC1" for the II stations.
   *
   * This hard codes
   *
   * @return true, if either the calibration blockette exists or Calibration channels exist
   */
  boolean hasCalibrationData() {
    if (randomCal != null && randomCal.size() > 0) {
      return true;
    } else if (metadata.getNetwork()
        .equals("II")) { //This hardcoded station needs to be address (Ticket 9727)
      if (hasChannelData("BC0") || hasChannelData("BC1") || hasChannelData("LC0") || hasChannelData(
          "LC1")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the channel cal data.
   *
   * @param channel the channel
   * @return the channel cal data
   */
  ArrayList<Blockette320> getChannelCalData(Channel channel) {
    return getChannelCalData(channel.getLocation(), channel.getChannel());
  }

  /**
   * Gets the channel cal data.
   *
   * @param location the location
   * @param name the name
   * @return the channel cal data
   */
  private ArrayList<Blockette320> getChannelCalData(String location, String name) {
    if (!hasCalibrationData()) {
      return null; // randomCal was never created --> Probably not a
    }
    // calibration day
    String locationName = location + "-" + name;
    Set<String> keys = randomCal.keySet();
    for (String key : keys) { // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
      if (key.contains(locationName)) {
        return randomCal.get(key);
      }
    }
    return null;
  }

  /**
   * Gets the channel timing quality data.
   *
   * @param channel the channel
   * @return the channel timing quality data
   */
  ArrayList<Integer> getChannelTimingQualityData(Channel channel) {
    return getChannelTimingQualityData(channel.getLocation(), channel.getChannel());
  }

  /**
   * Gets the channel timing quality data.
   *
   * @param location the location
   * @param name the name
   * @return the channel timing quality data
   */
  private ArrayList<Integer> getChannelTimingQualityData(String location, String name) {
    String locationName = location + "-" + name;
    Set<String> keys = qualityData.keySet();
    for (String key : keys) { // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
      if (key.contains(locationName)) {
        return qualityData.get(key);
      }
    }
    return null;
  }

  /**
   * Attach nextMetricData here for windows that span into next day
   *
   * @param nextMetricData the new next metric data
   */
  public void setNextMetricData(MetricData nextMetricData) {
    this.nextMetricData = nextMetricData;
  }

  /**
   * The name is a little misleading: getFilteredDisplacement will return
   * whatever output units are requested: DISPLACEMENT, VELOCITY,
   * ACCELERATION.
   *
   * @param responseUnits the response units
   * @param channel the channel
   * @param windowStartEpoch the window start epoch
   * @param windowEndEpoch the window end epoch
   * @param f1 the f1
   * @param f2 the f2
   * @param f3 the f3
   * @param f4 the f4
   * @return the filtered displacement
   * @throws ChannelMetaException the channel meta exception
   * @throws MetricException the metric exception
   */
  double[] getFilteredDisplacement(ResponseUnits responseUnits, Channel channel,
      long windowStartEpoch,
      long windowEndEpoch, double f1, double f2, double f3, double f4)
      throws ChannelMetaException, MetricException {
    if (!metadata.hasChannel(channel)) {
      logger.error(
          "Metadata NOT found for station=[{}-{}] channel=[{}] date=[{}] --> Can't return Displacement",
          metadata.getNetwork(), metadata.getStation(), channel, metadata.getDate());
      return null;
    }
    double[] timeseries = getWindowedData(channel, windowStartEpoch, windowEndEpoch);
    if (timeseries == null) {
      logger.warn(
          "Did not get requested window for station=[{}-{}] channel=[{}] date=[{}] --> Can't return Displacement",
          metadata.getNetwork(), metadata.getStation(), channel, metadata.getDate());
      return null;
    }
    return removeInstrumentAndFilter(responseUnits, channel, timeseries, f1, f2, f3, f4);
  }

  /**
   * Removes the instrument and filter. Needs documentation.
   *
   * @param responseUnits the response units
   * @param channel the channel
   * @param timeseries the timeseries
   * @param f1 the f1
   * @param f2 the f2
   * @param f3 the f3
   * @param f4 the f4
   * @return the filtered timeseries
   * @throws ChannelMetaException the channel meta exception
   * @throws MetricException the metric exception
   */
  private double[] removeInstrumentAndFilter(ResponseUnits responseUnits, Channel channel,
      double[] timeseries,
      double f1, double f2, double f3, double f4) throws ChannelMetaException, MetricException {

    if (!(f2 < f3)) {
      logger.error(String
          .format("removeInstrumentAndFilter: invalid freq: range: [%f-%f]", f2, f3));
      return null;
    }

    ChannelMeta chanMeta = metadata.getChannelMetadata(channel);
    double srate = chanMeta.getSampleRate();
    int ndata = timeseries.length;

    if (srate == 0) {
      throw new MetricException(String
          .format("channel=[%s] date=[%s] Got srate=0", channel.toString(), metadata.getDate()));
    }

    // Find smallest power of 2 >= ndata:
    int nfft = 1;
    while (nfft < ndata) {
      nfft = (nfft << 1);
    }

    // We are going to do an nfft point FFT which will return
    // nfft/2+1 +ve frequencies (including DC + Nyq)
    int nf = nfft / 2 + 1;

    double dt = 1. / srate;
    double df = 1. / (nfft * dt);

    double[] data = new double[timeseries.length];
    System.arraycopy(timeseries, 0, data, 0, timeseries.length);
    data = detrend(data);
    demeanInPlace(data);
    cosineTaper(data, .01);

    /*
    double[] freq = new double[nf];
    for (int k = 0; k < nf; k++) {
      freq[k] = (double) k * df;
    }
    */

    // return the (nf = nfft/2 + 1) positive frequencies
    // variable false is here because we don't have any reason to flip the data in this method
    FFTResult result = singleSidedFFT(data, srate, false);
    Complex[] xfft = result.getFFT();
    double[] freq = result.getFreqs();

    // Get the instrument response for requested ResponseUnits
    Complex[] instrumentResponse = chanMeta.getResponse(freq, responseUnits);

    for (int k = 0; k < freq.length; k++) {
      // Because Apache's FFT matches our imaginary sign, we don't
      // need a conjugate. If we were using Numerical Recipes we would
      // need to.
      if (instrumentResponse[k].equals(Complex.ZERO)) {
        xfft[k] = Complex.ZERO;
      } else {
        xfft[k] = xfft[k].divide(instrumentResponse[k]); // Remove instrument
      }
    }


    /*
    // we can almost certainly replace all of this with calls to FFTResult.getSingleSidedInverse
    Complex[] cfft = new Complex[nfft];
    cfft[0] = Complex.ZERO; // DC
    cfft[nf - 1] = xfft[nf - 1]; // Nyq
    for (int k = 1; k < nf - 1; k++) { // Reflect spec about the Nyquist to get negative freqs
      cfft[k] = xfft[k];
      cfft[2 * nf - 2 - k] = xfft[k].conjugate(); // this populates the negative frequencies
    }
     */

    double[] inverse = FFTResult.singleSidedInverseFFT(xfft, timeseries.length);
    return FilterUtils.bandFilter(inverse, srate, f2, f3, 2);
  }

  /**
   * Gets the windowed data.
   *
   * @param channel the channel
   * @param windowStartEpoch the window start epoch
   * @param windowEndEpoch the window end epoch
   * @return the windowed data
   */
  double[] getWindowedData(Channel channel, long windowStartEpoch, long windowEndEpoch) {
    if (windowStartEpoch > windowEndEpoch) {
      logger.error("Requested window Epoch (ms timestamp) [{} - {}] is NOT VALID (start > end)",
          windowStartEpoch,
          windowEndEpoch);
      return null;
    }

    if (!hasChannelData(channel)) {
      logger.warn("We have NO data for channel=[{}] date=[{}]", channel, metadata.getDate());
      return null;
    }
    ArrayList<DataSet> datasets = getChannelData(channel);
    DataSet data = null;
    boolean windowFound = false;

    for (DataSet dataset : datasets) {
      data = dataset;
      long startEpoch = data.getStartTime() / 1000; // Convert microsecs
      // --> millisecs
      long endEpoch = data.getEndTime() / 1000; // ...
      if (windowStartEpoch >= startEpoch && windowStartEpoch < endEpoch) {
        windowFound = true;
        break;
      }
    }

    if (!windowFound) {
      logger.warn(
          "Requested window Epoch (ms timestamp) [{} - {}] was NOT FOUND "
              + "within DataSet for channel=[{}] date=[{}]",
          windowStartEpoch, windowEndEpoch, channel, metadata.getDate());
      return null;
    }

    long dataStartEpoch = data.getStartTime() / 1000; // Convert microsecs
    // --> millisecs
    long dataEndEpoch = data.getEndTime() / 1000; // ...
    long interval = data.getInterval() / 1000; // Convert microsecs -->
    // millisecs (dt = sample
    // interval)
    double srate1 = data.getSampleRate();

    // Requested Window must start in Day 1 (taken from current dataset(0))
    if (windowStartEpoch < dataStartEpoch || windowStartEpoch > dataEndEpoch) {
      logger.warn(
          "Requested window Epoch (ms timestamp) [{} - {}] does NOT START "
              + "in current day data window Epoch [{} - {}] for channel=[{}] date=[{}]",
          windowStartEpoch, windowEndEpoch, dataStartEpoch, dataEndEpoch, channel,
          metadata.getDate());
      return null;
    }

    boolean spansDay = false;
    DataSet nextData = null;

    if (windowEndEpoch > dataEndEpoch) { // Window appears to span into next
      // day
      if (nextMetricData == null) {
        logger.warn(String.format(
            "== getWindowedData: Requested Epoch window[%d-%d] spans into next day, but we have NO data "
                + "for channel=[%s] date=[%s] for next day\n",
            windowStartEpoch, windowEndEpoch, channel, metadata.getDate()));
        return null;
      }
      if (!nextMetricData.hasChannelData(channel)) {
        logger.warn("Requested Epoch window spans into next day, but we have NO data "
            + "for channel=[{}] date=[{}] for next day", channel, metadata.getDate());
        return null;
      }

      datasets = nextMetricData.getChannelData(channel);
      nextData = datasets.get(0);

      long nextDataStartEpoch = nextData.getStartTime() / 1000; // Convert
      // microsecs
      // -->
      // millisecs
      long nextDataEndEpoch = nextData.getEndTime() / 1000; // ...
      double srate2 = nextData.getSampleRate();

      if (srate2 != srate1) {
        logger.warn(String.format(
            "== getWindowedData: Requested window Epoch [%d - %d] extends into "
                + "nextData window Epoch [%d - %d] for channel=[%s] date=[%s] but srate1[%f] != srate2[%f]\n",
            windowStartEpoch, windowEndEpoch, nextDataStartEpoch, nextDataEndEpoch, channel,
            metadata.getDate(), srate1, srate2));
        return null;
      }

      // Requested Window must end in Day 2 (taken from next day
      // dataset(0))

      if (windowEndEpoch > nextDataEndEpoch) {
        logger.warn(String.format(
            "== getWindowedData: Requested window Epoch [%d - %d] extends BEYOND "
                + "found nextData window Epoch [%d - %d] for channel=[%s] date=[%s]\n",
            windowStartEpoch, windowEndEpoch, nextDataStartEpoch, nextDataEndEpoch, channel,
            metadata.getDate()));
        return null;
      }

      spansDay = true;
    }

    long windowMilliSecs = windowEndEpoch - windowStartEpoch;
    int nWindowPoints = (int) (windowMilliSecs / interval);

    double[] dataArray = new double[nWindowPoints];

    int[] series1 = data.getSeries();
    int[] series2 = null;
    if (spansDay) {
      series2 = nextData.getSeries();
    }
    int j = 0;

    // MTH: this seems to line it up better with rdseed output window but
    // doesn't seem right ...
    int istart = (int) ((windowStartEpoch - dataStartEpoch) / interval) + 1;

    for (int i = 0; i < nWindowPoints; i++) {
      if ((istart + i) < data.getLength()) {
        dataArray[i] = (double) series1[i + istart];
      } else if (j < nextData.getLength()) {
        dataArray[i] = (double) series2[j++];
      }
    }

    return dataArray;

  }

  /**
   * Return segments representing contiguous regions of a full day (86400 sec) of data
   * assembled from a channel's DataSets, with any gaps zero-padded.
   *
   * @param channel the channel
   * @return the padded day data
   */
  double[][] getPaddedDayData(Channel channel) {
    if (!hasChannelData(channel)) {
      logger.warn(String
          .format("== getPaddedDayData(): We have NO data for channel=[%s] date=[%s]\n", channel,
              metadata.getDate()));
      return null;
    }
    List<DataSet> datasets = getChannelData(channel);

    /*epoch microsecs since 1970*/
    long dayStartTime = Time.calculateEpochMicroSeconds(metadata.getTimestamp());
    long interval = datasets.get(0).getInterval(); // sample dt in microsecs

    int nPointsPerDay = (int) (86400000000L / interval);
    List<double[]> segments = new ArrayList<>();

    long lastEndTime = dayStartTime;
    int totalPointCount = 0; // easy way to keep track of the number of points added to the list
    // append each dataset to the new data as necessary,
    for (DataSet dataset : datasets) {
      long startTime = dataset.getStartTime(); // microsecs since 1970 Jan. 1
      long endTime = dataset.getEndTime();
      // first, add zero padding over any gap between previous dataset and this current one
      int npad = (int) ((startTime - lastEndTime) / interval) - 1;
      if (npad > 0) {
        totalPointCount += npad;
        segments.add(new double[npad]);
      }
      // now convert the series to doubles and add it to the list of data
      int[] series = dataset.getSeries();
      double[] seriesAsDoubles = new double[series.length];
      for (int j = 0; j < series.length; ++j) {
        seriesAsDoubles[j] = (double) series[j];
      }
      segments.add(seriesAsDoubles);
      totalPointCount += series.length;
      // now the current time will be used to account for gap between this and next dataset start
      lastEndTime = endTime;
    }

    // in event the last segment doesn't reach the end of the day length, pad out until data does
    if (totalPointCount < nPointsPerDay) {
      double[] lastGapFiller = new double[nPointsPerDay - totalPointCount];
      segments.add(lastGapFiller);
    }

    return segments.toArray(new double[][]{});
  }

  /**
   * Return a linear-detrended full day (86400 sec) array of data assembled from a channel's
   * DataSets<br>
   * Zero pad any gaps between DataSets.
   *
   * @param channel the channel
   * @return the padded day data with linear trend removed
   */
  public double[] getDetrendedPaddedDayData(Channel channel) {
    if (!hasChannelData(channel)) {
      logger.warn(String
          .format("== getPaddedDayData(): We have NO data for channel=[%s] date=[%s]\n", channel,
              metadata.getDate()));
      return null;
    }
    double[][] segments = getPaddedDayData(channel);

    // now concatenate the segments and detrend the whole thing
    double[] toDetrend = concatAll(segments);
    return detrend(toDetrend);
  }

  /**
   * Creates the rotated channel data.
   *
   * Rotate/Create new derived channels: (chan1, chan2) --> (chanN, chanE) And
   * add these to StationData Channels we can derive end in H1,H2 (e.g.,
   * LH1,LH2 or HH1,HH2) --> LHND,LHED or HHND,HHED or N1,N2 (e.g., LN1,LN2 or
   * HN1,HN2) --> LNND,LNED or HNND,HNED
   *
   * @param location the location
   * @param channelPrefix the channel prefix
   * @throws MetricException thrown when unable to create rotated Channel data
   */
  private synchronized void createRotatedChannelData(String location, String channelPrefix)
      throws MetricException {
    try {
      boolean use12 = true; // Use ?H1,?H2 to rotate, else use ?HN,?HE

      // Raw horizontal channels used for rotation
      Channel channel1 = new Channel(location, String.format("%s1", channelPrefix));
      Channel channel2 = new Channel(location, String.format("%s2", channelPrefix));

      // If we can't find ?H1,?H2 --> try for ?HN,?HE
      if (!hasChannelData(channel1) || !hasChannelData(channel2)) {
        channel1.setChannel(String.format("%sN", channelPrefix));
        channel2.setChannel(String.format("%sE", channelPrefix));
        use12 = false;

        // If we still can't find 2 horizontals to rotate then give up
        if (!hasChannelData(channel1) || !hasChannelData(channel2)) {
          throw new ChannelException(String.format(
              "== createRotatedChannelData: -- Unable to find data "
                  + "for channel1=[%s] and/or channel2=[%s] date=[%s] --> Unable to Rotate!\n",
              channel1, channel2, metadata.getDate()));
        }

        if (!metadata.hasChannel(channel1) || !metadata.hasChannel(channel2)) {
          throw new ChannelException(String.format(
              "== createRotatedChannelData: -- Unable to find metadata "
                  + "for channel1=[%s] and/or channel2=[%s] date=[%s] --> Unable to Rotate!\n",
              channel1, channel2, metadata.getDate()));
        }
      }

      // Rotated (=derived) channels (e.g., 00-LHND,00-LHED -or-
      // 10-BHND,10-BHED, etc.)
      Channel channelN = new Channel(location, String.format("%sND", channelPrefix));
      Channel channelE = new Channel(location, String.format("%sED", channelPrefix));

      // Get overlapping data for 2 horizontal channels and confirm equal
      // sample rate, etc.
      long[] foo = new long[1];
      double[][] channelOverlap = getChannelOverlap(channel1, channel2, foo);
      // The startTime of the largest overlapping segment
      long startTime = foo[0];

      double[] chan1Data = channelOverlap[0];
      double[] chan2Data = channelOverlap[1];
			/*
			 * At this point chan1Data and chan2Data should have the SAME number
			 * of (overlapping) points
			 */

      int ndata = chan1Data.length;

      double srate1 = getChannelData(channel1).get(0).getSampleRate();
      double srate2 = getChannelData(channel2).get(0).getSampleRate();
      if (srate1 != srate2) {
        throw new MetricException(String.format(
            "createRotatedChannels: channel1=[%s] and/or channel2=[%s] date=[%s]: srate1 != srate2 !!",
            channel1, channel2, metadata.getDate()));
      }

      double[] chanNData = new double[ndata];
      double[] chanEData = new double[ndata];

      double az1 = (metadata.getChannelMetadata(channel1)).getAzimuth();
      double az2 = (metadata.getChannelMetadata(channel2)).getAzimuth();

      PreprocessingUtils.rotate_xy_to_ne(az1, az2, chan1Data, chan2Data, chanNData, chanEData);
			/*
			  az1 = azimuth of the H1 channel/vector. az2 = azimuth of the
			  H2 channel/vector // Find the smallest (<= 180) angle between
			  them --> This *should* be 90 (=orthogonal channels) double azDiff
			  = Math.abs(az1 - az2); if (azDiff > 180) azDiff = Math.abs(az1 -
			  az2 - 360);

			  if ( Math.abs( azDiff - 90. ) > 0.2 ) { System.out.format(
			  "== createRotatedChannels: channels are NOT perpendicular! az1-az2 = %f\n"
			  , Math.abs(az1 - az2) ); }
			 */

      // Here we need to convert the Series intArray[] into a DataSet with
      // header, etc ...

      // Make new channelData keys based on existing ones

      String northKey = null;
      String eastKey = null;

      // keys look like "IU_ANMO 00-BH1 (20.0 Hz)"
      // or "IU_ANMO 10-BH1 (20.0 Hz)"
      String lookupString;
      if (use12) {
        lookupString = location + "-" + channelPrefix + "1"; // e.g.,
        // "10-BH1"
      } else {
        lookupString = location + "-" + channelPrefix + "N"; // e.g.,
        // "10-BHN"
      }

      String northString = location + "-" + channelPrefix + "ND"; // e.g.,
      // "10-BHND"
      String eastString = location + "-" + channelPrefix + "ED"; // e.g.,
      // "10-BHED"

      Set<String> keys = data.keySet();
      for (String key : keys) {
        if (key.contains(lookupString)) { // "LH1" --> "LHND" and "LHED"
          northKey = key.replaceAll(lookupString, northString);
          eastKey = key.replaceAll(lookupString, eastString);
        }
      }

      DataSet ch1Temp = getChannelData(channel1).get(0);
      String network = ch1Temp.getNetwork();
      String station = ch1Temp.getStation();
      // String location = ch1Temp.getLocation();

      DataSet northDataSet = new DataSet();
      northDataSet.setNetwork(network);
      northDataSet.setStation(station);
      northDataSet.setLocation(location);
      northDataSet.setChannel(channelN.getChannel());
      northDataSet.setStartTime(startTime);

      northDataSet.setSampleRate(srate1);

      int[] intArray = new int[ndata];
      for (int i = 0; i < ndata; i++) {
        intArray[i] = (int) chanNData[i];
      }
      northDataSet.extend(intArray, 0, ndata);

      ArrayList<DataSet> dataList = new ArrayList<>();
      dataList.add(northDataSet);
      data.put(northKey, dataList);

      DataSet eastDataSet = new DataSet();
      eastDataSet.setNetwork(network);
      eastDataSet.setStation(station);
      eastDataSet.setLocation(location);
      eastDataSet.setChannel(channelE.getChannel());
      eastDataSet.setStartTime(startTime);

      eastDataSet.setSampleRate(srate1);

      for (int i = 0; i < ndata; i++) {
        intArray[i] = (int) chanEData[i];
      }
      eastDataSet.extend(intArray, 0, ndata);

      dataList = new ArrayList<>();
      dataList.add(eastDataSet);
      data.put(eastKey, dataList);
    } catch (TimeseriesException | ChannelException | IllegalSampleRateException e) {
      throw new MetricException("Data rotation failed", e);
    }
  }

  /**
   * Gets the channel overlap.
   *
   * @param channelX the channel x
   * @param channelY the channel y
   * @param startTime the start time
   * @return the channel overlap
   */
  private double[][] getChannelOverlap(Channel channelX, Channel channelY, long[] startTime) {

    ArrayList<ArrayList<DataSet>> dataLists = new ArrayList<>();

    ArrayList<DataSet> channelXData = getChannelData(channelX);
    ArrayList<DataSet> channelYData = getChannelData(channelY);
    if (channelXData == null) {
      logger.warn("== getChannelOverlap: Warning --> No DataSets found for Channel={} Date={}\n",
          channelX,
          metadata.getDate());
    }
    if (channelYData == null) {
      logger.warn("== getChannelOverlap: Warning --> No DataSets found for Channel={} Date={}\n",
          channelY,
          metadata.getDate());
    }
    dataLists.add(channelXData);
    dataLists.add(channelYData);

    BlockLocator locator = new BlockLocator(dataLists);
    locator.doInBackground(); //Not actually in background.
    ArrayList<ContiguousBlock> blocks = locator.getBlocks();

    ContiguousBlock largestBlock = null;
    ContiguousBlock lastBlock = null;
    for (ContiguousBlock block : blocks) {
      if ((largestBlock == null) || (largestBlock.getRange() < block.getRange())) {
        largestBlock = block;
      }
      if (lastBlock != null) {
        logger.error("Gap: {} data points ({} microseconds)",
            ((block.getStartTime() - lastBlock.getEndTime()) / block.getInterval()),
            (block.getStartTime() - lastBlock.getEndTime()));
      }
      lastBlock = block;
    }

    double[][] channels = {null, null};
    int[] channel;

    for (int i = 0; i < 2; i++) {
      for (DataSet set : dataLists.get(i)) {
        if (set.containsRange(largestBlock.getStartTime(), largestBlock.getEndTime())) {
          try {
            channel = set.getSeries(largestBlock.getStartTime(), largestBlock.getEndTime());
            channels[i] = intArrayToDoubleArray(channel);
          } catch (SequenceRangeException e) {
            logger.error("SequenceRangeException:", e);
          } catch (IndexOutOfBoundsException e) {
            logger.error("IndexOutOfBoundsException:", e);
          }
          break;
        }
      }
    }

    // See if we have a problem with the channel data we are about to
    // return:
    if (channels[0].length == 0 || channels[1].length == 0
        || channels[0].length != channels[1].length) {
      logger.warn("== getChannelOverlap: WARNING date=[{}] --> Something has gone wrong!",
          metadata.getDate());
    }

    // MTH: hack to return the startTime of the overlapping length of data
    // points
    startTime[0] = largestBlock.getStartTime();

    return channels;

  }

  /**
   * Converts an array of type int into an array of type double.
   *
   * @param source The array of int values to be converted.
   * @return An array of double values.
   */
  private static double[] intArrayToDoubleArray(int[] source) {
    double[] dest = new double[source.length];
    int length = source.length;
    for (int i = 0; i < length; i++) {
      dest[i] = source[i];
    }
    return dest;
  }

  /**
   * Determine if the current digest computed for a
   * channel or channelArray has changed from the value stored in the
   * database.
   *
   * @param channel the channel is translated into a ChannelArray
   * @param id contains Network, Station, Location, Channel information for identification.
   * @param forceUpdate set in config.xml. True forces a recompute if old and new digests match.
   * @return hashed digest in a ByteBuffer or null if computation isn't warranted.
   */
  synchronized ByteBuffer valueDigestChanged(Channel channel, MetricValueIdentifier id,
      boolean forceUpdate) {
    ChannelArray channelArray = new ChannelArray(channel.getLocation(), channel.getChannel());
    return valueDigestChanged(channelArray, id, forceUpdate);
  }

  /**
   * Determine if the current digest computed for a channel or channelArray
   * has changed from the value stored in the database.
   *
   * If a rotated channel is not located in the metadata, this method will
   * attempt to rotate the data.
   *
   * @param channelArray Array of 2 or 3 component channels for a single location.
   * @param id contains Network, Station, Location, Channel information for identification.
   * @param forceUpdate set in config.xml. True forces a recompute if old and new digests match.
   * @return hashed digest in a ByteBuffer or null if computation isn't warranted.
   */
  synchronized ByteBuffer valueDigestChanged(ChannelArray channelArray, MetricValueIdentifier id,
      boolean forceUpdate) {
    String metricName = id.getMetricName();
    Station station = id.getStation();
    LocalDate date = id.getDate();
    String strdate = date.format(DateTimeFormatter.ISO_ORDINAL_DATE);
    String channelId = MetricResult.createResultId(id.getChannel());


		/*
		 * We need at least metadata to compute a digest. If it doesn't exist,
		 * then maybe this is a rotated channel (e.g., "00-LHND") and we need to
		 * first try to make the metadata + data for it.
		 */
    if (!metadata.hasChannels(channelArray)) {
      checkForRotatedChannels(channelArray);
    }

		/*
		 * Check again for metadata. If we still don't have it (e.g., we weren't
		 * able to rotate) --> return null digest
		 */
    if (!metadata.hasChannels(channelArray)) {
      logger.warn(
          "valueDigestChanged (date=[{}]): We don't have metadata to compute the digest for this channelArray "
              + " --> return null digest\n",
          strdate);
      return null;
    }

		/*
		 * At this point we have the metadata but we may still not have any data
		 * for this channel(s). Check for data and if it doesn't exist, then
		 * return a null digest, EXCEPT if this is the AvailabilityMetric that
		 * is requesting the digest (in which case return a digest for the
		 * metadata alone)
		 */

    boolean availabilityMetric = false;
    if (id.getMetricName().equals("AvailabilityMetric")) {
      availabilityMetric = true;
    }

		/* Return null to skip non availability metric. */
    if (!hasChannelArrayData(channelArray) && !availabilityMetric) { // Return
      return null;
    }

    ByteBuffer newDigest = getHash(channelArray);
    if (newDigest == null) {
      logger.warn("Digest of [{}, {}, {}, {}] = null", strdate, metricName, station, channelId);
    }

		/* This can occur if MetricData was loaded from a serialized file. */
    if (metricReader == null) {
      return newDigest; // Go ahead and recompute the metric.
    }

    if (metricReader.isConnected()) {
			/*
			 * Retrieve old Digest from Database and compare to new Digest
			 */
      ByteBuffer oldDigest = metricReader
          .getMetricValueDigest(id.getDate(), id.getMetricName(), id.getStation(),
              id.getChannel());
      if (oldDigest != null) {
        if (newDigest.compareTo(oldDigest) == 0) {
          if (forceUpdate) {
						/*
						 * Don't do anything --> return the digest to force the
						 * metric computation
						 */
            logger.info(
                "== valueDigestChanged: metricName={} date={} Digests are Equal BUT forceUpdate=[true] so compute the metric anyway.",
                metricName, strdate);
          } else {
            newDigest = null;
          }
        } else if (!hasChannelArrayData(channelArray) && !forceUpdate) {
					/*
					 * This should catch availability metrics without data, but
					 * have precomputed values. If forceUpdate then drop out to
					 * the returnnewDigest
					 */
          logger
              .info("[{}, {}, {}, {}] Entry found in database, but no data to recompute.", strdate,
                  metricName, station, channelId);
          newDigest = null;
        }
      }
    }
		/*
		 * If metricReader is not connected, it will always fall through and recompute.
		 */

    return newDigest;
  }

  /**
   * Gets the hash.
   *
   * @param channelArray the channel array
   * @return the hash
   */
  private synchronized ByteBuffer getHash(ChannelArray channelArray) {
    ArrayList<ByteBuffer> digests = new ArrayList<>();

    List<Channel> channels = channelArray.getChannels();
    for (Channel channel : channels) {
      ChannelMeta chanMeta = getMetaData().getChannelMetadata(channel);
      if (chanMeta == null) {
        logger.warn("getHash: metadata not found for requested channel:{} date:{}", channel,
            metadata.getDate());
        return null;
      } else {
        digests.add(chanMeta.getDigestBytes());
      }

			/* If there is no channelData - Go ahead and pass back the digests for the metadata alone
			 The only Metric that should get to here is the
			 AvailabilityMetric */
      if (hasChannelData(channel)) { // Add in the data digests
        ArrayList<DataSet> datasets = getChannelData(channel);
        if (datasets == null) {
          logger.warn(
              String.format("getHash(): Data not found for requested channel:%s date:%s\n", channel,
                  metadata.getDate()));
          return null;
        } else {
          for (DataSet dataset : datasets) {
            digests.add(dataset.getDigestBytes());
          }
        }
      }
    }
    return MemberDigest.multiBuffer(digests);
  }

  /**
   * We've been handed a channelArray for which valueDigestChanged() was
   * unable to find metadata. We want to go through the channels and see if
   * any are rotated-derived channels (e.g., "00-LHND"). If so, then try to
   * create the rotated channel data + metadata
   *
   * @param channelArray the channel array
   */
  synchronized void checkForRotatedChannels(ChannelArray channelArray) {
    List<Channel> channels = channelArray.getChannels();
    for (Channel channel : channels) {

			/* channelPrefix = channel band + instrument code e.g., 'L' + 'H' ="LH"*/
      String channelPrefix;
      if (channel.getChannel().contains("ND")) {
        channelPrefix = channel.getChannel().replace("ND", "");
      } else if (channel.getChannel().contains("ED")) {
        channelPrefix = channel.getChannel().replace("ED", "");
      } else {
				/*We want to check for remaining channels.
				This may have been a list with LHZ*/
        continue;
      }

      // Check here since each derived channel (e.g., "00-LHND") will
      // cause us to generate
      // Rotated channel *pairs* ("00-LHND" AND "00-LHED") so we don't
      // need to repeat it
      if (!metadata.hasChannel(channel)) {
        metadata.addRotatedChannelMeta(channel.getLocation(), channelPrefix);
      }
      // MTH: Only try to add rotated channel data if we were successful
      // in adding the rotated channel
      // metadata above since createRotatedChannelData requires it
      try {
        if (!hasChannelData(channel) && metadata.hasChannel(channel)) {
          createRotatedChannelData(channel.getLocation(), channelPrefix);
        }
      } catch (MetricException e) {
        logger.warn(Logging.prettyExceptionWithCause(e));
      }
    }
  }
}