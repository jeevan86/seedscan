-- Function: fnsclgetgroups()

-- DROP FUNCTION fnsclgetgroups();

CREATE OR REPLACE FUNCTION fnsclgetgroups()
  RETURNS text AS
$BODY$
DECLARE
    groupString TEXT;
BEGIN


    
    SELECT 
    INTO groupString
        string_agg( DISTINCT
            CONCAT(
                  'G,'
                , gp.pkGroupID
                , ','
                , gp."name"
                , ','
                , gp."fkGroupTypeID"

                
            )
            , E'\n' 
        )
    FROM "tblGroup" gp;
        

    RETURN groupString;
    
END;
$BODY$
  LANGUAGE plpgsql STABLE
  COST 100;

