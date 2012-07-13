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

package asl.metadata.meta_new;

import asl.metadata.ChannelKey;
import java.util.ArrayList;
import java.util.TreeSet;

public class ChannelMeta
{
    private String name = null;
    private String location = null;
    private String comment = null;
    private double latitude, longitude; // Not sure if we need separate <lat,lon> for each ch
    private double sampleRate;
    private double dip;
    private double azimuth;
    private double depth;
    //private ArrayList<ResponseStage> stages;
    private TreeSet<ResponseStage> stages;

    // constructor(s)

    public ChannelMeta(ChannelKey channel)
    {
        this.name     = channel.getName();
        this.location = channel.getLocation();
        stages = new TreeSet<ResponseStage>();
    }

    public ChannelMeta(String location, String channel)
    {
        this.name     = channel;
        this.location = location;
        stages = new TreeSet<ResponseStage>();
    }

    // setter(s)

    public void setComment(String comment)
    {
        this.comment = comment;
    }
    public void setSampleRate(double sampleRate)
    {
        this.sampleRate = sampleRate;
    }
    public void setDip(double dip)
    {
        this.dip = dip;
    }
    public void setAzimuth(double azimuth)
    {
        this.azimuth = azimuth;
    }
    public void setDepth(double depth)
    {
        this.depth = depth;
    }
    public void addStage(ResponseStage stage){
      stages.add(stage);
    }

    // getter(s)

    public String getLocation() {
        return location;
    }
    public String getName() {
        return name;
    }
    public double getDepth() {
        return depth;
    }
    public double getDip() {
        return dip;
    }
    public double getAzimuth() {
        return azimuth;
    }
    public double getSampleRate() {
        return sampleRate;
    }
    public int getNumberOfStages() {
        return stages.size();
    }
    public TreeSet<ResponseStage> getStages() {
        return stages;
    }
    public void print() {
      System.out.println(this);
      for (ResponseStage stage : stages){
        stage.print();
        if (stage instanceof PoleZeroStage){
           //PoleZeroStage pz = (PoleZeroStage)stage;
           //pz.print();
        }
      }
      System.out.println();
    }

    @Override public String toString() {
      StringBuilder result = new StringBuilder();
      String NEW_LINE = System.getProperty("line.separator");
      result.append(String.format("%15s%s\t%15s%.2f\t%15s%.2f\n","Channel:",name,"sampleRate:",sampleRate,"Depth:",depth) );
      result.append(String.format("%15s%s\t%15s%.2f\t%15s%.2f\n","Location:",location,"Azimuth:",azimuth,"Dip:",dip) );
      result.append(String.format("%15s%s","num of stages:",stages.size()) );
      //result.append(NEW_LINE);
      return result.toString();
    }

}

