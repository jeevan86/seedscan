package asl.seedscan.database;

import java.sql.SQLException;
import org.junit.Test;

/**
 * Without mocking the Postgresql database, this is restricted to testing limited requirements.
 */
public class MetricDatabaseTest {

  @Test(expected = SQLException.class)
  public void testMetricDatabase_Exception() throws Exception {
    /* If the database wasn't able to connect it should throw an exception.*/
    new MetricDatabase("jdbc:postgresql://localhost/test", "username",
        "passwordthatshouldn'texist");
  }
}