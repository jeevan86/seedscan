package asl.azimuth;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import asl.seedsplitter.BlockLocator;
import asl.seedsplitter.ContiguousBlock;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.SeedSplitter;
import asl.seedsplitter.Sequence;
import asl.seedsplitter.SequenceRangeException;

/**
 * Contains main() program start.  Runs command line version of program.
 * 
 * @author  Joel Edwards
 */
public class Azimuth
{

    /**
     * Print a message if not empty, followed by the usage, then exits program.
     *
     * @param   message     The error message to print. If message is an empty 
     *                      string, the line will not be printed.
     * 
     * @return  always returns false
     */
    private static boolean usage(String message) {
        if (message.length() > 0) {
            System.out.println("E: " + message);
        }
        System.out.println("Usage: azimuth.jar <north_file> <east_file> <ref_file> <offset_angle> <out_file>");
        System.out.println("    offset_angle: anticipated offset from the reference (0.0 <= angle <= 360.0");
        System.exit(0);
        return false;
    }

    /**
     * Calls usage(String) with an empty message.
     */
    private static boolean usage() {
        return usage("");
    }

    /**
     * Print an optional error message, then exits.
     *
     * @param   message     The error message to print. If message is an empty 
     *                      string, the line will not be printed.
     * 
     * @return  always returns false
     */
    private static boolean error(String message) {
        System.out.println("E: " + (message.length() > 0 ? message : "An unknown error occured"));
        System.exit(0);
        return false;
    }

