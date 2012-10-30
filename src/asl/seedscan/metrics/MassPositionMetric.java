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

import java.nio.ByteBuffer;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Calendar;

import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.seedsplitter.*;

import java.nio.ByteBuffer;
import asl.util.Hex;

public class MassPositionMetric
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.MassPositionMetric");

    @Override public long getVersion()
    {
        return 1;
    }

    @Override public String getName()
    {
        return "MassPositionMetric";
    }

    public void process()
    {
        System.out.format("\n              [ == Metric %s == ]\n", getName() ); 

   // Create a 3-channel array to use for loop
        ChannelArray channelArray = new ChannelArray("00","VMZ", "VM1", "VM2");
        ArrayList<Channel> channels = channelArray.getChannels();

   // Loop over channels, get metadata & data for channel and Calculate Metric

        for (Channel channel : channels){

         // Check to see that we have data + metadata & see if the digest has changed wrt the database:
            ByteBuffer digest = metricData.hashChanged(channel, getResult());
            System.out.format("== %s: digest=%s\n", getName(), Hex.byteArrayToHexString(digest.array()) );

            if (digest == null) { 
                System.out.format("%s INFO: Data and metadata have NOT changed for this channel:%s --> Skipping\n"
                                  ,getName(), channel);
                continue;
            }

         // If we're here, it means we need to (re)compute the metric for this channel:

            ChannelMeta chanMeta = stationMeta.getChanMeta(channel);
            ArrayList<DataSet>datasets = metricData.getChannelData(channel);

            double a0 = 0;
            double a1 = 0;
            double upperBound = 0;
            double lowerBound = 0;

         // Get Stage 1, make sure it is a Polynomial Stage (MacLaurin) and get Coefficients
            ResponseStage stage = chanMeta.getStage(1);
            if (!(stage instanceof PolynomialStage)) {
                throw new RuntimeException("MassPositionMetric: Stage1 is NOT a PolynomialStage!");
            }
            PolynomialStage polyStage = (PolynomialStage)stage;
            double[] coefficients = polyStage.getRealPolynomialCoefficients();
            lowerBound   = polyStage.getLowerApproximationBound();
            upperBound   = polyStage.getUpperApproximationBound();
                  
         // We're expecting a MacLaurin Polynomial with 2 coefficients (a0, a1) to represent mass position
            if (coefficients.length != 2) {
                throw new RuntimeException("MassPositionMetric: We're expecting 2 coefficients for this PolynomialStage!");
            }
            else {
                a0 = coefficients[0];
                a1 = coefficients[1];
            }
          // Make sure we have enough ingredients to calculate something useful
            if (a0 == 0 && a1 == 0 || lowerBound == 0 && upperBound == 0) {
                throw new RuntimeException("MassPositionMetric: We don't have enough information to compute mass position!");
            }

            double massPosition  = 0;
            int ndata = 0;
            String dataHashString = null;

            for (DataSet dataset : datasets) {
                int intArray[] = dataset.getSeries();
                for (int i=0; i<intArray.length; i++){
                    massPosition += Math.pow( (a0 + intArray[i] * a1), 2);
                }
                ndata += dataset.getLength();
                dataHashString = dataset.getDigestString();
            } // end for each dataset

            massPosition = Math.sqrt( massPosition / (double)ndata );
         
            double massRange  = (upperBound - lowerBound)/2;
            double massCenter = lowerBound + massRange;
            double massPercent= 100 * Math.abs(massPosition - massCenter) / massRange;

            metricResult.addResult(channel, massPercent, digest);

/**
            System.out.format("%s-%s [%s] %s %s-%s ", stationMeta.getStation(), stationMeta.getNetwork(), 
              EpochData.epochToDateString(stationMeta.getTimestamp()), getName(), chanMeta.getLocation(), chanMeta.getName() );
            System.out.format("RMS-Volts:%.2f (%.0f%%) %s %s\n", massPosition, massPercent, chanMeta.getDigestString(), dataHashString); 
**/

        }// end foreach channel
    } // end process()
} // end class
