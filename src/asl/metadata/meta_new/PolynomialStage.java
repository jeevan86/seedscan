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
import freq.Cmplx;

public class PolynomialStage extends ResponseStage
{
    private double lowerFrequencyBound;
    private double upperFrequencyBound;
    private double lowerApproximationBound;
    private double upperApproximationBound;
    private String polynomialApproximationType;
    private ArrayList<Cmplx> coefficients;
    //private static final long serialVersionUID = 1L;

    public PolynomialStage copy() {
        return this;
    }

    // constructor(s)
    public PolynomialStage(int stageNumber, char stageType, double stageGain, double stageFrequency)
    {
        super(stageNumber, stageType, stageGain, stageFrequency);
        coefficients = new ArrayList<Cmplx>();
    }
    public void addCoefficient(Cmplx coefficient){
      coefficients.add(coefficient);
    }
    public int getNumberOfCoefficients(){
      return coefficients.size();
    }

    public void setPolynomialApproximationType(String type){
      this.polynomialApproximationType = type;
    }
    public void setLowerFrequencyBound(double lowerBound){
      this.lowerFrequencyBound = lowerBound;
    }
    public void setUpperFrequencyBound(double upperBound){
      this.upperFrequencyBound = upperBound;
    }
    public void setLowerApproximationBound(double lowerBound){
      this.lowerApproximationBound = lowerBound;
    }
    public void setUpperApproximationBound(double upperBound){
      this.upperApproximationBound = upperBound;
    }

    public double getLowerFrequencyBound(){
      return lowerFrequencyBound;
    }
    public double getUpperFrequencyBound(){
      return upperFrequencyBound;
    }
    public double getLowerApproximationBound(){
      return lowerApproximationBound;
    }
    public double getUpperApproximationBound(){
      return upperApproximationBound;
    }

    public double[] getRealPolynomialCoefficients() {
      int numberOfCoefficients = getNumberOfCoefficients();
      //double[] values = new double[numberOfCoefficients + 4];
      double[] values = new double[numberOfCoefficients];
      int i=0;
      for ( ; i<numberOfCoefficients; i++) {
        values[i] = coefficients.get(i).real();  // Only expecting REAL coeffs for now ... 
      }
/** This is a proto where we pack the bound limits in with the coeffs ..
      values[i]   = getLowerApproximationBound(); 
      values[++i] = getUpperApproximationBound(); 
      values[++i] = getLowerFrequencyBound(); 
      values[++i] = getUpperFrequencyBound(); 
**/
      return values;
    }

    public void print(){
      super.print();
      System.out.println("-This is a [Polynomial] stage-");
      System.out.format("%30s:\t%s\n","Polynomial Approximation Type", polynomialApproximationType);
      System.out.format("%30s:\t%f\n","Lower Approximation Bound", lowerApproximationBound);
      System.out.format("%30s:\t%f\n","Upper Approximation Bound", upperApproximationBound);
      System.out.format(" Number of Coefficients=%d\n",getNumberOfCoefficients());
      for (int j=0; j<getNumberOfCoefficients(); j++){
        System.out.println(coefficients.get(j) );
      }
    }
}

