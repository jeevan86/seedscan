CREATE OR REPLACE FUNCTION public.fnsclgetstations()
 RETURNS text
 LANGUAGE plpgsql
 STABLE
AS $function$
DECLARE
	stationString TEXT;
BEGIN


	
	SELECT 
	INTO stationString
		string_agg(
			CONCAT(
				  'S,'
				, pkstationID
				, ','
				, fkNetworkID
				, ','
				, st1."name"
				, ','
				, groupIDs
			    
			)
			, E'\n' 
		)
	FROM tblStation st1
	JOIN "tblGroup"
		ON st1.fkNetworkID = pkGroupID --to_char('2012-03-01'::date, 'J')::INT  || to_date(2456013::text, 'J')
	JOIN (
		SELECT "fkStationID" as statID, string_agg("fkGroupID"::text, ',') as groupIDs
			FROM "tblStationGroupTie"
			GROUP BY "fkStationID") as gst
		ON st1.pkStationID = gst.statID;

	RETURN stationString;
	
END;
$function$
