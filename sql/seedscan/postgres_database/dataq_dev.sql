--
-- PostgreSQL database dump
--

-- Dumped from database version 9.1.4
-- Dumped by pg_dump version 9.1.4
-- Started on 2012-08-06 12:34:02 MDT

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 187 (class 3079 OID 11677)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 2057 (class 0 OID 0)
-- Dependencies: 187
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- TOC entry 207 (class 1255 OID 17397)
-- Dependencies: 573 5
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
-- TOC entry 208 (class 1255 OID 17393)
-- Dependencies: 5 573
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
-- TOC entry 204 (class 1255 OID 17475)
-- Dependencies: 573 5
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
-- TOC entry 199 (class 1255 OID 17282)
-- Dependencies: 573 5
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
-- TOC entry 201 (class 1255 OID 17456)
-- Dependencies: 573 5
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
-- TOC entry 203 (class 1255 OID 17473)
-- Dependencies: 5 573
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
		string_agg( 
			CONCAT(
				  'T,'
				, "pkGroupTypeID"
				, ','
				, name

			    
			)
			, E'\n' 
		)
	FROM "tblGroupType";

	RETURN groupTypeString;
	
END;
$$;


ALTER FUNCTION public.fnsclgetgrouptypes() OWNER TO postgres;

--
-- TOC entry 202 (class 1255 OID 17472)
-- Dependencies: 573 5
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
				, DisplayName

			    
			)
			, E'\n' 
		)
	FROM tblMetric;

	RETURN metricString;
	
END;
$$;


ALTER FUNCTION public.fnsclgetmetrics() OWNER TO postgres;

--
-- TOC entry 205 (class 1255 OID 17390)
-- Dependencies: 5 573
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
			select '6' into stationData;
		ELSE
			--Insert error into error log
			select 'Error' into stationData;
	END CASE;

	
	RETURN stationData;
END;
$_$;


ALTER FUNCTION public.fnsclgetstationdata(integer[], integer, date, date) OWNER TO postgres;

--
-- TOC entry 206 (class 1255 OID 17392)
-- Dependencies: 5 573
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
-- TOC entry 200 (class 1255 OID 17433)
-- Dependencies: 573 5
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

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 169 (class 1259 OID 17084)
-- Dependencies: 1975 5
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
-- TOC entry 186 (class 1259 OID 17417)
-- Dependencies: 5
-- Name: tblGroupType; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE "tblGroupType" (
    "pkGroupTypeID" integer NOT NULL,
    name character varying(16) NOT NULL
);


ALTER TABLE public."tblGroupType" OWNER TO postgres;

--
-- TOC entry 185 (class 1259 OID 17415)
-- Dependencies: 186 5
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
-- TOC entry 2070 (class 0 OID 0)
-- Dependencies: 185
-- Name: tblGroupType_pkGroupTypeID_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE "tblGroupType_pkGroupTypeID_seq" OWNED BY "tblGroupType"."pkGroupTypeID";


--
-- TOC entry 184 (class 1259 OID 17398)
-- Dependencies: 5
-- Name: tblStationGroupTie; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE "tblStationGroupTie" (
    "fkGroupID" integer NOT NULL,
    "fkStationID" integer NOT NULL
);


ALTER TABLE public."tblStationGroupTie" OWNER TO postgres;

--
-- TOC entry 167 (class 1259 OID 17074)
-- Dependencies: 5
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
-- TOC entry 166 (class 1259 OID 17072)
-- Dependencies: 5 167
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
-- TOC entry 2073 (class 0 OID 0)
-- Dependencies: 166
-- Name: tblcalibrationdata_pkcalibrationdataid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq OWNED BY tblcalibrationdata.pkcalibrationdataid;


--
-- TOC entry 181 (class 1259 OID 17153)
-- Dependencies: 1985 5
-- Name: tblchannel; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblchannel (
    pkchannelid integer NOT NULL,
    fksensorid integer NOT NULL,
    name character varying(16) NOT NULL,
    derived integer NOT NULL,
    "isIgnored" boolean DEFAULT false NOT NULL
);


ALTER TABLE public.tblchannel OWNER TO postgres;

--
-- TOC entry 180 (class 1259 OID 17151)
-- Dependencies: 181 5
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
-- TOC entry 2076 (class 0 OID 0)
-- Dependencies: 180
-- Name: tblchannel_pkchannelid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblchannel_pkchannelid_seq OWNED BY tblchannel.pkchannelid;


