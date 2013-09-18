/*
 * Copyright 2011, United States Geological Survey or
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

package asl.metadata;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Channel
{
    private static final Logger logger = LoggerFactory.getLogger(asl.metadata.Channel.class);

    private Station station  = null; // We're not currently using this ... Do we need it ?
    private String  location = null;
    private String  channel  = null;

    public Channel (String location, String channel)
    {
        //setStation(station);
        setLocation(location);
        setChannel(channel);
    }

/**
 *  Static methods to validate channel naming and location.
 *  Individuaally validate Band, Instrument and Orientation Codes
 *  as per SEEDManual v2.4 Appendix A.
 *
 *  These will only be useful to validate original, SEISMIC channels (e.g., "VHZ")
 *  but will trip over derived channels (e.g., "00-10, LHND-LHND") and
 *  non-seismic channels (e.g., LDF)
 */
    public static boolean validLocationCode(String location) {
        if (location.length() != 2) {
            return false;
        }
    // Allow locations = {"00", "10", "20", ..., "99" and "--"}
        Pattern pattern  = Pattern.compile("^[0-9][0-9]$");
        Matcher matcher  = pattern.matcher(location);
        if (!matcher.matches() && !location.equals("--") ) {
            return false;
        }
        return true;
    }
    public static boolean validBandCode(String band) {
        if (band.length() != 1) {
            return false;
        }
        Pattern pattern  = Pattern.compile("[F,G,D,C,E,S,H,B,M,L,V,U,R,P,T,Q,A,O]");
        Matcher matcher  = pattern.matcher(band);
        if (!matcher.matches() ) {
            return false;
        }
        return true;
    }
    public static boolean validInstrumentCode(String instrument) {
        if (instrument.length() != 1) {
            return false;
        }
        Pattern pattern  = Pattern.compile("[H,L,G,M,N,D,F,I,K,R,W,C,E]");
        Matcher matcher  = pattern.matcher(instrument);
        if (!matcher.matches() ) {
            return false;
        }
        return true;
    }
    public static boolean validOrientationCode(String orientation) {
        if (orientation.length() != 1) {
            return false;
        }
        Pattern pattern  = Pattern.compile("[1,2,3,N,E,Z,U,V,W]");
        Matcher matcher  = pattern.matcher(orientation);
        if (!matcher.matches() ) {
            return false;
        }
        return true;
    }

// channel setter method(s)

    private void setLocation(String location) {
        if (location != null) {
        // Not sure how we want to validate since CoherencePBM for instance, calls
        //  Metric.createIdentifier --> MetricResult.createChannel --> new Channel ("00-10", ...)
            //if (!validLocationCode(location)) {
                //throw new RuntimeException("Channel.setLocation: ERROR INVALID LOCATION CODE=" + location);
            //}
            this.location = location;
        }
        else {
            this.location = "--"; // If no location given, set location = "--" [Default]
        }
    }

    public void setChannel(String channel) {
        if (channel == null) {
            throw new RuntimeException("channel cannot be null");
        }
    //  Most channels should be exactly 3-chars long (e.g., LH1), however, derived
    //    channels (e.g., LHND) will be 4-chars and maybe/probably there will be others
    //  e.g., MetricResult.createChannel ( new Channel("00-10" , "LHND-LHND") ) ...
        if (channel.length() < 3) {
            throw new RuntimeException("channel name MUST be at least 3-characters long");
        }
        this.channel = channel;
    }

    private void setStation(Station station) {
        if (station == null) {
            throw new RuntimeException("station cannot be null");
        }
        this.station = station;
    }


    @Override public String toString() {
      return getLocation() + "-" + getChannel();
    }

    public Station getStation() {
        return station;
    }

// channel getter method(s)

    public String getLocation() {
        return location;
    }

    public String getChannel() {
        return channel;
    }
}
