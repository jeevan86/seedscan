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

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

import java.nio.ByteBuffer;
import asl.util.Hex;

import asl.metadata.Channel;
import asl.metadata.EpochData;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.PoleZeroStage;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.Sequence;

import asl.seedscan.event.EventCMT;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TestMetric
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.TestMetric");

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getName()
    {
        return "TestMetric";
    }


    public void process()
    {
        System.out.format("\n              [ == Metric %s == ]\n", getName() ); 

    // Get a sorted list of continuous channels for this stationMeta and loop over:

        //List<Channel> channels = stationMeta.getChannelArray("LH");
        List<Channel> channels = stationMeta.getChannelArray("BH");

        for (Channel channel : channels){
            System.out.format("== Channel:[%s]\n", channel);

            //ByteBuffer digest = metricData.valueDigestChanged(channel, createIdentifier(channel));

            //System.out.format("== %s: stationMeta.hasChannel(%s)=%s\n", getName(), channel, stationMeta.hasChannel(channel) );
            computeMetric(channel);

        }// end foreach channel
System.exit(0);

    } // end process()

    private void computeMetric(Channel channel) {

        if (!metricData.hasChannelData(channel)) {
        }

     // Test EventCMT cal
/**
        GregorianCalendar gcal =  new GregorianCalendar( TimeZone.getTimeZone("GMT") );
        gcal.set(Calendar.YEAR, 2012);
        gcal.set(Calendar.DAY_OF_YEAR, 100);
        gcal.set(Calendar.HOUR_OF_DAY, 20);
        gcal.set(Calendar.MINUTE, 30);
        gcal.set(Calendar.SECOND, 40);
        gcal.set(Calendar.MILLISECOND, 500);

        EventCMT eventCMT = new EventCMT.Builder("TestEvent").calendar(gcal).latitude(45.45).longitude(-75.55).depth(12.5).build();
        eventCMT.printCMT();
        gcal.set(Calendar.YEAR, 2010);

        Calendar cal2 = eventCMT.getCalendar();
System.out.format("== eventCMT.timeInMillis = [%d]\n", cal2.getTimeInMillis() );

        System.out.format("== Old cal2 = [%s]\n", EpochData.epochToDateString(cal2) );
        cal2.setTimeInMillis( cal2.getTimeInMillis() + 86400000L );
        System.out.format("== New cal2 = [%s]\n", EpochData.epochToDateString(cal2) );

        eventCMT.printCMT();
**/

     // Plot PoleZero Amp & Phase Response of this channel:
        ChannelMeta chanMeta = stationMeta.getChanMeta(channel);
        chanMeta.plotPoleZeroResp();
        PoleZeroStage pz = (PoleZeroStage)chanMeta.getStage(1);
        pz.print();

/**
     // The actual (=from data) number of samples:
        ArrayList<DataSet>datasets = metricData.getChannelData(channel);

        int ndata    = 0;

        for (DataSet dataset : datasets) {
            ndata   += dataset.getLength();
        } // end for each dataset
**/

    } // end computeMetric()

}
