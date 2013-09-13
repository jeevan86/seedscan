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
package asl.seedsplitter;

import org.apache.log4j.Logger;

/**
 * @author 	Joel D. Edwards <jdedwards@usgs.gov>
 *
 * A subclass of DataSet which pairs Falcon specific information with the data.
 */
public class FalconDataSet 
    extends DataSet
{
    private static final Logger logger = Logger.getLogger("asl.seedsplitter.FalconDataSet");

    private int     m_id;
    private int     m_type;
    private String  m_description;

    /**
     * Constructor.
     */
    public FalconDataSet()
    {
        super();
        m_id = 0;
        m_type = 0;
        m_description = null;
    }

    /**
     * Sets the source id for this channel.
     * 
     * @param   id     the source id for this channel.
     */
    public void setId(int id)
    {
        m_id = id;
    }

    /**
     * Sets the data type code for this channel.
     * 
     * @param   type     the type of data for this channel.
     */
    public void setType(int type)
    {
        m_type = type;
    }

    /**
     * Sets the description of the falcon data channel.
     * 
     * @param   description     falcon data channel
     */
    public void setDescription(String description)
    {
        m_description = new String(description);
    }


    /**
     * Returns the source id for this channel.
     *
     * @return  source id for this channel.
     */
    public int getId()
    {
        return m_id;
    }

    /**
     * Returns the data type code for this channel.
     *
     * @return  data type for this channel
     */
    public int getType()
    {
        return m_type;
    }

    /**
     * Returns the description of the falcon data channel.
     *
     * @return  falcon data channel description
     */
    public String getDescription()
    {
        return m_description;
    }

}

