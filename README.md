SeedScan
========

###Purpose
    SeedScan is used to analyze miniSEED data and quantify data quality. This is done by processing
    various metrics that analyze the data.

###Configuration
    The main configuration is found in config.xml file. This file is not under source control, but
    sample files can be found in the samples folder.

    ######Date setup  

    To have a scan start on a specific date:  
    <cfg:start_date>YYYYDDD</cfg:start_date>  
    <cfg:start_date>2014170</cfg:start_date>  

    To have a scan start a certain number of days before the current date:  
    <cfg:start_day>3</cfg:start_day>

    To set the number of days to scan:  
    <cfg:days_to_scan>10</cfg:days_to_scan>  

    ######Network Station Restrictions  
    To restrict to certain networks:  
    <cfg:network_subset>IU</cfg:network_subset>  
    <cfg:network_subset>IU,NE</cfg:network_subset>  

    To restrict to certain stations:  
    <cfg:station_subset>YLE</cfg:station_subset>  
    <cfg:station_subset>YLE,ANMO,FURI</cfg:station_subset>  

###Usage

Basic execution  
ant seedscan
