--
-- PostgreSQL database dump
--

-- Dumped from database version 9.1.5
-- Dumped by pg_dump version 9.1.6
-- Started on 2012-11-14 14:09:06 MST

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

--
-- TOC entry 1982 (class 0 OID 0)
-- Dependencies: 170
-- Name: tblcomputetype_pkcomputetypeid_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('tblcomputetype_pkcomputetypeid_seq', 1, false);


--
-- TOC entry 1977 (class 0 OID 16674)
-- Dependencies: 169 1978
-- Data for Name: tblcomputetype; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY tblcomputetype (pkcomputetypeid, name, description, iscalibration) FROM stdin;
1	AVG_CH	Values are averaged over channel and number o	f
2	AVG_DAY	Values are averaged over number of days.	f
3	VALUE_CO	Values are totalled over the window of time.	f
4	PARENT	Not used in computations.	f
5	CAL_DATE	Value is the difference between the Calibrati	t
6	CAL_AVG	Values are averaged over the number of values	t
7	NONE	Values are not computed	f
\.


-- Completed on 2012-11-14 14:09:06 MST

--
-- PostgreSQL database dump complete
--

