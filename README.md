SeedScan
========

###Purpose
    SeedScan is used to analyze miniSEED data and quantify data quality. This is done by processing
    various metrics that analyze the data.

###Configuration
    The main configuration is found in config.xml file. This file is not under source control, but
    sample files can be found in the samples folder.

######Date Setup  

    To have a scan start on a specific date:  
```xml
    <cfg:start_date>YYYYDDD</cfg:start_date>  
    <cfg:start_date>2014170</cfg:start_date>  
```

    To have a scan start a certain number of days before the current date:  
```xml
    <cfg:start_day>3</cfg:start_day>
```

    To set the number of days to scan:  
```xml
    <cfg:days_to_scan>10</cfg:days_to_scan>  
```

######Network Station Restrictions  
    To restrict to certain networks:  
```xml
    <cfg:network_subset>IU</cfg:network_subset>  
    <cfg:network_subset>IU,NE</cfg:network_subset>  
```

    To restrict to certain stations:  
```xml
    <cfg:station_subset>YLE</cfg:station_subset>  
    <cfg:station_subset>YLE,ANMO,FURI</cfg:station_subset>
```

######Database Setup  
    SeedScan connects to a PostgreSQL database. The configuration is at the base level of the
    config.xml.
```xml
    <cfg:database>
        <cfg:uri>jdbc:postgresql://hostname.domain.tld:5432/dataq_db</cfg:uri>
        <cfg:username>username</cfg:username>
        <cfg:password>
            <cfg:plain>Password</cfg:plain>
        </cfg:password>
    </cfg:database>
```

######PowerBand Metric Setup  
    PowerBand metrics need inclusion multiple times in the metric tag. The range of the powerband is
    determined by the lower/upper limit attributes.  

```xml
<cfg:metric>
    <cfg:class_name>asl.seedscan.metrics.DifferencePBM</cfg:class_name>
    <cfg:argument cfg:name="lower-limit">4</cfg:argument>
    <cfg:argument cfg:name="upper-limit">8</cfg:argument>
    <cfg:argument cfg:name="makeplots">false</cfg:argument>
</cfg:metric>
<cfg:metric>
    <cfg:class_name>asl.seedscan.metrics.DifferencePBM</cfg:class_name>
    <cfg:argument cfg:name="lower-limit">18</cfg:argument>
    <cfg:argument cfg:name="upper-limit">22</cfg:argument>
    <cfg:argument cfg:name="makeplots">false</cfg:argument>
</cfg:metric>
<cfg:metric>
    <cfg:class_name>asl.seedscan.metrics.DifferencePBM</cfg:class_name>
    <cfg:argument cfg:name="lower-limit">90</cfg:argument>
    <cfg:argument cfg:name="upper-limit">110</cfg:argument>
    <cfg:argument cfg:name="makeplots">false</cfg:argument>
</cfg:metric>
```

###Usage

Basic execution  
ant seedscan