--
-- TOC entry 164 (class 1259 OID 17052)
-- Dependencies: 1971 1972 5
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
-- TOC entry 163 (class 1259 OID 17050)
-- Dependencies: 164 5
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
-- TOC entry 2079 (class 0 OID 0)
-- Dependencies: 163
-- Name: tblcomputetype_pkcomputetypeid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblcomputetype_pkcomputetypeid_seq OWNED BY tblcomputetype.pkcomputetypeid;


--
-- TOC entry 165 (class 1259 OID 17065)
-- Dependencies: 5
-- Name: tbldate; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tbldate (
    pkdateid integer NOT NULL,
    date date NOT NULL
);


ALTER TABLE public.tbldate OWNER TO postgres;

--
-- TOC entry 179 (class 1259 OID 17141)
-- Dependencies: 1983 5
-- Name: tblerrorlog; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblerrorlog (
    pkerrorlogid integer NOT NULL,
    errortime timestamp without time zone,
    errormessage character varying(20480) DEFAULT NULL::character varying
);


ALTER TABLE public.tblerrorlog OWNER TO postgres;

--
-- TOC entry 178 (class 1259 OID 17139)
-- Dependencies: 5 179
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
-- TOC entry 2083 (class 0 OID 0)
-- Dependencies: 178
-- Name: tblerrorlog_pkerrorlogid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblerrorlog_pkerrorlogid_seq OWNED BY tblerrorlog.pkerrorlogid;


--
-- TOC entry 177 (class 1259 OID 17130)
-- Dependencies: 1981 5
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
-- TOC entry 171 (class 1259 OID 17095)
-- Dependencies: 1977 1978 5
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
-- TOC entry 170 (class 1259 OID 17093)
-- Dependencies: 171 5
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
-- TOC entry 2087 (class 0 OID 0)
-- Dependencies: 170
-- Name: tblmetric_pkmetricid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblmetric_pkmetricid_seq OWNED BY tblmetric.pkmetricid;


--
-- TOC entry 174 (class 1259 OID 17115)
-- Dependencies: 5
-- Name: tblmetricdata; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblmetricdata (
    fkchannelid integer NOT NULL,
    date integer NOT NULL,
    fkmetricid integer NOT NULL,
    value double precision NOT NULL,
    fkparentprecomputedid integer
);


ALTER TABLE public.tblmetricdata OWNER TO postgres;

--
-- TOC entry 2089 (class 0 OID 0)
-- Dependencies: 174
-- Name: COLUMN tblmetricdata.date; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN tblmetricdata.date IS 'Julian date (number of days from Midnight November 4714 BC). This is based on the Gregorian proleptic Julian Day number standard and is natively supported in Postgresql.';


--
-- TOC entry 168 (class 1259 OID 17082)
-- Dependencies: 169 5
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
-- TOC entry 2091 (class 0 OID 0)
-- Dependencies: 168
-- Name: tblnetwork_pknetworkid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblnetwork_pknetworkid_seq OWNED BY "tblGroup".pkgroupid;


--
-- TOC entry 173 (class 1259 OID 17107)
-- Dependencies: 5
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
-- TOC entry 172 (class 1259 OID 17105)
-- Dependencies: 5 173
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
-- TOC entry 2094 (class 0 OID 0)
-- Dependencies: 172
-- Name: tblprecomputed_pkprecomputedid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblprecomputed_pkprecomputedid_seq OWNED BY tblprecomputed.pkprecomputedid;


--
-- TOC entry 183 (class 1259 OID 17164)
-- Dependencies: 5
-- Name: tblsensor; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblsensor (
    pksensorid integer NOT NULL,
    fkstationid integer NOT NULL,
    location character varying(16) NOT NULL
);


ALTER TABLE public.tblsensor OWNER TO postgres;

--
-- TOC entry 182 (class 1259 OID 17162)
-- Dependencies: 5 183
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
-- TOC entry 2097 (class 0 OID 0)
-- Dependencies: 182
-- Name: tblsensor_pksensorid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblsensor_pksensorid_seq OWNED BY tblsensor.pksensorid;


--
-- TOC entry 176 (class 1259 OID 17122)
-- Dependencies: 5
-- Name: tblstation; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tblstation (
    pkstationid integer NOT NULL,
    fknetworkid integer NOT NULL,
    name character varying(16) NOT NULL
);


ALTER TABLE public.tblstation OWNER TO postgres;

--
-- TOC entry 175 (class 1259 OID 17120)
-- Dependencies: 176 5
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
-- TOC entry 2100 (class 0 OID 0)
-- Dependencies: 175
-- Name: tblstation_pkstationid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE tblstation_pkstationid_seq OWNED BY tblstation.pkstationid;


