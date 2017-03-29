-- Function: fnfinishscan(uuid)

-- DROP FUNCTION fnfinishscan(uuid);

CREATE OR REPLACE FUNCTION fnfinishscan(scanid uuid)
  RETURNS void AS
$BODY$
BEGIN

--Update scan to finished
  UPDATE tblscan
   SET finished=true
 WHERE
pkscanid = scanid;

--Update timestamp
UPDATE tblscan
SET lastupdate = current_timestamp
WHERE
pkscanid = scanid;

 
--Update any parent scan if all of its children are finished.
  UPDATE tblscan
  SET finished = TRUE, lastupdate = current_timestamp
  WHERE
  fkparentscan IS NULL --Must be a parent scan
  AND
  taken = TRUE --Must have been processed
  AND
  pkscanid NOT IN (
  --List of all parents with unfinished children
      select distinct fkparentscan
      from tblscan
      where fkparentscan IS NOT NULL
      AND finished = FALSE
  );

  --Remove finished children of finished parent scans.
  DELETE
  FROM tblscan child
  USING tblscan parent
  WHERE
    child.fkparentscan = parent.pkscanid
    AND
    parent.finished = TRUE;


END
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

