CREATE OR REPLACE FUNCTION public.fnsclgetchannelplotdata(integer, integer, date, date)
 RETURNS text
 LANGUAGE plpgsql
 STABLE
AS $function$
DECLARE
	channelID alias for $1;
	metricID alias for $2;
	startDate alias for $3;
	endDate alias for $4;
	channelPlotData TEXT;
	computeType int;
BEGIN
	
	Select fkComputeTypeID from tblMetric where pkMetricID = metricID INTO computeType;
	CASE computeType
		--Metric Data
		WHEN 1 THEN
			--Average across total number of values
			SELECT INTO channelPlotData string_agg(CONCAT(sdate, ',',avg), E'\n') FROM (
				SELECT to_date(md1.date::text, 'J') as sdate, round(md1.value::numeric, 4) as avg
				FROM tblMetricData md1
				WHERE md1.fkChannelID = channelID
					AND 
					md1.date >= to_char(startDate, 'J')::INT
					AND md1.date <= to_char(endDate, 'J')::INT
					AND md1.fkMetricID = metricID
				 ) channels;
		WHEN 2 THEN
			--Average across days NOT ACCURATE
			select '2' into channelPlotData;
		WHEN 3 THEN
			--Count all values, return sum
			SELECT INTO channelPlotData string_agg(CONCAT(sdate, ',',avg), E'\n') FROM (
				SELECT to_date(md1.date::text, 'J') as sdate, round(md1.value::numeric, 4) as avg
				FROM tblMetricData md1
				WHERE md1.fkchannelID = channelID
					AND 
					md1.date >= to_char(startDate, 'J')::INT
					AND md1.date <= to_char(endDate, 'J')::INT
					AND md1.fkMetricID = metricID
				) stations;
		--Calibration Data
		WHEN 5 THEN
			--Calculate data between last calibrations
			select '5' into channelPlotData;
		WHEN 6 THEN
			--Average across number of values
			select '6' into channelPlotData;
		ELSE
			--Insert error into error log
			select 'Error' into channelPlotData;
	END CASE;

	
	RETURN channelPlotData;
END;
$function$
