CREATE OR REPLACE FUNCTION public.fnsclgetmetrics()
 RETURNS text
 LANGUAGE plpgsql
 STABLE
AS $function$
DECLARE
	metricString TEXT;
BEGIN


	
	SELECT 
	INTO metricString
		string_agg( 
			CONCAT(
				  'M,'
				, pkMetricID
				, ','
				, name -- We only want the name during testing to prevent confusion to groups are created.
				--coalesce(DisplayName, name, 'No name')

			    
			)
			, E'\n' 
		)
	FROM tblMetric;

	RETURN metricString;
	
END;
$function$
