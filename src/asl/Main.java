package asl;

public class Main {
	public static void usage() {
		System.out.println("usage: " + System.getProperty("program.name")
				+ " <program>");
		System.exit(1);
	}

	public static void main(String args[]) {
		if (args.length < 2) {
			usage();
		}

		System.out.println("You selected program: " + args[1]);
	}
}
