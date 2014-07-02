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

import freq.Cmplx;

/**
 * Our internal representation of a PoleZero Stage includes the analog polezero
 * info + the stage gain & frequency of gain
 */
public class PoleZeroStage extends ResponseStage implements Cloneable {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.meta_new.PoleZeroStage.class);
	private ArrayList<Cmplx> poles;
	private ArrayList<Cmplx> zeros;
	private double normalizationConstant;
	private boolean poleAdded = false;
	private boolean zeroAdded = false;
	private boolean normalizationSet = false;

	// private static final long serialVersionUID = 1L;

	/**
	 * Return a deep copy of this PoleZeroStage
	 */
	public PoleZeroStage copy() {
		PoleZeroStage stageCopy = new PoleZeroStage(this.stageNumber,
				this.stageType, this.stageGain, this.stageGainFrequency);
		for (int i = 0; i < poles.size(); i++) {
			stageCopy.addPole(this.poles.get(i));
		}
		for (int i = 0; i < zeros.size(); i++) {
			stageCopy.addZero(this.zeros.get(i));
		}
		stageCopy.setNormalization(this.normalizationConstant);
		stageCopy.setInputUnits(this.inputUnitsString);
		stageCopy.setOutputUnits(this.outputUnitsString);
		return stageCopy;
	}

	/**
	 * Return a shallow copy of this PoleZeroStage
	 */
	public PoleZeroStage clone() {
		try {
			return (PoleZeroStage) super.clone();
		} catch (CloneNotSupportedException e) {
			logger.error("CloneNoteSupportedException:", e);
			return null;
		}
	}

	// constructor(s)
	public PoleZeroStage(int stageNumber, char stageType, double stageGain,
			double stageFrequency) {
		super(stageNumber, stageType, stageGain, stageFrequency);
		poles = new ArrayList<Cmplx>();
		zeros = new ArrayList<Cmplx>();
	}

	public void addPole(Cmplx pole) {
		poles.add(pole);
		poleAdded = true;
	}

	public void addZero(Cmplx zero) {
		zeros.add(zero);
		zeroAdded = true;
	}

	public void setNormalization(double A0) {
		this.normalizationConstant = A0;
		normalizationSet = true;
	}

	public double getNormalization() {
		return normalizationConstant;
	}

	public int getNumberOfPoles() {
		return poles.size();
	}

	public int getNumberOfZeros() {
		return zeros.size();
	}

	public ArrayList<Cmplx> getZeros() {
		return zeros;
	}

	public ArrayList<Cmplx> getPoles() {
		return poles;
	}

	public void print() {
		super.print();
		System.out.println("-This is a pole-zero stage-");
		System.out.format(" Number of Poles=%d\n", getNumberOfPoles());
		for (int j = 0; j < getNumberOfPoles(); j++) {
			System.out.println(poles.get(j));
		}
		System.out.format(" Number of Zeros=%d\n", getNumberOfZeros());
		for (int j = 0; j < getNumberOfZeros(); j++) {
			System.out.println(zeros.get(j));
		}
		System.out.format(" A0 Normalization=%f\n\n", getNormalization());
	}

	/*
	 * This is just for checking purposes. It will cycle over a range of
	 * frequencies and call evalResp(f) to compute the polezero response at f,
	 * then print it out.
	 */
	public void printResponse() {
		Cmplx response;
		for (double x = .01; x <= 100; x += .01) { // 100sec -to- 100Hz
			try {
				response = evalResp(x);
				System.out.format("%12.4f\t%12.4f\n", x, response.mag());
			} catch (PoleZeroStageException e) {
				logger.error("PoleZeroStageException:", e);
			}
		}
	}

	/*
	 * Return complex response computed at given freqs[0,...length] Should
	 * really check that length > 0
	 */
	public Cmplx[] getResponse(double[] freqs) throws PoleZeroStageException {
		// Some polezero responses (e.g., ANMO.IU.20.BN?) appear to have NO
		// zeros
		// if (poleAdded && zeroAdded && normalizationSet) {
		if (poleAdded && normalizationSet) {
			// Looks like the polezero info has been loaded ... so continue ...
		} else {
			throw new PoleZeroStageException(
					"[ PoleZeroStage-->getResponse Error: PoleZero info does not appear to be loaded! ]");
		}
		if (!(freqs.length > 0)) {
			throw new PoleZeroStageException(
					"[ PoleZeroStage-->getResponse Error: Input freqs[] has no zero length! ]");
		}
		Cmplx[] response = new Cmplx[freqs.length];
		for (int i = 0; i < freqs.length; i++) {
			try {
				response[i] = evalResp(freqs[i]);
			} catch (PoleZeroStageException e) {
				logger.error("PoleZeroStageException:", e);
			}
			// System.out.format("%12.4f\t%12.4f\n",freqs[i], response[i].mag()
			// );
		}
		return response;
	}

	/*
	 * SEED Manual - Appendix C PoleZero Representation for Analog Stages The
	 * first part of any seismic sensor will be some sort of linear system that
	 * operates in continuous time, rather than discrete time. Usually, any such
	 * system has a frequency response that is the ratio of two complex
	 * polynomials, each with real coefficients. These polynomials can be
	 * represented either by their coefficients or (preferably) by their roots
	 * (poles and zeros).
	 * 
	 * The polynomials are specified by their roots. The roots of the numerator
	 * polynomial are the instrument zeros, and the roots of the denominator
	 * polynomial are the instrument poles. Because the polynomials have real
	 * coefficients, complex poles and zeros will occur in complex conjugate
	 * pairs. By convention, the real parts of the poles and zeros are negative.
	 * 
	 * The expansion formula gives the response ( G(f) ) at any frequency f
	 * (Hz), using the variable: s = 2*pi*i*f (rad/sec) if the PoleZero transfer
	 * function is type A -OR- s = i*f (Hz) if the PoleZero transfer function is
	 * type B
	 */

	/*
	 * Evaluate the polezero response at a single frequency, f Return G(f) = A0
	 * * pole zero expansion Note that the stage sensitivity Sd is *not*
	 * included, so that the response from this stage should be approx. 1 (flat)
	 * at the mid range.
	 */
	private Cmplx evalResp(double f) throws PoleZeroStageException {
		Cmplx numerator = new Cmplx(1, 0);
		Cmplx denomenator = new Cmplx(1, 0);
		Cmplx s;
		Cmplx Gf;

		if (getStageType() == 'A') {
			s = new Cmplx(0.0, 2 * Math.PI * f);
		} else if (getStageType() == 'B') {
			s = new Cmplx(0.0, f);
		} else {
			throw new PoleZeroStageException(
					"[ PoleZeroStage-->evalResponse Error: Cannot evalResp a non-PoleZero Stage!]");
		}

		for (int j = 0; j < getNumberOfZeros(); j++) {
			numerator = Cmplx.mul(numerator, Cmplx.sub(s, zeros.get(j)));
		}
		for (int j = 0; j < getNumberOfPoles(); j++) {
			denomenator = Cmplx.mul(denomenator, Cmplx.sub(s, poles.get(j)));
		}
		Gf = Cmplx.mul(normalizationConstant, numerator);
		Gf = Cmplx.div(Gf, denomenator);
		return Gf;
	}

}
