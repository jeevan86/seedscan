--
-- PostgreSQL database dump
--

-- Dumped from database version 9.1.5
-- Dumped by pg_dump version 9.1.6
-- Started on 2012-11-14 14:07:29 MST

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

--
-- TOC entry 1980 (class 0 OID 0)
-- Dependencies: 163
-- Name: tblGroupType_pkGroupTypeID_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('"tblGroupType_pkGroupTypeID_seq"', 1, false);


--
-- TOC entry 1975 (class 0 OID 16655)
-- Dependencies: 162 1976
-- Data for Name: tblGroupType; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY "tblGroupType" ("pkGroupTypeID", name) FROM stdin;
1	Network Code
2	Groups
3	Countries
4	Regions
\.


-- Completed on 2012-11-14 14:07:29 MST

--
-- PostgreSQL database dump complete
--

