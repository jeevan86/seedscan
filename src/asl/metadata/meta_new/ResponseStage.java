package asl.metadata.meta_new;

import java.io.Serializable;

/**
 * Every response stage type will contain generic info from SEED Blockette B058
 * (e.g., Stage Gain, Frequency of Gain) here.
 * 
 * In addition, info that is unique to a particular stage type will be stored in
 * the child class for that type (PoleZeroStage, PolynomialStage, etc.)
 * 
 * Stage Type SEED Blockette(s) Child Class
 * ---------------------------------------------------------- A [Analog Response
 * rad/sec] B053 PoleZeroStage B [Analog Response Hz] B053 PoleZeroStage P
 * [Polynomial] B062 PolynomialStage D [Digital] B054, B057 DigitalStage
 * 
 * @author Mike Hagerty <hagertmb@bc.edu>
 */
public abstract class ResponseStage implements Comparable<ResponseStage>,
		Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;
	
	/** The stage number. */
	protected int stageNumber;
	
	/** The stage type. */
	protected char stageType;
	
	/** The stage gain. */
	protected double stageGain;
	
	/** The stage gain frequency. */
	protected double stageGainFrequency;
	
	/** The input units. */
	protected int inputUnits;
	
	/** The output units.
	 * TODO: Never used in code Consider removal. 
	 */
	protected int outputUnits;
	
	/** The input units string. */
	protected String inputUnitsString;
	
	/** The output units string. */
	protected String outputUnitsString;

	/**
	 * Copy the response stage.
	 *
	 * @return the response stage
	 */
	abstract public ResponseStage copy();

	// constructor(s)
	/**
	 * Instantiates a new response stage.
	 *
	 * @param number the number
	 * @param type the type
	 * @param gain the gain
	 * @param frequency the frequency
	 */
	public ResponseStage(int number, char type, double gain, double frequency) {
		stageNumber = number;
		stageType = type;
		stageGain = gain;
		stageGainFrequency = frequency;
	}

	/**
	 * Set inputUnits of this stage: 0 = Unknown 1 = Displacement (m) 2 =
	 * Velocity (m/s) 3 = Acceleration (m/s^2) 4 = Pressure (Pa) 5 = Pressure
	 * (KPa) 6 = Magnetic Flux Density (Teslas - T) 7 = Magnetic Flux Density
	 * (nanoTeslas - NT) 8 = Degrees Centigrade (C) 9 = Degrees Orientation
	 * 0-360 (theta) 10 = Volts (V).
	 * 
	 * The input string is normalized to lower case before comparison.
	 * 
	 * TODO: Passing magic numbers is not good, These should be replaced with an enum.
	 *
	 * @param inputUnitsString the string containing the input units
	 */
	public void setInputUnits(String inputUnitsString) {
		this.inputUnitsString = inputUnitsString.toLowerCase();

		if (this.inputUnitsString.contains("displacement")) {
			inputUnits = 1;
		} else if (this.inputUnitsString.contains("velocity")) {
			inputUnits = 2;
		} else if (this.inputUnitsString.contains("acceleration")
				|| this.inputUnitsString.contains("m/s**2".toLowerCase())) {
			inputUnits = 3;
		} else if (this.inputUnitsString.contains("pressure")) {
			if (this.inputUnitsString.contains("kpa")) {
				inputUnits = 5;
			} else {
				inputUnits = 4;
			}
		} else if (this.inputUnitsString.contains("magnetic")) {
			if (this.inputUnitsString.contains("nanoteslas")) {
				inputUnits = 7;
			} else {
				inputUnits = 6;
			}
		} else if (this.inputUnitsString.contains("degrees")) {
			if (this.inputUnitsString.contains("centigrade")) {
				inputUnits = 8;
			} else {
				inputUnits = 9;
			}
		} else if (this.inputUnitsString.contains("volts")) {
			inputUnits = 10;
		} else { // We didn't find anything
			inputUnits = 0;
		}

	}

	/**
	 * Sets the output unit string.
	 *
	 * @param outputUnitsString the new output units, this is normalized to lower case
	 */
	public void setOutputUnits(String outputUnitsString) {
		this.outputUnitsString = outputUnitsString.toLowerCase();
	}

	/**
	 * Gets the input units.
	 * 
	 * @see asl.metadata.meta_new.ResponseStage.setInputUnits(String)
	 *
	 * @return the input units
	 */
	public int getInputUnits() {
		return inputUnits;
	}

	/**
	 * Gets the input units string.
	 *
	 * @return the input units string
	 */
	public String getInputUnitsString() {
		return inputUnitsString;
	}

	/**
	 * Gets the output units string.
	 *
	 * @return the output units string
	 */
	public String getOutputUnitsString() {
		return outputUnitsString;
	}

	/**
	 * Gets the stage gain frequency.
	 *
	 * @return the stage gain frequency
	 */
	public double getStageGainFrequency() {
		return stageGainFrequency;
	}

	/**
	 * Gets the stage number.
	 *
	 * @return the stage number
	 */
	public int getStageNumber() {
		return stageNumber;
	}

	/**
	 * Gets the stage type.
	 *
	 * @return the stage type
	 */
	public char getStageType() {
		return stageType;
	}

	/**
	 * Gets the stage gain.
	 *
	 * @return the stage gain
	 */
	public double getStageGain() {
		return stageGain;
	}

	/**
	 * Prints the response stage as set in the overridden toString().
	 * @see asl.metadata.meta_new.ResponseStage.toString()
	 */
	public void print() {
		System.out.println(this);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");
		result.append(String.format(
				"Stage:%d  [Type='%1s'] Gain=%.2f FreqOfGain=%.2f\n",
				stageNumber, stageType, stageGain, stageGainFrequency));
		result.append(String.format("Units In:[%s]  Units Out:[%s]\n",
				inputUnitsString, outputUnitsString));
		return result.toString();
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ResponseStage stage) {
		if (this.getStageNumber() > stage.getStageNumber()) {
			return 1;
		} else if (this.getStageNumber() < stage.getStageNumber()) {
			return -1;
		} else { // Stage numbers must be the same
			return 0;
		}
	}

}
