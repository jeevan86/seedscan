CREATE OR REPLACE FUNCTION public.fnsclgetstationplotdata(integer, integer, date, date)
 RETURNS text
 LANGUAGE plpgsql
 STABLE
AS $function$
DECLARE
	stationID alias for $1;
	metricID alias for $2;
	startDate alias for $3;
	endDate alias for $4;
	stationPlotData TEXT;
	computeType int;
BEGIN
	
	Select fkComputeTypeID from tblMetric where pkMetricID = metricID INTO computeType;
	CASE computeType
		--Metric Data
		WHEN 1 THEN
			--Average across total number of values
			SELECT INTO stationPlotData string_agg(CONCAT(sdate, ',',avg), E'\n') FROM (
				SELECT to_date(md1.date::text, 'J') as sdate, round((SUM(md1.value)/count(md1.*))::numeric, 4) as avg
				FROM tblMetricData md1
				JOIN tblChannel ch1
					ON ch1.pkChannelID = md1.fkChannelID
					AND NOT ch1."isIgnored"
				JOIN tblSensor sen1
					ON ch1.fkSensorID = sen1.pkSensorID
				WHERE sen1.fkStationID = stationID
					AND 
					md1.date >= to_char(startDate, 'J')::INT
					AND md1.date <= to_char(endDate, 'J')::INT
					AND md1.fkMetricID = metricID
				GROUP BY md1.date ) stations;
		WHEN 2 THEN
			--Average across days NOT ACCURATE
			select '2' into stationPlotData;
		WHEN 3 THEN
			--Count all values, return sum
			SELECT INTO stationPlotData string_agg(CONCAT(sdate, ',',avg), E'\n') FROM (
				SELECT to_date(md1.date::text, 'J') as sdate, round(SUM(md1.value)::numeric, 4) as avg
				FROM tblMetricData md1
				JOIN tblChannel ch1
					ON ch1.pkChannelID = md1.fkChannelID
					AND NOT ch1."isIgnored"
				JOIN tblSensor sen1
					ON ch1.fkSensorID = sen1.pkSensorID
				WHERE sen1.fkStationID = stationID
					AND 
					md1.date >= to_char(startDate, 'J')::INT
					AND md1.date <= to_char(endDate, 'J')::INT
					AND md1.fkMetricID = metricID
				GROUP BY md1.date ) stations;
		--Calibration Data
		WHEN 5 THEN
			--Calculate data between last calibrations
			select '5' into stationPlotData;
		WHEN 6 THEN
			--Average across number of values
			select '6' into stationPlotData;
		ELSE
			--Insert error into error log
			select 'Error' into stationPlotData;
	END CASE;

	
	RETURN stationPlotData;
END;
$function$
