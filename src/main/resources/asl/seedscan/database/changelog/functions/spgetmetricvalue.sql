-- Function: spgetmetricvalue(date, character varying, character varying, character varying, character varying, character varying)

-- DROP FUNCTION spgetmetricvalue(date, character varying, character varying, character varying, character varying, character varying);

CREATE OR REPLACE FUNCTION spgetmetricvalue(
    date,
    character varying,
    character varying,
    character varying,
    character varying,
    character varying)
  RETURNS double precision AS
$BODY$
DECLARE
	nDate alias for $1;
	metricName alias for $2;
	networkName alias for $3;
	stationName alias for $4;
	locationName alias for $5;
	channelName alias for $6;
	value double precision;
	debug text;

BEGIN
--select name from tblStation into debug;
--RAISE NOTICE 'stationID(%)', debug;

	SELECT
	  tblMetricData.value
	FROM

	  public.tblmetricdata,
	  public.tblmetric,
	  public.tblchannel,
	  public.tblsensor,
	  public.tblstation,
	  public."tblGroup"
	WHERE
	  --JOINS
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
	INTO value;
	RETURN value;

    END;
$BODY$
  LANGUAGE plpgsql STABLE
  COST 100;