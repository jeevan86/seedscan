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

package seed;

import java.io.ByteArrayOutputStream;

/**
 * OpaqueContext
 *
 * Created on October 4, 2012 @ 06:06 MDT
 *
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 */
class OpaqueContext
{
    private String tagString = "";
    private int recordNumber = -1;
    private OpaqueState state = OpaqueState.INIT;
    private ByteArrayOutputStream byteStream;

    public OpaqueContext(String tagString)
    {
        this.tagString = tagString;
        byteStream = new ByteArrayOutputStream();
    }

    public String getTagString()
    {
        return tagString;
    }

    public void setRecordNumber(int recordNumber)
    {
        this.recordNumber = recordNumber;
    }

    public int getRecordNumber()
    {
        return recordNumber;
    }

    public void setState(OpaqueState state)
    {
        this.state = state;
    }

    public OpaqueState getState()
    {
        return state;
    }

    public boolean isComplete()
    {
        boolean complete = false;
        switch (state) {
            case RECORD:
            case STREAM_END:
            case FILE_END:
                complete = true;
		default:
			break;

        }
        return complete;
    }

    public void update(byte[] data, int offset, int length)
    {
        byteStream.write(data, offset, length);
    }

    public byte[] getData()
    {
        return byteStream.toByteArray();
    }
}
