-- Function: fnsclgetdates()

-- DROP FUNCTION fnsclgetdates();

CREATE OR REPLACE FUNCTION fnsclgetdates()
  RETURNS text AS
$BODY$
DECLARE
    dateString TEXT;
BEGIN
    
    SELECT INTO dateString
        string_agg(
            "date"
            , E'\n'
        )
    FROM (

    SELECT CONCAT('DS,', MIN(date)) as date
      FROM tbldate
      UNION
    SELECT CONCAT('DE,', MAX(date)) as date
      FROM tbldate
    ) dates; --to_char('2012-03-01'::date, 'J')::INT  || to_date(2456013::text, 'J')

    RETURN dateString;
END;
$BODY$
  LANGUAGE plpgsql STABLE
  COST 100;

