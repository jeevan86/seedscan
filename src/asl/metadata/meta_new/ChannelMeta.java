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
import freq.Cmplx;
import asl.security.MemberDigest;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Hashtable;
import java.util.Calendar;

/** A ChannelMeta consists of a series of ResponseStages.
    Typically there will be 3 ResponseStages, numbered 0, 1 and 2.
    ResponseStages 0 and 2 will likely contain only gain and frequencyOfgain info (e.g., Blockette B058)
    ResponseStage 1 will contain this info + specific instrument response (e.g., PoleZero, Polynomial) info,
    so that the complete channel response can be obtained by scaling ResponseStage1 response
    by the gains in ResponseStage1 and ResponseStage2.

    In the future, we may wish to read in higher Stages (3, 4, ...) that are 'D'igital stages
    with non-zero coefficients (numerator and denominator), so DigitalStage has been left
    general in order to be able to read and store these coefficients.
**/


public class ChannelMeta extends MemberDigest
{
    private String name = null;
    private String location = null;
    private String comment = null;
    private String instrumentType = null;
    private double sampleRate;
    private double dip;
    private double azimuth;
    private double depth;
    private Calendar metaTimestamp = null; // This should be same as the stationMeta metaTimestamp
    private Boolean dayBreak = false;      // This will be set to true if channelMeta changes during requested day
    private Hashtable<Integer, ResponseStage> stages;

    public void addDigestMembers() {

      addToDigest(sampleRate);
      addToDigest(getNumberOfStages());
      for (Integer stageID : stages.keySet() ){
        ResponseStage stage = getStage(stageID);
        addToDigest(stage.getStageGain());
        addToDigest(stage.getStageGainFrequency());
        addToDigest(stage.getStageType());
     // Add PoleZero Stage to Digest
        if (stage instanceof PoleZeroStage){
          PoleZeroStage pz = (PoleZeroStage)stage;
          addToDigest(pz.getNormalization());
          ArrayList<Cmplx> poles = pz.getPoles();
          for (int j=0; j<poles.size(); j++){
            addToDigest(poles.get(j).real());
            addToDigest(poles.get(j).imag());
          }
          ArrayList<Cmplx> zeros = pz.getZeros();
          for (int j=0; j<zeros.size(); j++){
            addToDigest(zeros.get(j).real());
            addToDigest(zeros.get(j).imag());
          }
        }
     // Add Polynomial Stage to Digest
        else if (stage instanceof PolynomialStage){
          PolynomialStage poly = (PolynomialStage)stage;
          addToDigest(poly.getLowerApproximationBound());
          addToDigest(poly.getUpperApproximationBound());
          addToDigest(poly.getNumberOfCoefficients());
          double[] coeffs = poly.getRealPolynomialCoefficients();
          for (int j=0; j<coeffs.length; j++){
            addToDigest(coeffs[j]);
          }
        }
     // Add Digital Stage to Digest
        else if (stage instanceof DigitalStage){
          DigitalStage dig = (DigitalStage)stage;
          addToDigest(dig.getInputSampleRate());
          addToDigest(dig.getDecimation());
        }
      } //end loop over response stages

    } // end addDigestMembers()

    // constructor(s)

    public ChannelMeta(ChannelKey channel, Calendar metaTimestamp)
    {
   // We need to call the super constructor to start the MessageDigest
        super();
        this.name     = channel.getName();
        this.location = channel.getLocation();
        this.metaTimestamp = metaTimestamp;
        stages = new Hashtable<Integer, ResponseStage>();
    }
    public ChannelMeta(String location, String channel, Calendar metaTimestamp)
    {
        this(new ChannelKey(location, channel), metaTimestamp);
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
    public void setInstrumentType(String instrumentType)
    {
        this.instrumentType = instrumentType;
    }
    public void setDayBreak()
    {
        this.dayBreak = true;
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
    public String getInstrumentType()
    {
        return instrumentType;
    }
    public Boolean hasDayBreak() {
        return dayBreak;
    }
    public Calendar getTimestamp() {
        return metaTimestamp;
    }

   // Stages
    public void addStage(Integer stageID, ResponseStage responseStage)
    {
        stages.put(stageID, responseStage);
    }

    public boolean hasStage(Integer stageID)
    {
        return stages.containsKey(stageID);
    }

    public ResponseStage getStage(Integer stageID)
    {
        return stages.get(stageID);
    }

    public Hashtable<Integer, ResponseStage> getStages()
    {
        return stages;
    }

//  Be careful when using this since:
//  A pole-zero response will have 3 stages: 0, 1, 2
//  A polynomial response will have 1 stage: 1

    public int getNumberOfStages()
    {
        return stages.size();
    }

//  Return true if any errors found in loaded ResponseStages
    public boolean invalidResponse()
    {
      if (getNumberOfStages() == 0) {
        System.out.format("ChannelMeta.invalidResponse(): Error: No stages have been loaded for chan-loc=%s-%s\n",
                                    this.getLocation(), this.getName() );
        return true;
      }
      if (!hasStage(1) || !hasStage(2)){
        System.out.format("ChannelMeta.invalidResponse(): Error: Stages 1 & 2 have NOT been loaded for chan-loc=%s-%s\n",
                                    this.getLocation(), this.getName() );
        return true;
      }
      double stageGain1 = stages.get(1).getStageGain();
      double stageGain2 = stages.get(2).getStageGain();

      if (stageGain1 <=0 || stageGain2 <=0 ) {
        System.out.format("ChannelMeta.invalidResponse(): Error: Gain =0 for either stages 1 or 2 for chan-loc=%s-%s\n",
                                    this.getLocation(), this.getName() );
        return true;
      }

   // Check stage1Gain * stage2Gain against the mid-level sensitivity (=stage0Gain):
      if (hasStage(0)){ 
        double stageGain0 = stages.get(0).getStageGain();
        double diff = (stageGain0 - (stageGain1 * stageGain2)) / stageGain0;
        diff *= 100;
        if (diff > 10) { // Alert user that difference is > 1% of Sensitivity
          System.out.format("Alert: stageGain0=%f VS. stage1=%f * stage2=%f (diff=%f%%)\n", stageGain0, stageGain1, stageGain2, diff);
        }
      }
   // If we made it to here then we must have a loaded response

      return false;
    }


//  Return complex response computed at given freqs[0,...length]

    public Cmplx[] getResponse(double[] freqs){

      if (freqs.length == 0) {
        throw new RuntimeException("getResponse(): freqs.length = 0!");
      }
      if (invalidResponse()) {
        throw new RuntimeException("getResponse(): Invalid Response!");
      }
      Cmplx[] response = null;

 // Set response = polezero response (with A0 factored in):
      ResponseStage stage = stages.get(1);
      if (stage instanceof PoleZeroStage){
        PoleZeroStage pz = (PoleZeroStage)stage;
        response = pz.getResponse(freqs);
        //pz.print();
      }
      else {
        throw new RuntimeException("getResponse(): Stage1 is NOT a PoleZeroStage!");
      }

 // Scale polezero response by stage1Gain * stage2Gain:
      double stage1Gain = stages.get(1).getStageGain();
      double stage2Gain = stages.get(2).getStageGain();
      //Cmplx scale = new Cmplx(stage1Gain*stage2Gain, 0);
      double scale = stage1Gain*stage2Gain;
      for (int i=0; i<freqs.length; i++){
        response[i] = Cmplx.mul(scale, response[i]);
      }
      return response;
}

    public void print() {
      System.out.println(this);
      for (Integer stageID : stages.keySet() ){
        ResponseStage stage = stages.get(stageID);
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

