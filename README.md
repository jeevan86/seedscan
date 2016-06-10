SeedScan
========

###Purpose
    SeedScan is used to analyze miniSEED data and quantify data quality. This is done by processing
    various metrics that analyze the data.
    
###Requirements
######Software
Java 1.8  
Gradle 2.5  
######Hardware
This is dependent upon the quantity of data that is scanned.  
Each thread takes between 1 to 5 GB of RAM depending on the station's data for the day.  
One thread can run an entire day of data for one station in 30 minutes to 1 hour.  

###Configuration
    The main configuration is found in config.xml file. This file is not under source control, but
    sample files can be found in the samples folder.

######Date Setup  

    To have a scan start on a specific date:  
```xml
    <cfg:start_date>YYYYDDD</cfg:start_date>  
    <cfg:start_date>2015252</cfg:start_date>  
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

######Data Quality Restrictions
    The available quality flags are {All, D, Q, R, M}. The program will
    only run a scan on data that has a quality flag that matches one of entries listed in the
    configuration file. 
```xml
    <cfg:qualityflags>All</cfg:qualityflags>  
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

######Data Directory Setup
    There are three data directories that need setup. Path is where the actual miniSEED data is stored.
    It needs to be stored in a directory structure like in the example.  
    The metadata/dataless is stored in dataless_dir. This is usually network dataless files.
    Metrics that use synthetics require a events_dir directory to be setup. The files here are sac files.
```xml
    <cfg:path>/home/asluser/dataloc/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}_${NETWORK}_${STATION}</cfg:path>
    <cfg:dataless_dir>/home/asluser/metadata/</cfg:dataless_dir>
    <cfg:events_dir>/SYNTHETICS/</cfg:events_dir>
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

######Noise Model Deviation Metric Setup
    Seedscan has default noise models. If different noise models need to be specified, they can be set in the metric.
    
```xml
 <cfg:metric>
     <cfg:class_name>asl.seedscan.metrics.NLNMDeviationMetric</cfg:class_name>
     <cfg:argument cfg:name="lower-limit">4</cfg:argument>
     <cfg:argument cfg:name="upper-limit">8</cfg:argument>
     <cfg:argument cfg:name="nlnm-modelfile">resources/NLNM.ascii/</cfg:argument>
     <cfg:argument cfg:name="nhnm-modelfile">resources/NHNM.ascii/</cfg:argument>
     <cfg:argument cfg:name="makeplots">true</cfg:argument>
 </cfg:metric>
```

######Channel Band Restrictions Metric Setup  
    Many metrics allow for restricting what bands are computed. This is set with the channel-restriction argument.  
    Applicable Metrics: ALNM, Event Compare Strong Motion, Event Compare Synthetic, NLNM  
    It can be a single band "LH" or multiple "LH,BH,HH".  
    Defaults are metric specific.  

```xml
 <cfg:metric>
    <cfg:class_name>asl.seedscan.metrics.ALNMDeviationMetric</cfg:class_name>
    <cfg:argument cfg:name="lower-limit">18</cfg:argument>
    <cfg:argument cfg:name="upper-limit">22</cfg:argument>
    <cfg:argument cfg:name="channel-restriction">LN,HN</cfg:argument>
    <cfg:argument cfg:name="makeplots">true</cfg:argument>
 </cfg:metric>
```
######Comparison Based Metric Setup  
    Metrics that compare sensors with sensors or synthetics, can have the default setting overridden.  
    base-channel: Sets the channel to which everything is compared.  
    Applicable Metrics: Coherence, Difference, Event Compare Strong Motion, Event Compare Synthetic (Sets what band of synthetic)  

```xml
 <cfg:metric>
    <cfg:class_name>asl.seedscan.metrics.CoherencePBM</cfg:class_name>
    <cfg:argument cfg:name="lower-limit">90</cfg:argument>
    <cfg:argument cfg:name="upper-limit">110</cfg:argument>
    <cfg:argument cfg:name="makeplots">true</cfg:argument>
    <cfg:argument cfg:name="base-channel">00-LH</cfg:argument>
 </cfg:metric>
```
###Usage

######Compilation
    To compile execute "gradle build". This will download required dependencies, compile, and test the source code against current unit tests.
    
######Basic Execution  
    To simply compile and execute seedscan run "gradle run" in the seedscan folder.
    If you wish to log the output to a file, run "gradle run | tee filename". This will both print
    to the screen and write to the file.

######Creating a JAR
    Executing "gradle jar" will create a jar file within build/libs/. The jar will create a logs folder wherever it is run, it looks for config.xml file in the folder it is located.
