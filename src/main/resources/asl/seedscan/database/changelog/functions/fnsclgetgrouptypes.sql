-- Function: fnsclgetgrouptypes()

-- DROP FUNCTION fnsclgetgrouptypes();

CREATE OR REPLACE FUNCTION fnsclgetgrouptypes()
  RETURNS text AS
$BODY$
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
$BODY$
  LANGUAGE plpgsql STABLE
  COST 100;

