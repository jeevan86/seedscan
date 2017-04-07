-- Function: spgetmetricvaluedigest(date, character varying, character varying, character varying, character varying, character varying)

-- DROP FUNCTION spgetmetricvaluedigest(date, character varying, character varying, character varying, character varying, character varying);

CREATE OR REPLACE FUNCTION spgetmetricvaluedigest(
    IN date,
    IN character varying,
    IN character varying,
    IN character varying,
    IN character varying,
    IN character varying,
    OUT bytea)
  RETURNS bytea AS
$BODY$
DECLARE
	nDate alias for $1;
	metricName alias for $2;
	networkName alias for $3;
	stationName alias for $4;
	locationName alias for $5;
	channelName alias for $6;
	hash alias for $7;
	debug text;

BEGIN

--select name from tblStation into debug;
--RAISE NOTICE 'stationID(%)', debug;

--SELECT to_char('2012-06-19'::DATE, 'J')::INT;

	SELECT
	  tblHash.hash
	FROM
	  public.tblhash,
	  public.tblmetricdata,
	  public.tblmetric,
	  public.tblchannel,
	  public.tblsensor,
	  public.tblstation,
	  public."tblGroup"
	WHERE
	  --JOINS
	  tblmetricdata."fkHashID" = tblHash."pkHashID" AND
	  tblmetricdata.fkmetricid = tblmetric.pkmetricid AND
	  tblmetricdata.fkchannelid = tblchannel.pkchannelid AND
	  tblchannel.fksensorid = tblsensor.pksensorid AND
	  tblsensor.fkstationid = tblstation.pkstationid AND
	  tblstation.fknetworkid = "tblGroup".pkgroupid AND
	  --Criteria
	  tblMetric.name = metricName AND
	  "tblGroup".name = networkName AND
	  tblStation.name = stationName AND
	  tblSensor.location = locationName AND
	  tblChannel.name = channelName AND
	  tblMetricData.date = to_char(nDate, 'J')::INT
	INTO hash;


    END;
$BODY$
  LANGUAGE plpgsql STABLE
  COST 100;