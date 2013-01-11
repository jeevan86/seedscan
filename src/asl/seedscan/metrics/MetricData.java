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

import asl.seedscan.database.*;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.EpochData;
import asl.metadata.Station;
import asl.metadata.meta_new.StationMeta;
import asl.metadata.meta_new.ChannelMeta;
import asl.security.MemberDigest;
import asl.seedsplitter.BlockLocator;
import asl.seedsplitter.ContiguousBlock;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.SeedSplitter;
import asl.seedsplitter.IllegalSampleRateException;
import asl.seedsplitter.Sequence;
import asl.seedsplitter.SequenceRangeException;
import asl.util.Hex;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Logger;
import java.util.Calendar;

public class MetricData
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.MetricData");

    private Hashtable<String, ArrayList<DataSet>> data;
    private StationMeta metadata;
    private Hashtable<String, String> synthetics;
    private MetricReader metricReader;

  //constructor(s)
    public MetricData(	MetricReader metricReader, Hashtable<String,
    					ArrayList<DataSet>> data, StationMeta metadata,
    					Hashtable<String, String> synthetics)
    {
    	this.metricReader = metricReader;
        this.data = data;
        this.metadata = metadata;
        this.synthetics = synthetics;
    }

    public MetricData(	MetricReader metricReader, Hashtable<String,
    					ArrayList<DataSet>> data, StationMeta metadata)
    {
    	this.metricReader = metricReader;
        this.data = data;
        this.metadata = metadata;
    }

    public MetricData(	MetricReader metricReader,
    					Hashtable<String, ArrayList<DataSet>> data)
    {
    	this.metricReader = metricReader;
        this.data = data;
    }

    // MTH: Added simple constructor for AvailabilityMetric when there is NO data
    public MetricData( StationMeta metadata)
    {
        this.metadata = metadata;
    }

    public StationMeta getMetaData()
    {
        return metadata;
    }

    public Boolean hasChannelArrayData(ChannelArray channelArray)
    {
        for (Channel channel : channelArray.getChannels() ) {
            if (!hasChannelData(channel) )
                return false;
        }
        return true;
    }

    public Boolean hasChannelData(Channel channel)
    {
        return hasChannelData(channel.getLocation(), channel.getChannel() );           
    }

    public Boolean hasChannelData(String location, String name)
    {
        if (data == null) { return false; }

        Boolean hasChannel = false;
        String locationName = location + "-" + name;
        Set<String> keys = data.keySet();
        for (String key : keys){          // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
            if (key.contains(locationName) ){
                hasChannel = true;
            }
        }
        return hasChannel;           
    }

/**
 *
 * @ return ArrayList<DataSet> = All DataSets for a given channel (e.g., "00-BHZ")
 *
*/
    public ArrayList<DataSet> getChannelData(String location, String name)
    {
        String locationName = location + "-" + name;
        Set<String> keys = data.keySet();
        for (String key : keys){          // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
           if (key.contains(locationName) ){
            //System.out.format(" key=%s contains locationName=%s\n", key, locationName);
              return data.get(key);       // return ArrayList<DataSet>
           }
        }
        return null;           
    }

    public ArrayList<DataSet> getChannelData(Channel channel)
    {
        return getChannelData(channel.getLocation(), channel.getChannel() );           
    }

