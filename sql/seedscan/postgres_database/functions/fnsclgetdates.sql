CREATE OR REPLACE FUNCTION public.fnsclgetdates()
 RETURNS text
 LANGUAGE plpgsql
 STABLE
AS $function$
DECLARE
	dateString TEXT;
BEGIN
	
	SELECT INTO dateString
		string_agg(
		CONCAT(
		      
		      'D,'
		    , year
		    , ','
		    , month
		    
		)
		, E'\n' 
		)
	FROM (
		Select DISTINCT date_part('year',date) as year, date_part('month',date) as month
		FROM tblDate
		ORDER BY year, month
	) dates; --to_char('2012-03-01'::date, 'J')::INT  || to_date(2456013::text, 'J')

	RETURN dateString;
END;
$function$
