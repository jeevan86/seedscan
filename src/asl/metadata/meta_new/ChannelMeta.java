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

import org.apache.log4j.Logger;

import asl.metadata.*;
import asl.security.MemberDigest;
import asl.util.PlotMaker;
import freq.Cmplx;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Hashtable;
import java.util.Calendar;

import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.File;

/** 
 * A ChannelMeta consists of a series of ResponseStages.
 * Typically there will be 3 ResponseStages, numbered 0, 1 and 2.
 * ResponseStages 0 and 2 will likely contain only gain and frequencyOfgain info (e.g., Blockette B058)
 * ResponseStage 1 will contain this info + specific instrument response (e.g., PoleZero, Polynomial) info,
 * so that the complete channel response can be obtained by scaling ResponseStage1 response
 * by the gains in ResponseStage1 and ResponseStage2.
 *
 * In the future, we may wish to read in higher Stages (3, 4, ...) that are 'D'igital stages
 * with non-zero coefficients (numerator and denominator), so DigitalStage has been left
 * general in order to be able to read and store these coefficients.
 *
 * @author Mike Hagerty    <hagertmb@bc.edu>
 */
public class ChannelMeta extends MemberDigest 
                         implements Serializable, Cloneable
{
    private static final long serialVersionUID = 1L;

    private String name = null;
    private String location = null;
    private String comment = null;
    private String instrumentType = null;
    private String channelFlags = null; 

    private double sampleRate;
    private double dip;
    private double azimuth;
    private double depth;
    private Calendar metaTimestamp = null; // This should be same as the stationMeta metaTimestamp
    private boolean dayBreak = false;      // This will be set to true if channelMeta changes during requested day
    private Hashtable<Integer, ResponseStage> stages;

    private Station station;

    public enum ResponseUnits {
        DISPLACEMENT, VELOCITY, ACCELERATION, SEEDUNITS;
    }

    // constructor(s)
    public ChannelMeta(ChannelKey channel, Calendar metaTimestamp, Station station)
    {
   // We need to call the super constructor to start the MessageDigest
        super();
        this.name     = channel.getName();
        this.location = channel.getLocation();
        this.metaTimestamp = (Calendar)metaTimestamp.clone();
        this.station = station;
        stages = new Hashtable<Integer, ResponseStage>();
    }

    public ChannelMeta(ChannelKey channel, Calendar metaTimestamp)
    {
        this (channel, metaTimestamp, null);
    }

    public ChannelMeta(String location, String channel, Calendar metaTimestamp)
    {
        this(new ChannelKey(location, channel), metaTimestamp);
    }


/**
 *  Deep copy method(s)
 */
    public ChannelMeta copy() {
        return copy(this.name);
    }
    public ChannelMeta copy(Channel channel) {
        return copy(channel.getChannel());
    }
    public ChannelMeta copy(String name) {
        String useName = null;
        if (name != null) {
            useName = name;
        }
        else {
            useName = this.getName();
        }
        ChannelMeta copyChan = new ChannelMeta(this.getLocation(), useName, this.getTimestamp() );
        copyChan.sampleRate     = this.sampleRate;
        copyChan.dip            = this.dip;
        copyChan.azimuth        = this.azimuth;
        copyChan.depth          = this.depth;
        copyChan.dayBreak       = this.dayBreak;
        copyChan.instrumentType = this.instrumentType;
        copyChan.channelFlags   = this.channelFlags;

        for (Integer stageID : this.stages.keySet() ){
            ResponseStage stage = this.getStage(stageID);
            ResponseStage copyStage = stage.copy();
            copyChan.addStage(stageID, copyStage);
        }
        return copyChan;
    }

/**
 *  Add parts of this channelMeta to its digest
 */
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
    public void setChannelFlags(String channelFlags)
    {
        this.channelFlags = channelFlags;
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
    public Channel getChannel() {
        return new Channel( this.getLocation(), this.getName() );
    }
    public Station getStation() {
        return new Station( station.getNetwork(), station.getStation() );
        //return station;
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
    public String getChannelFlags()
    {
        return channelFlags;
    }
    public boolean hasDayBreak() {
        return dayBreak;
    }
    public Calendar getTimestamp() {
        return (Calendar)metaTimestamp.clone();
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
//  If we have a seismic channel we need to ensure a valid response

        boolean isSeismicChannel = false;
        boolean isMassPosition   = false;

        String seismicCodes = "HN"; // The 2nd char of channels: BH?, LH?, UH?, VH?, EH?, HH?, EN?, LN?, HN?
        if (seismicCodes.contains(this.getName().substring(1,2))) {
            isSeismicChannel = true;
        }

        if (this.getName().substring(1,2).equals("M") ){
            isMassPosition = true;
        }
/**
    String excludeCodes = "MDIKRW"; // Channel codes that we DON'T expect to have a stage 0 (e.g., VM?, LD?, LIO, etc.)
    if (excludeCodes.contains(this.getName().substring(1,2))) {
        expectChannel0 = false;
    }
**/
        if (getNumberOfStages() == 0) {
            System.out.format("ChannelMeta.invalidResponse(): Error: No stages have been loaded for chan-loc=%s-%s\n"
                               ,this.getLocation(), this.getName() );
            return true;
        }

        if (isSeismicChannel) {
            if (!hasStage(0) || !hasStage(1) || !hasStage(2)){
                System.out.format("ChannelMeta.invalidResponse(): Error: All Stages[=0,1,2] have NOT been loaded for chan-loc=%s-%s\n"
                                   ,this.getLocation(), this.getName() );
                return true;
            }
            double stageGain0 = stages.get(0).getStageGain(); // Really this is the Sensitivity
            double stageGain1 = stages.get(1).getStageGain();
            double stageGain2 = stages.get(2).getStageGain();

            if (stageGain0 <= 0 || stageGain1 <=0 || stageGain2 <=0 ) {
                System.out.format("ChannelMeta.invalidResponse(): Error: Gain =0 for either stages 0, 1 or 2 for chan-loc=%s-%s\n"
                                   ,this.getLocation(), this.getName() );
                return true;
            }
   // Check stage1Gain * stage2Gain against the mid-level sensitivity (=stage0Gain):
            double diff = (stageGain0 - (stageGain1 * stageGain2)) / stageGain0;
            diff *= 100;
// MTH: Adam says that some Q680's have this problem and we should use the Sensitivity (stageGain0) instead:
            if (diff > 10) { // Alert user that difference is > 1% of Sensitivity
                System.out.format("***Alert: stageGain0=%f VS. stage1=%f * stage2=%f (diff=%f%%)\n", stageGain0, stageGain1, stageGain2, diff);
            }

   // MTH: We could also check here that the PoleZero stage(s) was properly loaded 
   //      But currently this is done when PoleZero.getResponse() is called (it will throw an Exception)
        }

   // If we made it to here then we must have a loaded response

      return false;
    }

/*
 **   Return complex PoleZero response computed at given freqs[]
 **    If stage1 != PoleZero stage --> return null
 */
    public Cmplx[] getPoleZeroResponse(double[] freqs){
        PoleZeroStage pz = (PoleZeroStage)this.getStage(1);
        if (pz != null) {
            return pz.getResponse(freqs);
        }
        else {
            return null;
        }
    }


//  Return complex response computed at given freqs[0,...length]

    public Cmplx[] getResponse(double[] freqs, ResponseUnits responseOut) {
        int outUnits=0;
        switch (responseOut) {
            case DISPLACEMENT:      // return Displacement Response
                outUnits = 1;
                break;
            case VELOCITY:          // return Velocity     Response
                outUnits = 2;
                break;
            case ACCELERATION:      // return Acceleration Response
                outUnits = 3;
                break;
            case SEEDUNITS:         // return Default Dataless SEED units response
                outUnits = 0;
                break;
        }

        if (freqs.length == 0) {
            throw new RuntimeException("getResponse(): freqs.length = 0!");
        }
        if (invalidResponse()) {
          throw new RuntimeException("getResponse(): Invalid Response!");
        }
        Cmplx[] response = null;

 // Set response = polezero response (with A0 factored in):
        ResponseStage stage = stages.get(1);

        if (!(stage instanceof PoleZeroStage)) {
            throw new RuntimeException("getResponse(): Stage1 is NOT a PoleZeroStage!");
        }
        else {
            PoleZeroStage pz = (PoleZeroStage)stage;
            response = pz.getResponse(freqs);

            if (outUnits == 0) {  
                // Default response (in SEED Units) requested --> Don't integrate or differentiate
            }
            else { // Convert response to desired responseOut Units
                int inUnits = stage.getInputUnits(); // e.g., 0=Unknown ; 1=Disp(m) ; 2=Vel(m/s^2) ; 3=Acc ; ...
                if (inUnits == 0) {
                    String msg = String.format("getResponse(): [%s] Response requested but PoleZero Stage Input Units = Unknown!",
                                               responseOut);
                    throw new RuntimeException(msg);
                }
                int n = outUnits - inUnits;

 // We need to convert the returned response if the desired response units != the stored response units:
 // inUnit
 //   1 - Displacement
 //   2 - Velocity
 //   3 - Acceleration
 //
 // In the Four Trans convention used by SEED:
 //   x(t)  ~ Int[ X(w)e^+iwt ] dw  ==>  x'(t) ~ Int[ iw * X(w)e^+iwt ] dw  ==>  FFT[x'(t)] = iw x FFT[x(t)]
 //
 // x(t) = v(t) * i(t) 
 // X(w) = V(w) x I(w) --> V(w) = X(w)/I(w) = FFT[u'(t)] = iw x U(w) where U(w) = FFT[u(t)]
 // or     U(w) = X(w)/{iw x I(w)}
 //        U(w) = X(w)/II(w)       where II(w) = iw x I(w) <-- divide by this response to achieve integration
 //
 //  So, integration     n times: multiply I(w) by (iw)^n
 //      differentiation n times: multiply I(w) by (-i/w)^n
 //
 //  Ex: if the response units are Velocity (inUnits=2) and we want our output units = Acceleration (outUnits=3), then
 //      n = 3 - 2 = 1, and we return I'(w)=I(w)/(iw) = -i/w * I(w)

                double s;
                if (pz.getStageType() == 'A'){
                    s = 2.*Math.PI;
                }
                else if (pz.getStageType() == 'B'){
                    s = 1.;
                }
                else {
                    throw new RuntimeException("getResponse(): Unknown PoleZero StageType!");
                }
//System.out.format("== Channel=%s inUnits=%d outUnits=%d n=%d s=%f\n", this.getChannel(), inUnits, outUnits, n);

                if (n < 0) {                    // INTEGRATION RESPONSE I(w) x (iw)^n
                    for (int i=0; i<freqs.length; i++){
                        Cmplx iw    = new Cmplx(0.0, s*freqs[i]);
                        for (int j=1; j<Math.abs(n); j++) iw = Cmplx.mul(iw, iw);
                        response[i] = Cmplx.mul(iw, response[i]);
                    }
                }
                else if (n > 0) {               // DIFFERENTIATION RESPONSE I(w) / (iw)^n
                    for (int i=0; i<freqs.length; i++){
                        Cmplx iw    = new Cmplx(0.0, -1.0/(s*freqs[i]) );
                        for (int j=1; j<Math.abs(n); j++) iw = Cmplx.mul(iw, iw);
                        response[i] = Cmplx.mul(iw, response[i]);
                    }
                }

            } // Convert
        } // else


 // Scale polezero response by stage1Gain * stage2Gain:
 // Unless stage1Gain*stage2Gain is different from stage0Gain (=Sensitivity) by more than 10%,
 //   in which case, use the Sensitivity (Adam says this is a problem with Q680's, e.g., IC_ENH
        double stage0Gain = stages.get(0).getStageGain();
        double stage1Gain = stages.get(1).getStageGain();
        double stage2Gain = stages.get(2).getStageGain();

   // Check stage1Gain * stage2Gain against the mid-level sensitivity (=stage0Gain):
        double diff = 100 * (stage0Gain - (stage1Gain * stage2Gain)) / stage0Gain;

        double scale;
        if (diff > 10) { 
            System.out.println("== ChannelMeta.getResponse(): WARNING: Sensitivity != Stage1Gain * Stage2Gain "
                            + "--> Use Sensitivity to scale!");
            scale = stage0Gain;
        }
        else {
            scale = stage1Gain*stage2Gain;
        }

        if (scale <= 0.) {
            System.out.println("== ChannelMeta.getResponse(): WARNING: Channel response scale <= 0 !!");
        }

        for (int i=0; i<freqs.length; i++){
            response[i] = Cmplx.mul(scale, response[i]);
        }

        return response;
    }



/**
  * processEpochData 
  * Convert EpochData = Hashtable<StageNumber, StageData> for this Channel + Epoch
  * Into a sequence of ResponseStages, one for each StageNumber 
  * For now we're just pulling/saving the first 3 stages
  *
  * For each stageNumber, check for a B058 and if present, grab Gain + freqOfGain
  * Then, if you see a B054 --> create a new DigitalStage  & add to ChannelMeta
  * else, if you see a B053 --> create a new PoleZeroStage & add to ChannelMeta
  * else ...
**/
    public void processEpochData(EpochData epochData){

        for (int stageNumber = 0; stageNumber < 3; stageNumber++){
            if (epochData.hasStage(stageNumber)) {
                StageData stage = epochData.getStage(stageNumber);
                double Gain = 0;
                double frequencyOfGain = 0;
             // Process Blockette B058:
                if (stage.hasBlockette(58)) {
                    Blockette blockette = stage.getBlockette(58);
                    Gain = Double.parseDouble(blockette.getFieldValue(4, 0));
                    String temp[] = blockette.getFieldValue(5, 0).split(" ");
                    frequencyOfGain = Double.parseDouble(temp[0]);
                    //if (stageNumber == 0) { // Only stage 0 may consist of solely a B058 block
                                              // In this case Gain=Sensitivity
                    if ( (stageNumber != 1) && !(stage.hasBlockette(54)) ) { 
                        DigitalStage digitalStage = new DigitalStage(stageNumber, 'D', Gain, frequencyOfGain);
                        this.addStage(stageNumber, digitalStage);
                        if (stageNumber != 0) {
                            //System.out.format("== Warning: MetaGenerator: [%s_%s %s-%s] stage:%d has NO Blockette B054\n", 
                                               //station.getNetwork(), station.getStation(), location, name, stageNumber);
                        }
                    }
                }
                //else { // No B058: What we do here depends on the stageNumber and the channel name
                //}

             // Process Blockette B053:
                if (stage.hasBlockette(53)) {
                    Blockette blockette = stage.getBlockette(53);
                    //blockette.print();
                    String TransferFunctionType = blockette.getFieldValue(3, 0);
                    String ResponseInUnits = blockette.getFieldValue(5, 0);
                    String ResponseOutUnits = blockette.getFieldValue(6, 0);
                    Double A0Normalization = Double.parseDouble(blockette.getFieldValue(7, 0));
                    Double frequencyOfNormalization = Double.parseDouble(blockette.getFieldValue(8, 0));
                    int numberOfZeros = Integer.parseInt(blockette.getFieldValue(9, 0));
                    int numberOfPoles = Integer.parseInt(blockette.getFieldValue(14, 0));
                    ArrayList<String> RealPoles = blockette.getFieldValues(15);
                    ArrayList<String> ImagPoles = blockette.getFieldValues(16);
                    ArrayList<String> RealZeros = blockette.getFieldValues(10);
                    ArrayList<String> ImagZeros = blockette.getFieldValues(11);
                    char[] respType  = TransferFunctionType.toCharArray();

                    PoleZeroStage pz = new PoleZeroStage(stageNumber, respType[0], Gain, frequencyOfGain);
                    pz.setNormalization(A0Normalization);
                    pz.setInputUnits(ResponseInUnits);
                    pz.setOutputUnits(ResponseOutUnits);

                    for (int i=0; i<numberOfPoles; i++){
                        Double pole_re = Double.parseDouble(RealPoles.get(i));
                        Double pole_im = Double.parseDouble(ImagPoles.get(i));
                        Cmplx pole_complex = new Cmplx(pole_re, pole_im);
                        pz.addPole(pole_complex);
                    }
                    for (int i=0; i<numberOfZeros; i++){
                        Double zero_re = Double.parseDouble(RealZeros.get(i));
                        Double zero_im = Double.parseDouble(ImagZeros.get(i));
                        Cmplx zero_complex = new Cmplx(zero_re, zero_im);
                        pz.addZero(zero_complex);
                    }

                    this.addStage(stageNumber, pz);
                } // end B053

             // Process Blockette B062:

                if (stage.hasBlockette(62)) {        // This is a polynomial stage, e.g., ANMO_IU_00_VMZ
                    Blockette blockette = stage.getBlockette(62);
                    //blockette.print();
                    String TransferFunctionType = blockette.getFieldValue(3, 0); // Should be "P [Polynomial]"
                    String ResponseInUnits  = blockette.getFieldValue(5, 0);
                    String ResponseOutUnits = blockette.getFieldValue(6, 0);
                    String PolynomialApproximationType = blockette.getFieldValue(7, 0); // e.g., "M [MacLaurin]"
                    Double lowerFrequencyBound = Double.parseDouble(blockette.getFieldValue(9, 0));
                    Double upperFrequencyBound = Double.parseDouble(blockette.getFieldValue(10, 0));
                    Double lowerApproximationBound = Double.parseDouble(blockette.getFieldValue(11, 0));
                    Double upperApproximationBound = Double.parseDouble(blockette.getFieldValue(12, 0));
                    int numberOfCoefficients = Integer.parseInt(blockette.getFieldValue(14, 0));
                    ArrayList<String> RealCoefficients = blockette.getFieldValues(15);
                    ArrayList<String> ImagCoefficients = blockette.getFieldValues(16);
                    char[] respType  = TransferFunctionType.toCharArray();

                    PolynomialStage polyStage = new PolynomialStage(stageNumber, respType[0], Gain, frequencyOfGain);

                    polyStage.setInputUnits(ResponseInUnits);
                    polyStage.setOutputUnits(ResponseOutUnits);
                    polyStage.setLowerFrequencyBound(lowerFrequencyBound);
                    polyStage.setUpperFrequencyBound(upperFrequencyBound);
                    polyStage.setLowerApproximationBound(lowerApproximationBound);
                    polyStage.setUpperApproximationBound(upperApproximationBound);
                    polyStage.setPolynomialApproximationType(PolynomialApproximationType);
                    for (int i=0; i<numberOfCoefficients; i++){
                        Double coeff_re = Double.parseDouble(RealCoefficients.get(i));
                        Double coeff_im = Double.parseDouble(ImagCoefficients.get(i));
                        Cmplx coefficient = new Cmplx(coeff_re, coeff_im);
                        polyStage.addCoefficient(coefficient);
                    }
                    this.addStage(stageNumber, polyStage);
                } // end B062

             // Process Blockette B054:

                if (stage.hasBlockette(54)) { 
                    Blockette blockette = stage.getBlockette(54);
                    //blockette.print();
                    char[] respType=null;
                    String ResponseInUnits = null;
                    String ResponseOutUnits = null;

                    String TransferFunctionType = blockette.getFieldValue(3, 0);
                    respType  = TransferFunctionType.toCharArray();
                    ResponseInUnits = blockette.getFieldValue(5, 0);
                    ResponseOutUnits = blockette.getFieldValue(6, 0);

                    DigitalStage digitalStage = new DigitalStage(stageNumber, 'D', Gain, frequencyOfGain);
                    digitalStage.setInputUnits(ResponseInUnits);
                    digitalStage.setOutputUnits(ResponseOutUnits);

                    this.addStage(stageNumber, digitalStage);
                } // end B054

            }
            else { // No Stage stageNumber: What we do here ...      
            }
        } // end Loop stageNumber

        this.setAzimuth(epochData.getAzimuth() );
        this.setDepth(epochData.getDepth() );
        this.setDip(epochData.getDip() );
        this.setSampleRate(epochData.getSampleRate() );
        this.setInstrumentType(epochData.getInstrumentType() );
        this.setChannelFlags(epochData.getChannelFlags() );

    } // end processEpochData


    public void plotPoleZeroResp() {

        int magMin = -4; // .0001 Hz
        int magMax =  2; // 100 Hz
        int nMags  =  magMax - magMin + 1; // +1 for 0 (for 10^0)
        int nfreqPerMag = 45; // This should be factor of 9 ...
        int factor = nfreqPerMag/9;

        int nf = nMags * nfreqPerMag;
        double freq[] = new double[nf];

        int kk=0;
        for (int im = magMin; im < magMax; im++){
            double f0 = Math.pow(10, im);
            double df = f0/(double)factor;
            for (int i = 0; i < nfreqPerMag; i++){
                freq[kk++] = f0 + i * df; 
            }
        }

        PoleZeroStage pz = (PoleZeroStage)this.getStage(1);
        Cmplx[] instResponse = pz.getResponse(freq);

        double[] instRespAmp = new double[nf];
        double[] instRespPhs = new double[nf];
        for (int k=0; k<nf; k++) {
            instRespAmp[k] = instResponse[k].mag();
            instRespPhs[k] = instResponse[k].phs() * 180./Math.PI;
        }

        PlotMaker plotMaker = new PlotMaker(this.getStation(), this.getChannel(), this.getTimestamp());
        //plotMaker.plotSpecAmp(freq, instRespAmp, "pzResponse");
        plotMaker.plotSpecAmp(freq, instRespAmp, instRespPhs, "pzResponse");

    } // end plotResp

    public void print() {
      System.out.println("####### ChannelMeta.print() -- START ################################");
      System.out.println(this);
      for (Integer stageID : stages.keySet() ){
        ResponseStage stage = stages.get(stageID);
        stage.print();
        if (stage instanceof PoleZeroStage){
           //PoleZeroStage pz = (PoleZeroStage)stage;
           //pz.print();
        }
      }
      System.out.println("####### ChannelMeta.print() -- STOP  ################################");
      //System.out.println();
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

