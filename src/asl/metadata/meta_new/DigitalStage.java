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

import java.util.ArrayList;

public class DigitalStage extends ResponseStage
{
    private int decimationFactor = 1;
    private int numberOfNumerators;
    private int numberOfDenomenators;
    private int numberOfCoefficients;
    private double inputSampleRate;
    private ArrayList<Double> coefficients;

    // constructor(s)
    public DigitalStage(int stageNumber, char stageType, double stageGain)
    {
        super(stageNumber, stageType, stageGain);
    }
    public void setInputSampleRate(double sampleRate)
    {
        this.inputSampleRate = sampleRate;
    }
    public void setDecimation(int factor)
    {
        this.decimationFactor = factor;
    }
    public double getInputSampleRate()
    {
        return inputSampleRate;
    }
    public int getDecimation()
    {
        return decimationFactor;
    }
    public void addCoefficient(double coefficient){
    if (coefficients == null){
      coefficients = new ArrayList<Double>();
    }
      coefficients.add(coefficient);
      numberOfCoefficients++;
    }

/**
    @Override public String toString(){
      StringBuilder result = new StringBuilder();
      String NEW_LINE = System.getProperty("line.separator");
      result.append("Stage #" + stageNumber + " [Type="+stageType+"] Digital Filter:" + NEW_LINE);
      result.append("  COEFFICIENTS " + numberOfCoefficients + NEW_LINE);
      for (int j=0; j<numberOfCoefficients; j++){
        String temp = String.format("%1.6e",coefficients.get(j) );
        result.append(temp + NEW_LINE);
      }
      return result.toString();
    }
**/



}

