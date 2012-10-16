package asl;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

public class Opaque
{
    public static void usage()
    {
        System.out.println("usage: " +System.getProperty("program.name")+ " <file1> [file2] ...");
        System.exit(1);
    }

    public static void main(String args[])
    {
        if (args.length < 2) {
            usage();
        }

        for (int i = 1; i < args.length; i++) {
            File file = new File(args[i]);
        }
    }
}