--
-- TOC entry 1974 (class 2604 OID 17087)
-- Dependencies: 169 168 169
-- Name: pkgroupid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "tblGroup" ALTER COLUMN pkgroupid SET DEFAULT nextval('tblnetwork_pknetworkid_seq'::regclass);


--
-- TOC entry 1987 (class 2604 OID 17420)
-- Dependencies: 185 186 186
-- Name: pkGroupTypeID; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "tblGroupType" ALTER COLUMN "pkGroupTypeID" SET DEFAULT nextval('"tblGroupType_pkGroupTypeID_seq"'::regclass);


--
-- TOC entry 1973 (class 2604 OID 17077)
-- Dependencies: 167 166 167
-- Name: pkcalibrationdataid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblcalibrationdata ALTER COLUMN pkcalibrationdataid SET DEFAULT nextval('tblcalibrationdata_pkcalibrationdataid_seq'::regclass);


--
-- TOC entry 1984 (class 2604 OID 17156)
-- Dependencies: 180 181 181
-- Name: pkchannelid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblchannel ALTER COLUMN pkchannelid SET DEFAULT nextval('tblchannel_pkchannelid_seq'::regclass);


--
-- TOC entry 1970 (class 2604 OID 17055)
-- Dependencies: 164 163 164
-- Name: pkcomputetypeid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblcomputetype ALTER COLUMN pkcomputetypeid SET DEFAULT nextval('tblcomputetype_pkcomputetypeid_seq'::regclass);


--
-- TOC entry 1982 (class 2604 OID 17144)
-- Dependencies: 179 178 179
-- Name: pkerrorlogid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblerrorlog ALTER COLUMN pkerrorlogid SET DEFAULT nextval('tblerrorlog_pkerrorlogid_seq'::regclass);


--
-- TOC entry 1976 (class 2604 OID 17098)
-- Dependencies: 171 170 171
-- Name: pkmetricid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblmetric ALTER COLUMN pkmetricid SET DEFAULT nextval('tblmetric_pkmetricid_seq'::regclass);


--
-- TOC entry 1979 (class 2604 OID 17110)
-- Dependencies: 173 172 173
-- Name: pkprecomputedid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblprecomputed ALTER COLUMN pkprecomputedid SET DEFAULT nextval('tblprecomputed_pkprecomputedid_seq'::regclass);


--
-- TOC entry 1986 (class 2604 OID 17167)
-- Dependencies: 183 182 183
-- Name: pksensorid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblsensor ALTER COLUMN pksensorid SET DEFAULT nextval('tblsensor_pksensorid_seq'::regclass);


--
-- TOC entry 1980 (class 2604 OID 17125)
-- Dependencies: 176 175 176
-- Name: pkstationid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblstation ALTER COLUMN pkstationid SET DEFAULT nextval('tblstation_pkstationid_seq'::regclass);


--
-- TOC entry 2031 (class 2606 OID 17414)
-- Dependencies: 184 184 184
-- Name: Primary_tblstationGrouptie; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "tblStationGroupTie"
    ADD CONSTRAINT "Primary_tblstationGrouptie" PRIMARY KEY ("fkGroupID", "fkStationID");


--
-- TOC entry 2013 (class 2606 OID 17389)
-- Dependencies: 174 174 174 174
-- Name: pk_metric_date_channel; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblmetricdata
    ADD CONSTRAINT pk_metric_date_channel PRIMARY KEY (fkmetricid, date, fkchannelid);


--
-- TOC entry 2033 (class 2606 OID 17422)
-- Dependencies: 186 186
-- Name: primary_tblGroupType; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "tblGroupType"
    ADD CONSTRAINT "primary_tblGroupType" PRIMARY KEY ("pkGroupTypeID");


--
-- TOC entry 1997 (class 2606 OID 17081)
-- Dependencies: 167 167 167 167 167 167 167 167 167
-- Name: tblcalibrationdata_fkchannelid_fkmetcaltypeid_calday_calmon_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblcalibrationdata
    ADD CONSTRAINT tblcalibrationdata_fkchannelid_fkmetcaltypeid_calday_calmon_key UNIQUE (fkchannelid, fkmetcaltypeid, calday, calmonth, calyear, day, month, year);


--
-- TOC entry 1999 (class 2606 OID 17079)
-- Dependencies: 167 167
-- Name: tblcalibrationdata_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblcalibrationdata
    ADD CONSTRAINT tblcalibrationdata_pkey PRIMARY KEY (pkcalibrationdataid);


