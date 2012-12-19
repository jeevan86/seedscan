CREATE OR REPLACE FUNCTION public.spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea)
 RETURNS void
 LANGUAGE plpgsql
AS $function$
DECLARE
	nDate alias for $1;
	metricName alias for $2;
	networkName alias for $3;
	stationName alias for $4;
	locationName alias for $5;
	channelName alias for $6;
	valueIN alias for $7;
	hashIN alias for $8;
	networkID int;
	stationID int;
	sensorID int;
	channelID int;
	metricID int;
	hashID int;
	debug text;

BEGIN
--select name from tblStation into debug;
--RAISE NOTICE 'stationID(%)', debug;

--Insert network if doesn't exist then get ID
    BEGIN
        INSERT INTO "tblGroup" (name,"fkGroupTypeID") VALUES (networkName, 1); --Group Type 1 is Network
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
    SELECT pkGroupID
        FROM "tblGroup"
        WHERE name = networkName
    INTO networkID;

--Insert station if doesn't exist then get ID
    BEGIN
        INSERT INTO tblStation(name,fkNetworkID) VALUES (stationName, networkID);
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
    SELECT pkStationID
        FROM tblStation
        WHERE name = stationName AND fkNetworkID = networkID
    INTO stationID;
    
    BEGIN --Ties the Station to its Network for the GUI to use.
        INSERT INTO "tblStationGroupTie" ("fkGroupID", "fkStationID")
		VALUES (networkID, stationID);
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;

--Insert sensor if doesn't exist then get ID
    BEGIN
        INSERT INTO tblSensor(location,fkStationID) VALUES (locationName, stationID); 
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
    SELECT pkSensorID
        FROM tblSensor
        WHERE location = locationName AND fkStationID = stationID
    INTO sensorID;
--Insert channel if doesn't exist then get ID
    BEGIN
        INSERT INTO tblChannel(name,fkSensorID) VALUES (channelName, sensorID); 
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
    SELECT pkChannelID
        FROM tblChannel
        WHERE name = channelName AND fkSensorID = sensorID
    INTO channelID;
--Insert metric if doesn't exist then get ID
    BEGIN
        INSERT INTO tblMetric(name, fkComputeTypeID, displayName) VALUES (metricName, 1, metricName); --Compute Type 1 is averaged over channel and days.
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
    SELECT pkMetricID
        FROM tblMetric
        WHERE name = metricName
    INTO metricID;

--Insert hash if doesn't exist then get ID
    BEGIN
        INSERT INTO tblHash(hash) VALUES (hashIN); 
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
   --select pkHashID from tblStation into debug;
--RAISE NOTICE 'stationID(%)', debug;
    SELECT "pkHashID"
        FROM tblHash
        WHERE hash = hashIN
    INTO hashID;
    
--Insert date into tblDate
    BEGIN
        INSERT INTO tblDate (pkDateID, date)
	    VALUES (to_char(nDate, 'J')::INT, nDate);
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
--Insert/Update metric value for day
    UPDATE tblMetricData 
	SET value = valueIN, "fkHashID" = hashID 
	WHERE date = to_char(nDate, 'J')::INT AND fkMetricID = metricID AND fkChannelID = channelID;
    IF NOT found THEN
    BEGIN
	INSERT INTO tblMetricData (fkChannelID, date, fkMetricID, value, "fkHashID") 
	    VALUES (channelID, to_char(nDate, 'J')::INT, metricID, valueIN, hashID);
    EXCEPTION WHEN unique_violation THEN
	INSERT INTO tblErrorLog (errortime, errormessage)
	    VALUES (CURRENT_TIMESTAMP, "Multiple simultaneous data inserts for metric:"+metricID+
	    " date:"+to_char(nDate, 'J')::INT);
    END;
    END IF;
    
        
    END;
$function$
