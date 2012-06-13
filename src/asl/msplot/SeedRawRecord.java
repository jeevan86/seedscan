package asl.msplot;

public class SeedRawRecord
{
    // Pass in a miniseed header record to generate space for full record
    SeedRawRecord(byte frame[])
    {
        record = new byte[DEFAULT_SEED_RECORD_SIZE];
        for (int i=0; i < frame.length && i < record.length; i++)
        {
            record[i] = frame[i];
        }
    } // constructor SeedRawRecord()

    public byte record[];

    public final int DEFAULT_SEED_RECORD_SIZE = 512;

    byte [] GetRecord()
    {
        return record;
    }

} // class SeedRawRecord
