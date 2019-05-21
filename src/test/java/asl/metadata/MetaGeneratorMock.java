package asl.metadata;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.StationMeta;

/**
 * Mock class that enables reading in metadata from a text dump produced by rdseed;
 * the class it is based on (asl.metadata.MetaGenerator) is
 */
public class MetaGeneratorMock extends MetaGenerator {

    private static final Logger logger = LoggerFactory
            .getLogger(asl.metadata.MetaGenerator.class);



    public MetaGeneratorMock(String rdseedTextDumpLocation, String networkName, String stationName) {

        List<String> strings = new ArrayList<>();
        BufferedReader reader = null;
        try {

            reader = new BufferedReader(new InputStreamReader(
                    MetaGeneratorMock.class.getResourceAsStream(rdseedTextDumpLocation)));
            String line;
            while ((line = reader.readLine()) != null) {
                strings.add(line);
            }
        } catch (IOException e) {
            logger.error("IOException:", e);
        }

        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        SeedVolume volume = null;

        try {
            volume = buildVolumesFromStringData(strings, networkName, stationName);
        } catch (Exception e) {
            logger.error("== processing dataless volume for file=[{}]", rdseedTextDumpLocation);
        }

        if (volume == null) {
            logger.error("== processing dataless volume==null! for file=[{}]", rdseedTextDumpLocation);
            System.exit(0);
        } else {
            addVolume(volume);
        }

    }

}
