package asl.metadata;

import java.util.ArrayList;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Class Blockette.
 * This is used to store blockette numbers and the fields associated.
 * 
 * @author Joel Edwards - USGS
 * @author Mike Hagerty
 */
public class Blockette implements java.io.Serializable {
	
	/** The Constant logger. */
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.Blockette.class);
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The number. */
	private int number;
	
	/** The fields. */
	private Hashtable<Integer, Field> fields;

	/** The last start id. */
	private int lastStartID = 0;

	/**
	 * Instantiates a new blockette.
	 *
	 * @param number the blockette number
	 */
	public Blockette(int number) {
		this.number = number;
		fields = new Hashtable<Integer, Field>();
	}

	/**
	 * Gets the blockette number.
	 *
	 * @return the number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * Adds the field data.
	 *
	 * @param fieldIdentifier the field identifier
	 * @param data the data
	 * @return true, if successful
	 * @throws BlocketteFieldIdentifierFormatException the blockette field identifier format exception
	 */
	public boolean addFieldData(String fieldIdentifier, String data)
			throws BlocketteFieldIdentifierFormatException {
		String[] range = fieldIdentifier.split("-");
		if (range.length < 1) {
			throw new BlocketteFieldIdentifierFormatException(
					"Invalid field identifier '" + fieldIdentifier + "'");
		}

		int start = Integer.parseInt(range[0]);
		String description = "";

		// The following determines if the field identifier is out of order.
		// We also use this to determine if a new Blockette was encountered
		// while parsing.
		//
		// Some IDs can be out of order, so we add exceptions for those.
		if ((lastStartID > start)
				&& ((((number != 52) || (lastStartID > 4)) && (start == 3)) || ((number == 52) && (start == 4)))) {
			return false;
		}

		Field field;
		int id = start;
		lastStartID = start;
		// We are dealing with multiple field identifiers
		if (range.length > 1) {
			int end = Integer.parseInt(range[1]);
			String[] dataItems = data.trim().split("\\s+");
			int index = dataItems.length - (end - start + 1);
			// System.out.format(" addFieldData(): start=%d end=%d index=%d dataItems.length=%d\n",start,end,index,dataItems.length);
			for (; index < dataItems.length; index++) {
				if (!fields.containsKey(id)) {
					field = new Field(id, description);
					fields.put(id, field);
				} else {
					field = fields.get(id);
				}
				String value = dataItems[index].trim();
				field.addValue(value);
				id++;
			}
		}
		// We are only dealing with a single field identifier
		else {
			String[] parts = data.split(":", 2);
			String value;
			if (parts.length > 1) {
				description = parts[0].trim();
				value = parts[1].trim();
			} else {
				value = parts[0].trim();
			}

			if (!fields.containsKey(id)) {
				field = new Field(id, description);
				fields.put(id, field);
			} else {
				field = fields.get(id);
			}
			field.addValue(value);
		}

		return true;
	}

	/**
	 * Gets the field value.
	 *
	 * @param fieldID the field id
	 * @param valueIndex the value index
	 * @return the field value
	 */
	public String getFieldValue(int fieldID, int valueIndex) {
		String value = null;
		if (fields.containsKey(fieldID)) {
			value = fields.get(fieldID).getValue(valueIndex);
		}
		return value;
	}

	/**
	 * Gets the field values.
	 *
	 * @param fieldID the field id
	 * @return the field values
	 */
	public ArrayList<String> getFieldValues(int fieldID) {
		ArrayList<String> values = null;
		if (fields.containsKey(fieldID)) {
			values = fields.get(fieldID).getValues();
		}
		return values;
	}

	/**
	 * Prints the blockette's fields.
	 */
	public void print() {
		System.out.format("Blockette Number:%02d\n", number);
		for (Integer key : fields.keySet()) {
			fields.get(key).print();
		}
	}

	// 
	/**
	 * Gets the blockette fields.
	 * MTH: added this to return the fields hashtable for this blockette
	 * @return the blockette's fields
	 */
	public Hashtable<Integer, Field> getFields() {
		if (fields == null) {
			logger.error("Blockette [{}] fields are null! ", this.number);
		}
		return fields;
	}
}
