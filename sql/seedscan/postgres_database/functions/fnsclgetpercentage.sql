CREATE OR REPLACE FUNCTION public.fnsclgetpercentage(double precision, character varying)
 RETURNS text
 LANGUAGE plpgsql
 STABLE
AS $function$
DECLARE
	valueIn alias for $1;
	metricName alias for $2;
	percent double precision;
	isNum boolean;
BEGIN

	SELECT TRUE INTO isNum;
	CASE metricName

		--State of Health
		WHEN 'AvailabilityMetric' THEN
			SELECT valueIN INTO percent;
		WHEN 'GapCountMetric' THEN
			SELECT (100.0 - 20*log(valueIn +1)) INTO percent;

		--Coherence
		WHEN 'CoherencePBM:2-4' THEN
			SELECT FALSE INTO isNum; --add formula once created
		WHEN 'CoherencePBM:4-8' THEN
			SELECT (100 * power(valueIn, 2)) INTO percent;
		WHEN 'CoherencePBM:18-22' THEN
			SELECT (100 * power(valueIn, 0.708)) INTO percent;
		WHEN 'CoherencePBM:90-110' THEN
			SELECT (100 * power(valueIn, 0.146)) INTO percent;
		WHEN 'CoherencePBM:200-500' THEN
			SELECT (100 * power(valueIn, 0.107)) INTO percent;

		--Station Deviation add formula once created
		WHEN 'StationDeviationMetric:4-8' THEN
			SELECT FALSE INTO isNum;

		--Power Difference Does not exist when added, name may need changed.
		WHEN 'PowerDifferencePBM:4-8' THEN
			SELECT (100 - 12.85*log(abs(valueIn) + 1)) INTO percent;
		WHEN 'PowerDifferencePBM:18-22' THEN
			SELECT (100 - 7.09*log(abs(valueIn) + 1)) INTO percent;
		WHEN 'PowerDifferencePBM:90-110' THEN
			SELECT (100 - 5.50*log(abs(valueIn) + 1)) INTO percent;
		WHEN 'PowerDifferencePBM:200-500' THEN
			SELECT (100 - 5.07*log(abs(valueIn) + 1)) INTO percent;

		--Noise Does not exist when added, name may need changed
		WHEN 'NoisePBM:4-8' THEN
			SELECT (100 - 12.85*log(abs(valueIn) + 1)) INTO percent;
		WHEN 'NoisePBM:18-22' THEN
			SELECT (100 - 8.94*log(abs(valueIn) + 1)) INTO percent;
		WHEN 'NoisePBM:90-110' THEN
			SELECT (100 - 7.56*log(abs(valueIn) + 1)) INTO percent;
		WHEN 'NoisePBM:200-500' THEN
			SELECT (100 - 7.21*log(abs(valueIn) + 1)) INTO percent;

		--Calibrations Does not exist when added, name may need changed.
		WHEN 'LastCal' THEN
			SELECT (100 - 10*power(valueIn/365, 2)) INTO percent;
		WHEN 'MeanError' THEN
			SELECT (100 - 500*valueIn) INTO percent;
		ELSE
			SELECT FALSE INTO isNum;
	END CASE;

	IF isNum = TRUE THEN
		IF percent >= 100 THEN
			RETURN '100';
		ELSIF percent <= 0 THEN
			RETURN '0';
		ELSE
			RETURN percent::text; 
		END IF;
	ELSE
		RETURN 'n'; --Front end strips out anything that isn't a number
	END IF;
END;
$function$
