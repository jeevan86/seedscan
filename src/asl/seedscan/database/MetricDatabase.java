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

import java.nio.ByteBuffer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import org.apache.log4j.Logger;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.seedscan.config.*;
import asl.seedscan.metrics.*;

public class MetricDatabase
{
    public static final Logger logger = Logger.getLogger("asl.seedscan.database.MetricDatabase");

    private Connection connection;
    private String URI;
    private String username;
    private String password;
    
    private CallableStatement callStatement;

    public MetricDatabase(DatabaseT config) {
        this(config.getUri(), config.getUsername(), config.getPassword().getPlain());
    }
    public MetricDatabase(String URI, String username, String password) {
    	this.URI = URI;
    	this.username = username;
    	this.password = password;

        logger.info("MetricDatabase Constructor(): Attempt to connect to the dbase");
        try {
            logger.info(String.format("Connection String = \"%s\", User = \"%s\", Pass = \"%s\"", this.URI, this.username, this.password));
            connection = DriverManager.getConnection(URI, username, password);
        } catch (SQLException e) {
            System.err.print(e);
            logger.error("Could not open station database.");
// MTH: For now let's continue
            //throw new RuntimeException("Could not open station database.");
        }
    }

    public boolean isConnected()
    {
    	Connection foo = getConnection();
        if (foo == null) 
            return false;
        else 
            return true;
    }
    
    public Connection getConnection()
    {
    	return connection;
    }

    
    public ByteBuffer getMetricDigest(	Calendar date,
    									String metricName,
    									Station station)
    {
    	ByteBuffer digest = null;
    	
        try {
	        callStatement = connection.prepareCall("SELECT spGetMetricDigest(?, ?, ?, ?)");
	        java.sql.Date sqlDate = new java.sql.Date(date.getTime().getTime());
	        callStatement.setDate(1, sqlDate, date);
	        callStatement.setString(2, metricName);
	        callStatement.setString(3, station.getNetwork());
	        callStatement.setString(4, station.getStation());
	        ResultSet resultSet = callStatement.executeQuery();
	        if (resultSet.next()) {
	        	digest = ByteBuffer.wrap(resultSet.getBytes(1));
	        }
        }
        catch (SQLException e) {
            //System.out.print(e);
            logger.error(e);
        }
    	return digest;
    }
    
    public ByteBuffer getMetricValueDigest(	Calendar date,
    										String metricName,
    										Station station,
    										Channel channel)
    {
    	ByteBuffer digest = null;
    	
        try {
	        callStatement = connection.prepareCall("SELECT spGetMetricValueDigest(?, ?, ?, ?, ?, ?)");
	        java.sql.Date sqlDate = new java.sql.Date(date.getTime().getTime());
	        callStatement.setDate(1, sqlDate, date);
	        callStatement.setString(2, metricName);
	        callStatement.setString(3, station.getNetwork());
	        callStatement.setString(4, station.getStation());
	        callStatement.setString(5, channel.getLocation());
	        callStatement.setString(6, channel.getChannel());
	        ResultSet resultSet = callStatement.executeQuery();
	        if (resultSet.next()) {
	        	byte[] digestIn = resultSet.getBytes(1);
                if(digestIn != null)
                    digest = ByteBuffer.wrap(digestIn);
	        }
        }
        catch (SQLException e) {
            //System.out.print(e);
            logger.error(e);
        }
    	return digest;
    }
    
    public Double getMetricValue(	Calendar date,
    								String metricName,
    								Station station,
    								Channel channel)
    {
    	Double value = null;
        try {
	        callStatement = connection.prepareCall("SELECT spGetMetricValue(?, ?, ?, ?, ?, ?)");
	        java.sql.Date sqlDate = new java.sql.Date(date.getTime().getTime());
	        callStatement.setDate(1, sqlDate, date);
	        callStatement.setString(2, metricName);
	        callStatement.setString(3, station.getNetwork());
	        callStatement.setString(4, station.getStation());
	        callStatement.setString(5, channel.getLocation());
	        callStatement.setString(6, channel.getChannel());
	        ResultSet resultSet = callStatement.executeQuery();
	        if (resultSet.next()) {
	        	value = resultSet.getDouble(1);
	        }
        }
        catch (SQLException e) {
            //System.out.print(e);
            logger.error(e);
        }
    	return value;
    }
    
    public int insertMetricData(MetricResult results)
    {
    	int result = -1;
        try {
            for (String id: results.getIdSet()) {
            	Channel channel = MetricResult.createChannel(id);
	            callStatement = connection.prepareCall("SELECT spInsertMetricData(?, ?, ?, ?, ?, ?, ?, ?)");
	            java.sql.Date date = new java.sql.Date(results.getDate().getTime().getTime());
	            callStatement.setDate(1, date, results.getDate());
	            callStatement.setString(2, results.getMetricName());
	            callStatement.setString(3, results.getStation().getNetwork());
	            callStatement.setString(4, results.getStation().getStation());
	            callStatement.setString(5, channel.getLocation());
	            callStatement.setString(6, channel.getChannel());
	            callStatement.setDouble(7, results.getResult(id));
	            callStatement.setBytes(8, results.getDigest(id).array());
	            callStatement.executeQuery();
            }
            result = 0;
        }
        catch (SQLException e) {
            //System.out.print(e);
            logger.error(e);
        }
        return result;
    }
    
    public String selectAll(String startDate, String endDate){
    	String result = "";
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
            //System.out.print(e);
            logger.error(e);
        }
        return result;
    }
    
}
