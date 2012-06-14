/**
 * 
 */
package asl.ofcweb;

import asl.fmash.Fmash;
import seed.Utility;

/**
 * @author fshelly
 * Class to decompress Opaque blockette 2000 seed records written to the OFC
 * channel for the Falcon system monitor.
 */
public class OFCb2000
{
    public int []   average_data=null;
    public int []   low_data=null;
    public int []   high_data=null;
    public long   	start_time;
    public long   	end_time;
    public String  	name;
    public boolean	bContinuation;
    public int	    type;
    public int	    data_record_length;
    public int	    data_offset;
    public int	    record_number;
    public int	    word_order;
    public byte	    data_flags;
    public int	    number_header_fields;
    public String   data_header_fields;

    private byte [] continueData;

    public boolean get_big_endian() {return word_order != 0;}
    public String get_name()        {return name;}
    public int get_type()           {return type;}
    public int get_length()         {return data_record_length;}
    public int get_data_offset()    {return data_offset;}
    public int get_record_number()  {return record_number;}
    public int get_word_order()     {return word_order;}
    public byte get_data_flags()    {return data_flags;}
    public String get_header_fields()   {return data_header_fields;}
    public boolean get_Continuation()   {return bContinuation;}
    public long get_start_time()    {return start_time;}
    public long get_end_time()      {return end_time;}
    public int[] get_average_data() {return average_data;}
    public int[] get_low_data()     {return low_data;}
    public int[] get_high_data()    {return high_data;}

    private boolean bSwapBytes;

    public OFCb2000(byte[] blockette2000)
    {
        bContinuation = false;

        word_order = blockette2000[12];
        bSwapBytes = word_order == 0;
        type = Utility.uBytesToInt(blockette2000[0], blockette2000[1], bSwapBytes);
        data_record_length = Utility.uBytesToInt(blockette2000[4], blockette2000[5], bSwapBytes);
        data_offset = Utility.uBytesToInt(blockette2000[6], blockette2000[7], bSwapBytes);
        record_number = Utility.bytesToInt(blockette2000[8],  blockette2000[9],
                                           blockette2000[10], blockette2000[11], bSwapBytes);

        data_flags = blockette2000[13];
        number_header_fields = ((int)blockette2000[14] & 0xff);
        data_header_fields = new String(blockette2000).substring(15,data_offset);

        if (type == 2000)
        {
            // Assume we have an OFC opaque blockette
            // Make sure it is not a continuation blockette
            if ((data_flags & 0x28) != 0)
            {
                // User should never call constructor on a continuation blockette
                System.err.println("OFCb2000 constructor called with a continuation blockette");
                return;
            } // were any continuation bits set

            // Handle case where this starts a continuation series
            if (((data_flags & 0x0c) == 0x04) ||
                    ((data_flags & 0x30) == 0x10))
            {
                bContinuation = true;
                continueData = new byte[data_record_length - 15];
                System.arraycopy(blockette2000, data_offset, 
                        continueData, 0, data_record_length-data_offset);
                return;
            }

            // Decompress the data
            byte[] rawData = new byte[data_record_length - 15];
            System.arraycopy(blockette2000, data_offset, 
                    rawData, 0, data_record_length-data_offset);
            Fmash crack = new Fmash(rawData, word_order != 0);
            name = crack.getDescription();
            start_time = crack.startTime;
            end_time = crack.endTime;

            if (crack.getRowCount() > 0)
            {
                average_data = new int[crack.getRowCount()];
                low_data = new int[crack.getRowCount()];
                high_data = new int[crack.getRowCount()];
            }
            for (int i=0; i < crack.getRowCount(); i++)
            { 
                average_data[i] = (int) crack.rows.get(i).m_average;
                low_data[i] = (int) crack.rows.get(i).m_low;
                high_data[i] = (int) crack.rows.get(i).m_high;
            }

        } // We have an OFC Falcon blockette 2000

    } // constructor OFCb2000()

    // Should be called when bContinue is true
    // When bContinue goes back to false, record is done
    public boolean Continue(byte[] blockette2000)
    {
        word_order = blockette2000[12];
        bSwapBytes = word_order == 0;
        type = Utility.uBytesToInt(blockette2000[0], blockette2000[1], bSwapBytes);
        data_record_length = Utility.uBytesToInt(blockette2000[4], blockette2000[5], bSwapBytes);
        data_offset = Utility.uBytesToInt(blockette2000[6], blockette2000[7], bSwapBytes);
        record_number = Utility.bytesToInt(blockette2000[8], blockette2000[9],
                                           blockette2000[10], blockette2000[11], bSwapBytes);

        data_flags = blockette2000[13];
        number_header_fields = ((int)blockette2000[14] & 0xff);
        data_header_fields = new String(blockette2000).substring(15,data_offset);

        if (type == 2000)
        {
            // We have an OFC opaque blockette
            // Make sure it is a continuation blockette
            if (((data_flags & 0x28) == 0) ||
                    ((data_flags & 0x0c) == 0x04) ||
                    ((data_flags & 0x30) == 0x10))
            {
                // User should never call Continue on a non continuation blockette
                System.err.println("OFCb2000 Continue called with a non continuation blockette");
                bContinuation = false;
                return bContinuation;
            } // were any continuation bits set

            // Append the new data to the old
            byte[] rawData = new byte[data_record_length - 15 + continueData.length];
            System.arraycopy(blockette2000, data_offset, 
                    rawData, continueData.length, data_record_length-data_offset);
            System.arraycopy(continueData, 0, rawData, 0, continueData.length);
            continueData = rawData;			

            // See if this is the last record in continuation stream
            if (!(((data_flags & 0x0c) == 0x08) ||
                        ((data_flags & 0x30) == 0x30)))
            {
                // We've appended the next round of data, done for now
                return bContinuation;
            }

            // Decompress the data
            Fmash crack = new Fmash(rawData, word_order != 0);
            name = crack.getDescription();
            if (crack.getRowCount() > 0)
            {
                average_data = new int[crack.getRowCount()];
                low_data = new int[crack.getRowCount()];
                high_data = new int[crack.getRowCount()];
            }
            for (int i=0; i < crack.getRowCount(); i++)
            { 
                average_data[i] = (int) crack.rows.get(i).m_average;
                low_data[i] = (int) crack.rows.get(i).m_low;
                high_data[i] = (int) crack.rows.get(i).m_high;
            }

            bContinuation = false;
        } // We have an OFC Falcon blockette 2000

        return bContinuation;
    } // Continue()

} // class OFCb2000
