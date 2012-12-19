CREATE OR REPLACE FUNCTION public.fnsclgetchannels(integer[])
 RETURNS text
 LANGUAGE plpgsql
 STABLE
AS $function$
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
	WHERE tblSensor.fkStationID = any(stationIDs) ;

	RETURN channelString;
	
END;
$function$
