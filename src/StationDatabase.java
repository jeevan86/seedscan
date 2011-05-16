import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Logger;

public class StationDatabase
{
    public static final Logger logger = Logger.getLogger("StationDatabase");

    private Connection connection = null;

    public StationDatabase(String url, String username, char[] password) {
        try {
            connection = DriverManager.getConnection(url, username, new String(password));
        } catch (SQLException e) {
            logger.severe("Could not open station database.");
            throw new RuntimeException("Could not open station database.");
        }
    }

    public ArrayList<Station> getStations(int limit) {
        ArrayList<Station> results = null;
        if (limit != 0) {
            try {
                Statement s = connection.createStatement();
                String query = "SELECT network,name FROM Station";
                if (limit > 0) {
                    query = query+ " LIMIT " +limit;
                } 
                s.executeQuery(query);
                ResultSet r = s.getResultSet();
                results = new ArrayList<Station>();
                while (r.next()) {
                    results.add(new Station(r.getString("network"), r.getString("name")));
                }
                try {
                    r.close();
                } catch (SQLException e) {
                    // We can still proceed even if we could not close 
                    // this ResultSet. It will eventually go out of scope.
                }
            } catch (SQLException e) {
                results = null;
            }
        }
        return results;
    }

    public ArrayList<Station> getStations() {
        return getStations(-1);
    }

}
