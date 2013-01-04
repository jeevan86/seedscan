CREATE OR REPLACE FUNCTION public.fnsclgetgroups()
 RETURNS text
 LANGUAGE plpgsql
 STABLE
AS $function$
DECLARE
	groupString TEXT;
BEGIN


	
	SELECT 
	INTO groupString
		string_agg( DISTINCT
			CONCAT(
				  'G,'
				, gst."fkGroupID"
				, ','
				, gp."name"
				, ','
				, gp."fkGroupTypeID"

			    
			)
			, E'\n' 
		)
	FROM "tblStationGroupTie" gst
	JOIN "tblGroup" gp
		ON gst."fkGroupID" = gp.pkGroupID;

	RETURN groupString;
	
END;
$function$
