
package asl.seedscan.database;

import java.beans.PropertyVetoException;
import java.nio.ByteBuffer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.seedscan.config.DatabaseT;
import asl.seedscan.metrics.MetricResult;
import asl.seedscan.worker.Scan;

/**
 * The Class MetricDatabase. This contains methods for inserting and retrieving
 * data from the database.
 * 
 * @author James Holland - USGS
 * @author Joel Edwards - USGS
 */
public class MetricDatabase {

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.database.MetricDatabase.class);

	/** The connection. */
	private ComboPooledDataSource dataSource;

	/** The uri. */
	private String URI;

	/** The username. */
	private String username;

	// private CallableStatement callStatement;

	/**
	 * Instantiates a new metric database based off the jaxb config.
	 *
	 * @param config
	 *            the config
	 * @throws SQLException if the database is unable to be communicated with.
	 */
	public MetricDatabase(DatabaseT config) throws SQLException {
		this(config.getUri(), config.getUsername(), config.getPassword().getPlain());
	}

	/**
	 * Instantiates a new metric database.
	 *
	 * @param URI
	 *            the location of the database
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 * @throws SQLException if the database is unable to be communicated with.
	 */
	private MetricDatabase(String URI, String username, String password) throws SQLException {
		this.URI = URI;
		this.username = username;
		logger.info("MetricDatabase Constructor(): Attempting to connect to the database");

		logger.info(String.format("Connection String = \"%s\", User = \"%s\"", this.URI, this.username));
		try {
			dataSource = new ComboPooledDataSource();
			dataSource.setDriverClass("org.postgresql.Driver");

			dataSource.setJdbcUrl(URI);
			dataSource.setUser(username);
			dataSource.setPassword(password);

			// the settings below are optional -- c3p0 can work with defaults
			dataSource.setMinPoolSize(3);
			dataSource.setAcquireIncrement(5);
			dataSource.setMaxPoolSize(20);
		} catch (PropertyVetoException e) {
			logger.error("Unable to establish connection to database");
			if (dataSource != null) {
				dataSource.close();
				dataSource = null;
			}
		}
		//Reset any orphaned scans immiediately after connection.
		resetStationScans();
	}

	/**
	 * Used for testing purposes only. Where java requires call to super() in
	 * mock Class.
	 */
	public MetricDatabase() {
	}

	/**
	 * Checks if is connected.
	 *
	 * @return true, if is connected
	 */
	public boolean isConnected() {
		if (dataSource == null)
			return false;

		return true;
	}

	/**
	 * Gets the metric value digest for a particular channel, metric, day.
	 *
	 * @param date
	 *            the date
	 * @param metricName
	 *            the metric name
	 * @param station
	 *            the network and station information
	 * @param channel
	 *            the channel and location information
	 * @return the metric value digest
	 */
	public ByteBuffer getMetricValueDigest(Calendar date, String metricName, Station station, Channel channel) {
		ByteBuffer digest = null;
		Connection connection = null;
		CallableStatement callStatement = null;
		ResultSet resultSet = null;
		try {
			try {
				connection = dataSource.getConnection();
				callStatement = connection.prepareCall("SELECT spGetMetricValueDigest(?, ?, ?, ?, ?, ?)");

				java.sql.Date sqlDate = new java.sql.Date(date.getTime().getTime());
				callStatement.setDate(1, sqlDate, date);
				callStatement.setString(2, metricName);
				callStatement.setString(3, station.getNetwork());
				callStatement.setString(4, station.getStation());
				callStatement.setString(5, channel.getLocation());
				callStatement.setString(6, channel.getChannel());

				resultSet = callStatement.executeQuery();

				if (resultSet.next()) {
					byte[] digestIn = resultSet.getBytes(1);

					if (digestIn != null)
						digest = ByteBuffer.wrap(digestIn);
				}
			} finally {
				if(resultSet != null)resultSet.close();
				if(callStatement != null) callStatement.close();
				if(connection != null)connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}

		return digest;
	}

	/**
	 * Gets the metric value for a particular channel, metric, day.
	 *
	 * @param date
	 *            the date
	 * @param metricName
	 *            the metric name
	 * @param station
	 *            the network and station information
	 * @param channel
	 *            the channel and location information
	 * @return the metric value
	 */
	public Double getMetricValue(Calendar date, String metricName, Station station, Channel channel) {
		Double value = null;
		String sqlDateString = null;
		Connection connection = null;
		CallableStatement callStatement = null;
		ResultSet resultSet = null;
		try {
			try {
				connection = dataSource.getConnection();
				callStatement = connection.prepareCall("SELECT spGetMetricValue(?, ?, ?, ?, ?, ?)");
				java.sql.Date sqlDate = new java.sql.Date(date.getTime().getTime());
				sqlDateString = sqlDate.toString();
				callStatement.setDate(1, sqlDate, date);
				callStatement.setString(2, metricName);
				callStatement.setString(3, station.getNetwork());
				callStatement.setString(4, station.getStation());
				callStatement.setString(5, channel.getLocation());
				callStatement.setString(6, channel.getChannel());
				resultSet = callStatement.executeQuery();
				if (resultSet.next()) {
					value = resultSet.getDouble(1);
				}
			} finally {
				if(resultSet != null)resultSet.close();
				if(callStatement != null) callStatement.close();
				if(connection != null)connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}
		if (value == null) {
			logger.warn("No value returned for sqldate:[{}] metric:[{}] station:[{}] channel:[{}]", sqlDateString,
					metricName, station, channel);
		}
		return value;
	}

	/**
	 * Insert metric result
	 *
	 * @param results
	 *            the metric result to insert
	 * @return 0 if successful
	 */
	public int insertMetricData(MetricResult results) {
		int result = -1;
		Connection connection = null;
		CallableStatement callStatement = null;
		try {
			try {
				connection = dataSource.getConnection();

				callStatement = connection
						.prepareCall("SELECT spInsertMetricData(?, ?, ?, ?, ?, ?, ?, ?)");

				for (String id : results.getIdSet()) {
					java.sql.Date date = new java.sql.Date(results.getDate().getTime().getTime());
					Channel channel = MetricResult.createChannel(id);

					callStatement.setDate(1, date, results.getDate());
					callStatement.setString(2, results.getMetricName());
					callStatement.setString(3, results.getStation().getNetwork());
					callStatement.setString(4, results.getStation().getStation());
					callStatement.setString(5, channel.getLocation());
					callStatement.setString(6, channel.getChannel());
					callStatement.setBytes(8, results.getDigest(id).array());

					if (results.getMetricName().equals("CalibrationMetric")) {
						callStatement.setString(7, id);
					} else {
						callStatement.setDouble(7, results.getResult(id));
					}

					callStatement.executeQuery();
				}
				result = 0;
			} finally {
				if(callStatement != null) callStatement.close();
				if(connection != null)connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}

		return result;
	}

	/**
	 * Reset any existing station scans that are taken.
	 * This prevents orphaned scans, if seedscan dies while running a scan.
	 * @throws SQLException 
	 */
	private void resetStationScans() throws SQLException {
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			connection = dataSource.getConnection();
			statement = connection.prepareStatement(
					"UPDATE tblscan SET taken=FALSE WHERE fkparentscan IS NULL AND finished = FALSE AND taken = TRUE");

			int orphanCount = statement.executeUpdate();
			logger.info("Reset {} orphaned scans", orphanCount);
		} finally {
			if(statement != null) statement.close();
			if(connection != null)connection.close();
		}
	}
	
	/**
	 * Gets the next priority scan from the database.
	 * The database handles its copy of the queue.
	 * Priority in the database queue may not exactly match priority in Seedscan.
	 * @return A Scan object to be added to the Priority Queue.
	 */
	public Scan takeNextScan(){
		Connection connection = null;
		CallableStatement callStatement = null;
		ResultSet rs = null;
		try {
			try {
				connection = dataSource.getConnection();
				callStatement = connection.prepareCall("SELECT * from fntakenextscan()");
				
				rs = callStatement.executeQuery();
				return new Scan(
						(java.util.UUID)rs.getObject("pkscanid"),
						(java.util.UUID)rs.getObject("fkparentscanid"),
						rs.getString("metricfilter"),
						rs.getString("networkfilter"),
						rs.getString("stationfilter"),
						rs.getString("locationfilter"),
						rs.getString("channelfilter"),
						rs.getObject("startdate", LocalDate.class),
						rs.getObject("enddate", LocalDate.class),
						rs.getBoolean("deleteexisting")
					);
			} finally {
				if(rs != null)rs.close();
				if(callStatement != null) callStatement.close();
				if(connection != null)connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}
		return null;
	}
	
	/**
	 * Mark a scan as finished.
	 * The database handles further work, such as completing parent scans and collapsing finished scans.
	 * @param pkScanID The UUID of the finished station scan.
	 */
	public void finishScan(UUID pkScanID){
		Connection connection = null;
		CallableStatement callStatement = null;
		try {
			try {
				connection = dataSource.getConnection();
				callStatement = connection.prepareCall("SELECT * from fnfinishscan(?)");
				callStatement.setObject(1, pkScanID);
				callStatement.executeQuery();
				
			} finally {
				if(callStatement != null) callStatement.close();
				if(connection != null)connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}
	}
	
	/**
	 * Insert a non scan specific error into the database logs.
	 * @param message
	 */
	public void insertError(String message) {
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			try {
				connection = dataSource.getConnection();
				//We will let the db set the timestamp.
				statement = connection.prepareStatement(
						"INSERT INTO tblerrorlog(errormessage) VALUES (?)");
				statement.setString(1, message);
				if(statement.executeUpdate() != 1){
					throw new SQLException("Failed to insert following error message into database:");
				}
			} finally {
				if (statement != null)
					statement.close();
				if (connection != null)
					connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
			logger.error("Error Message not inserted:\n"+message);
		}
	}
	
	public void insertScanMessage(UUID scanID, String network, String station, String location, String channel, String metric, String message){
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			try {
				connection = dataSource.getConnection();
				//We will let the db set the timestamp.
				statement = connection.prepareStatement(
						"INSERT INTO tblscanmessage(fkscanid, network, station, location, channel, metric, message)VALUES (?, ?, ?, ?, ?, ?, ?)");
				int i = 1;
				statement.setObject(i++, scanID);
				statement.setString(i++, network);
				statement.setString(i++, station);
				statement.setString(i++, location);
				statement.setString(i++, channel);
				statement.setString(i++, metric);
				statement.setString(i++, message);

				if(statement.executeUpdate() != 1){
					throw new SQLException("Failed to insert following scan message into database:");
				}
			} finally {
				if (statement != null)
					statement.close();
				if (connection != null)
					connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
			logger.error("Scan Message not inserted:\n"+message);
		}
	}

	/**
	 * Closes the connection pool and sets dataSource to null.
	 * This signals that it is no longer connected.
	 */
	public void close() {
		dataSource.close();
		dataSource = null;
	}
	
}
