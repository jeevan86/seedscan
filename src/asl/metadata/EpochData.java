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
package asl.metadata;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.Calendar;
import java.util.Collections;
import java.text.SimpleDateFormat;

public class EpochData
{
    private static final Logger logger = Logger.getLogger("asl.metadata.EpochData");

    private Blockette format = null;
    private Blockette info = null;
    private ArrayList<Blockette> misc;
    private Hashtable<Integer, StageData> stages;

//MTH:
    private Calendar startTimestamp = null;
    private Calendar endTimestamp = null;
    private double dip;
    private double azimuth;
    private double depth;
    private double sampleRate;

//  epochToDateString(Calendar timestamp):
//  Return date string (e.g., "2002:324:14:30") for given Calendar timestamp
//  Return "(null)" if timestamp==null

    public static String epochToDateString(Calendar time)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:DDD:HH:mm");
        if (time != null){
          return sdf.format(time.getTime());
        }
        else {
          return "(null)";
        }
    }


    // Constructors
    public EpochData(Blockette info)
    {
        this.info = info;
        misc = new ArrayList<Blockette>();
        stages =  new Hashtable<Integer, StageData>();
        String startDateString = info.getFieldValue(22, 0);
        String endDateString   = info.getFieldValue(23, 0);
        if (!startDateString.equals("(null)") ) {
          try {
            startTimestamp = BlocketteTimestamp.parseTimestamp(startDateString);
          }
          catch (TimestampFormatException e) {
          }
        }
        if (!endDateString.equals("(null)") ) {
          try {
            endTimestamp   = BlocketteTimestamp.parseTimestamp(endDateString);
          }
          catch (TimestampFormatException e) {
          }
        }

        this.depth      = Double.parseDouble(info.getFieldValue(13, 0));
        this.azimuth    = Double.parseDouble(info.getFieldValue(14, 0));
        this.dip        = Double.parseDouble(info.getFieldValue(15, 0));
        this.sampleRate = Double.parseDouble(info.getFieldValue(18, 0));
    }

    public EpochData(Blockette format, Blockette info)
    {
        this.format = format;
        this.info = info;
        misc = new ArrayList<Blockette>();
        stages =  new Hashtable<Integer, StageData>();
    }

    // Info
    public void setInfo(Blockette info)
    {
        this.info = info;
    }

    public Blockette getInfo()
    {
        return info;
    }

    // Format
    public void setFormat(Blockette format)
    {
        this.format = format;
    }

    public Blockette getFormat()
    {
        return format;
    }

    // Misc Blockettes
    public void addMiscBlockette(Blockette blockette)
    {
        misc.add(blockette);
    }

    public ArrayList<Blockette> getMiscBlockettes()
    {
        return misc;
    }
    
    // Stages
    public void addStage(Integer stageID, StageData data)
    {
        stages.put(stageID, data);
    }

    public boolean hasStage(Integer stageID)
    {
        return stages.containsKey(stageID);
    }

    public StageData getStage(Integer stageID)
    {
        return stages.get(stageID);
    }

    public Hashtable<Integer, StageData> getStages()
    {
        return stages;
    }

    public int getNumberOfStages()
    {
        //ArrayList<Integer> stageNumbers = new ArrayList<Integer>();
        //stageNumbers.addAll(stages.keySet());
        //Collections.sort(stageNumbers);
        return stages.size();
    }

    public Calendar getStartTime() {
      return startTimestamp;
    }
    public Calendar getEndTime() {
      return endTimestamp;
    }
    public double getDip() {
      return dip;
    }
    public double getDepth() {
      return depth;
    }
    public double getAzimuth() {
      return azimuth;
    }
    public double getSampleRate() {
      return sampleRate;
    }
}

