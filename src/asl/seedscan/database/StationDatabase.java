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

package asl.seedscan.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.logging.Logger;

import asl.security.*;
import asl.seedscan.*;
import asl.seedscan.config.*;

public class StationDatabase
{
    public static final Logger logger = Logger.getLogger("asl.seedscan.database.StationDatabase");

    private Connection connection = null;
    private DatabaseT config = null;
    private String conString = "jdbc:mysql://asltrans.cr.usgs.gov:3306/metricsDev";
    private String user = "dev";
    private String password = "asldev";
    private PreparedStatement prepStatement = null;

    public StationDatabase(DatabaseT config) {
        this.config = config;
        try {
            connection = DriverManager.getConnection(conString, user, password);
        } catch (SQLException e) {
            logger.severe("Could not open station database.");
            throw new RuntimeException("Could not open station database.");
        }
    }
    
    public String selectAll(String startDate, String endDate){
        try {
            ResultSet resultSet = null;
            prepStatement = connection.prepareStatement("Select spGetAll(?, ?)");
            prepStatement.setString(1, startDate);
            prepStatement.setString(2, endDate);
            resultSet = prepStatement.executeQuery();
            System.out.print(resultSet);
        }
        catch (SQLException e) {
            System.out.print(e);
        }
        String stringy = "Stringy";
        return stringy;
    }
    
}