    /**
     * Opens a GUI if no arguments are found, otherwise attempts to run 
     * in command line mode.
     *
     * @param   args    The command line arguments.
     */
    public static void main(String args[]) {
        if (args.length == 0) {
            // Create and execute GUI
            new AZdisplay();
            return;
        }
        if (args.length != 5) {
            usage("Wrong number of arguments");
        }

        File northFile = new File(args[0]);
        File eastFile = new File(args[1]);
        File referenceFile = new File(args[2]);
        double offsetAngle = 0.0;
        try {
            offsetAngle = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            usage("offset_angle must be a real number");
        }
        if ((offsetAngle < 0.0) || (offsetAngle > 360.0)) {
            usage("offset_angle outside of allowed range");
        }
        File outputFile = new File(args[4]);

        // Confirm that the files exist and we have permission to read them before proceeding
        if (!northFile.exists())
            error("The selected North file '" + northFile + "' does not exist");
        if (!northFile.canRead())
            error("You do not have permission to read North file '" + northFile + "'");

        if (!eastFile.exists())
            error("The selected East file '" + eastFile + "' does not exist");
        if (!eastFile.canRead())
            error("You do not have permission to read East file '" + eastFile + "'");

        if (!referenceFile.exists())
            error("The selected reference file '" + referenceFile + "' does not exist");
        if (!referenceFile.canRead())
            error("You do not have permission to read Reference file '" + referenceFile + "'");

        if (outputFile.exists())
            error("The selected output file '" + outputFile + "' already exists");

        File[] files = {northFile, eastFile, referenceFile};

        System.out.println("Reading SEED files...");

        Hashtable<String,ArrayList<DataSet>> dataTable;
        SeedSplitter reader = new SeedSplitter(files); 
        //Thread readThread = new Thread(reader);
        //readThread.start();
        reader.doInBackground();
        dataTable = reader.getTable();

        System.out.println("Processing complete.");
        System.out.println("Stopping here as the logic for the command line tool needs to be re-worked.");

        // XXX: Re-work the following to handle the output of SeedSplitter
        /*
        long expectedInterval = dataLists.get(0).get(0).getInterval();
        if ((expectedInterval != dataLists.get(1).get(0).getInterval()) ||
                (expectedInterval != dataLists.get(2).get(0).getInterval())) {
            System.out.println("Intervals do not match across all files:");
            System.out.println("  " +northFile+ ": interval is " +dataLists.get(0).get(0).getInterval()+ " us");
            System.out.println("  " +eastFile+ ": interval is " +dataLists.get(1).get(0).getInterval()+ " us");
            System.out.println("  " +referenceFile+ ": interval is " +dataLists.get(2).get(0).getInterval()+ " us");
            System.out.println("Bailing!");
            System.exit(1);
        }


        for (int i = 0; i < dataLists.size(); i++) {
            System.out.println("ArrayList " + i);
            ArrayList<DataSet> dataList = dataLists.get(i);
            for (int j = 0; j < dataList.size(); j++) {
                DataSet dataSet = dataList.get(j);
                System.out.println("  DataSet " + j);
                System.out.println("    " + dataSet.getNetwork() + "_" + dataSet.getStation() + " " + dataSet.getLocation() + "-" + dataSet.getChannel() + "(" + dataSet.getLength() + " data points)");
                System.out.println("    " + Sequence.timestampToString(dataSet.getStartTime()) + " - " + Sequence.timestampToString(dataSet.getEndTime()));
            }
        }

        System.out.println("Locating contiguous blocks...");

        ArrayList<ContiguousBlock> blocks = null;
        BlockLocator locator = new BlockLocator(dataLists);
        //Thread blockThread = new Thread(locator);
        //blockThread.start();
        locator.doInBackground();
        blocks = locator.getBlocks();

        System.out.println("Found " + blocks.size() + " Contiguous Blocks");

        ContiguousBlock largestBlock = null;
        ContiguousBlock lastBlock = null;
        for (ContiguousBlock block: blocks) {
            if ((largestBlock == null) || (largestBlock.getRange() < block.getRange())) {
                largestBlock = block;
            }
            if (lastBlock != null) {
                System.out.println("    Gap: " + ((block.getStartTime() - lastBlock.getEndTime()) / block.getInterval()) + " data points (" + (block.getStartTime() - lastBlock.getEndTime()) + " microseconds)");
            }
            System.out.println("  Time Range: " + Sequence.timestampToString(block.getStartTime()) + " - " + Sequence.timestampToString(block.getEndTime()) + " (" + ((block.getEndTime() - block.getStartTime()) / block.getInterval() + 1) + " data points)");
            lastBlock = block;
        }
        System.out.println("");
        System.out.println("Largest Block:");
        System.out.println("  Time Range: " + Sequence.timestampToString(largestBlock.getStartTime()) + " - " + Sequence.timestampToString(largestBlock.getEndTime()) + " (" + ((largestBlock.getEndTime() - largestBlock.getStartTime()) / largestBlock.getInterval() + 1) + " data points)");

        double[][] channels = {null, null, null};
        int[] channel = null;

        for (int i = 0; i < 3; i++) {
            boolean found = false;
            for (DataSet set: dataLists.get(i)) {
                if ((!found) && set.containsRange(largestBlock.getStartTime(), largestBlock.getEndTime())) {
                    try {
                        System.out.println("  DataSet[" +i+ "]: " + Sequence.timestampToString(set.getStartTime()) + " - " + Sequence.timestampToString(set.getEndTime()) + " (" + ((set.getEndTime() - set.getStartTime()) / set.getInterval() + 1) + " data points)");
                        channel = set.getSeries(largestBlock.getStartTime(), largestBlock.getEndTime());
                        channels[i] = intArrayToDoubleArray(channel);
                    } catch (SequenceRangeException e) {
                        //System.out.println("SequenceRangeException");
                        e.printStackTrace();
                    }
                    // Add if we decide to clean up memory as we go.
                    // Might not want to if we wish to allow the user to 
                    // pick a different block without re-scanning.
                    found = true;
                    break;
                }
            }
        }

        double[] north = channels[0];
        double[] east  = channels[1];
        double[] reference = channels[2];

        System.out.println("Evaluating Azimuth...");

        AzimuthLocator factory = new AzimuthLocator(north, east, reference);   
        try {
            factory.doInBackground();
        } catch (AzimuthConvergenceException e) {
            System.out.println("Convergence Failed!"); //
            System.exit(1);
        }

        System.out.println("Done.");
        System.out.println("");

        // === Everything below this line, within main(), is for testing purposes ===

        Runtime run = Runtime.getRuntime();
        System.out.println("=========================");
        System.out.println("  Max Memory: " + run.maxMemory());
        System.out.println("-------------------------");
        System.out.println(" Used Memory: " + (run.totalMemory() - run.freeMemory()));
        System.out.println(" Free Memory: " + run.freeMemory());
        System.out.println("Total Memory: " + run.totalMemory());
        System.out.println("=========================");
        System.out.println("");

        // ===== CLEAN UP MEMORY =====
        reader = null;
        files = null;
        northFile = null;
        eastFile = null;
        referenceFile = null;
        reader = null;
        //factory.flush();
        factory = null;

        blocks.clear();
        blocks = null;
        locator = null;
        lastBlock = null;
        largestBlock = null;

        channels[0] = null;
        channels[1] = null;
        channels[2] = null;
        channels = null;
        north = null;
        east = null;
        reference = null;
        for (ArrayList<DataSet> list: dataLists) {
            for (DataSet set: list) {
                set.clear();
            }
            list.clear();
        }
        dataLists.clear();
        dataLists = null;
        // ===== CLEAN UP MEMORY ===== DONE

        System.out.println("Running Garbage Collector...");
        long startM = System.currentTimeMillis();
        run.gc();
        long endM = System.currentTimeMillis();
        System.out.println("Garbage Collection Complete. (Took " +(endM - startM)+ " ms)");

        System.out.println("");
        System.out.println("=========================");
        System.out.println("  Max Memory: " + run.maxMemory());
        System.out.println("-------------------------");
        System.out.println(" Used Memory: " + (run.totalMemory() - run.freeMemory()));
        System.out.println(" Free Memory: " + run.freeMemory());
        System.out.println("Total Memory: " + run.totalMemory());
        System.out.println("=========================");
        */
    }

    /**
     * Converts an array of type int into an array of type double.
     *
     * @param   source     The array of int values to be converted.
     * 
     * @return  An array of double values.
     */
    static double[] intArrayToDoubleArray(int[] source) 
    {
        double[] dest = new double[source.length];
        int length = source.length;
        for (int i = 0; i < length; i++) {
            dest[i] = source[i];
        }
        return dest;
    }
}

