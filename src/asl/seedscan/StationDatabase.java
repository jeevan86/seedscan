/*
 * Copyright 2011, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */

package asl.seedscan;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Logger;

import asl.seedscan.config.DatabaseT;
import asl.security.Password;

public class StationDatabase
{
    public static final Logger logger = Logger.getLogger("asl.seedscan.StationDatabase");

    private Connection connection = null;
    private DatabaseT config = null;

    public StationDatabase(DatabaseT config) {
        this.config = config;
        try {
            //connection = DriverManager.getConnection(config.getUri(), config.getUsername(),
            //                                         new String(config.getPassword().getPassword()));
            connection = DriverManager.getConnection("", "", "");
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