--
-- TOC entry 2023 (class 2606 OID 17161)
-- Dependencies: 181 181 181
-- Name: tblchannel_fksensorid_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblchannel
    ADD CONSTRAINT tblchannel_fksensorid_name_key UNIQUE (fksensorid, name);


--
-- TOC entry 2025 (class 2606 OID 17159)
-- Dependencies: 181 181
-- Name: tblchannel_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblchannel
    ADD CONSTRAINT tblchannel_pkey PRIMARY KEY (pkchannelid);


--
-- TOC entry 1989 (class 2606 OID 17064)
-- Dependencies: 164 164
-- Name: tblcomputetype_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblcomputetype
    ADD CONSTRAINT tblcomputetype_name_key UNIQUE (name);


--
-- TOC entry 1991 (class 2606 OID 17062)
-- Dependencies: 164 164
-- Name: tblcomputetype_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblcomputetype
    ADD CONSTRAINT tblcomputetype_pkey PRIMARY KEY (pkcomputetypeid);


--
-- TOC entry 1993 (class 2606 OID 17071)
-- Dependencies: 165 165
-- Name: tbldate_date_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tbldate
    ADD CONSTRAINT tbldate_date_key UNIQUE (date);


--
-- TOC entry 1995 (class 2606 OID 17069)
-- Dependencies: 165 165
-- Name: tbldate_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tbldate
    ADD CONSTRAINT tbldate_pkey PRIMARY KEY (pkdateid);


--
-- TOC entry 2021 (class 2606 OID 17150)
-- Dependencies: 179 179
-- Name: tblerrorlog_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblerrorlog
    ADD CONSTRAINT tblerrorlog_pkey PRIMARY KEY (pkerrorlogid);


--
-- TOC entry 2019 (class 2606 OID 17138)
-- Dependencies: 177 177 177
-- Name: tblmetadata_fkchannelid_epoch_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblmetadata
    ADD CONSTRAINT tblmetadata_fkchannelid_epoch_key UNIQUE (fkchannelid, epoch);


--
-- TOC entry 2005 (class 2606 OID 17104)
-- Dependencies: 171 171 171
-- Name: tblmetric_name_fkparentmetricid_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblmetric
    ADD CONSTRAINT tblmetric_name_fkparentmetricid_key UNIQUE (name, fkparentmetricid);


--
-- TOC entry 2007 (class 2606 OID 17102)
-- Dependencies: 171 171
-- Name: tblmetric_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblmetric
    ADD CONSTRAINT tblmetric_pkey PRIMARY KEY (pkmetricid);


--
-- TOC entry 2001 (class 2606 OID 17090)
-- Dependencies: 169 169
-- Name: tblnetwork_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "tblGroup"
    ADD CONSTRAINT tblnetwork_pkey PRIMARY KEY (pkgroupid);


--
-- TOC entry 2009 (class 2606 OID 17112)
-- Dependencies: 173 173
-- Name: tblprecomputed_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblprecomputed
    ADD CONSTRAINT tblprecomputed_pkey PRIMARY KEY (pkprecomputedid);


--
-- TOC entry 2011 (class 2606 OID 17114)
-- Dependencies: 173 173 173 173 173
-- Name: tblprecomputed_start_end_fkmetricid_fkchannelid_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblprecomputed
    ADD CONSTRAINT tblprecomputed_start_end_fkmetricid_fkchannelid_key UNIQUE (start, "end", fkmetricid, fkchannelid);


--
-- TOC entry 2027 (class 2606 OID 17171)
-- Dependencies: 183 183 183
-- Name: tblsensor_fkstationid_location_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblsensor
    ADD CONSTRAINT tblsensor_fkstationid_location_key UNIQUE (fkstationid, location);


--
-- TOC entry 2029 (class 2606 OID 17169)
-- Dependencies: 183 183
-- Name: tblsensor_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblsensor
    ADD CONSTRAINT tblsensor_pkey PRIMARY KEY (pksensorid);


--
-- TOC entry 2015 (class 2606 OID 17129)
-- Dependencies: 176 176 176
-- Name: tblstation_fknetworkid_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblstation
    ADD CONSTRAINT tblstation_fknetworkid_name_key UNIQUE (fknetworkid, name);


--
-- TOC entry 2017 (class 2606 OID 17127)
-- Dependencies: 176 176
-- Name: tblstation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tblstation
    ADD CONSTRAINT tblstation_pkey PRIMARY KEY (pkstationid);


