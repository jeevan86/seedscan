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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DigitalStage extends ResponseStage implements Cloneable {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.meta_new.DigitalStage.class);
	private static final long serialVersionUID = 1L;

	private int decimationFactor = 1;
	private int numberOfNumerators;
	private int numberOfDenomenators;
	private int numberOfCoefficients;
	private double inputSampleRate;
	private ArrayList<Double> coefficients;

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
	 * .... or for stage # > 2: B054F03 Transfer function type: D B054F04 Stage
	 * sequence number: 3 B054F05 Response in units lookup: COUNTS - Digital
	 * Counts B054F06 Response out units lookup: COUNTS - Digital Counts B054F07
	 * Number of numerators: 64 B054F10 Number of denominators: 0 # Numerator
	 * coefficients: # i, coefficient, error B054F08-09 0 -1.097070E-03
	 * 0.000000E+00 B054F08-09 1 -9.933270E-04 0.000000E+00 ...
	 * 
	 **/

	// constructor(s)
	public DigitalStage(int stageNumber, char stageType, double stageGain,
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

	public void addCoefficient(double coefficient) {
		if (coefficients == null) {
			coefficients = new ArrayList<Double>();
		}
		coefficients.add(coefficient);
		numberOfCoefficients++;
	}

	/**
	 * @Override public String toString(){ StringBuilder result = new
	 *           StringBuilder(); String NEW_LINE =
	 *           System.getProperty("line.separator"); result.append("Stage #" +
	 *           stageNumber + " [Type="+stageType+"] Digital Filter:" +
	 *           NEW_LINE); result.append("  COEFFICIENTS " +
	 *           numberOfCoefficients + NEW_LINE); for (int j=0;
	 *           j<numberOfCoefficients; j++){ String temp =
	 *           String.format("%1.6e",coefficients.get(j) ); result.append(temp
	 *           + NEW_LINE); } return result.toString(); }
	 **/
}
