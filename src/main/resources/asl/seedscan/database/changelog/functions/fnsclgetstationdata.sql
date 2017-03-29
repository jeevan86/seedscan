-- Function: fnsclgetstationdata(integer[], integer, date, date)

-- DROP FUNCTION fnsclgetstationdata(integer[], integer, date, date);

CREATE OR REPLACE FUNCTION fnsclgetstationdata(
    integer[],
    integer,
    date,
    date)
  RETURNS text AS
$BODY$
DECLARE
    stationIDs alias for $1;
    metricID alias for $2;
    startDate alias for $3;
    endDate alias for $4;
    stationData TEXT;
    computeType int;
    metricName TEXT;
BEGIN
/*SELECT sum(value) as valueSum, sum(day) as dayCount, sen1.fkStationID, metricID
FROM(
    (
    --#EXPLAIN EXTENDED
    Select    --#pc1.valueSum, pc1.dayCount
            pc1.valueSum as value, pc1.dayCount as day
            , pc1.fkMetricID as metricID, pc1.fkChannelID as channelID
        FROM tblPreComputed pc1 --#FORCE INDEX (idx_tblPreComputed_Dates_fkParent)
        LEFT OUTER JOIN tblPreComputed pc2 --FORCE INDEX FOR JOIN (idx_tblPreComputed_Dates_primary)
            ON pc1.fkParentPreComputedID = pc2.pkPreComputedID 
                AND 2455988 <= pc2.start
                AND 2456018 >= pc2."end"
        WHERE   2455988 <= pc1.start
            AND 2456018 >= pc1."end"
            AND pc2.pkPreComputedID IS NULL
            
        --#GROUP BY pc1.fkChannelID, pc1.fkMetricID ORDER BY NULL
    )
    UNION ALL
    (
   -- #EXPLAIN EXTENDED
    Select   md1.value as value, 1 as day
            , md1.fkMetricID as metricID, md1.fkChannelID as channelID
        FROM tblMetricData md1
        WHERE 
            (date >= 2455988
                AND date <=  2455988 + 10 - (2455988 % 10) --#2455990
            )
            OR
            (date >=  2456018 - (2456018 % 10) --#2456010
                AND date <= 2456018)

        --#GROUP BY md1.fkChannelID, md1.fkMetricID ORDER BY NULL
    )
) semisum
INNER JOIN tblChannel ch1
    ON semisum.channelID = ch1.pkChannelID
        AND NOT ch1."isIgnored"
INNER JOIN tblSensor sen1
    ON ch1.fkSensorID = sen1.pkSensorID

GROUP BY sen1.fkStationID, semisum.metricID
*/
    Select fkComputeTypeID, name from tblMetric where pkMetricID = metricID INTO computeType, metricName;
    CASE computeType
        --Metric Data
        WHEN 1 THEN
            --Average across total number of values
            SELECT INTO stationData string_agg(CONCAT(id, ',',avg, ',', fnsclGetPercentage(avg, metricName)), E'\n') FROM (
                SELECT sen1.fkStationID as id, round((SUM(md1.value)/count(md1.*))::numeric, 4)::numeric as avg
                FROM tblMetricData md1
                JOIN tblChannel ch1
                    ON ch1.pkChannelID = md1.fkChannelID
                    AND NOT ch1."isIgnored"
                JOIN tblSensor sen1
                    ON ch1.fkSensorID = sen1.pkSensorID
                WHERE sen1.fkStationID = any(stationIDs)
                    AND 
                    md1.date >= to_char(startDate, 'J')::INT
                    AND md1.date <= to_char(endDate, 'J')::INT
                    AND md1.fkMetricID = metricID
                GROUP BY sen1.fkStationID ) stations;
        WHEN 2 THEN
            --Average across days NOT ACCURATE
            select '2' into stationData;
        WHEN 3 THEN
            --Count all values, return sum
            SELECT INTO stationData string_agg(CONCAT(id, ',',sum, ',', fnsclGetPercentage(sum, metricName)), E'\n') FROM (
                SELECT sen1.fkStationID as id, round(SUM(md1.value)::numeric, 0) as sum
                FROM tblMetricData md1
                JOIN tblChannel ch1
                    ON ch1.pkChannelID = md1.fkChannelID
                    AND NOT ch1."isIgnored"
                JOIN tblSensor sen1
                    ON ch1.fkSensorID = sen1.pkSensorID
                WHERE sen1.fkStationID = any(stationIDs)
                    AND 
                    md1.date >= to_char(startDate, 'J')::INT
                    AND md1.date <= to_char(endDate, 'J')::INT
                    AND md1.fkMetricID = metricID
                GROUP BY sen1.fkStationID ) stations;
        WHEN 5 THEN
            --Calculate date since last calibration
            SELECT INTO stationData string_agg(CONCAT(id, ',',sum, ',', fnsclGetPercentage(sum, metricName)), E'\n') FROM (
                SELECT sen1.fkStationID as id, round((to_char(endDate, 'J')::INT-max(date))::numeric, 4) as sum
                FROM tblMetricstringData md1
                JOIN tblChannel ch1
                    ON ch1.pkChannelID = md1.fkChannelID
                    AND NOT ch1."isIgnored"
                JOIN tblSensor sen1
                    ON ch1.fkSensorID = sen1.pkSensorID
                WHERE sen1.fkStationID = any(stationIDs)
                    AND md1.date <= to_char(endDate, 'J')::INT
                    AND md1.fkMetricID = metricID
                GROUP BY sen1.fkStationID ) stations;
        WHEN 6 THEN
            --Average across number of values
            select NULL into stationData;
        ELSE
            --Insert error into error log
            select 'Error' into stationData;
    END CASE;

    
    RETURN stationData;
END;
$BODY$
  LANGUAGE plpgsql STABLE
  COST 100;