--
-- TOC entry 2035 (class 2606 OID 17424)
-- Dependencies: 186 186
-- Name: un_name; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "tblGroupType"
    ADD CONSTRAINT un_name UNIQUE (name);


--
-- TOC entry 2003 (class 2606 OID 17460)
-- Dependencies: 169 169 169
-- Name: un_name_fkGroupType; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "tblGroup"
    ADD CONSTRAINT "un_name_fkGroupType" UNIQUE (name, "fkGroupTypeID");


--
-- TOC entry 2036 (class 2606 OID 17187)
-- Dependencies: 181 167 2024
-- Name: fk_tblCalibrationData_tblChannel; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblcalibrationdata
    ADD CONSTRAINT "fk_tblCalibrationData_tblChannel" FOREIGN KEY (fkchannelid) REFERENCES tblchannel(pkchannelid);


--
-- TOC entry 2037 (class 2606 OID 17192)
-- Dependencies: 171 167 2006
-- Name: fk_tblCalibrationData_tblMetric; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblcalibrationdata
    ADD CONSTRAINT "fk_tblCalibrationData_tblMetric" FOREIGN KEY (fkmetricid) REFERENCES tblmetric(pkmetricid);


--
-- TOC entry 2043 (class 2606 OID 17262)
-- Dependencies: 181 2024 173
-- Name: fk_tblChannel; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblprecomputed
    ADD CONSTRAINT "fk_tblChannel" FOREIGN KEY (fkchannelid) REFERENCES tblchannel(pkchannelid) ON DELETE CASCADE;


--
-- TOC entry 2044 (class 2606 OID 17373)
-- Dependencies: 174 181 2024
-- Name: fk_tblChannel; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblmetricdata
    ADD CONSTRAINT "fk_tblChannel" FOREIGN KEY (fkchannelid) REFERENCES tblchannel(pkchannelid) ON DELETE CASCADE;


--
-- TOC entry 2039 (class 2606 OID 17212)
-- Dependencies: 164 171 1990
-- Name: fk_tblComputeType; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblmetric
    ADD CONSTRAINT "fk_tblComputeType" FOREIGN KEY (fkcomputetypeid) REFERENCES tblcomputetype(pkcomputetypeid) ON DELETE CASCADE;


