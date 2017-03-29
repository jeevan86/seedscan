-- Function: fnsclgetchannels(integer[])

-- DROP FUNCTION fnsclgetchannels(integer[]);

CREATE OR REPLACE FUNCTION fnsclgetchannels(integer[])
  RETURNS text AS
$BODY$
DECLARE
    stationIDs alias for $1;
    channelString TEXT;
BEGIN
    SELECT 
    INTO channelString
        string_agg( 
            CONCAT(
                  'C,'
                , pkchannelID
                , ','
                , name
                , ','
                , tblSensor.location
                , ','
                , fkStationID
            )
            , E'\n' 
        )
    FROM tblChannel
    JOIN tblSensor
        ON tblChannel.fkSensorID = tblSensor.pkSensorID
    WHERE tblSensor.fkStationID = any(stationIDs)
    AND NOT tblChannel."isIgnored" ;

    RETURN channelString;
    
END;
$BODY$
  LANGUAGE plpgsql STABLE
  COST 100;