/**
 *  Return a full day (86400 sec) array of data assembled from a channel's DataSets
 *  Zero pad any gaps between DataSets
 */
    public double[] getPaddedDayData(Channel channel) 
    {
        if (!hasChannelData(channel)){
            System.out.format("== MetricData.getPaddedDayData() ERROR: We have NO data for channel=[%s]\n", channel);
            return null;
        }
        ArrayList<DataSet>datasets = getChannelData(channel);

        long dayStartTime = metadata.getTimestamp().getTimeInMillis() * 1000; // epoch microsecs since 1970
        long interval     = datasets.get(0).getInterval();                    // sample dt in microsecs

        int nPointsPerDay = (int)(86400000000L/interval);

        double[] data     = new double[nPointsPerDay];

        long lastEndTime = 0;
        int k=0;

        for (int i=0; i<datasets.size(); i++) {
            DataSet dataset= datasets.get(i);
            long startTime = dataset.getStartTime();  // microsecs since Jan. 1, 1970
            long endTime   = dataset.getEndTime();
            int length     = dataset.getLength();
            int[] series   = dataset.getSeries();

            if (i == 0) {
                lastEndTime = dayStartTime;
            }
            int npad = (int)( (startTime - lastEndTime) / interval ) - 1;

            for (int j=0; j<npad; j++){
                if (k < data.length) {
                    data[k] = 0.;
                }
                k++;
            }
            for (int j=0; j<length; j++){
                if (k < data.length) {
                    data[k] = (double)series[j];
                }
                k++;
            }

            lastEndTime = endTime;
        }
        //System.out.format("== fullDayData: nDataSets=%d interval=%d nPointsPerDay%d k=%d\n", datasets.size(),
                          //interval, nPointsPerDay, k );
        return data;
    }



