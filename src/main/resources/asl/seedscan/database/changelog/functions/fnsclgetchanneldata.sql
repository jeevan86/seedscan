-- Function: fnsclgetchanneldata(integer[], integer, date, date)

-- DROP FUNCTION fnsclgetchanneldata(integer[], integer, date, date);

CREATE OR REPLACE FUNCTION fnsclgetchanneldata(
    integer[],
    integer,
    date,
    date)
  RETURNS text AS
$BODY$
DECLARE
    channelIDs alias for $1;
    metricID alias for $2;
    startDate alias for $3;
    endDate alias for $4;
    channelData TEXT;
    computeType int;
    metricName TEXT;
BEGIN
    Select fkComputeTypeID, name from tblMetric where pkMetricID = metricID INTO computeType, metricName;
    CASE computeType
        --Metric Data
        WHEN 1 THEN
            --Average across total number of values
            SELECT INTO channelData string_agg(CONCAT(id, ',',avg, ',', fnsclGetPercentage(avg, metricName)), E'\n') FROM (
                SELECT md1.fkChannelID as id, round((SUM(md1.value)/count(md1.*))::numeric, 2) as avg
                FROM tblMetricData md1
                WHERE md1.fkChannelID = any(channelIDs)
                    AND 
                    md1.date >= to_char(startDate, 'J')::INT
                    AND md1.date <= to_char(endDate, 'J')::INT
                    AND md1.fkMetricID = metricID
                GROUP BY md1.fkChannelID ) channels;
        WHEN 2 THEN
            --Average across days NOT ACCURATE
            select '2' into channelData;
        WHEN 3 THEN
            --Count all values, return sum
            SELECT INTO channelData string_agg(CONCAT(id, ',',sum, ',', fnsclGetPercentage(sum, metricName)), E'\n') FROM (
                SELECT md1.fkChannelID as id, round(SUM(md1.value)::numeric, 0) as sum
                FROM tblMetricData md1
                WHERE md1.fkChannelID = any(channelIDs)
                    AND 
                    md1.date >= to_char(startDate, 'J')::INT
                    AND md1.date <= to_char(endDate, 'J')::INT
                    AND md1.fkMetricID = metricID
                GROUP BY md1.fkChannelID ) channels;
        
        WHEN 5 THEN
            --Calculate data between last calibrations
            SELECT INTO channelData string_agg(CONCAT(id, ',',sum, ',', fnsclGetPercentage(sum, metricName)), E'\n') FROM (
                SELECT md1.fkChannelID as id, round((to_char(endDate, 'J')::INT-max(date))::numeric, 0) as sum
                FROM tblMetricStringData md1
                WHERE md1.fkChannelID = any(channelIDs)
                    AND md1.date <= to_char(endDate, 'J')::INT
                    AND md1.fkMetricID = metricID
                GROUP BY md1.fkChannelID ) channels;
        WHEN 6 THEN
            --Average across number of values
            select '6' into channelData;
        ELSE
            --Insert error into error log
            select 'Error' into channelData;
    END CASE;

    
    RETURN channelData;
END;
$BODY$
  LANGUAGE plpgsql STABLE
  COST 100;

