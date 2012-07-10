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
import java.sql.CallableStatement;
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
import asl.metadata.*;

public class StationDatabase
{
    public static final Logger logger = Logger.getLogger("asl.seedscan.database.StationDatabase");

    private Connection connection = null;
    private DatabaseT config = null;
    private String conString = "jdbc:mysql://asltrans.cr.usgs.gov:3306/metricsDev?useInformationSchema=true&noAccessToProcedureBodies=true";
    private String user = "dev";
    private String password = "asldev";
    private CallableStatement callStatement = null;
    private String result;

    public StationDatabase() {
        this.config = config;
System.out.println("StationDatabase Constructor(): This is where we would make the connection to the dbase");
/**
        try {
            connection = DriverManager.getConnection(conString, user, password);
        } catch (SQLException e) {
            System.err.print(e);
            logger.severe("Could not open station database.");
            throw new RuntimeException("Could not open station database.");
        }
**/
    }
    
    public String selectAll(String startDate, String endDate){
        try {
            ResultSet resultSet = null;
            callStatement = connection.prepareCall("CALL spGetAll(?, ?, ?)");
            callStatement.setString(1, startDate);
            callStatement.setString(2, endDate);
            callStatement.registerOutParameter(3, java.sql.Types.VARCHAR);
            resultSet = callStatement.executeQuery();
            result = callStatement.getString(3);
        }
        catch (SQLException e) {
            System.out.print(e);
        }
        return result;
    }
    
}