/*
 *  Rotate/Create new derived channels: (chan1, chan2) --> (chanN, chanE)
 *  And add these to StationData
 *  Channels we can derive end in H1,H2 (e.g., LH1,LH2 or HH1,HH2) --> LHND,LHED or HHND,HHED
 *                             or N1,N2 (e.g., LN1,LN2 or HN1,HN2) --> LNND,LNED or HNND,HNED
 */
    public void createRotatedChannelData(String location, String channelPrefix)
    {
    // channelPrefix = 'LH' or 'HN'. Use this to make horizontal channels 1,2:

    // Raw horizontal channels used for rotation
        Channel channel1 = new Channel(location, String.format("%s1", channelPrefix) );
        Channel channel2 = new Channel(location, String.format("%s2", channelPrefix) );

        if (hasChannelData(channel1)==false || hasChannelData(channel2)==false){
            System.out.format("== createRotatedChannelData: Error -- Unable to find data "
            + "for channel1=[%s] and/or channel2=[%s] --> Unable to Rotate!\n",channel1, channel2);
            return;
        }
        if (metadata.hasChannel(channel1)==false || metadata.hasChannel(channel2)==false){
            System.out.format("== createRotatedChannelData: Error -- Unable to find metadata "
            + "for channel1=[%s] and/or channel2=[%s] --> Unable to Rotate!\n",channel1, channel2);
            return;
        }

    // Rotated (=derived) channels (e.g., 00-LHND,00-LHED -or- 10-BHND,10-BHED, etc.)
        Channel channelN = new Channel(location, String.format("%sND", channelPrefix) );
        Channel channelE = new Channel(location, String.format("%sED", channelPrefix) );

    // Get overlapping data for 2 horizontal channels and confirm equal sample rate, etc.
        long[] foo = new long[1];
        double[][] channelOverlap = getChannelOverlap(channel1, channel2, foo);
    // The startTime of the largest overlapping segment
        long startTime = foo[0]; 

        double[]   chan1Data = channelOverlap[0];
        double[]   chan2Data = channelOverlap[1];
    // At this point chan1Data and chan2Data should have the SAME number of (overlapping) points

        int ndata = chan1Data.length;

        double srate1 = getChannelData(channel1).get(0).getSampleRate();
        double srate2 = getChannelData(channel2).get(0).getSampleRate();
        if (srate1 != srate2) {
            throw new RuntimeException("MetricData.createRotatedChannels(): Error: srate1 != srate2 !!");
        }

        double[]   chanNData = new double[ndata];
        double[]   chanEData = new double[ndata];

    // INITIALLY: Lets assume the horizontal channels are PERPENDICULAR and use a single azimuth to rotate
    //  We'll check the azimuths and flip signs to put channel1 to +N half and channel 2 to +E
        double az1 = (metadata.getChanMeta( channel1 )).getAzimuth(); 
        double az2 = (metadata.getChanMeta( channel2 )).getAzimuth(); 

        int quadrant=0;
        double  azimuth=-999;
        int sign1 = 1;
        int sign2 = 1;
        if (az1 >= 0 && az1 < 90) {
            quadrant = 1;
            azimuth  = az1;
        }
        else if (az1 >= 90 && az1 < 180) {
            quadrant = 2;
            azimuth  = az1 - 180;
            sign1    =-1;
        }
        else if (az1 >= 180 && az1 < 270) {
            quadrant = 3;
            azimuth  = az1 - 180;
            sign1    =-1;
        }
        else if (az1 >= 270 && az1 < 360) {
            quadrant = 4;
            azimuth  = az1 - 360;
        }
        else { // ?? 
            System.out.format("== OOPS: MetricData.createRotatedChannels(): Don't know how to rotate az1=%f\n", az1);
        }

        sign2 = 1;
        if (az2 >= 0 && az2 < 180) {
            sign2    = 1;
        }
        else if (az2 >= 180 && az2 < 360) {
            sign2    =-1;
        }
        else { // ?? 
            System.out.format("== OOPS: MetricData.createRotatedChannels(): Don't know how to rotate az2=%f\n", az2);
        }

        System.out.format("== MetricData.createRotatedChannels for [%s-%s]: az1=%.2f --> azimuth=%.2f az2=%.2f"
          + " Quadrant=%d (sign1=%d, sign2=%d)\n", location, channelPrefix, az1, azimuth, az2, quadrant, sign1, sign2);

        double cosAz   = Math.cos( azimuth * Math.PI/180 );
        double sinAz   = Math.sin( azimuth * Math.PI/180 );

        for (int i=0; i<ndata; i++){
            chanNData[i] = sign1 * chan1Data[i] * cosAz - sign2 * chan2Data[i] * sinAz;
            chanEData[i] = sign1 * chan1Data[i] * sinAz + sign2 * chan2Data[i] * cosAz;
        }

/**
    // az1 = azimuth of the H1 channel/vector.  az2 = azimuth of the H2 channel/vector
    // Find the smallest (<= 180) angle between them --> This *should* be 90 (=orthogonal channels)
        double azDiff = Math.abs(az1 - az2);
        if (azDiff > 180) azDiff = Math.abs(az1 - az2 - 360);

        if ( Math.abs( azDiff - 90. ) > 0.2 ) {
            System.out.format("== createRotatedChannels: channels are NOT perpendicular! az1-az2 = %f\n",
                               Math.abs(az1 - az2) );
        }
**/

// Here we need to convert the Series intArray[] into a DataSet with header, etc ...

// Make new channelData keys based on existing ones

        String northKey = null;
        String eastKey  = null;

        // keys look like "IU_ANMO 00-BH1 (20.0 Hz)"
        //             or "IU_ANMO 10-BH1 (20.0 Hz)"
        String lookupString = location + "-" + channelPrefix + "1";  // e.g., "10-BH1"
        String northString  = location + "-" + channelPrefix + "ND"; // e.g., "10-BHND"
        String eastString   = location + "-" + channelPrefix + "ED"; // e.g., "10-BHED"

        Set<String> keys = data.keySet();
        for (String key : keys){   
           if (key.contains(lookupString)) { // "LH1" --> "LHND" and "LHED"
                northKey = key.replaceAll(lookupString, northString);
                eastKey  = key.replaceAll(lookupString, eastString);
           }
        }
        //System.out.format("== MetricData.createRotatedChannels(): channel1=%s, channelPrefex=%s\n", channel1, channelPrefix);
        //System.out.format("== MetricData.createRotatedChannels(): northKey=[%s] eastKey=[%s]\n", northKey, eastKey);

        DataSet ch1Temp = getChannelData(channel1).get(0);
        String network  = ch1Temp.getNetwork();
        String station  = ch1Temp.getStation();
        //String location = ch1Temp.getLocation();

        DataSet northDataSet = new DataSet();
        northDataSet.setNetwork(network);
        northDataSet.setStation(station);
        northDataSet.setLocation(location);
        northDataSet.setChannel(channelN.getChannel());
        northDataSet.setStartTime(startTime);
        try {
            northDataSet.setSampleRate(srate1);
        } catch (IllegalSampleRateException e) {
            logger.finer(String.format("MetricData.createRotatedChannels(): Invalid Sample Rate = %f", srate1) );
        }

        int[] intArray = new int[ndata];
        for (int i=0; i<ndata; i++){
            intArray[i] = (int)chanNData[i];
        }
        northDataSet.extend(intArray, 0, ndata);

        ArrayList<DataSet> dataList = new ArrayList<DataSet>();
        dataList.add(northDataSet);
        data.put(northKey, dataList);

        DataSet eastDataSet = new DataSet();
        eastDataSet.setNetwork(network);
        eastDataSet.setStation(station);
        eastDataSet.setLocation(location);
        eastDataSet.setChannel(channelE.getChannel());
        eastDataSet.setStartTime(startTime);
        try {
            eastDataSet.setSampleRate(srate1);
        } catch (IllegalSampleRateException e) {
            logger.finer(String.format("MetricData.createRotatedChannels(): Invalid Sample Rate = %f", srate1) );
        }

        for (int i=0; i<ndata; i++){
            intArray[i] = (int)chanEData[i];
        }
        eastDataSet.extend(intArray, 0, ndata);

        dataList = new ArrayList<DataSet>();
        dataList.add(eastDataSet);
        data.put(eastKey, dataList);

/**
        tmp = getChannelData(channelE).get(0);
        System.out.format("== Got %s_%s %s-%s nsamps=%d nBlocks=%d\n", tmp.getNetwork(), tmp.getStation(), 
                           tmp.getLocation(), tmp.getChannel(), tmp.getLength() , tmp.getBlockCount() );
**/

    } // end createRotatedChannels()

    public double[][] getChannelOverlap(Channel channelX, Channel channelY) {
        // Dummy var to hold startTime of overlap
        // Call the 3 param version below if you want the startTime back
        long[] foo = new long[1];
        return getChannelOverlap(channelX, channelY, foo);
    }