--
-- TOC entry 2051 (class 2606 OID 17408)
-- Dependencies: 184 2000 169
-- Name: fk_tblGroup; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "tblStationGroupTie"
    ADD CONSTRAINT "fk_tblGroup" FOREIGN KEY ("fkGroupID") REFERENCES "tblGroup"(pkgroupid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- TOC entry 2040 (class 2606 OID 17217)
-- Dependencies: 171 2006 171
-- Name: fk_tblMetric; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblmetric
    ADD CONSTRAINT "fk_tblMetric" FOREIGN KEY (fkparentmetricid) REFERENCES tblmetric(pkmetricid) ON DELETE CASCADE;


--
-- TOC entry 2042 (class 2606 OID 17257)
-- Dependencies: 171 2006 173
-- Name: fk_tblMetric; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblprecomputed
    ADD CONSTRAINT "fk_tblMetric" FOREIGN KEY (fkmetricid) REFERENCES tblmetric(pkmetricid) ON DELETE CASCADE;


--
-- TOC entry 2045 (class 2606 OID 17378)
-- Dependencies: 2006 174 171
-- Name: fk_tblMetric; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblmetricdata
    ADD CONSTRAINT "fk_tblMetric" FOREIGN KEY (fkmetricid) REFERENCES tblmetric(pkmetricid) ON DELETE CASCADE;


--
-- TOC entry 2047 (class 2606 OID 17272)
-- Dependencies: 176 169 2000
-- Name: fk_tblNetwork; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblstation
    ADD CONSTRAINT "fk_tblNetwork" FOREIGN KEY (fknetworkid) REFERENCES "tblGroup"(pkgroupid) ON DELETE CASCADE;


--
-- TOC entry 2041 (class 2606 OID 17252)
-- Dependencies: 173 2008 173
-- Name: fk_tblPreComputed; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblprecomputed
    ADD CONSTRAINT "fk_tblPreComputed" FOREIGN KEY (fkparentprecomputedid) REFERENCES tblprecomputed(pkprecomputedid) ON DELETE CASCADE;


--
-- TOC entry 2046 (class 2606 OID 17383)
-- Dependencies: 2008 174 173
-- Name: fk_tblPreComputed; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblmetricdata
    ADD CONSTRAINT "fk_tblPreComputed" FOREIGN KEY (fkparentprecomputedid) REFERENCES tblprecomputed(pkprecomputedid) ON DELETE SET NULL;


--
-- TOC entry 2048 (class 2606 OID 17277)
-- Dependencies: 2028 183 181
-- Name: fk_tblSensor; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblchannel
    ADD CONSTRAINT "fk_tblSensor" FOREIGN KEY (fksensorid) REFERENCES tblsensor(pksensorid) ON DELETE CASCADE;


--
-- TOC entry 2049 (class 2606 OID 17267)
-- Dependencies: 2016 176 183
-- Name: fk_tblStation; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tblsensor
    ADD CONSTRAINT "fk_tblStation" FOREIGN KEY (fkstationid) REFERENCES tblstation(pkstationid) ON DELETE CASCADE;


--
-- TOC entry 2050 (class 2606 OID 17403)
-- Dependencies: 176 2016 184
-- Name: fk_tblStation; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "tblStationGroupTie"
    ADD CONSTRAINT "fk_tblStation" FOREIGN KEY ("fkStationID") REFERENCES tblstation(pkstationid) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- TOC entry 2038 (class 2606 OID 17466)
-- Dependencies: 186 169 2032
-- Name: fk_tblgrouptype; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "tblGroup"
    ADD CONSTRAINT fk_tblgrouptype FOREIGN KEY ("fkGroupTypeID") REFERENCES "tblGroupType"("pkGroupTypeID");


--
-- TOC entry 2056 (class 0 OID 0)
-- Dependencies: 5
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- TOC entry 2058 (class 0 OID 0)
-- Dependencies: 207
-- Name: fnsclgetchanneldata(integer[], integer, date, date); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetchanneldata(integer[], integer, date, date) FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetchanneldata(integer[], integer, date, date) FROM postgres;
GRANT ALL ON FUNCTION fnsclgetchanneldata(integer[], integer, date, date) TO postgres;
GRANT ALL ON FUNCTION fnsclgetchanneldata(integer[], integer, date, date) TO PUBLIC;


--
-- TOC entry 2059 (class 0 OID 0)
-- Dependencies: 208
-- Name: fnsclgetchannelplotdata(integer, integer, date, date); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetchannelplotdata(integer, integer, date, date) FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetchannelplotdata(integer, integer, date, date) FROM postgres;
GRANT ALL ON FUNCTION fnsclgetchannelplotdata(integer, integer, date, date) TO postgres;
GRANT ALL ON FUNCTION fnsclgetchannelplotdata(integer, integer, date, date) TO PUBLIC;


--
-- TOC entry 2060 (class 0 OID 0)
-- Dependencies: 204
-- Name: fnsclgetchannels(integer[]); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetchannels(integer[]) FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetchannels(integer[]) FROM postgres;
GRANT ALL ON FUNCTION fnsclgetchannels(integer[]) TO postgres;
GRANT ALL ON FUNCTION fnsclgetchannels(integer[]) TO PUBLIC;


--
-- TOC entry 2061 (class 0 OID 0)
-- Dependencies: 199
-- Name: fnsclgetdates(); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetdates() FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetdates() FROM postgres;
GRANT ALL ON FUNCTION fnsclgetdates() TO postgres;
GRANT ALL ON FUNCTION fnsclgetdates() TO PUBLIC;


--
-- TOC entry 2062 (class 0 OID 0)
-- Dependencies: 201
-- Name: fnsclgetgroups(); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetgroups() FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetgroups() FROM postgres;
GRANT ALL ON FUNCTION fnsclgetgroups() TO postgres;
GRANT ALL ON FUNCTION fnsclgetgroups() TO PUBLIC;


--
-- TOC entry 2063 (class 0 OID 0)
-- Dependencies: 203
-- Name: fnsclgetgrouptypes(); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetgrouptypes() FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetgrouptypes() FROM postgres;
GRANT ALL ON FUNCTION fnsclgetgrouptypes() TO postgres;
GRANT ALL ON FUNCTION fnsclgetgrouptypes() TO PUBLIC;


--
-- TOC entry 2064 (class 0 OID 0)
-- Dependencies: 202
-- Name: fnsclgetmetrics(); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetmetrics() FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetmetrics() FROM postgres;
GRANT ALL ON FUNCTION fnsclgetmetrics() TO postgres;
GRANT ALL ON FUNCTION fnsclgetmetrics() TO PUBLIC;


--
-- TOC entry 2065 (class 0 OID 0)
-- Dependencies: 205
-- Name: fnsclgetstationdata(integer[], integer, date, date); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetstationdata(integer[], integer, date, date) FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetstationdata(integer[], integer, date, date) FROM postgres;
GRANT ALL ON FUNCTION fnsclgetstationdata(integer[], integer, date, date) TO postgres;
GRANT ALL ON FUNCTION fnsclgetstationdata(integer[], integer, date, date) TO PUBLIC;


--
-- TOC entry 2066 (class 0 OID 0)
-- Dependencies: 206
-- Name: fnsclgetstationplotdata(integer, integer, date, date); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetstationplotdata(integer, integer, date, date) FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetstationplotdata(integer, integer, date, date) FROM postgres;
GRANT ALL ON FUNCTION fnsclgetstationplotdata(integer, integer, date, date) TO postgres;
GRANT ALL ON FUNCTION fnsclgetstationplotdata(integer, integer, date, date) TO PUBLIC;


--
-- TOC entry 2067 (class 0 OID 0)
-- Dependencies: 200
-- Name: fnsclgetstations(); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION fnsclgetstations() FROM PUBLIC;
REVOKE ALL ON FUNCTION fnsclgetstations() FROM postgres;
GRANT ALL ON FUNCTION fnsclgetstations() TO postgres;
GRANT ALL ON FUNCTION fnsclgetstations() TO PUBLIC;


--
-- TOC entry 2068 (class 0 OID 0)
-- Dependencies: 169
-- Name: tblGroup; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "tblGroup" FROM PUBLIC;
REVOKE ALL ON TABLE "tblGroup" FROM postgres;
GRANT ALL ON TABLE "tblGroup" TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE "tblGroup" TO PUBLIC;


--
-- TOC entry 2069 (class 0 OID 0)
-- Dependencies: 186
-- Name: tblGroupType; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "tblGroupType" FROM PUBLIC;
REVOKE ALL ON TABLE "tblGroupType" FROM postgres;
GRANT ALL ON TABLE "tblGroupType" TO postgres;
GRANT ALL ON TABLE "tblGroupType" TO PUBLIC;


--
-- TOC entry 2071 (class 0 OID 0)
-- Dependencies: 184
-- Name: tblStationGroupTie; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "tblStationGroupTie" FROM PUBLIC;
REVOKE ALL ON TABLE "tblStationGroupTie" FROM postgres;
GRANT ALL ON TABLE "tblStationGroupTie" TO postgres;
GRANT ALL ON TABLE "tblStationGroupTie" TO PUBLIC;


--
-- TOC entry 2072 (class 0 OID 0)
-- Dependencies: 167
-- Name: tblcalibrationdata; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblcalibrationdata FROM PUBLIC;
REVOKE ALL ON TABLE tblcalibrationdata FROM postgres;
GRANT ALL ON TABLE tblcalibrationdata TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblcalibrationdata TO PUBLIC;


--
-- TOC entry 2074 (class 0 OID 0)
-- Dependencies: 166
-- Name: tblcalibrationdata_pkcalibrationdataid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblcalibrationdata_pkcalibrationdataid_seq TO PUBLIC;


--
-- TOC entry 2075 (class 0 OID 0)
-- Dependencies: 181
-- Name: tblchannel; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblchannel FROM PUBLIC;
REVOKE ALL ON TABLE tblchannel FROM postgres;
GRANT ALL ON TABLE tblchannel TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblchannel TO PUBLIC;


--
-- TOC entry 2077 (class 0 OID 0)
-- Dependencies: 180
-- Name: tblchannel_pkchannelid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblchannel_pkchannelid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblchannel_pkchannelid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblchannel_pkchannelid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblchannel_pkchannelid_seq TO PUBLIC;


--
-- TOC entry 2078 (class 0 OID 0)
-- Dependencies: 164
-- Name: tblcomputetype; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblcomputetype FROM PUBLIC;
REVOKE ALL ON TABLE tblcomputetype FROM postgres;
GRANT ALL ON TABLE tblcomputetype TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblcomputetype TO PUBLIC;


--
-- TOC entry 2080 (class 0 OID 0)
-- Dependencies: 163
-- Name: tblcomputetype_pkcomputetypeid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblcomputetype_pkcomputetypeid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblcomputetype_pkcomputetypeid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblcomputetype_pkcomputetypeid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblcomputetype_pkcomputetypeid_seq TO PUBLIC;


--
-- TOC entry 2081 (class 0 OID 0)
-- Dependencies: 165
-- Name: tbldate; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tbldate FROM PUBLIC;
REVOKE ALL ON TABLE tbldate FROM postgres;
GRANT ALL ON TABLE tbldate TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tbldate TO PUBLIC;


--
-- TOC entry 2082 (class 0 OID 0)
-- Dependencies: 179
-- Name: tblerrorlog; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblerrorlog FROM PUBLIC;
REVOKE ALL ON TABLE tblerrorlog FROM postgres;
GRANT ALL ON TABLE tblerrorlog TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblerrorlog TO PUBLIC;


--
-- TOC entry 2084 (class 0 OID 0)
-- Dependencies: 178
-- Name: tblerrorlog_pkerrorlogid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblerrorlog_pkerrorlogid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblerrorlog_pkerrorlogid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblerrorlog_pkerrorlogid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblerrorlog_pkerrorlogid_seq TO PUBLIC;


--
-- TOC entry 2085 (class 0 OID 0)
-- Dependencies: 177
-- Name: tblmetadata; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblmetadata FROM PUBLIC;
REVOKE ALL ON TABLE tblmetadata FROM postgres;
GRANT ALL ON TABLE tblmetadata TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblmetadata TO PUBLIC;


--
-- TOC entry 2086 (class 0 OID 0)
-- Dependencies: 171
-- Name: tblmetric; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblmetric FROM PUBLIC;
REVOKE ALL ON TABLE tblmetric FROM postgres;
GRANT ALL ON TABLE tblmetric TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblmetric TO PUBLIC;


--
-- TOC entry 2088 (class 0 OID 0)
-- Dependencies: 170
-- Name: tblmetric_pkmetricid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblmetric_pkmetricid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblmetric_pkmetricid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblmetric_pkmetricid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblmetric_pkmetricid_seq TO PUBLIC;


--
-- TOC entry 2090 (class 0 OID 0)
-- Dependencies: 174
-- Name: tblmetricdata; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblmetricdata FROM PUBLIC;
REVOKE ALL ON TABLE tblmetricdata FROM postgres;
GRANT ALL ON TABLE tblmetricdata TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblmetricdata TO PUBLIC;


--
-- TOC entry 2092 (class 0 OID 0)
-- Dependencies: 168
-- Name: tblnetwork_pknetworkid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblnetwork_pknetworkid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblnetwork_pknetworkid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblnetwork_pknetworkid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblnetwork_pknetworkid_seq TO PUBLIC;


--
-- TOC entry 2093 (class 0 OID 0)
-- Dependencies: 173
-- Name: tblprecomputed; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblprecomputed FROM PUBLIC;
REVOKE ALL ON TABLE tblprecomputed FROM postgres;
GRANT ALL ON TABLE tblprecomputed TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblprecomputed TO PUBLIC;


--
-- TOC entry 2095 (class 0 OID 0)
-- Dependencies: 172
-- Name: tblprecomputed_pkprecomputedid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblprecomputed_pkprecomputedid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblprecomputed_pkprecomputedid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblprecomputed_pkprecomputedid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblprecomputed_pkprecomputedid_seq TO PUBLIC;


--
-- TOC entry 2096 (class 0 OID 0)
-- Dependencies: 183
-- Name: tblsensor; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblsensor FROM PUBLIC;
REVOKE ALL ON TABLE tblsensor FROM postgres;
GRANT ALL ON TABLE tblsensor TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblsensor TO PUBLIC;


--
-- TOC entry 2098 (class 0 OID 0)
-- Dependencies: 182
-- Name: tblsensor_pksensorid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblsensor_pksensorid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblsensor_pksensorid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblsensor_pksensorid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblsensor_pksensorid_seq TO PUBLIC;


--
-- TOC entry 2099 (class 0 OID 0)
-- Dependencies: 176
-- Name: tblstation; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE tblstation FROM PUBLIC;
REVOKE ALL ON TABLE tblstation FROM postgres;
GRANT ALL ON TABLE tblstation TO postgres;
GRANT SELECT,REFERENCES,TRIGGER ON TABLE tblstation TO PUBLIC;


--
-- TOC entry 2101 (class 0 OID 0)
-- Dependencies: 175
-- Name: tblstation_pkstationid_seq; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON SEQUENCE tblstation_pkstationid_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tblstation_pkstationid_seq FROM postgres;
GRANT ALL ON SEQUENCE tblstation_pkstationid_seq TO postgres;
GRANT SELECT ON SEQUENCE tblstation_pkstationid_seq TO PUBLIC;


--
-- TOC entry 1501 (class 826 OID 17290)
-- Dependencies: 5
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES  FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES  TO PUBLIC;


-- Completed on 2012-08-06 12:34:03 MDT

--
-- PostgreSQL database dump complete
--

