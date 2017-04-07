-- Function: spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea)

-- DROP FUNCTION spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea);

CREATE OR REPLACE FUNCTION spinsertmetricdata(
    date,
    character varying,
    character varying,
    character varying,
    character varying,
    character varying,
    double precision,
    bytea)
  RETURNS void AS
$BODY$
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
--INSERT INTO tblerrorlog (errortime, errormessage) values (CURRENT_TIMESTAMP,'It inserted'||nDate||' '||locationName||' '||channelName||' '||stationName||' '||metricName);

    IF fnsclisnumeric(valueIN::TEXT) = FALSE THEN
	INSERT INTO tblerrorlog (errortime, errormessage)
		VALUES (
			CURRENT_TIMESTAMP,
			'Non Numeric value: Nothing Inserted '||nDate||' '||locationName||' '||channelName||' '||stationName||' '||metricName||' '||valueIN);
	RETURN;
    END IF;

--Insert network if doesn't exist then get ID

    LOCK TABLE "tblGroup" IN SHARE ROW EXCLUSIVE MODE;
    INSERT INTO "tblGroup" (name,"fkGroupTypeID")
	SELECT networkName, 1  --Group Type 1 is Network
	WHERE NOT EXISTS (
	    SELECT * FROM "tblGroup" WHERE name = networkName
	);

    SELECT pkGroupID
        FROM "tblGroup"
        WHERE name = networkName
    INTO networkID;

--Insert station if doesn't exist then get ID
    LOCK TABLE tblStation IN SHARE ROW EXCLUSIVE MODE;
    INSERT INTO tblStation (name,fkNetworkID)
	SELECT stationName, networkID
	WHERE NOT EXISTS (
	    SELECT * FROM tblStation WHERE name = stationName AND fkNetworkID = networkID
	);

    SELECT pkStationID
        FROM tblStation
        WHERE name = stationName AND fkNetworkID = networkID
    INTO stationID;

--Ties the Station to its Network for the GUI to use.
    LOCK TABLE "tblStationGroupTie" IN SHARE ROW EXCLUSIVE MODE;
    INSERT INTO "tblStationGroupTie" ("fkGroupID", "fkStationID")
	SELECT networkID, stationID
	WHERE NOT EXISTS (
	    SELECT * FROM "tblStationGroupTie" WHERE "fkGroupID" = networkID AND "fkStationID" = stationID
	);

--Insert sensor if doesn't exist then get ID
    LOCK TABLE tblSensor IN SHARE ROW EXCLUSIVE MODE;
    INSERT INTO tblSensor (location,fkStationID)
	SELECT locationName, stationID
	WHERE NOT EXISTS (
	    SELECT * FROM tblSensor WHERE location = locationName AND fkStationID = stationID
	);

    SELECT pkSensorID
        FROM tblSensor
        WHERE location = locationName AND fkStationID = stationID
    INTO sensorID;

--Insert channel if doesn't exist then get ID
    LOCK TABLE tblChannel IN SHARE ROW EXCLUSIVE MODE;
    INSERT INTO tblChannel (name, fkSensorID)
	SELECT channelName, sensorID
	WHERE NOT EXISTS (
	    SELECT * FROM tblChannel WHERE name = channelName AND fkSensorID = sensorID
	);

    SELECT pkChannelID
        FROM tblChannel
        WHERE name = channelName AND fkSensorID = sensorID
    INTO channelID;

--Insert metric if doesn't exist then get ID
    LOCK TABLE tblMetric IN SHARE ROW EXCLUSIVE MODE;
    INSERT INTO tblMetric (name, fkComputeTypeID, displayName)
	SELECT metricName, 1, metricName --Compute Type 1 is averaged over channel and days.
	WHERE NOT EXISTS (
	    SELECT * FROM tblMetric WHERE name = metricName
	);

    SELECT pkMetricID
        FROM tblMetric
        WHERE name = metricName
    INTO metricID;

--Insert hash if doesn't exist then get ID
    LOCK TABLE tblHash IN SHARE ROW EXCLUSIVE MODE;
    INSERT INTO tblHash (hash)
	SELECT hashIN
	WHERE NOT EXISTS (
	    SELECT * FROM tblHash WHERE hash = hashIN
	);

   --select pkHashID from tblStation into debug;
--RAISE NOTICE 'stationID(%)', debug;
    SELECT "pkHashID"
        FROM tblHash
        WHERE hash = hashIN
    INTO hashID;

--Insert date into tblDate
    LOCK TABLE tblDate IN SHARE ROW EXCLUSIVE MODE;
    BEGIN
    INSERT INTO tblDate (pkDateID, date)
	SELECT to_char(nDate, 'J')::INT, nDate
	WHERE NOT EXISTS (
	    SELECT * FROM tblDate WHERE date = nDate
	);


    EXCEPTION WHEN unique_violation THEN
        INSERT INTO tblErrorLog (errortime, errormessage)
	    VALUES (CURRENT_TIMESTAMP, "tblDate has a date with incorrect pkDateID date:"
	    +to_char(nDate, 'J')::INT);
    END;
--Insert/Update metric value for day
    UPDATE tblMetricData
	SET value = valueIN, "fkHashID" = hashID
	WHERE date = to_char(nDate, 'J')::INT AND fkMetricID = metricID AND fkChannelID = channelID;
    IF NOT FOUND THEN
    BEGIN
	INSERT INTO tblMetricData (fkChannelID, date, fkMetricID, value, "fkHashID")
	    VALUES (channelID, to_char(nDate, 'J')::INT, metricID, valueIN, hashID);
    --We could remove this possibility with a table lock, but I fear locking such a large table.
    EXCEPTION WHEN unique_violation THEN
	INSERT INTO tblErrorLog (errortime, errormessage)
	    VALUES (CURRENT_TIMESTAMP, "Multiple simultaneous data inserts for metric:"+metricID+
	    " date:"+to_char(nDate, 'J')::INT);
    END;
    END IF;


    END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;