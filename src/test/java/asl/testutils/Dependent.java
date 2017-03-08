package asl.testutils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.junit.Assume;

/**
 * Dependent contains methods for verifying whether a test or test class can run.
 * Examples include requiring that rdseed exists to setup a MetaGenerator.
 */
public class Dependent {

  /**
   * Make sure rdseed exists otherwise the test can't run
   */
  public static void assumeRDSeed() throws Exception {
    Assume.assumeTrue(
        Pattern.compile(File.pathSeparator)
            .splitAsStream(System.getenv("PATH"))
            .map(Paths::get)
            .anyMatch(path -> Files.exists(path.resolve("rdseed"))));
  }

}
