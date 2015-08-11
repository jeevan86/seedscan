package asl.metadata.meta_new;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents Blockette 54, it however doesn't actually handle the
 * coefficients. It appears to do little aside from aiding in the creation of
 * MemberDigests.
 * 
 * @author Mike Hagerty
 * @author James Holland
 *
 */
class DigitalStage extends ResponseStage implements Cloneable, Serializable {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.meta_new.DigitalStage.class);
	private static final long serialVersionUID = 1L;

	private int decimationFactor = 1;
	private double inputSampleRate;

	/**
	 * This is here to implement the abstract method copy() in ResponseStage,
	 * but it's just implementing the shallow copy (=clone) below
	 */
	public DigitalStage copy() {
		return this.clone();
	}

	/**
	 * Shallow copy - this will work for the primitives, but NOT the ArrayList
	 * coeff's (it will only copy *references* to the coeffs ...)
	 */
	public DigitalStage clone() {
		try {
			return (DigitalStage) super.clone();
		} catch (CloneNotSupportedException e) {
			logger.error("CloneNotSupported:", e);
			return null;
		}
	}

	/**
	 * Relevant SEED Blockettes
	 * 
	 * B054F03 Transfer function type: D B054F04 Stage sequence number: 2
	 * B054F05 Response in units lookup: V - Volts B054F06 Response out units
	 * lookup: COUNTS - Digital Counts B054F07 Number of numerators: 0 B054F10
	 * Number of denominators: 0
	 * 
	 * B057F03 Stage sequence number: 2 B057F04 Input sample rate: 5.120000E+03
	 * B057F05 Decimation factor: 1 B057F06 Decimation offset: 0 B057F07
	 * Estimated delay (seconds): 0.000000E+00 B057F08 Correction applied
	 * (seconds): 0.000000E+00
	 * 
	 * .... {@literal or for stage # > 2: B054F03} Transfer function type: D B054F04 Stage
	 * sequence number: 3 B054F05 Response in units lookup: COUNTS - Digital
	 * Counts B054F06 Response out units lookup: COUNTS - Digital Counts B054F07
	 * Number of numerators: 64 B054F10 Number of denominators: 0 # Numerator
	 * coefficients: # i, coefficient, error B054F08-09 0 -1.097070E-03
	 * 0.000000E+00 B054F08-09 1 -9.933270E-04 0.000000E+00 ...
	 * 
	 **/

	DigitalStage(int stageNumber, char stageType, double stageGain,
			double stageFrequency) {
		super(stageNumber, stageType, stageGain, stageFrequency);
	}

	public void setInputSampleRate(double sampleRate) {
		this.inputSampleRate = sampleRate;
	}

	public void setDecimation(int factor) {
		this.decimationFactor = factor;
	}

	public double getInputSampleRate() {
		return inputSampleRate;
	}

	public int getDecimation() {
		return decimationFactor;
	}
}
