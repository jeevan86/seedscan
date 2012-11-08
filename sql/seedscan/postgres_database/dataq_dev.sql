--
-- PostgreSQL database dump
--

-- Dumped from database version 9.1.5
-- Dumped by pg_dump version 9.1.6
-- Started on 2012-11-07 17:51:48 MST

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 187 (class 3079 OID 11681)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 2079 (class 0 OID 0)
-- Dependencies: 187
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- TOC entry 206 (class 1255 OID 16641)
-- Dependencies: 582 6
-- Name: fnsclgetchanneldata(integer[], integer, date, date); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION fnsclgetchanneldata(integer[], integer, date, date) RETURNS text
    LANGUAGE plpgsql STABLE
    AS $_$
DECLARE
	channelIDs alias for $1;
	metricID alias for $2;
	startDate alias for $3;
	endDate alias for $4;
	channelData TEXT;
	computeType int;
BEGIN
	Select fkComputeTypeID from tblMetric where pkMetricID = metricID INTO computeType;
	CASE computeType
		--Metric Data
		WHEN 1 THEN
			--Average across total number of values
			SELECT INTO channelData string_agg(CONCAT(id, ',',avg), E'\n') FROM (
				SELECT md1.fkChannelID as id, round((SUM(md1.value)/count(md1.*))::numeric, 4) as avg
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
			SELECT INTO channelData string_agg(CONCAT(id, ',',sum), E'\n') FROM (
				SELECT md1.fkChannelID as id, round(SUM(md1.value)::numeric, 4) as sum
				FROM tblMetricData md1
				WHERE md1.fkChannelID = any(channelIDs)
					AND 
					md1.date >= to_char(startDate, 'J')::INT
					AND md1.date <= to_char(endDate, 'J')::INT
					AND md1.fkMetricID = metricID
				GROUP BY md1.fkChannelID ) channels;
		--Calibration Data
		WHEN 5 THEN
			--Calculate data between last calibrations
			select '5' into channelData;
		WHEN 6 THEN
			--Average across number of values
			select '6' into channelData;
		ELSE
			--Insert error into error log
			select 'Error' into channelData;
	END CASE;

	
	RETURN channelData;
END;
$_$;


ALTER FUNCTION public.fnsclgetchanneldata(integer[], integer, date, date) OWNER TO postgres;

--
-- TOC entry 207 (class 1255 OID 16642)
-- Dependencies: 582 6
-- Name: fnsclgetchannelplotdata(integer, integer, date, date); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION fnsclgetchannelplotdata(integer, integer, date, date) RETURNS text
    LANGUAGE plpgsql STABLE
    AS $_$
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
				SELECT to_date(md1.date::text, 'J') as sdate, round((SUM(md1.value)/count(md1.*))::numeric, 4) as avg
				FROM tblMetricData md1
				WHERE md1.fkChannelID = channelID
					AND 
					md1.date >= to_char(startDate, 'J')::INT
					AND md1.date <= to_char(endDate, 'J')::INT
					AND md1.fkMetricID = metricID
				GROUP BY md1.date ) channels;
		WHEN 2 THEN
			--Average across days NOT ACCURATE
			select '2' into channelPlotData;
		WHEN 3 THEN
			--Count all values, return sum
			SELECT INTO channelPlotData string_agg(CONCAT(sdate, ',',avg), E'\n') FROM (
				SELECT to_date(md1.date::text, 'J') as sdate, round(SUM(md1.value)::numeric, 4) as avg
				FROM tblMetricData md1
				WHERE md1.fkchannelID = channelID
					AND 
					md1.date >= to_char(startDate, 'J')::INT
					AND md1.date <= to_char(endDate, 'J')::INT
					AND md1.fkMetricID = metricID
				GROUP BY md1.date ) stations;
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
$_$;


ALTER FUNCTION public.fnsclgetchannelplotdata(integer, integer, date, date) OWNER TO postgres;

--
-- TOC entry 201 (class 1255 OID 16643)
-- Dependencies: 6 582
-- Name: fnsclgetchannels(integer[]); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION fnsclgetchannels(integer[]) RETURNS text
    LANGUAGE plpgsql STABLE
    AS $_$
DECLARE
	stationIDs alias for $1;
	channelString TEXT;
BEGIN
	SELECT 
	INTO channelString
		string_agg( 
			CONCAT(
				  'C,'
				, pkchannelID
				, ','
				, name
				, ','
				, fkStationID
			)
			, E'\n' 
		)
	FROM tblChannel
	JOIN tblSensor
		ON tblChannel.fkSensorID = tblSensor.pkSensorID
	WHERE tblSensor.fkStationID = any(stationIDs) ;

	RETURN channelString;
	
END;
$_$;


ALTER FUNCTION public.fnsclgetchannels(integer[]) OWNER TO postgres;

