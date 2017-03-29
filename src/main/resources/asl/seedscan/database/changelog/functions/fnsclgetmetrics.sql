-- Function: fnsclgetmetrics()

-- DROP FUNCTION fnsclgetmetrics();

CREATE OR REPLACE FUNCTION fnsclgetmetrics()
  RETURNS text AS
$BODY$
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
                , coalesce(DisplayName, name, 'No name')

                
            )
            , E'\n' 
        )
    FROM tblMetric;

    RETURN metricString;
    
END;
$BODY$
  LANGUAGE plpgsql STABLE
  COST 100;

