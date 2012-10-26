<?xml version="1.0"?>
<cfg:config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="https://aslweb.cr.usgs.gov SeedScanConfig.xsd"
            xmlns:cfg="config.seedscan.asl">

    <!--cfg:lockfile>/qcwork/seedscan/seedscan.lock</cfg:lockfile-->
    <cfg:lockfile>seedscan.lock</cfg:lockfile>
 
    <cfg:log>
        <cfg:levels>
            <!-- OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL -->
            <cfg:level cfg:name="">INFO</cfg:level>
<!--
            <cfg:level cfg:name="">ALL</cfg:level>
-->
            <cfg:level cfg:name="com.sun.xml">INFO</cfg:level>
            <cfg:level cfg:name="javax.xml">INFO</cfg:level>
            <cfg:level cfg:name="asl.seedscan">FINEST</cfg:level>
            <cfg:level cfg:name="asl.seedscan.Scanner">INFO</cfg:level>
            <cfg:level cfg:name="asl.seedsplitter">INFO</cfg:level>
<!--
            <cfg:level cfg:name="asl.seedsplitter">INFO</cfg:level>
-->
        </cfg:levels>

        <cfg:handlers>
            <cfg:log_console>
                <cfg:handler_level>FINEST</cfg:handler_level>
            </cfg:log_console>

            <cfg:log_file>
                <cfg:handler_level>FINE</cfg:handler_level>
                <cfg:directory>logs</cfg:directory>
                <cfg:prefix>seedscan-</cfg:prefix>
                <cfg:suffix>.log</cfg:suffix>
            </cfg:log_file>

            <cfg:log_db>
                <cfg:handler_level>CONFIG</cfg:handler_level>
                <cfg:database>
                    <!--cfg:uri>jdbc:mysql://catbox2.cr.usgs.gov/stations</cfg:uri-->
                    <cfg:uri>jdbc:mysql://136.177.121.210:54321/seedscan</cfg:uri>
                    <cfg:username>seedscan_write</cfg:username>
                    <cfg:password>
                        <cfg:plain>S33dSc@nWr1t3r!</cfg:plain>
                        <!--cfg:encrypted>
                            <cfg:salt>952bf002cc030243</cfg:salt>
                            <cfg:iv>952bf002cc030243952bf002cc030243</cfg:iv>
                            <cfg:ciphertext>2f9cb9a02ee92a392f9cb9a02ee92a39</cfg:ciphertext>
                            <cfg:hmac>952bf002cc030243952bf002cc030243</cfg:hmac>
                        <cfg:encrypted-->
                    </cfg:password>
                </cfg:database>
            </cfg:log_db>
        </cfg:handlers>
    </cfg:log>
  
    <cfg:database>
        <!--cfg:uri>jdbc:mysql://catbox2.cr.usgs.gov/stations</cfg:uri-->
        <!--cfg:uri>jdbc:mysql://136.177.121.210:54321/seedscan</cfg:uri-->
        <cfg:uri>jdbc:postgresql://136.177.121.210:5432/dataq_dev</cfg:uri>
        <cfg:username>devwrite</cfg:username>
        <cfg:password>
            <cfg:plain>asldev</cfg:plain>
            <!--cfg:encrypted>
                <cfg:salt>952bf002cc030243</cfg:salt>
                <cfg:iv>952bf002cc030243952bf002cc030243</cfg:iv>
                <cfg:ciphertext>2f9cb9a02ee92a392f9cb9a02ee92a39</cfg:ciphertext>
                <cfg:hmac>952bf002cc030243952bf002cc030243</cfg:hmac>
            <cfg:encrypted-->
        </cfg:password>
    </cfg:database>

    <cfg:scans>
        <cfg:scan cfg:name="daily">
            <cfg:path>/xs0/seed/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}_${NETWORK}_${STATION}</cfg:path>
            <cfg:dataless_dir>/dcc/metadata/dataless/</cfg:dataless_dir>
            <cfg:start_day>140</cfg:start_day>
            <cfg:days_to_scan>1</cfg:days_to_scan>
            <cfg:metrics>
<!--
                <cfg:metric>
                    <cfg:class_name>asl.seedscan.metrics.CalibrationMetric</cfg:class_name>
                </cfg:metric>
-->
                <cfg:metric>
                    <cfg:class_name>asl.seedscan.metrics.AvailabilityMetric</cfg:class_name>
                </cfg:metric>
                <cfg:metric>
                    <cfg:class_name>asl.seedscan.metrics.GapCountMetric</cfg:class_name>
                </cfg:metric>
<!-- PowerBand Metrics:  -->
<!--
                <cfg:metric>
                    <cfg:class_name>asl.seedscan.metrics.CoherencePBM</cfg:class_name>
                    <cfg:argument cfg:name="lower-limit">4</cfg:argument>
                    <cfg:argument cfg:name="upper-limit">8</cfg:argument>
                </cfg:metric>
                <cfg:metric>
                    <cfg:class_name>asl.seedscan.metrics.CoherencePBM</cfg:class_name>
                    <cfg:argument cfg:name="lower-limit">2</cfg:argument>
                    <cfg:argument cfg:name="upper-limit">4</cfg:argument>
                </cfg:metric>
-->
                <cfg:metric>
                    <cfg:class_name>asl.seedscan.metrics.NLNMDeviationMetric</cfg:class_name>
                    <cfg:argument cfg:name="lower-limit">4</cfg:argument>
                    <cfg:argument cfg:name="upper-limit">8</cfg:argument>
                    <cfg:argument cfg:name="modelfile">/home/mhagerty/dev/asl-java-tools/resources/NLNM.ascii/</cfg:argument>
                </cfg:metric>
                <cfg:metric>
                    <cfg:class_name>asl.seedscan.metrics.NLNMDeviationMetric</cfg:class_name>
                    <cfg:argument cfg:name="lower-limit">16</cfg:argument>
                    <cfg:argument cfg:name="upper-limit">22</cfg:argument>
                    <cfg:argument cfg:name="modelfile">/home/mhagerty/dev/asl-java-tools/resources/NLNM.ascii/</cfg:argument>
                </cfg:metric>
                <cfg:metric>
                    <cfg:class_name>asl.seedscan.metrics.StationDeviationMetric</cfg:class_name>
                    <cfg:argument cfg:name="lower-limit">4</cfg:argument>
                    <cfg:argument cfg:name="upper-limit">8</cfg:argument>
<!--
                    <cfg:argument cfg:name="modelpath">/Users/mth/mth/Projects/xs0/stationmodel/${NETWORK}_${STATION}/</cfg:argument>
-->
                    <cfg:argument cfg:name="modelpath">/qcwork/dqresults/dqbin/PSDmodel/senmodel/</cfg:argument>
                </cfg:metric>
            </cfg:metrics>
        </cfg:scan>
<!--

        <cfg:scan cfg:name="yearly">
            <cfg:path>/xs0/seed/${NETWORK}_${STATION}/${YEAR}/${YEAR}_${JDAY}_${NETWORK}_${STATION}</cfg:path>
            <cfg:start_day>1</cfg:start_day>
            <cfg:days_to_scan>366</cfg:days_to_scan>
            <cfg:metrics>
                <cfg:metric>
                    <cfg:class_name>asl.seedscan.metrics.AvailabilityMetric</cfg:class_name>
                </cfg:metric>
            </cfg:metrics>
        </cfg:scan>
-->
    </cfg:scans>
    
</cfg:config>
