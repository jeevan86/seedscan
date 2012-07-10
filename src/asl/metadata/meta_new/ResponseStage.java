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

/** A ChannelMeta consists of a series of ResponseStages.
    Typically there will be 3 ResponseStages, numbered 0, 1 and 2.
    ResponseStages 0 and 2 will likely contain only gain and frequencyOfgain info (e.g., Blockette B058)
    ResponseStage 1 will contain this info + specific instrument response (e.g., PoleZero) info,
    so that the complete channel response can be obtained by scaling ResponseStage1 response
    by the gains in ResponseStage1 and ResponseStage2.
**/

public abstract class ResponseStage implements Comparable<ResponseStage>
{
    protected int stageNumber;
    protected char stageType;
    protected double stageGain; 
    protected double frequencyOfstageGain; 
/**
 *  A [Analog Response rad/sec]
 *  B [Analog Response Hz]
 *  D [Digital]
 *  P [Polynomial]
 *
**/

    // constructor(s)
    public ResponseStage(int number, char type, double gain)
    {
        stageNumber = number;
        stageType   = type;
        stageGain   = gain;
    }
    public int getStageNumber()
    {
        return stageNumber;
    }
    public char getStageType()
    {
        return stageType;
    }
    public double getStageGain()
    {
        return stageGain;
    }

    public void print(){
      System.out.println(this);
    }

    @Override public String toString()
    {
      StringBuilder result = new StringBuilder();
      String NEW_LINE = System.getProperty("line.separator");
      result.append(String.format("Stage:%d  [Type='%1s'] Gain=%.2f",stageNumber,stageType,stageGain) );
      return result.toString();
    }

    @Override public int compareTo( ResponseStage stage ) {
      if ( this.getStageNumber() > stage.getStageNumber() ) {
         return 1;
      }
      else if ( this.getStageNumber() < stage.getStageNumber() ) {
         return -1;
      }
      else { // Stage numbers must be the same
         return 0;
      }
   }

}

