package asl.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Field implements Serializable {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.Field.class);
	private static final long serialVersionUID = 1L;

	private int fieldID;
	private String description;
	private ArrayList<String> values;

	// Constructor(s)
	public Field(int fieldID, String description) {
		this.fieldID = fieldID;
		this.description = description;
		values = new ArrayList<>();
	}

	public int getID() {
		return fieldID;
	}

	// description
	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	// values
	public void addValue(String value) {
		this.values.add(value);
	}

	public void addValues(Collection<String> values) {
		for (String value : values) {
			this.values.add(value);
		}
	}

	public String getValue(int index) {
		String value = null;
		if (values.size() > index) {
			value = values.get(index);
		}
		return value;
	}

	public ArrayList<String> getValues() {
		return values;
	}

	public void print() {
		for (String value : values) {
			logger.info("Field:{} Description:{} Value:{}\n", String.format("%02d", fieldID),
					description, value);
		}
	}

}