/**
 *  getChannelOverlap - find the overlapping samples between 2+ channels
 *
 */
    public double[][] getChannelOverlap(Channel channelX, Channel channelY, long[] startTime) {

        ArrayList<ArrayList<DataSet>> dataLists = new ArrayList<ArrayList<DataSet>>();

        ArrayList<DataSet> channelXData = getChannelData(channelX);
        ArrayList<DataSet> channelYData = getChannelData(channelY);
        if (channelXData == null) {
            System.out.format("== getChannelOverlap: Error --> No DataSets found for Channel=%s\n", channelX);
        }
        if (channelYData == null) {
            System.out.format("== getChannelOverlap: Error --> No DataSets found for Channel=%s\n", channelY);
        }
        dataLists.add(channelXData);
        dataLists.add(channelYData);

        //System.out.println("Locating contiguous blocks...");

        ArrayList<ContiguousBlock> blocks = null;
        BlockLocator locator = new BlockLocator(dataLists);
        //Thread blockThread = new Thread(locator);
        //blockThread.start();
        locator.doInBackground();
        blocks = locator.getBlocks();

        //System.out.println("Found " + blocks.size() + " Contiguous Blocks");

        ContiguousBlock largestBlock = null;
        ContiguousBlock lastBlock = null;
        for (ContiguousBlock block: blocks) {
            if ((largestBlock == null) || (largestBlock.getRange() < block.getRange())) {
                largestBlock = block;
            }
            if (lastBlock != null) {
                System.out.println("    Gap: " + ((block.getStartTime() - lastBlock.getEndTime()) / block.getInterval()) + " data points (" + (block.getStartTime() - lastBlock.getEndTime()) + " microseconds)");
            }
            //System.out.println("  Time Range: " + Sequence.timestampToString(block.getStartTime()) + " - " + Sequence.timestampToString(block.getEndTime()) + " (" + ((block.getEndTime() - block.getStartTime()) / block.getInterval() + 1) + " data points)");
            lastBlock = block;
        }

        double[][] channels = {null, null};
        int[] channel = null;

        for (int i = 0; i < 2; i++) {
            boolean found = false;
            for (DataSet set: dataLists.get(i)) {
                if ((!found) && set.containsRange(largestBlock.getStartTime(), largestBlock.getEndTime())) {
                    try {
                        //System.out.println("  DataSet[" +i+ "]: " + Sequence.timestampToString(set.getStartTime()) + " - " + Sequence.timestampToString(set.getEndTime()) + " (" + ((set.getEndTime() - set.getStartTime()) / set.getInterval() + 1) + " data points)");
                        channel = set.getSeries(largestBlock.getStartTime(), largestBlock.getEndTime());
                        channels[i] = intArrayToDoubleArray(channel);
                    } catch (SequenceRangeException e) {
                        //System.out.println("SequenceRangeException");
                        e.printStackTrace();
                    }
                    found = true;
                    break;
                }
            }
        }

    // See if we have a problem with the channel data we are about to return:
        if (channels[0].length == 0 || channels[1].length == 0 || channels[0].length != channels[1].length){
            System.out.println("== getChannelOverlap: WARNING --> Something has gone wrong!");
        }

    // MTH: hack to return the startTime of the overlapping length of data points
        startTime[0] = largestBlock.getStartTime();

        return channels;

    } // end getChannelOverlap


    /**
     * Converts an array of type int into an array of type double.
     *
     * @param   source     The array of int values to be converted.
     * 
     * @return  An array of double values.
     */
    static double[] intArrayToDoubleArray(int[] source) 
    {
        double[] dest = new double[source.length];
        int length = source.length;
        for (int i = 0; i < length; i++) {
            dest[i] = source[i];
        }
        return dest;
    }


    public ByteBuffer valueDigestChanged(Channel channel, MetricValueIdentifier id)
    {
        ChannelArray channelArray = new ChannelArray(channel.getLocation(), channel.getChannel());
        return valueDigestChanged(channelArray, id);
    }

    public ByteBuffer valueDigestChanged(ChannelArray channelArray, MetricValueIdentifier id)
    {
        String metricName = id.getMetricName();
        Station station   = id.getStation();
        Calendar date     = id.getDate();
        String channelId  = MetricResult.createResultId(id.getChannel());
/**
        logger.fine(String.format(
                    "MetricValueIdentifier --> date=%04d-%02d-%02d (%03d) %02d:%02d:%02d | metricName=%s station=%s channel=%s",
                    date.get(Calendar.YEAR), (date.get(Calendar.MONTH)+1), date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.DAY_OF_YEAR), 
                    date.get(Calendar.HOUR), date.get(Calendar.MINUTE), date.get(Calendar.SECOND),
                    id.getMetricName(), id.getStation(), id.getChannel()
        ));
**/

    // We need at least metadata to compute a digest. If it doesn't exist, then maybe this is a rotated
    // channel (e.g., "00-LHND") and we need to first try to make the metadata + data for it.
        if (!metadata.hasChannels(channelArray)) { 
            checkForRotatedChannels(channelArray);
        }

    // Check again for metadata. If we still don't have it (e.g., we weren't able to rotate) --> return null digest
        if (!metadata.hasChannels(channelArray)) { 
            System.out.format("MetricData.valueDigestChanged: We don't have metadata to compute the digest for this channelArray "
                              + " --> return null digest\n");
            return null;
        }

    // At this point we have the metadata but we may still not have any data for this channel(s).
    // Check for data and if it doesn't exist, then return a null digest, EXCEPT if this is the
    // AvailabilityMetric that is requesting the digest (in which case return a digest for the metadata alone)

        Boolean availabilityMetric = false;
        if (id.getMetricName().contains("AvailabilityMetric") ) {
            availabilityMetric = true;
        }

        if (!hasChannelArrayData(channelArray) && !availabilityMetric) {  // Return null digest so Metric will be skipped
            System.out.println("== valueDigestChanged: We do NOT have data for this channel(s) AND this is NOT the "
                               + "AvailabilityMetric --> return null digest");
            return null;
        }

        ByteBuffer newDigest = getHash(channelArray);
        if (newDigest == null) {
            logger.warning("New digest is null!");
        }

        if (metricReader == null) { // This could be the case if we are computing AvailabilityMetric and there is no data
            return newDigest;
        }

        if (metricReader.isConnected()) {   // Retrieve old Digest from Database and compare to new Digest
            //System.out.println("=== MetricData.metricReader *IS* connected");
            ByteBuffer oldDigest = metricReader.getMetricValueDigest(id);
            if (oldDigest == null) {
                logger.fine("Old digest is null.");
            }
            else if (newDigest.compareTo(oldDigest) == 0) {
                logger.fine("Digests are Equal !!");
                newDigest = null;
            }
            logger.fine(String.format( "valueDigestChanged() --> oldDigest = getMetricValueDigest(%s, %s, %s, %s)",
                                       EpochData.epochToDateString(date), metricName, station, channelId));
        }
        else {
            //System.out.println("=== MetricData.metricReader *IS NOT* connected");
        }

        return newDigest;
    }


    public ByteBuffer getHash(Channel channel)
    {
        ChannelArray channelArray = new ChannelArray(channel.getLocation(), channel.getChannel());
        return getHash(channelArray);
    }


    private ByteBuffer getHash(ChannelArray channelArray)
    {
        ArrayList<ByteBuffer> digests = new ArrayList<ByteBuffer>();

        ArrayList<Channel> channels = channelArray.getChannels();
        for (Channel channel : channels){
            ChannelMeta chanMeta  = getMetaData().getChanMeta(channel);
            if (chanMeta == null){
                System.out.format("MetricData.getHash() Error: metadata not found for requested channel:%s\n",channel);
                return null;
            }
            else {
                digests.add(chanMeta.getDigestBytes());
            }

            if (!hasChannelData(channel))  {
                   // Go ahead and pass back the digests for the metadata alone
                   // The only Metric that should get to here is the AvailabilityMetric
            }
            else { // Add in the data digests
                ArrayList<DataSet>datasets = getChannelData(channel);
                if (datasets == null){
                    System.out.format("MetricData.getHash() Error: Data not found for requested channel:%s\n",channel);
                    return null;
                }
                else {
                    for (int i=0; i<datasets.size(); i++) {
                        //digests.add(datasets.get(0).getDigestBytes());
                        digests.add(datasets.get(i).getDigestBytes());
                    }
                }
            }
        }

        return MemberDigest.multiBuffer(digests);

    }

