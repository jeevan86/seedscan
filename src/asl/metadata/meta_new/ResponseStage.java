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

public abstract class ResponseStage implements Comparable<ResponseStage>
{
    protected int stageNumber;
    protected char stageType;
    protected double stageGain; 
    protected double frequencyOfstageGain; 
    protected String inputUnits; 
    protected String outputUnits; 
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

    public void setInputUnits(String inputUnits){
      this.inputUnits = inputUnits;
    }
    public void setOutputUnits(String outputUnits){
      this.outputUnits = outputUnits;
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
      result.append(String.format("Stage:%d  [Type='%1s'] Gain=%.2f\n",stageNumber,stageType,stageGain) );
      result.append(String.format("Units In:[%s]  Units Out:[%s]\n",inputUnits, outputUnits) );
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

