package asl;

import java.io.File;

public class Opaque {
	public static void usage() {
		System.out.println("usage: " + System.getProperty("program.name")
				+ " <file1> [file2] ...");
		System.exit(1);
	}

	public static void main(String args[]) {
		if (args.length < 2) {
			usage();
		}

		for (int i = 1; i < args.length; i++) {
			File file = new File(args[i]);
		}
	}
}
