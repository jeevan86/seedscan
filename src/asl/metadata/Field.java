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
package asl.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class Field
    implements java.io.Serializable
{
    private static final Logger logger = LoggerFactory.getLogger(asl.metadata.Field.class);
    private static final long serialVersionUID = 1L;

    private int fieldID;
    private String description;
    private ArrayList<String> values;

    // Constructor(s)
    public Field(int fieldID, String description)
    {
        this.fieldID = fieldID;
        this.description = description;
        values = new ArrayList<String>();
    }
    public int getID()
    {
        return fieldID;
    }

    // description
    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    // values
    public void addValue(String value)
    {
        this.values.add(value);
    }

    public void addValues(Collection<String> values)
    {
        for (String value: values) {
            this.values.add(value);
        }
    }

    public String getValue(int index)
    {
        String value = null;
        if (values.size() > index) {
            value = new String(values.get(index));
        }
        return value;
    }

    public ArrayList<String> getValues()
    {
        return values;
    }

    public void print()
    {
        for (String value : values) {
            System.out.format("Field:%02d Description:%s Value:%s\n",fieldID, description, value );
        }
    }

}

