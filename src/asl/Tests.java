package asl;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import asl.security.MemberDigest;
import asl.util.Hex;

public class Tests
{
    public static void usage()
    {
        System.out.println("usage: " +System.getProperty("program.name")+ " <test>");
        System.exit(1);
    }

    public static void main(String args[])
    {
        MemberDigest memberDigest = new MemberDigest() {
            protected void addDigestMembers() {
                addToDigest("Test string is testing");
            }
        };
        System.out.format("Testing MemberDigest & Hex:\n", memberDigest.getDigestString());
        System.out.format(" 1) MemberDigest.getDigestString() - %s\n", memberDigest.getDigestString());

        byte[] pre = memberDigest.getDigestBytes();
        System.out.format(" 2) MemberDigest.getDigestBytes()  - ");
        for (byte b: pre) {
            System.out.format("%02x", b);
        }
        System.out.print("\n");

        String hexStr = Hex.byteArrayToHexString(pre);
        System.out.format(" 3) Hex.byteArrayToHexString()     - %s\n", hexStr);

        byte[] post = Hex.hexStringToByteArray(hexStr);
        System.out.format(" 4) Hex.hexStringToByteArray()     - ");
        for (byte b: post) {
            System.out.format("%02x", b);
        }


        /*
        if (args.length < 2) {
            usage();
        }

        System.out.println("You selected test: " +args[1]);
        */
    }
}
