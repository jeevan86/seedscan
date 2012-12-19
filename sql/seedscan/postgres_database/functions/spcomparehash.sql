CREATE OR REPLACE FUNCTION public.spcomparehash(date, character varying, character varying, character varying, character varying, character varying, bytea)
 RETURNS boolean
 LANGUAGE plpgsql
 STABLE
AS $function$
DECLARE
	nDate alias for $1;
	metricName alias for $2;
	networkName alias for $3;
	stationName alias for $4;
	locationName alias for $5;
	channelName alias for $6;
	hashIN alias for $7;
	hashID int;
	debug text;

BEGIN
--select name from tblStation into debug;
--RAISE NOTICE 'stationID(%)', debug;

	SELECT 
	  tblhash."pkHashID"
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
	  tblhash."pkHashID" = tblmetricdata."fkHashID" AND
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
	  tblChannel.name = channelName
	  
	INTO hashID;

	IF hashID IS NOT NULL THEN
		RETURN 1;
	ELSE
		RETURN 0;
	END IF;
        
    END;
$function$
