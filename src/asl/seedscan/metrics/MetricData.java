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
import asl.seedsplitter.DataSet;
import asl.security.MemberDigest;
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

/*
 *  Rotate/Create new derived channels: (chan1, chan2) --> (chanN, chanE)
 *  And add these to StationData
 */
    public void createRotatedChannelData(Channel derivedChannel)
    {
        if (derivedChannel.getChannel().contains("HND") || derivedChannel.getChannel().contains("HED")) {
            // These are channels we know how to rotate, so continue
        }
        else {
            // Unknown channel reqeuested ?
            System.out.format("== createRotatedChannelData: Error -- Don't know how to make channel=[%s]\n", derivedChannel);
            return;
        }

    // Use the channelBand to decide which horizontal channels to use for rotation
        String location    = derivedChannel.getLocation();
        String channelBand = derivedChannel.getChannel().substring(0,1); // e.g., "L", "B", etc.
        Channel channel1 = new Channel(location, String.format("%sH1", channelBand) );
        Channel channel2 = new Channel(location, String.format("%sH2", channelBand) );

        if (hasChannelData(channel1)==false || hasChannelData(channel2)==false){
            System.out.format("== createRotatedChannelData: Error -- Request for rotated channel=[%s] "
            + "but can't find data for channel1=[%s] and/or channel2=[%s]\n",derivedChannel, channel1, channel2);
            return;
        }
    // The (new) derived channels (e.g., 00-LHND,00-LHED -or- 10-BHND,10-BHED, etc.)
        Channel channelN = new Channel(location, channel1.getChannel().replace("H1", "HND") );
        Channel channelE = new Channel(location, channel1.getChannel().replace("H1", "HED") );

    // We have data for channel 1 and channel 2.  At this point we *should* limit the rotation to
    //   time periods where the channels both have data (i.e., the ContiguousBlocks) ...

        ArrayList<DataSet>datasets1 = getChannelData(channel1);
        ArrayList<DataSet>datasets2 = getChannelData(channel2);
        //for (DataSet dataset : datasets1) {
        //} 
        DataSet dataset1 = datasets1.get(0);
        DataSet dataset2 = datasets2.get(0);
        int    ndata1    = dataset1.getLength();
        int    ndata2    = dataset2.getLength();

        if (ndata1 != ndata2) {
            System.out.format("== MetricData.createRotatedChannels: chan1 ndata=%d BUT chan2 ndata=%d\n", ndata1, ndata2);
        }
        int ndata = ndata1;
        
        double srate1   = dataset1.getSampleRate();
        double srate2   = dataset2.getSampleRate();
        if (srate1 != srate2) {
            System.out.format("== MetricData.createRotatedChannels: chan1 srate=%f BUT chan2 srate=%f\n", srate1, srate2);
            return;
        }

        int[] intArray1 = dataset1.getSeries();
        int[] intArray2 = dataset2.getSeries();

        int[] intArrayN = new int[ndata];
        int[] intArrayE = new int[ndata];

        double[] chN = new double[ndata];
        double[] chE = new double[ndata];

    // az1 = azimuth of the H1 channel/vector.  az2 = azimuth of the H2 channel/vector
    // Find the smallest (<= 180) angle between them --> This *should* be 90 (=orthogonal channels)
        double az1 = (metadata.getChanMeta( channel1 )).getAzimuth(); 
        double az2 = (metadata.getChanMeta( channel2 )).getAzimuth(); 
        double azDiff = Math.abs(az1 - az2);
        if (azDiff > 180) azDiff = Math.abs(az1 - az2 - 360);

System.out.format("== createRotatedChannels: az1=%f az2=%f azDiff=%f\n", az1, az2, azDiff);

        if ( Math.abs( azDiff - 90. ) > 0.2 ) {
            System.out.format("== createRotatedChannels: channels are NOT perpendicular! az1-az2 = %f\n",
                               Math.abs(az1 - az2) );
        }

// Convert azimuths to radians
        az1 = az1 * Math.PI/180.;
        az2 = az2 * Math.PI/180.;

// This should work even if the 2 channels are not perpendicular
// If the channels are perpendicular then az2 = 90 - az1 and the normal 2D rotation matrix follows
//  We should really be checking for and only using
//     ContiguousBlocks!!
        for (int i=0; i<ndata; i++){
            chN[i] = (double)intArray1[i] * Math.cos( az1 ) + (double)intArray2[i] * Math.cos( az2 );
            chE[i] = (double)intArray1[i] * Math.sin( az1 ) + (double)intArray2[i] * Math.sin( az2 );
            //intArrayN[i] = intArray1[i] * Math.cos( az1 ) + intArray2[i] * Math.cos( az2 );
            //intArrayE[i] = intArray1[i] * Math.sin( az1 ) + intArray2[i] * Math.sin( az2 );
        }

// Here we need to convert the Series intArray[] into a DataSet with header, etc ...

// Make new channelData keys based on existing ones

        String northKey = null;
        String eastKey  = null;

        Set<String> keys = data.keySet();
        for (String key : keys){          // key looks like "IU_ANMO 00-BHZ (20.0 Hz)"
           //if (key.contains(locationName) ){
           if (key.contains(channel1.getChannel()) ){
                northKey = key.replaceAll("H1", "HND");
                eastKey  = key.replaceAll("H1", "HED");
           }
        }
        System.out.format(" northKey=[%s] eastKey=[%s]\n", northKey, eastKey);

// Use the new keys to add the new ChannelData to the Hashtable ...
/**
        say northArrayList = ArrayList<DataSet> ...
        data.put(northKey, northArrayList);
**/

    } // end createRotatedChannels()


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
        logger.fine(String.format(
                    "MetricValueIdentifier --> date=%04d-%02d-%02d (%03d) %02d:%02d:%02d | metricName=%s station=%s channel=%s",
                    date.get(Calendar.YEAR), (date.get(Calendar.MONTH)+1), date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.DAY_OF_YEAR), //Java uses months 0-11 so I added 1 to the returned value
                    date.get(Calendar.HOUR), date.get(Calendar.MINUTE), date.get(Calendar.SECOND),
                    id.getMetricName(), id.getStation(), id.getChannel()
        ));

    // Make sure we have metadata + data for all channels of channelArray before attempting to compute the digest

        if (!metadata.hasChannels(channelArray)) { 
        // See if channelArray contains rotated-derived channels (e.g., "00-LHND") and if so,
        //   try to create the metadata + data for them 
            checkForRotatedChannels(channelArray);
        }

        if (!metadata.hasChannels(channelArray)) { 
        // if we were unable to do the rotations then fail out ...
            System.out.format("MetricData.valueDigestChanged() Error: We don't have metadata to compute digest for this channelArray\n");
            return null;
        }

        if (!hasChannelArrayData(channelArray)) { 
                System.out.format("MetricData.valueDigestChanged() Error: We don't have data to compute digest for this channelArray\n");
                return null;
        }
        
        ByteBuffer newDigest = getHash(channelArray);
        if (newDigest == null) {
            logger.warning("New digest is null!");
        }

        if (metricReader.isConnected()) {
            System.out.println("=== MetricData.metricReader *IS* connected");
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
            System.out.println("=== MetricData.metricReader *IS NOT* connected");
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

            ArrayList<DataSet>datasets = getChannelData(channel);
            if (datasets == null){
                System.out.format("MetricData.getHash() Error: Data not found for requested channel:%s\n",channel);
                return null;
            }
            else {
                digests.add(datasets.get(0).getDigestBytes());
            }
        }

        return MemberDigest.multiBuffer(digests);

        //System.out.format("=== getHash(): newDigest=%s\n", Hex.byteArrayToHexString(newDigest.array()) );
    }


//MTH: I think this is now obsolete:
    public ByteBuffer hashChanged(Channel channel)
    {
        ChannelArray channelArray = new ChannelArray(channel.getLocation(), channel.getChannel());
        //return hashChanged(channelArray);
        return null;
    }

/**
 *  We've been handed a channelArray -- We want to go through it and see if any of the channels
 *  are rotated-derived channels (e.g., "00-LHND").  If so, then see if we have metadata for
 *  them -- if not, try to make it
 */
    public void checkForRotatedChannels(ChannelArray channelArray)
    {
        ArrayList<Channel> channels = channelArray.getChannels();
        for (Channel channel : channels){
            System.out.format("== checkForRotatedChannels: request channel=%s\n", channel);
            if (channel.getChannel().contains("HND") || channel.getChannel().contains("HED")) {
                System.out.format("== checkForRotatedChannels: Request for rotated-derived channel=%s\n", channel);
                metadata.addRotatedChannel(channel);
                createRotatedChannelData(channel);
            }
            else {
                System.out.format("== checkForRotatedChannels: Request for UNKNOWN channel=%s\n", channel);
            }
        }
    }

}
