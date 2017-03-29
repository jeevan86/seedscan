-- Function: fnsclgetpercentage(double precision, character varying)

-- DROP FUNCTION fnsclgetpercentage(double precision, character varying);

CREATE OR REPLACE FUNCTION fnsclgetpercentage(
    double precision,
    character varying)
  RETURNS text AS
$BODY$
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
            SELECT (100.0 - 15*(valueIn - 0.00274)/0.992) INTO percent;
        WHEN 'MassPositionMetric' THEN
            SELECT (100.0 - 15*(valueIn - 3.52)/10.79) INTO percent;
        WHEN 'TimingQualityMetric' THEN
            SELECT valueIN INTO percent;
        WHEN 'DeadChannelMetric:4-8' THEN
            SELECT (valueIN*100) INTO percent;
        
        --Coherence
        WHEN 'CoherencePBM:4-8' THEN
            SELECT (100.0 - 15*(1 - valueIn)/0.0377) INTO percent;
        WHEN 'CoherencePBM:18-22' THEN
            SELECT (100.0 - 15*(0.99 - valueIn)/0.12) INTO percent;
        WHEN 'CoherencePBM:90-110' THEN
            SELECT (100.0 - 15*(0.93 - valueIn)/0.0337) INTO percent;
        WHEN 'CoherencePBM:200-500' THEN
            SELECT (100.0 - 15*(0.83 - valueIn)/0.346) INTO percent;

        --Power Difference
        WHEN 'DifferencePBM:4-8' THEN
            SELECT (100.0 - 15*(abs(valueIn) - 0.01)/0.348) INTO percent;
        WHEN 'DifferencePBM:18-22' THEN
            SELECT (100.0 - 15*(abs(valueIn) - 0.01)/1.17) INTO percent;
        WHEN 'DifferencePBM:90-110' THEN
            SELECT (100.0 - 15*(abs(valueIn) - 0.04)/4.66) INTO percent;
        WHEN 'DifferencePBM:200-500' THEN
            SELECT (100.0 - 15*(abs(valueIn) - 0.03)/5.97) INTO percent;

        --Noise/StationDeviationMetric
        WHEN 'StationDeviationMetric:4-8' THEN
            SELECT (100.0 - 15*(abs(valueIn) - 0.11)/3.32) INTO percent;
        WHEN 'StationDeviationMetric:18-22' THEN
            SELECT (100.0 - 15*(abs(valueIn) - 0.17)/2.57) INTO percent;
        WHEN 'StationDeviationMetric:90-110' THEN
            SELECT (100.0 - 15*(abs(valueIn) - 0.02)/2.88) INTO percent;
        WHEN 'StationDeviationMetric:200-500' THEN
            SELECT (100.0 - 15*(abs(valueIn) - 0.07)/2.90) INTO percent;

        --NLNM Deviation
        WHEN 'NLNMDeviationMetric:4-8' THEN
            SELECT (100.0 - 15*(valueIn - 3.33)/12.53) INTO percent;
        WHEN 'NLNMDeviationMetric:18-22' THEN
            SELECT (100.0 - 15*(valueIn - 13.41)/12.64) INTO percent;
        WHEN 'NLNMDeviationMetric:90-110' THEN
            SELECT (100.0 - 15*(valueIn - 13.57)/14.79) INTO percent;
        WHEN 'NLNMDeviationMetric:200-500' THEN
            SELECT (100.0 - 15*(valueIn - 20.74)/15.09) INTO percent;

        --Calibrations Does not exist when added, name may need changed.
        WHEN 'CalibrationMetric' THEN
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
$BODY$
  LANGUAGE plpgsql STABLE
  COST 100;

