-- Function: fnsclisnumeric(text)

-- DROP FUNCTION fnsclisnumeric(text);

CREATE OR REPLACE FUNCTION fnsclisnumeric("inputText" text)
  RETURNS boolean AS
$BODY$
  DECLARE num NUMERIC;
BEGIN
    IF "inputText" = 'NaN' THEN
        RETURN FALSE;
    END IF;

    num = "inputText"::NUMERIC;
    --No exceptions and hasn't returned false yet, so it must be a numeric.
    RETURN TRUE;
    EXCEPTION WHEN invalid_text_representation THEN
    RETURN FALSE;
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100;

