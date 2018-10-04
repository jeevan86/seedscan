-- Function: fntakenextscan()

-- DROP FUNCTION fntakenextscan();

CREATE OR REPLACE FUNCTION fntakenextscan()
  RETURNS SETOF tblscan AS
$BODY$
DECLARE
    scanID uuid;
BEGIN
--We do not want multiple connections taking the same scan
    LOCK TABLE tblscan IN ACCESS EXCLUSIVE MODE;

    --Find our priority scan.
    SELECT pkscanid
      FROM tblscan
      WHERE
          finished = FALSE
          AND
          (
          scheduledrun < current_date
          OR
          scheduledrun IS NULL
          )
          AND
          taken = FALSE
      ORDER BY
          priority desc,
          enddate desc,
          startdate desc
      LIMIT 1
  INTO scanID;

--Set taken update timestamp
  UPDATE tblscan
    SET taken=true, lastupdate = current_timestamp
  WHERE
    pkscanid = scanID;

RETURN QUERY
SELECT pkscanid, fkparentscan, lastupdate, metricfilter, networkfilter,
       stationfilter, channelfilter, startdate, enddate, priority, deleteexisting,
       scheduledrun, finished, taken, locationfilter
  FROM tblscan
  WHERE
  pkscanid = scanID;



END
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100
  ROWS 1000;

