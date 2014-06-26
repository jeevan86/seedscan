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
    config.xml. The database drivers are found in the lib directory. If an upgrade is required,
    simply remove the existing driver and add the new driver.  
```xml
    <cfg:database>
        <cfg:uri>jdbc:postgresql://hostname.domain.tld:5432/dataq_db</cfg:uri>
        <cfg:username>username</cfg:username>
        <cfg:password>
            <cfg:plain>Password</cfg:plain>
        </cfg:password>
    </cfg:database>
```

######General Metric Setup  
    Class Name:  
    The class name needs to match the actual name of the class.  
```xml
    <cfg:class_name>asl.seedscan.metrics.AvailabilityMetric</cfg:class_name>
```
######Plot Setup
    Plots are stored in a directory determined in the config.xml.  If this field is not specified
    you can find the plots in a directory called "null" in the seedscan directory.
```xml 
    <cfg:plots_dir>./outputs</cfg:plots_dir>
```
    Metrics that produce plots can have this ability toggled by changing the makeplots argument.
```xml
    <cfg:argument cfg:name="makeplots">false</cfg:argument>
    <cfg:argument cfg:name="makeplots">true</cfg:argument>
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

######Basic Execution  
    To simply compile and execute seedscan run "ant seedscan" in the seedscan folder.
    If you wish to log the output to a file, run "ant seedscan | tee filename". This will both print
    to the screen and write to the file.

######Creating a JAR
    The ant build is capable of creating a jar file, but currently the jar has errors when executing.
    Use "ant jar" to create a jar file.