/**
 *  We've been handed a channelArray for which valueDigestChanged() was unable to find metadata.
 *  We want to go through the channels and see if any are rotated-derived channels (e.g., "00-LHND").  
 *  If so, then try to create the rotated channel data + metadata
 */
    public void checkForRotatedChannels(ChannelArray channelArray)
    {
        ArrayList<Channel> channels = channelArray.getChannels();
        for (Channel channel : channels){
            //System.out.format("== checkForRotatedChannels: request channel=%s\n", channel);

        // channelPrefix = channel band + instrument code  e.g., 'L' + 'H' = "LH"
            String channelPrefix = null;
            if (channel.getChannel().contains("ND") ) {
                channelPrefix = channel.getChannel().replace("ND","");
            }
            else if (channel.getChannel().contains("ED") ) {
                channelPrefix = channel.getChannel().replace("ED","");
            }
            else {
                System.out.format("== MetricData.checkForRotatedChannels: Request for UNKNOWN channel=%s\n", channel);
                return;
            }

        // Check here since each derived channel (e.g., "00-LHND") will cause us to generate
        //  Rotated channel *pairs* ("00-LHND" AND "00-LHED") so we don't need to repeat it
            if (!metadata.hasChannel(channel)) { 
                metadata.addRotatedChannelMeta(channel.getLocation(), channelPrefix);
            }
        // MTH: Only try to add rotated channel data if we were successful in adding the rotated channel
        //      metadata above since createRotatedChannelData requires it
            if (!hasChannelData(channel) && metadata.hasChannel(channel) ) { 
                createRotatedChannelData(channel.getLocation(), channelPrefix);
            }
        }
    }


}