--
-- TOC entry 203 (class 1255 OID 16644)
-- Dependencies: 6 582
-- Name: fnsclgetdates(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION fnsclgetdates() RETURNS text
    LANGUAGE plpgsql STABLE
    AS $$
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
$$;


ALTER FUNCTION public.fnsclgetdates() OWNER TO postgres;

--
-- TOC entry 200 (class 1255 OID 16645)
-- Dependencies: 582 6
-- Name: fnsclgetgroups(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION fnsclgetgroups() RETURNS text
    LANGUAGE plpgsql STABLE
    AS $$
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
$$;


ALTER FUNCTION public.fnsclgetgroups() OWNER TO postgres;

--
-- TOC entry 202 (class 1255 OID 16646)
-- Dependencies: 6 582
-- Name: fnsclgetgrouptypes(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION fnsclgetgrouptypes() RETURNS text
    LANGUAGE plpgsql STABLE
    AS $$
DECLARE
	groupTypeString TEXT;
BEGIN


         SELECT                                                              
         INTO groupTypeString                                                
                 string_agg( groupTypeData                                   
                         , E'\n'                                             
                 )                                                           
                 FROM                                                        
                         (SELECT                                             
                                 CONCAT(                                     
                                           'T,'                              
                                         , "pkGroupTypeID"                   
                                         , ','                               
                                         , "tblGroupType".name               
                                         ,','                                
                                         , string_agg(                       
                                                   "tblGroup".pkGroupID::text
                                                 , ','                       
                                                 ORDER BY "tblGroup".name)   
                                 ) AS groupTypeData                          
                         FROM "tblGroupType"                                 
                         Join "tblGroup"                                     
                                 ON "fkGroupTypeID" = "pkGroupTypeID"        
                         GROUP BY "pkGroupTypeID"                            
                         ORDER BY "tblGroupType".name) AS grouptypes         
         ;                                                                   
                                                                             
         RETURN groupTypeString;
	
END;
$$;


ALTER FUNCTION public.fnsclgetgrouptypes() OWNER TO postgres;

--
-- TOC entry 199 (class 1255 OID 16647)
-- Dependencies: 6 582
-- Name: fnsclgetmetrics(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION fnsclgetmetrics() RETURNS text
    LANGUAGE plpgsql STABLE
    AS $$
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
$$;


ALTER FUNCTION public.fnsclgetmetrics() OWNER TO postgres;

--
-- TOC entry 208 (class 1255 OID 16648)
-- Dependencies: 6 582
-- Name: fnsclgetstationdata(integer[], integer, date, date); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION fnsclgetstationdata(integer[], integer, date, date) RETURNS text
    LANGUAGE plpgsql STABLE
    AS $_$
DECLARE
	stationIDs alias for $1;
	metricID alias for $2;
	startDate alias for $3;
	endDate alias for $4;
	stationData TEXT;
	computeType int;
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
	Select fkComputeTypeID from tblMetric where pkMetricID = metricID INTO computeType;
	CASE computeType
		--Metric Data
		WHEN 1 THEN
			--Average across total number of values
			SELECT INTO stationData string_agg(CONCAT(id, ',',avg), E'\n') FROM (
				SELECT sen1.fkStationID as id, round((SUM(md1.value)/count(md1.*))::numeric, 4) as avg
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
			SELECT INTO stationData string_agg(CONCAT(id, ',',sum), E'\n') FROM (
				SELECT sen1.fkStationID as id, round(SUM(md1.value)::numeric, 4) as sum
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
		--Calibration Data
		WHEN 5 THEN
			--Calculate data between last calibrations
			select '5' into stationData;
		WHEN 6 THEN
			--Average across number of values
			select NULL into stationData;
		ELSE
			--Insert error into error log
			select 'Error' into stationData;
	END CASE;

	
	RETURN stationData;
END;
$_$;


ALTER FUNCTION public.fnsclgetstationdata(integer[], integer, date, date) OWNER TO postgres;

--
-- TOC entry 205 (class 1255 OID 16649)
-- Dependencies: 582 6
-- Name: fnsclgetstationplotdata(integer, integer, date, date); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION fnsclgetstationplotdata(integer, integer, date, date) RETURNS text
    LANGUAGE plpgsql STABLE
    AS $_$
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
$_$;


ALTER FUNCTION public.fnsclgetstationplotdata(integer, integer, date, date) OWNER TO postgres;

--
-- TOC entry 204 (class 1255 OID 16650)
-- Dependencies: 582 6
-- Name: fnsclgetstations(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION fnsclgetstations() RETURNS text
    LANGUAGE plpgsql STABLE
    AS $$
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
$$;


ALTER FUNCTION public.fnsclgetstations() OWNER TO postgres;

--
-- TOC entry 210 (class 1255 OID 16963)
-- Dependencies: 6 582
-- Name: spcomparehash(date, character varying, character varying, character varying, character varying, character varying, bytea); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION spcomparehash(date, character varying, character varying, character varying, character varying, character varying, bytea) RETURNS boolean
    LANGUAGE plpgsql STABLE
    AS $_$
DECLARE
	nDate alias for $1;
	metricName alias for $2;
	networkName alias for $3;
	stationName alias for $4;
	locationName alias for $5;
	channelName alias for $6;
	hashIN alias for $7;
	hashID int;
	debug text;

BEGIN
--select name from tblStation into debug;
--RAISE NOTICE 'stationID(%)', debug;

	SELECT 
	  tblhash."pkHashID"
	FROM 
	  public.tblhash, 
	  public.tblmetricdata, 
	  public.tblmetric, 
	  public.tblchannel, 
	  public.tblsensor, 
	  public.tblstation, 
	  public."tblGroup"
	WHERE 
	  --JOINS
	  tblhash."pkHashID" = tblmetricdata."fkHashID" AND
	  tblmetricdata.fkmetricid = tblmetric.pkmetricid AND
	  tblmetricdata.fkchannelid = tblchannel.pkchannelid AND
	  tblchannel.fksensorid = tblsensor.pksensorid AND
	  tblsensor.fkstationid = tblstation.pkstationid AND
	  tblstation.fknetworkid = "tblGroup".pkgroupid AND
	  --Criteria
	  tblMetric.name = metricName AND
	  "tblGroup".name = networkName AND
	  tblStation.name = stationName AND
	  tblSensor.location = locationName AND
	  tblChannel.name = channelName
	  
	INTO hashID;

	IF hashID IS NOT NULL THEN
		RETURN 1;
	ELSE
		RETURN 0;
	END IF;
        
    END;
$_$;


ALTER FUNCTION public.spcomparehash(date, character varying, character varying, character varying, character varying, character varying, bytea) OWNER TO postgres;

--
-- TOC entry 211 (class 1255 OID 24811)
-- Dependencies: 6 582
-- Name: spgetmetricvalue(date, character varying, character varying, character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION spgetmetricvalue(date, character varying, character varying, character varying, character varying, character varying) RETURNS double precision
    LANGUAGE plpgsql STABLE
    AS $_$
DECLARE
	nDate alias for $1;
	metricName alias for $2;
	networkName alias for $3;
	stationName alias for $4;
	locationName alias for $5;
	channelName alias for $6;
	value double precision;
	debug text;

BEGIN
--select name from tblStation into debug;
--RAISE NOTICE 'stationID(%)', debug;

	SELECT 
	  tblMetricData.value
	FROM 
	  
	  public.tblmetricdata, 
	  public.tblmetric, 
	  public.tblchannel, 
	  public.tblsensor, 
	  public.tblstation, 
	  public."tblGroup"
	WHERE 
	  --JOINS
	   tblmetricdata.fkmetricid = tblmetric.pkmetricid AND
	  tblmetricdata.fkchannelid = tblchannel.pkchannelid AND
	  tblchannel.fksensorid = tblsensor.pksensorid AND
	  tblsensor.fkstationid = tblstation.pkstationid AND
	  tblstation.fknetworkid = "tblGroup".pkgroupid AND
	  --Criteria
	  tblMetric.name = metricName AND
	  "tblGroup".name = networkName AND
	  tblStation.name = stationName AND
	  tblSensor.location = locationName AND
	  tblChannel.name = channelName AND
	  tblMetricData.date = to_char(nDate, 'J')::INT
	INTO value;
	RETURN value;
        
    END;
$_$;


ALTER FUNCTION public.spgetmetricvalue(date, character varying, character varying, character varying, character varying, character varying) OWNER TO postgres;

--
-- TOC entry 212 (class 1255 OID 24813)
-- Dependencies: 582 6
-- Name: spgetmetricvaluedigest(date, character varying, character varying, character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION spgetmetricvaluedigest(date, character varying, character varying, character varying, character varying, character varying, OUT bytea) RETURNS bytea
    LANGUAGE plpgsql STABLE
    AS $_$
DECLARE
	nDate alias for $1;
	metricName alias for $2;
	networkName alias for $3;
	stationName alias for $4;
	locationName alias for $5;
	channelName alias for $6;
	hash alias for $7;
	debug text;

BEGIN
--select name from tblStation into debug;
--RAISE NOTICE 'stationID(%)', debug;

--SELECT to_char('2012-06-19'::DATE, 'J')::INT;
	SELECT 
	  tblHash.hash
	FROM 
	  public.tblhash,
	  public.tblmetricdata, 
	  public.tblmetric, 
	  public.tblchannel, 
	  public.tblsensor, 
	  public.tblstation, 
	  public."tblGroup"
	WHERE 
	  --JOINS
	  tblmetricdata."fkHashID" = tblHash."pkHashID" AND
	  tblmetricdata.fkmetricid = tblmetric.pkmetricid AND
	  tblmetricdata.fkchannelid = tblchannel.pkchannelid AND
	  tblchannel.fksensorid = tblsensor.pksensorid AND
	  tblsensor.fkstationid = tblstation.pkstationid AND
	  tblstation.fknetworkid = "tblGroup".pkgroupid AND
	  --Criteria
	  tblMetric.name = metricName AND
	  "tblGroup".name = networkName AND
	  tblStation.name = stationName AND
	  tblSensor.location = locationName AND
	  tblChannel.name = channelName AND
	  tblMetricData.date = to_char(nDate, 'J')::INT
	INTO hash;
	
        
    END;
$_$;


ALTER FUNCTION public.spgetmetricvaluedigest(date, character varying, character varying, character varying, character varying, character varying, OUT bytea) OWNER TO postgres;

--
-- TOC entry 209 (class 1255 OID 16924)
-- Dependencies: 6 582
-- Name: spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea) RETURNS void
    LANGUAGE plpgsql
    AS $_$
DECLARE
	nDate alias for $1;
	metricName alias for $2;
	networkName alias for $3;
	stationName alias for $4;
	locationName alias for $5;
	channelName alias for $6;
	valueIN alias for $7;
	hashIN alias for $8;
	networkID int;
	stationID int;
	sensorID int;
	channelID int;
	metricID int;
	hashID int;
	debug text;

BEGIN
--select name from tblStation into debug;
--RAISE NOTICE 'stationID(%)', debug;

--Insert network if doesn't exist then get ID
    BEGIN
        INSERT INTO "tblGroup" (name,"fkGroupTypeID") VALUES (networkName, 1); --Group Type 1 is Network
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
    SELECT pkGroupID
        FROM "tblGroup"
        WHERE name = networkName
    INTO networkID;

--Insert station if doesn't exist then get ID
    BEGIN
        INSERT INTO tblStation(name,fkNetworkID) VALUES (stationName, networkID);
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
    SELECT pkStationID
        FROM tblStation
        WHERE name = stationName AND fkNetworkID = networkID
    INTO stationID;
    
    BEGIN --Ties the Station to its Network for the GUI to use.
        INSERT INTO "tblStationGroupTie" ("fkGroupID", "fkStationID")
		VALUES (networkID, stationID);
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;

--Insert sensor if doesn't exist then get ID
    BEGIN
        INSERT INTO tblSensor(location,fkStationID) VALUES (locationName, stationID); 
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
    SELECT pkSensorID
        FROM tblSensor
        WHERE location = locationName AND fkStationID = stationID
    INTO sensorID;
--Insert channel if doesn't exist then get ID
    BEGIN
        INSERT INTO tblChannel(name,fkSensorID) VALUES (channelName, sensorID); 
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
    SELECT pkChannelID
        FROM tblChannel
        WHERE name = channelName AND fkSensorID = sensorID
    INTO channelID;
--Insert metric if doesn't exist then get ID
    BEGIN
        INSERT INTO tblMetric(name, fkComputeTypeID, displayName) VALUES (metricName, 1, metricName); --Compute Type 1 is averaged over channel and days.
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
    SELECT pkMetricID
        FROM tblMetric
        WHERE name = metricName
    INTO metricID;

--Insert hash if doesn't exist then get ID
    BEGIN
        INSERT INTO tblHash(hash) VALUES (hashIN); 
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
   --select pkHashID from tblStation into debug;
--RAISE NOTICE 'stationID(%)', debug;
    SELECT "pkHashID"
        FROM tblHash
        WHERE hash = hashIN
    INTO hashID;
    
--Insert date into tblDate
    BEGIN
        INSERT INTO tblDate (pkDateID, date)
	    VALUES (to_char(nDate, 'J')::INT, nDate);
    EXCEPTION WHEN unique_violation THEN
        --Do nothing, it already exists
    END;
--Insert/Update metric value for day
    UPDATE tblMetricData 
	SET value = valueIN, "fkHashID" = hashID 
	WHERE date = to_char(nDate, 'J')::INT AND fkMetricID = metricID AND fkChannelID = channelID;
    IF NOT found THEN
    BEGIN
	INSERT INTO tblMetricData (fkChannelID, date, fkMetricID, value, "fkHashID") 
	    VALUES (channelID, to_char(nDate, 'J')::INT, metricID, valueIN, hashID);
    EXCEPTION WHEN unique_violation THEN
	INSERT INTO tblErrorLog (errortime, errormessage)
	    VALUES (CURRENT_TIMESTAMP, "Multiple simultaneous data inserts for metric:"+metricID+
	    " date:"+to_char(nDate, 'J')::INT);
    END;
    END IF;
    
        
    END;
$_$;


ALTER FUNCTION public.spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea) OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 161 (class 1259 OID 16651)
-- Dependencies: 1985 6
-- Name: tblGroup; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE "tblGroup" (
    pkgroupid integer NOT NULL,
    name character varying(36) NOT NULL,
    "isIgnored" boolean DEFAULT false NOT NULL,
    "fkGroupTypeID" integer
);


ALTER TABLE public."tblGroup" OWNER TO postgres;

--
-- TOC entry 162 (class 1259 OID 16655)
-- Dependencies: 6
-- Name: tblGroupType; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE "tblGroupType" (
    "pkGroupTypeID" integer NOT NULL,
    name character varying(16) NOT NULL
);


ALTER TABLE public."tblGroupType" OWNER TO postgres;

--
-- TOC entry 163 (class 1259 OID 16658)
-- Dependencies: 6 162
-- Name: tblGroupType_pkGroupTypeID_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE "tblGroupType_pkGroupTypeID_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."tblGroupType_pkGroupTypeID_seq" OWNER TO postgres;

--
-- TOC entry 2096 (class 0 OID 0)
-- Dependencies: 163
-- Name: tblGroupType_pkGroupTypeID_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE "tblGroupType_pkGroupTypeID_seq" OWNED BY "tblGroupType"."pkGroupTypeID";


--
-- TOC entry 164 (class 1259 OID 16660)
-- Dependencies: 6
-- Name: tblStationGroupTie; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE "tblStationGroupTie" (
    "fkGroupID" integer NOT NULL,
    "fkStationID" integer NOT NULL
);


ALTER TABLE public."tblStationGroupTie" OWNER TO postgres;

--
-- TOC entry 165 (class 1259 OID 16663)
-- Dependencies: 6
-- Name: tblcalibrationdata; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblcalibrationdata (
    pkcalibrationdataid integer NOT NULL,
    fkchannelid integer NOT NULL,
    year smallint NOT NULL,
    month smallint NOT NULL,
    day smallint NOT NULL,
    date date NOT NULL,
    calyear integer NOT NULL,
    calmonth smallint NOT NULL,
    calday smallint NOT NULL,
    caldate date NOT NULL,
    fkmetcaltypeid integer NOT NULL,
    value double precision NOT NULL,
    fkmetricid integer NOT NULL
);


ALTER TABLE public.tblcalibrationdata OWNER TO postgres;

--
-- TOC entry 166 (class 1259 OID 16666)
-- Dependencies: 6 165
-- Name: tblcalibrationdata_pkcalibrationdataid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tblcalibrationdata_pkcalibrationdataid_seq OWNER TO postgres;

--
-- TOC entry 2100 (class 0 OID 0)
-- Dependencies: 166
-- Name: tblcalibrationdata_pkcalibrationdataid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq OWNED BY tblcalibrationdata.pkcalibrationdataid;


--
-- TOC entry 167 (class 1259 OID 16668)
-- Dependencies: 1989 1991 6
-- Name: tblchannel; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblchannel (
    pkchannelid integer NOT NULL,
    fksensorid integer NOT NULL,
    name character varying(16) NOT NULL,
    derived integer DEFAULT 0 NOT NULL,
    "isIgnored" boolean DEFAULT false NOT NULL
);


ALTER TABLE public.tblchannel OWNER TO postgres;

--
-- TOC entry 168 (class 1259 OID 16672)
-- Dependencies: 6 167
-- Name: tblchannel_pkchannelid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE tblchannel_pkchannelid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tblchannel_pkchannelid_seq OWNER TO postgres;

--
-- TOC entry 2103 (class 0 OID 0)
-- Dependencies: 168
-- Name: tblchannel_pkchannelid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblchannel_pkchannelid_seq OWNED BY tblchannel.pkchannelid;


--
-- TOC entry 169 (class 1259 OID 16674)
-- Dependencies: 1992 1993 6
-- Name: tblcomputetype; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblcomputetype (
    pkcomputetypeid integer NOT NULL,
    name character varying(8) NOT NULL,
    description character varying(2000) DEFAULT NULL::character varying,
    iscalibration boolean DEFAULT false NOT NULL
);


ALTER TABLE public.tblcomputetype OWNER TO postgres;

--
-- TOC entry 170 (class 1259 OID 16682)
-- Dependencies: 169 6
-- Name: tblcomputetype_pkcomputetypeid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE tblcomputetype_pkcomputetypeid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tblcomputetype_pkcomputetypeid_seq OWNER TO postgres;

--
-- TOC entry 2106 (class 0 OID 0)
-- Dependencies: 170
-- Name: tblcomputetype_pkcomputetypeid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblcomputetype_pkcomputetypeid_seq OWNED BY tblcomputetype.pkcomputetypeid;


--
-- TOC entry 171 (class 1259 OID 16684)
-- Dependencies: 6
-- Name: tbldate; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tbldate (
    pkdateid integer NOT NULL,
    date date NOT NULL
);


ALTER TABLE public.tbldate OWNER TO postgres;

--
-- TOC entry 172 (class 1259 OID 16687)
-- Dependencies: 1995 6
-- Name: tblerrorlog; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblerrorlog (
    pkerrorlogid integer NOT NULL,
    errortime timestamp without time zone,
    errormessage character varying(20480) DEFAULT NULL::character varying
);


ALTER TABLE public.tblerrorlog OWNER TO postgres;

--
-- TOC entry 173 (class 1259 OID 16694)
-- Dependencies: 6 172
-- Name: tblerrorlog_pkerrorlogid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE tblerrorlog_pkerrorlogid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tblerrorlog_pkerrorlogid_seq OWNER TO postgres;

--
-- TOC entry 2110 (class 0 OID 0)
-- Dependencies: 173
-- Name: tblerrorlog_pkerrorlogid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblerrorlog_pkerrorlogid_seq OWNED BY tblerrorlog.pkerrorlogid;


--
-- TOC entry 185 (class 1259 OID 16875)
-- Dependencies: 6
-- Name: tblhash; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblhash (
    "pkHashID" bigint NOT NULL,
    hash bytea NOT NULL
);


ALTER TABLE public.tblhash OWNER TO postgres;

--
-- TOC entry 186 (class 1259 OID 16903)
-- Dependencies: 6 185
-- Name: tblhash_pkHashID_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE "tblhash_pkHashID_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."tblhash_pkHashID_seq" OWNER TO postgres;

--
-- TOC entry 2113 (class 0 OID 0)
-- Dependencies: 186
-- Name: tblhash_pkHashID_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE "tblhash_pkHashID_seq" OWNED BY tblhash."pkHashID";


--
-- TOC entry 174 (class 1259 OID 16696)
-- Dependencies: 1997 6
-- Name: tblmetadata; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblmetadata (
    fkchannelid integer NOT NULL,
    epoch timestamp without time zone NOT NULL,
    sensor_info character varying(64) DEFAULT NULL::character varying,
    raw_metadata bytea
);


ALTER TABLE public.tblmetadata OWNER TO postgres;

--
-- TOC entry 175 (class 1259 OID 16703)
-- Dependencies: 1998 1999 6
-- Name: tblmetric; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblmetric (
    pkmetricid integer NOT NULL,
    name character varying(64) NOT NULL,
    fkparentmetricid integer,
    legend character varying(128) DEFAULT NULL::character varying,
    fkcomputetypeid integer NOT NULL,
    displayname character varying(64) DEFAULT NULL::character varying
);


ALTER TABLE public.tblmetric OWNER TO postgres;

--
-- TOC entry 176 (class 1259 OID 16708)
-- Dependencies: 6 175
-- Name: tblmetric_pkmetricid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE tblmetric_pkmetricid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tblmetric_pkmetricid_seq OWNER TO postgres;

--
-- TOC entry 2117 (class 0 OID 0)
-- Dependencies: 176
-- Name: tblmetric_pkmetricid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblmetric_pkmetricid_seq OWNED BY tblmetric.pkmetricid;


--
-- TOC entry 177 (class 1259 OID 16710)
-- Dependencies: 6
-- Name: tblmetricdata; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblmetricdata (
    fkchannelid integer NOT NULL,
    date integer NOT NULL,
    fkmetricid integer NOT NULL,
    value double precision NOT NULL,
    "fkHashID" bigint NOT NULL
);


ALTER TABLE public.tblmetricdata OWNER TO postgres;

--
-- TOC entry 2119 (class 0 OID 0)
-- Dependencies: 177
-- Name: COLUMN tblmetricdata.date; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN tblmetricdata.date IS 'Julian date (number of days from Midnight November 4714 BC). This is based on the Gregorian proleptic Julian Day number standard and is natively supported in Postgresql.';


--
-- TOC entry 178 (class 1259 OID 16713)
-- Dependencies: 161 6
-- Name: tblnetwork_pknetworkid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE tblnetwork_pknetworkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tblnetwork_pknetworkid_seq OWNER TO postgres;

--
-- TOC entry 2121 (class 0 OID 0)
-- Dependencies: 178
-- Name: tblnetwork_pknetworkid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblnetwork_pknetworkid_seq OWNED BY "tblGroup".pkgroupid;


--
-- TOC entry 179 (class 1259 OID 16715)
-- Dependencies: 6
-- Name: tblprecomputed; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblprecomputed (
    pkprecomputedid integer NOT NULL,
    start integer NOT NULL,
    "end" integer NOT NULL,
    daycount smallint NOT NULL,
    valuesum double precision NOT NULL,
    fkparentprecomputedid integer,
    fkmetricid integer NOT NULL,
    fkchannelid integer NOT NULL
);


ALTER TABLE public.tblprecomputed OWNER TO postgres;

--
-- TOC entry 180 (class 1259 OID 16718)
-- Dependencies: 6 179
-- Name: tblprecomputed_pkprecomputedid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE tblprecomputed_pkprecomputedid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tblprecomputed_pkprecomputedid_seq OWNER TO postgres;

--
-- TOC entry 2124 (class 0 OID 0)
-- Dependencies: 180
-- Name: tblprecomputed_pkprecomputedid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblprecomputed_pkprecomputedid_seq OWNED BY tblprecomputed.pkprecomputedid;


--
-- TOC entry 181 (class 1259 OID 16720)
-- Dependencies: 6
-- Name: tblsensor; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblsensor (
    pksensorid integer NOT NULL,
    fkstationid integer NOT NULL,
    location character varying(16) NOT NULL
);


ALTER TABLE public.tblsensor OWNER TO postgres;

--
-- TOC entry 182 (class 1259 OID 16723)
-- Dependencies: 181 6
-- Name: tblsensor_pksensorid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE tblsensor_pksensorid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tblsensor_pksensorid_seq OWNER TO postgres;

--
-- TOC entry 2127 (class 0 OID 0)
-- Dependencies: 182
-- Name: tblsensor_pksensorid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblsensor_pksensorid_seq OWNED BY tblsensor.pksensorid;


--
-- TOC entry 183 (class 1259 OID 16725)
-- Dependencies: 6
-- Name: tblstation; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblstation (
    pkstationid integer NOT NULL,
    fknetworkid integer NOT NULL,
    name character varying(16) NOT NULL
);


ALTER TABLE public.tblstation OWNER TO postgres;

--
-- TOC entry 184 (class 1259 OID 16728)
-- Dependencies: 6 183
-- Name: tblstation_pkstationid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE tblstation_pkstationid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tblstation_pkstationid_seq OWNER TO postgres;

--
-- TOC entry 2130 (class 0 OID 0)
-- Dependencies: 184
-- Name: tblstation_pkstationid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblstation_pkstationid_seq OWNED BY tblstation.pkstationid;


--
-- TOC entry 1986 (class 2604 OID 16730)
-- Dependencies: 178 161
-- Name: pkgroupid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "tblGroup" ALTER COLUMN pkgroupid SET DEFAULT nextval('tblnetwork_pknetworkid_seq'::regclass);


--
-- TOC entry 1987 (class 2604 OID 16731)
-- Dependencies: 163 162
-- Name: pkGroupTypeID; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "tblGroupType" ALTER COLUMN "pkGroupTypeID" SET DEFAULT nextval('"tblGroupType_pkGroupTypeID_seq"'::regclass);


--
-- TOC entry 1988 (class 2604 OID 16732)
-- Dependencies: 166 165
-- Name: pkcalibrationdataid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblcalibrationdata ALTER COLUMN pkcalibrationdataid SET DEFAULT nextval('tblcalibrationdata_pkcalibrationdataid_seq'::regclass);


--
-- TOC entry 1990 (class 2604 OID 16733)
-- Dependencies: 168 167
-- Name: pkchannelid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblchannel ALTER COLUMN pkchannelid SET DEFAULT nextval('tblchannel_pkchannelid_seq'::regclass);


--
-- TOC entry 1994 (class 2604 OID 16734)
-- Dependencies: 170 169
-- Name: pkcomputetypeid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblcomputetype ALTER COLUMN pkcomputetypeid SET DEFAULT nextval('tblcomputetype_pkcomputetypeid_seq'::regclass);


--
-- TOC entry 1996 (class 2604 OID 16735)
-- Dependencies: 173 172
-- Name: pkerrorlogid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblerrorlog ALTER COLUMN pkerrorlogid SET DEFAULT nextval('tblerrorlog_pkerrorlogid_seq'::regclass);


--
-- TOC entry 2004 (class 2604 OID 16909)
-- Dependencies: 186 185
-- Name: pkHashID; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblhash ALTER COLUMN "pkHashID" SET DEFAULT nextval('"tblhash_pkHashID_seq"'::regclass);


--
-- TOC entry 2000 (class 2604 OID 16736)
-- Dependencies: 176 175
-- Name: pkmetricid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblmetric ALTER COLUMN pkmetricid SET DEFAULT nextval('tblmetric_pkmetricid_seq'::regclass);


--
-- TOC entry 2001 (class 2604 OID 16737)
-- Dependencies: 180 179
-- Name: pkprecomputedid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblprecomputed ALTER COLUMN pkprecomputedid SET DEFAULT nextval('tblprecomputed_pkprecomputedid_seq'::regclass);


--
-- TOC entry 2002 (class 2604 OID 16738)
-- Dependencies: 182 181
-- Name: pksensorid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblsensor ALTER COLUMN pksensorid SET DEFAULT nextval('tblsensor_pksensorid_seq'::regclass);


--
-- TOC entry 2003 (class 2604 OID 16739)
-- Dependencies: 184 183
-- Name: pkstationid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblstation ALTER COLUMN pkstationid SET DEFAULT nextval('tblstation_pkstationid_seq'::regclass);


--
-- TOC entry 2014 (class 2606 OID 16741)
-- Dependencies: 164 164 164 2073
-- Name: Primary_tblstationGrouptie; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "tblStationGroupTie"
    ADD CONSTRAINT "Primary_tblstationGrouptie" PRIMARY KEY ("fkGroupID", "fkStationID");


--
-- TOC entry 2054 (class 2606 OID 16917)
-- Dependencies: 185 185 2073
-- Name: pkTblHash; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblhash
    ADD CONSTRAINT "pkTblHash" PRIMARY KEY ("pkHashID");


--
-- TOC entry 2040 (class 2606 OID 16743)
-- Dependencies: 177 177 177 177 2073
-- Name: pk_metric_date_channel; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblmetricdata
    ADD CONSTRAINT pk_metric_date_channel PRIMARY KEY (fkmetricid, date, fkchannelid);


--
-- TOC entry 2010 (class 2606 OID 16745)
-- Dependencies: 162 162 2073
-- Name: primary_tblGroupType; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "tblGroupType"
    ADD CONSTRAINT "primary_tblGroupType" PRIMARY KEY ("pkGroupTypeID");


--
-- TOC entry 2016 (class 2606 OID 16747)
-- Dependencies: 165 165 165 165 165 165 165 165 165 2073
-- Name: tblcalibrationdata_fkchannelid_fkmetcaltypeid_calday_calmon_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblcalibrationdata
    ADD CONSTRAINT tblcalibrationdata_fkchannelid_fkmetcaltypeid_calday_calmon_key UNIQUE (fkchannelid, fkmetcaltypeid, calday, calmonth, calyear, day, month, year);


--
-- TOC entry 2018 (class 2606 OID 16749)
-- Dependencies: 165 165 2073
-- Name: tblcalibrationdata_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblcalibrationdata
    ADD CONSTRAINT tblcalibrationdata_pkey PRIMARY KEY (pkcalibrationdataid);


--
-- TOC entry 2020 (class 2606 OID 16751)
-- Dependencies: 167 167 167 2073
-- Name: tblchannel_fksensorid_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblchannel
    ADD CONSTRAINT tblchannel_fksensorid_name_key UNIQUE (fksensorid, name);


--
-- TOC entry 2022 (class 2606 OID 16753)
-- Dependencies: 167 167 2073
-- Name: tblchannel_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblchannel
    ADD CONSTRAINT tblchannel_pkey PRIMARY KEY (pkchannelid);


--
-- TOC entry 2024 (class 2606 OID 16755)
-- Dependencies: 169 169 2073
-- Name: tblcomputetype_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblcomputetype
    ADD CONSTRAINT tblcomputetype_name_key UNIQUE (name);


--
-- TOC entry 2026 (class 2606 OID 16757)
-- Dependencies: 169 169 2073
-- Name: tblcomputetype_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblcomputetype
    ADD CONSTRAINT tblcomputetype_pkey PRIMARY KEY (pkcomputetypeid);


--
-- TOC entry 2028 (class 2606 OID 16759)
-- Dependencies: 171 171 2073
-- Name: tbldate_date_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tbldate
    ADD CONSTRAINT tbldate_date_key UNIQUE (date);


--
-- TOC entry 2030 (class 2606 OID 16761)
-- Dependencies: 171 171 2073
-- Name: tbldate_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tbldate
    ADD CONSTRAINT tbldate_pkey PRIMARY KEY (pkdateid);


--
-- TOC entry 2032 (class 2606 OID 16763)
-- Dependencies: 172 172 2073
-- Name: tblerrorlog_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblerrorlog
    ADD CONSTRAINT tblerrorlog_pkey PRIMARY KEY (pkerrorlogid);


--
-- TOC entry 2034 (class 2606 OID 16765)
-- Dependencies: 174 174 174 2073
-- Name: tblmetadata_fkchannelid_epoch_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblmetadata
    ADD CONSTRAINT tblmetadata_fkchannelid_epoch_key UNIQUE (fkchannelid, epoch);


--
-- TOC entry 2036 (class 2606 OID 16769)
-- Dependencies: 175 175 2073
-- Name: tblmetric_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblmetric
    ADD CONSTRAINT tblmetric_pkey PRIMARY KEY (pkmetricid);


--
-- TOC entry 2006 (class 2606 OID 16771)
-- Dependencies: 161 161 2073
-- Name: tblnetwork_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "tblGroup"
    ADD CONSTRAINT tblnetwork_pkey PRIMARY KEY (pkgroupid);


--
-- TOC entry 2042 (class 2606 OID 16773)
-- Dependencies: 179 179 2073
-- Name: tblprecomputed_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblprecomputed
    ADD CONSTRAINT tblprecomputed_pkey PRIMARY KEY (pkprecomputedid);


--
-- TOC entry 2044 (class 2606 OID 16775)
-- Dependencies: 179 179 179 179 179 2073
-- Name: tblprecomputed_start_end_fkmetricid_fkchannelid_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblprecomputed
    ADD CONSTRAINT tblprecomputed_start_end_fkmetricid_fkchannelid_key UNIQUE (start, "end", fkmetricid, fkchannelid);


--
-- TOC entry 2046 (class 2606 OID 16777)
-- Dependencies: 181 181 181 2073
-- Name: tblsensor_fkstationid_location_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblsensor
    ADD CONSTRAINT tblsensor_fkstationid_location_key UNIQUE (fkstationid, location);


--
-- TOC entry 2048 (class 2606 OID 16779)
-- Dependencies: 181 181 2073
-- Name: tblsensor_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblsensor
    ADD CONSTRAINT tblsensor_pkey PRIMARY KEY (pksensorid);


--
-- TOC entry 2050 (class 2606 OID 16781)
-- Dependencies: 183 183 183 2073
-- Name: tblstation_fknetworkid_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblstation
    ADD CONSTRAINT tblstation_fknetworkid_name_key UNIQUE (fknetworkid, name);


--
-- TOC entry 2052 (class 2606 OID 16783)
-- Dependencies: 183 183 2073
-- Name: tblstation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblstation
    ADD CONSTRAINT tblstation_pkey PRIMARY KEY (pkstationid);


--
-- TOC entry 2012 (class 2606 OID 16785)
-- Dependencies: 162 162 2073
-- Name: un_name; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "tblGroupType"
    ADD CONSTRAINT un_name UNIQUE (name);


--
-- TOC entry 2008 (class 2606 OID 16787)
-- Dependencies: 161 161 161 2073
-- Name: un_name_fkGroupType; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "tblGroup"
    ADD CONSTRAINT "un_name_fkGroupType" UNIQUE (name, "fkGroupTypeID");


--
-- TOC entry 2056 (class 2606 OID 16919)
-- Dependencies: 185 185 2073
-- Name: un_tblHash_hash; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblhash
    ADD CONSTRAINT "un_tblHash_hash" UNIQUE (hash);


--
-- TOC entry 2038 (class 2606 OID 16894)
-- Dependencies: 175 175 2073
-- Name: un_tblMetric_name; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblmetric
    ADD CONSTRAINT "un_tblMetric_name" UNIQUE (name);


--
-- TOC entry 2060 (class 2606 OID 16788)
-- Dependencies: 2021 165 167 2073
-- Name: fk_tblCalibrationData_tblChannel; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblcalibrationdata
    ADD CONSTRAINT "fk_tblCalibrationData_tblChannel" FOREIGN KEY (fkchannelid) REFERENCES tblchannel(pkchannelid);


--
-- TOC entry 2061 (class 2606 OID 16793)
-- Dependencies: 165 175 2035 2073
-- Name: fk_tblCalibrationData_tblMetric; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblcalibrationdata
    ADD CONSTRAINT "fk_tblCalibrationData_tblMetric" FOREIGN KEY (fkmetricid) REFERENCES tblmetric(pkmetricid);


--
-- TOC entry 2067 (class 2606 OID 16798)
-- Dependencies: 167 179 2021 2073
-- Name: fk_tblChannel; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblprecomputed
    ADD CONSTRAINT "fk_tblChannel" FOREIGN KEY (fkchannelid) REFERENCES tblchannel(pkchannelid) ON DELETE CASCADE;


--
-- TOC entry 2065 (class 2606 OID 16803)
-- Dependencies: 2021 167 177 2073
-- Name: fk_tblChannel; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblmetricdata
    ADD CONSTRAINT "fk_tblChannel" FOREIGN KEY (fkchannelid) REFERENCES tblchannel(pkchannelid) ON DELETE CASCADE;


--
-- TOC entry 2063 (class 2606 OID 16808)
-- Dependencies: 2025 169 175 2073
-- Name: fk_tblComputeType; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblmetric
    ADD CONSTRAINT "fk_tblComputeType" FOREIGN KEY (fkcomputetypeid) REFERENCES tblcomputetype(pkcomputetypeid) ON DELETE CASCADE;


--
-- TOC entry 2058 (class 2606 OID 16813)
-- Dependencies: 164 2005 161 2073
-- Name: fk_tblGroup; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "tblStationGroupTie"
    ADD CONSTRAINT "fk_tblGroup" FOREIGN KEY ("fkGroupID") REFERENCES "tblGroup"(pkgroupid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- TOC entry 2064 (class 2606 OID 16818)
-- Dependencies: 175 175 2035 2073
-- Name: fk_tblMetric; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblmetric
    ADD CONSTRAINT "fk_tblMetric" FOREIGN KEY (fkparentmetricid) REFERENCES tblmetric(pkmetricid) ON DELETE CASCADE;


--
-- TOC entry 2068 (class 2606 OID 16823)
-- Dependencies: 2035 179 175 2073
-- Name: fk_tblMetric; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblprecomputed
    ADD CONSTRAINT "fk_tblMetric" FOREIGN KEY (fkmetricid) REFERENCES tblmetric(pkmetricid) ON DELETE CASCADE;


--
-- TOC entry 2066 (class 2606 OID 16828)
-- Dependencies: 177 175 2035 2073
-- Name: fk_tblMetric; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblmetricdata
    ADD CONSTRAINT "fk_tblMetric" FOREIGN KEY (fkmetricid) REFERENCES tblmetric(pkmetricid) ON DELETE CASCADE;


--
-- TOC entry 2071 (class 2606 OID 16833)
-- Dependencies: 183 161 2005 2073
-- Name: fk_tblNetwork; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblstation
    ADD CONSTRAINT "fk_tblNetwork" FOREIGN KEY (fknetworkid) REFERENCES "tblGroup"(pkgroupid) ON DELETE CASCADE;


--
-- TOC entry 2069 (class 2606 OID 16838)
-- Dependencies: 179 179 2041 2073
-- Name: fk_tblPreComputed; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblprecomputed
    ADD CONSTRAINT "fk_tblPreComputed" FOREIGN KEY (fkparentprecomputedid) REFERENCES tblprecomputed(pkprecomputedid) ON DELETE CASCADE;


--
-- TOC entry 2062 (class 2606 OID 16848)
-- Dependencies: 167 2047 181 2073
-- Name: fk_tblSensor; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblchannel
    ADD CONSTRAINT "fk_tblSensor" FOREIGN KEY (fksensorid) REFERENCES tblsensor(pksensorid) ON DELETE CASCADE;


--
-- TOC entry 2070 (class 2606 OID 16853)
-- Dependencies: 183 181 2051 2073
-- Name: fk_tblStation; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblsensor
    ADD CONSTRAINT "fk_tblStation" FOREIGN KEY (fkstationid) REFERENCES tblstation(pkstationid) ON DELETE CASCADE;


--
-- TOC entry 2059 (class 2606 OID 16858)
-- Dependencies: 164 2051 183 2073
-- Name: fk_tblStation; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "tblStationGroupTie"
    ADD CONSTRAINT "fk_tblStation" FOREIGN KEY ("fkStationID") REFERENCES tblstation(pkstationid) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- TOC entry 2057 (class 2606 OID 16863)
-- Dependencies: 161 2009 162 2073
-- Name: fk_tblgrouptype; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "tblGroup"
    ADD CONSTRAINT fk_tblgrouptype FOREIGN KEY ("fkGroupTypeID") REFERENCES "tblGroupType"("pkGroupTypeID");


--
-- TOC entry 2078 (class 0 OID 0)
-- Dependencies: 6
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- TOC entry 2080 (class 0 OID 0)
-- Dependencies: 206
-- Name: fnsclgetchanneldata(integer[], integer, date, date); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetchanneldata(integer[], integer, date, date) FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetchanneldata(integer[], integer, date, date) FROM postgres;
GRANT ALL ON FUNCTION fnsclgetchanneldata(integer[], integer, date, date) TO postgres;
GRANT ALL ON FUNCTION fnsclgetchanneldata(integer[], integer, date, date) TO PUBLIC;


--
-- TOC entry 2081 (class 0 OID 0)
-- Dependencies: 207
-- Name: fnsclgetchannelplotdata(integer, integer, date, date); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetchannelplotdata(integer, integer, date, date) FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetchannelplotdata(integer, integer, date, date) FROM postgres;
GRANT ALL ON FUNCTION fnsclgetchannelplotdata(integer, integer, date, date) TO postgres;
GRANT ALL ON FUNCTION fnsclgetchannelplotdata(integer, integer, date, date) TO PUBLIC;


--
-- TOC entry 2082 (class 0 OID 0)
-- Dependencies: 201
-- Name: fnsclgetchannels(integer[]); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetchannels(integer[]) FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetchannels(integer[]) FROM postgres;
GRANT ALL ON FUNCTION fnsclgetchannels(integer[]) TO postgres;
GRANT ALL ON FUNCTION fnsclgetchannels(integer[]) TO PUBLIC;


--
-- TOC entry 2083 (class 0 OID 0)
-- Dependencies: 203
-- Name: fnsclgetdates(); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetdates() FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetdates() FROM postgres;
GRANT ALL ON FUNCTION fnsclgetdates() TO postgres;
GRANT ALL ON FUNCTION fnsclgetdates() TO PUBLIC;


--
-- TOC entry 2084 (class 0 OID 0)
-- Dependencies: 200
-- Name: fnsclgetgroups(); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetgroups() FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetgroups() FROM postgres;
GRANT ALL ON FUNCTION fnsclgetgroups() TO postgres;
GRANT ALL ON FUNCTION fnsclgetgroups() TO PUBLIC;


--
-- TOC entry 2085 (class 0 OID 0)
-- Dependencies: 202
-- Name: fnsclgetgrouptypes(); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetgrouptypes() FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetgrouptypes() FROM postgres;
GRANT ALL ON FUNCTION fnsclgetgrouptypes() TO postgres;
GRANT ALL ON FUNCTION fnsclgetgrouptypes() TO PUBLIC;


--
-- TOC entry 2086 (class 0 OID 0)
-- Dependencies: 199
-- Name: fnsclgetmetrics(); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetmetrics() FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetmetrics() FROM postgres;
GRANT ALL ON FUNCTION fnsclgetmetrics() TO postgres;
GRANT ALL ON FUNCTION fnsclgetmetrics() TO PUBLIC;


--
-- TOC entry 2087 (class 0 OID 0)
-- Dependencies: 208
-- Name: fnsclgetstationdata(integer[], integer, date, date); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetstationdata(integer[], integer, date, date) FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetstationdata(integer[], integer, date, date) FROM postgres;
GRANT ALL ON FUNCTION fnsclgetstationdata(integer[], integer, date, date) TO postgres;
GRANT ALL ON FUNCTION fnsclgetstationdata(integer[], integer, date, date) TO PUBLIC;


--
-- TOC entry 2088 (class 0 OID 0)
-- Dependencies: 205
-- Name: fnsclgetstationplotdata(integer, integer, date, date); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetstationplotdata(integer, integer, date, date) FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetstationplotdata(integer, integer, date, date) FROM postgres;
GRANT ALL ON FUNCTION fnsclgetstationplotdata(integer, integer, date, date) TO postgres;
GRANT ALL ON FUNCTION fnsclgetstationplotdata(integer, integer, date, date) TO PUBLIC;


--
-- TOC entry 2089 (class 0 OID 0)
-- Dependencies: 204
-- Name: fnsclgetstations(); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetstations() FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetstations() FROM postgres;
GRANT ALL ON FUNCTION fnsclgetstations() TO postgres;
GRANT ALL ON FUNCTION fnsclgetstations() TO PUBLIC;


--
-- TOC entry 2090 (class 0 OID 0)
-- Dependencies: 210
-- Name: spcomparehash(date, character varying, character varying, character varying, character varying, character varying, bytea); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION spcomparehash(date, character varying, character varying, character varying, character varying, character varying, bytea) FROM PUBLIC;
REVOKE ALL ON FUNCTION spcomparehash(date, character varying, character varying, character varying, character varying, character varying, bytea) FROM postgres;
GRANT ALL ON FUNCTION spcomparehash(date, character varying, character varying, character varying, character varying, character varying, bytea) TO postgres;
GRANT ALL ON FUNCTION spcomparehash(date, character varying, character varying, character varying, character varying, character varying, bytea) TO PUBLIC;
GRANT ALL ON FUNCTION spcomparehash(date, character varying, character varying, character varying, character varying, character varying, bytea) TO "dataqInsert";


--
-- TOC entry 2091 (class 0 OID 0)
-- Dependencies: 211
-- Name: spgetmetricvalue(date, character varying, character varying, character varying, character varying, character varying); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION spgetmetricvalue(date, character varying, character varying, character varying, character varying, character varying) FROM PUBLIC;
REVOKE ALL ON FUNCTION spgetmetricvalue(date, character varying, character varying, character varying, character varying, character varying) FROM postgres;
GRANT ALL ON FUNCTION spgetmetricvalue(date, character varying, character varying, character varying, character varying, character varying) TO postgres;
GRANT ALL ON FUNCTION spgetmetricvalue(date, character varying, character varying, character varying, character varying, character varying) TO PUBLIC;
GRANT ALL ON FUNCTION spgetmetricvalue(date, character varying, character varying, character varying, character varying, character varying) TO "dataqInsert";


--
-- TOC entry 2092 (class 0 OID 0)
-- Dependencies: 212
-- Name: spgetmetricvaluedigest(date, character varying, character varying, character varying, character varying, character varying); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION spgetmetricvaluedigest(date, character varying, character varying, character varying, character varying, character varying, OUT bytea) FROM PUBLIC;
REVOKE ALL ON FUNCTION spgetmetricvaluedigest(date, character varying, character varying, character varying, character varying, character varying, OUT bytea) FROM postgres;
GRANT ALL ON FUNCTION spgetmetricvaluedigest(date, character varying, character varying, character varying, character varying, character varying, OUT bytea) TO postgres;
GRANT ALL ON FUNCTION spgetmetricvaluedigest(date, character varying, character varying, character varying, character varying, character varying, OUT bytea) TO PUBLIC;
GRANT ALL ON FUNCTION spgetmetricvaluedigest(date, character varying, character varying, character varying, character varying, character varying, OUT bytea) TO "dataqInsert";


--
-- TOC entry 2093 (class 0 OID 0)
-- Dependencies: 209
-- Name: spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea) FROM PUBLIC;
REVOKE ALL ON FUNCTION spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea) FROM postgres;
GRANT ALL ON FUNCTION spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea) TO postgres;
GRANT ALL ON FUNCTION spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea) TO PUBLIC;
GRANT ALL ON FUNCTION spinsertmetricdata(date, character varying, character varying, character varying, character varying, character varying, double precision, bytea) TO "dataqInsert";


--
-- TOC entry 2094 (class 0 OID 0)
-- Dependencies: 161
-- Name: tblGroup; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "tblGroup" FROM PUBLIC;
REVOKE ALL ON TABLE "tblGroup" FROM postgres;
GRANT ALL ON TABLE "tblGroup" TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE "tblGroup" TO PUBLIC;
GRANT ALL ON TABLE "tblGroup" TO "dataqInsert";


--
-- TOC entry 2095 (class 0 OID 0)
-- Dependencies: 162
-- Name: tblGroupType; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "tblGroupType" FROM PUBLIC;
REVOKE ALL ON TABLE "tblGroupType" FROM postgres;
GRANT ALL ON TABLE "tblGroupType" TO postgres;
GRANT ALL ON TABLE "tblGroupType" TO PUBLIC;
GRANT ALL ON TABLE "tblGroupType" TO "dataqInsert";


--
-- TOC entry 2097 (class 0 OID 0)
-- Dependencies: 163
-- Name: tblGroupType_pkGroupTypeID_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE "tblGroupType_pkGroupTypeID_seq" FROM PUBLIC;
REVOKE ALL ON SEQUENCE "tblGroupType_pkGroupTypeID_seq" FROM postgres;
GRANT ALL ON SEQUENCE "tblGroupType_pkGroupTypeID_seq" TO postgres;
GRANT ALL ON SEQUENCE "tblGroupType_pkGroupTypeID_seq" TO "dataqInsert";


--
-- TOC entry 2098 (class 0 OID 0)
-- Dependencies: 164
-- Name: tblStationGroupTie; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "tblStationGroupTie" FROM PUBLIC;
REVOKE ALL ON TABLE "tblStationGroupTie" FROM postgres;
GRANT ALL ON TABLE "tblStationGroupTie" TO postgres;
GRANT ALL ON TABLE "tblStationGroupTie" TO PUBLIC;
GRANT ALL ON TABLE "tblStationGroupTie" TO "dataqInsert";


--
-- TOC entry 2099 (class 0 OID 0)
-- Dependencies: 165
-- Name: tblcalibrationdata; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblcalibrationdata FROM PUBLIC;
REVOKE ALL ON TABLE tblcalibrationdata FROM postgres;
GRANT ALL ON TABLE tblcalibrationdata TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblcalibrationdata TO PUBLIC;
GRANT ALL ON TABLE tblcalibrationdata TO "dataqInsert";


--
-- TOC entry 2101 (class 0 OID 0)
-- Dependencies: 166
-- Name: tblcalibrationdata_pkcalibrationdataid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq TO PUBLIC;
GRANT ALL ON SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq TO "dataqInsert";


--
-- TOC entry 2102 (class 0 OID 0)
-- Dependencies: 167
-- Name: tblchannel; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblchannel FROM PUBLIC;
REVOKE ALL ON TABLE tblchannel FROM postgres;
GRANT ALL ON TABLE tblchannel TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblchannel TO PUBLIC;
GRANT ALL ON TABLE tblchannel TO "dataqInsert";


--
-- TOC entry 2104 (class 0 OID 0)
-- Dependencies: 168
-- Name: tblchannel_pkchannelid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblchannel_pkchannelid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblchannel_pkchannelid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblchannel_pkchannelid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblchannel_pkchannelid_seq TO PUBLIC;
GRANT ALL ON SEQUENCE tblchannel_pkchannelid_seq TO "dataqInsert";


--
-- TOC entry 2105 (class 0 OID 0)
-- Dependencies: 169
-- Name: tblcomputetype; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblcomputetype FROM PUBLIC;
REVOKE ALL ON TABLE tblcomputetype FROM postgres;
GRANT ALL ON TABLE tblcomputetype TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblcomputetype TO PUBLIC;
GRANT ALL ON TABLE tblcomputetype TO "dataqInsert";


--
-- TOC entry 2107 (class 0 OID 0)
-- Dependencies: 170
-- Name: tblcomputetype_pkcomputetypeid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblcomputetype_pkcomputetypeid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblcomputetype_pkcomputetypeid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblcomputetype_pkcomputetypeid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblcomputetype_pkcomputetypeid_seq TO PUBLIC;
GRANT ALL ON SEQUENCE tblcomputetype_pkcomputetypeid_seq TO "dataqInsert";


--
-- TOC entry 2108 (class 0 OID 0)
-- Dependencies: 171
-- Name: tbldate; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tbldate FROM PUBLIC;
REVOKE ALL ON TABLE tbldate FROM postgres;
GRANT ALL ON TABLE tbldate TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tbldate TO PUBLIC;
GRANT ALL ON TABLE tbldate TO "dataqInsert";


--
-- TOC entry 2109 (class 0 OID 0)
-- Dependencies: 172
-- Name: tblerrorlog; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblerrorlog FROM PUBLIC;
REVOKE ALL ON TABLE tblerrorlog FROM postgres;
GRANT ALL ON TABLE tblerrorlog TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblerrorlog TO PUBLIC;
GRANT ALL ON TABLE tblerrorlog TO "dataqInsert";


--
-- TOC entry 2111 (class 0 OID 0)
-- Dependencies: 173
-- Name: tblerrorlog_pkerrorlogid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblerrorlog_pkerrorlogid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblerrorlog_pkerrorlogid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblerrorlog_pkerrorlogid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblerrorlog_pkerrorlogid_seq TO PUBLIC;
GRANT ALL ON SEQUENCE tblerrorlog_pkerrorlogid_seq TO "dataqInsert";


--
-- TOC entry 2112 (class 0 OID 0)
-- Dependencies: 185
-- Name: tblhash; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblhash FROM PUBLIC;
REVOKE ALL ON TABLE tblhash FROM postgres;
GRANT ALL ON TABLE tblhash TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblhash TO PUBLIC;
GRANT ALL ON TABLE tblhash TO "dataqInsert";


--
-- TOC entry 2114 (class 0 OID 0)
-- Dependencies: 186
-- Name: tblhash_pkHashID_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE "tblhash_pkHashID_seq" FROM PUBLIC;
REVOKE ALL ON SEQUENCE "tblhash_pkHashID_seq" FROM postgres;
GRANT ALL ON SEQUENCE "tblhash_pkHashID_seq" TO postgres;
GRANT ALL ON SEQUENCE "tblhash_pkHashID_seq" TO "dataqInsert";


--
-- TOC entry 2115 (class 0 OID 0)
-- Dependencies: 174
-- Name: tblmetadata; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblmetadata FROM PUBLIC;
REVOKE ALL ON TABLE tblmetadata FROM postgres;
GRANT ALL ON TABLE tblmetadata TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblmetadata TO PUBLIC;
GRANT ALL ON TABLE tblmetadata TO "dataqInsert";


--
-- TOC entry 2116 (class 0 OID 0)
-- Dependencies: 175
-- Name: tblmetric; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblmetric FROM PUBLIC;
REVOKE ALL ON TABLE tblmetric FROM postgres;
GRANT ALL ON TABLE tblmetric TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblmetric TO PUBLIC;
GRANT ALL ON TABLE tblmetric TO "dataqInsert";


--
-- TOC entry 2118 (class 0 OID 0)
-- Dependencies: 176
-- Name: tblmetric_pkmetricid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblmetric_pkmetricid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblmetric_pkmetricid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblmetric_pkmetricid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblmetric_pkmetricid_seq TO PUBLIC;
GRANT ALL ON SEQUENCE tblmetric_pkmetricid_seq TO "dataqInsert";


--
-- TOC entry 2120 (class 0 OID 0)
-- Dependencies: 177
-- Name: tblmetricdata; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblmetricdata FROM PUBLIC;
REVOKE ALL ON TABLE tblmetricdata FROM postgres;
GRANT ALL ON TABLE tblmetricdata TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblmetricdata TO PUBLIC;
GRANT ALL ON TABLE tblmetricdata TO "dataqInsert";


--
-- TOC entry 2122 (class 0 OID 0)
-- Dependencies: 178
-- Name: tblnetwork_pknetworkid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblnetwork_pknetworkid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblnetwork_pknetworkid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblnetwork_pknetworkid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblnetwork_pknetworkid_seq TO PUBLIC;
GRANT ALL ON SEQUENCE tblnetwork_pknetworkid_seq TO "dataqInsert";


--
-- TOC entry 2123 (class 0 OID 0)
-- Dependencies: 179
-- Name: tblprecomputed; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblprecomputed FROM PUBLIC;
REVOKE ALL ON TABLE tblprecomputed FROM postgres;
GRANT ALL ON TABLE tblprecomputed TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblprecomputed TO PUBLIC;
GRANT ALL ON TABLE tblprecomputed TO "dataqInsert";


--
-- TOC entry 2125 (class 0 OID 0)
-- Dependencies: 180
-- Name: tblprecomputed_pkprecomputedid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblprecomputed_pkprecomputedid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblprecomputed_pkprecomputedid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblprecomputed_pkprecomputedid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblprecomputed_pkprecomputedid_seq TO PUBLIC;
GRANT ALL ON SEQUENCE tblprecomputed_pkprecomputedid_seq TO "dataqInsert";


--
-- TOC entry 2126 (class 0 OID 0)
-- Dependencies: 181
-- Name: tblsensor; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblsensor FROM PUBLIC;
REVOKE ALL ON TABLE tblsensor FROM postgres;
GRANT ALL ON TABLE tblsensor TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblsensor TO PUBLIC;
GRANT ALL ON TABLE tblsensor TO "dataqInsert";


--
-- TOC entry 2128 (class 0 OID 0)
-- Dependencies: 182
-- Name: tblsensor_pksensorid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblsensor_pksensorid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblsensor_pksensorid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblsensor_pksensorid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblsensor_pksensorid_seq TO PUBLIC;
GRANT ALL ON SEQUENCE tblsensor_pksensorid_seq TO "dataqInsert";


--
-- TOC entry 2129 (class 0 OID 0)
-- Dependencies: 183
-- Name: tblstation; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblstation FROM PUBLIC;
REVOKE ALL ON TABLE tblstation FROM postgres;
GRANT ALL ON TABLE tblstation TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblstation TO PUBLIC;
GRANT ALL ON TABLE tblstation TO "dataqInsert";


--
-- TOC entry 2131 (class 0 OID 0)
-- Dependencies: 184
-- Name: tblstation_pkstationid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblstation_pkstationid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblstation_pkstationid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblstation_pkstationid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblstation_pkstationid_seq TO PUBLIC;
GRANT ALL ON SEQUENCE tblstation_pkstationid_seq TO "dataqInsert";


--
-- TOC entry 1510 (class 826 OID 16868)
-- Dependencies: 6 2073
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES  FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES  TO PUBLIC;


--
-- TOC entry 1511 (class 826 OID 16897)
-- Dependencies: 2073
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: -; Owner: jholland
--

ALTER DEFAULT PRIVILEGES FOR ROLE jholland REVOKE ALL ON TABLES  FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE jholland REVOKE ALL ON TABLES  FROM jholland;
ALTER DEFAULT PRIVILEGES FOR ROLE jholland GRANT ALL ON TABLES  TO jholland;
ALTER DEFAULT PRIVILEGES FOR ROLE jholland GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO "dataqInsert";


--
-- TOC entry 1512 (class 826 OID 16898)
-- Dependencies: 6 2073
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: jholland
--

ALTER DEFAULT PRIVILEGES FOR ROLE jholland IN SCHEMA public REVOKE ALL ON TABLES  FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE jholland IN SCHEMA public REVOKE ALL ON TABLES  FROM jholland;
ALTER DEFAULT PRIVILEGES FOR ROLE jholland IN SCHEMA public GRANT ALL ON TABLES  TO "dataqInsert";


-- Completed on 2012-11-07 17:51:48 MST

--
-- PostgreSQL database dump complete
--

