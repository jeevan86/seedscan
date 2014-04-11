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

import java.io.Serializable;

/** 
 * Every response stage type will contain generic info from SEED 
 *  Blockette B058 (e.g., Stage Gain, Frequency of Gain) here.
 * 
 * In addition, info that is unique to a particular stage type will be stored 
 *   in the child class for that type (PoleZeroStage, PolynomialStage, etc.)
 *
 *   Stage Type             SEED Blockette(s)   Child Class
 * ----------------------------------------------------------
 * A [Analog Response rad/sec]  B053        PoleZeroStage
 * B [Analog Response Hz]       B053        PoleZeroStage
 * P [Polynomial]               B062        PolynomialStage
 * D [Digital]                  B054, B057  DigitalStage
 *
 * @author Mike Hagerty <hagertmb@bc.edu>
 */
public abstract class ResponseStage implements Comparable<ResponseStage>, Serializable
{

	private static final long serialVersionUID = 1L;
	protected int    stageNumber;
    protected char   stageType;
    protected double stageGain; 
    protected double stageGainFrequency; 
    protected int    inputUnits; 
    protected int    outputUnits; 
    protected String inputUnitsString; 
    protected String outputUnitsString; 

    abstract public ResponseStage copy();

    // constructor(s)
    public ResponseStage(int number, char type, double gain, double frequency)
    {
        stageNumber = number;
        stageType   = type;
        stageGain   = gain;
        stageGainFrequency   = frequency;
    }

/**
 * Set inputUnits of this stage:
 *     0 = Unknown 
 *     1 = Displacement (m)
 *     2 = Velocity (m/s)
 *     3 = Acceleration (m/s^2)
 *     4 = Pressure (Pa) 
 *     5 = Pressure (KPa) 
 *     6 = Magnetic Flux Density (Teslas - T)
 *     7 = Magnetic Flux Density (nanoTeslas - NT)
 *     8 = Degrees Centigrade (C)
 *     9 = Degrees Orientation 0-360 (theta)
 *    10 = Volts (V)
 */
    public void setInputUnits(String inputUnitsString){
      this.inputUnitsString = inputUnitsString;


      if (inputUnitsString.contains("Displacement")      || inputUnitsString.contains("displacement") ){
          inputUnits = 1;
      }
      else if (inputUnitsString.contains("Velocity")     || inputUnitsString.contains("velocity") ){
          inputUnits = 2;
      }
      else if (inputUnitsString.contains("Acceleration") || inputUnitsString.contains("M/S**2") ){
          inputUnits = 3;
      }
      else if (inputUnitsString.contains("Pressure") ){
           if (inputUnitsString.contains("KPA")){
               inputUnits = 5;
           }
           else  {
               inputUnits = 4;
           }
      }
      else if (inputUnitsString.contains("Magnetic") ){
           if (inputUnitsString.contains("nanoTeslas")){
               inputUnits = 7;
           }
           else  {
               inputUnits = 6;
           }
      }
      else if (inputUnitsString.contains("Degrees") ){
           if (inputUnitsString.contains("Centigrade")){
               inputUnits = 8;
           }
           else  {
               inputUnits = 9;
           }
      }
      else if (inputUnitsString.contains("Volts") || inputUnitsString.contains("VOLTS") ){
          inputUnits = 10;
      }
      else {                // We didn't find anything
          inputUnits = 0;
      }

    }


    public void setOutputUnits(String outputUnitsString){
      this.outputUnitsString = outputUnitsString;
    }
    public int getInputUnits(){
      return inputUnits;
    }
    public String getInputUnitsString(){
      return inputUnitsString;
    }
    public String getOutputUnitsString(){
      return outputUnitsString;
    }
    public double getStageGainFrequency(){
      return stageGainFrequency;
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
      result.append(String.format("Stage:%d  [Type='%1s'] Gain=%.2f FreqOfGain=%.2f\n",stageNumber,stageType,stageGain,stageGainFrequency) );
      result.append(String.format("Units In:[%s]  Units Out:[%s]\n",inputUnitsString, outputUnitsString) );
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

