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

public class PoleZeroStage extends ResponseStage
{
    private ArrayList<Complex> poles;
    private ArrayList<Complex> zeros;
    private double normalizationConstant;
    private boolean poleAdded = false;
    private boolean zeroAdded = false;
    private boolean normalizationSet = false;

    // constructor(s)
    public PoleZeroStage(int stageNumber, char stageType, double stageGain)
    {
        super(stageNumber, stageType, stageGain);
        poles = new ArrayList<Complex>();
        zeros = new ArrayList<Complex>();
    }

    public void addPole(Complex pole){
      poles.add(pole);
      poleAdded = true;
    }
    public void addZero(Complex zero){
      zeros.add(zero);
      zeroAdded = true;
    }
    public void setNormalization(double A0){
      this.normalizationConstant = A0;
      normalizationSet = true;
    }
    public double getNormalization(){
      return normalizationConstant;
    }
    public int getNumberOfPoles(){
      return poles.size();
    }
    public int getNumberOfZeros(){
      return zeros.size();
    }

    public void print(){
      super.print();
      System.out.println("-This is a pole-zero stage-");
      System.out.format(" Number of Poles=%d\n",getNumberOfPoles());
      for (int j=0; j<getNumberOfPoles(); j++){
        System.out.println(poles.get(j) );
      }
      System.out.format(" Number of Zeros=%d\n",getNumberOfZeros());
      for (int j=0; j<getNumberOfZeros(); j++){
        System.out.println(zeros.get(j) );
      }
      System.out.format(" A0 Normalization=%f\n\n",getNormalization());
    }

/*  This is just for checking purposes.
 *  It will cycle over a range of frequencies and call 
 *  evalResp(f) to compute the polezero response at f,
 *  then print it out.
**/
    public void printResponse(){
      Complex response;
      for (double x=.01; x<=100; x += .01){ // 100sec -to- 100Hz
        response = evalResp(x);
        System.out.format("%12.4f\t%12.4f\n",x, response.mod() );
      }
    }

/*  Return complex response computed at given freqs[0,...length]
 *  Should really check that length > 0
**/
    public Complex[] getResponse(double[] freqs){
      //Some polezero responses (e.g., ANMO.IU.20.BN?) appear to have NO zeros
      //if (poleAdded && zeroAdded && normalizationSet) {
      if (poleAdded && normalizationSet) {
      // Looks like the polezero info has been loaded ... so continue ...
      }
      else {
        throw new RuntimeException("[ PoleZeroStage-->getResponse Error: PoleZero info does not appear to be loaded! ]");
      }
      if (!(freqs.length > 0)){
        throw new RuntimeException("[ PoleZeroStage-->getResponse Error: Input freqs[] has no zero length! ]");
      }
      Complex[] response = new Complex[freqs.length];
      for (int i=0; i<freqs.length; i++){
        response[i] = evalResp(freqs[i]);
      //System.out.format("%12.4f\t%12.4f\n",freqs[i], response[i].mod() );
      }
      return response;
    }

/*  SEED Manual - Appendix C
 *  PoleZero Representation for Analog Stages
 *    The first part of any seismic sensor will be some sort of linear system that operates in
 *    continuous time, rather than discrete time. Usually, any such system has a frequency response
 *    that is the ratio of two complex polynomials, each with real coefficients.  These polynomials
 *    can be represented either by their coefficients or (preferably) by their roots (poles and zeros).
 *
 *    The polynomials are specified by their roots.  The roots of the numerator polynomial are the
 *    instrument zeros, and the roots of the denominator polynomial are the instrument poles.
 *    Because the polynomials have real coefficients, complex poles and zeros will occur in complex
 *    conjugate pairs.  By convention, the real parts of the poles and zeros are negative.
 *
 *    The expansion formula gives the response ( G(f) ) at any frequency f (Hz), using the variable:
 *      s = 2*pi*i*f (rad/sec) if the PoleZero transfer function is type A  -OR-
 *      s = i*f (Hz)           if the PoleZero transfer function is type B
*/

/*  Evaluate the polezero response at a single frequency, f
 *  Return G(f) = A0 * pole zero expansion
 *  Note that the stage sensitivity Sd is *not* included, so that the response from this
 *  stage should be approx. 1 (flat) at the mid range.
**/
    private Complex evalResp(double f){
      Complex numerator   = new Complex(1,0);
      Complex denomenator = new Complex(1,0);
      Complex s;
      Complex Gf;

      if (getStageType() == 'A'){
        s = new Complex(0.0, 2*Math.PI*f);
      }
      else if (getStageType() == 'B'){
        s = new Complex(0.0, f);
      }
      else {
        throw new RuntimeException("[ PoleZeroStage-->evalResponse Error: Cannot evalResp a non-PoleZero Stage!]");
      }

      for (int j=0; j<getNumberOfZeros(); j++){
        numerator = numerator.times(s.minus(zeros.get(j))) ;
      }
      for (int j=0; j<getNumberOfPoles(); j++){
        denomenator = denomenator.times(s.minus(poles.get(j))) ;
      }
      Complex A0 = new Complex(normalizationConstant, 0);
      Gf = A0.times(numerator);
      Gf = Gf.div(denomenator);
      return Gf;
    }

}

