
package asl.seedscan.database;

import java.beans.PropertyVetoException;
import java.nio.ByteBuffer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.seedscan.config.DatabaseT;
import asl.seedscan.metrics.MetricResult;

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

	/** The connection. If null isconnected() returns false.*/
	private ComboPooledDataSource dataSource;

	/** The uri. */
	private String URI;

	/** The username. */
	private String username;

	/**
	 * Used for testing purposes only. Where java requires call to super() in
	 * mock Class.
	 */
	public MetricDatabase() {
	}

	/**
	 * Instantiates a new metric database based off the jaxb config.
	 *
	 * @param config
	 *            the config
	 * @throws SQLException
	 *             if the database is unable to be communicated with.
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
	 * @throws SQLException
	 *             if the database is unable to be communicated with.
	 */
	private MetricDatabase(String URI, String username, String password) throws SQLException {
		this.URI = URI;
		this.username = username;
		logger.info("MetricDatabase Constructor(): Attempting to connect to the database");

		logger.info("Connection String = \"{}\", User = \"{}\"", this.URI, this.username);
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
		// Reset any orphaned scans immiediately after connection.
		// This also serves as a check if we have write access to the database.
		resetStationScans();
	}

	/**
	 * Closes the connection pool and sets dataSource to null.
	 */
	public void close() {
		dataSource.close();
		dataSource = null;
	}

	/**
	 * Mark a scan as finished. The database handles further work, such as
	 * completing parent scans and collapsing finished scans.
	 * 
	 * @param pkScanID
	 *            The UUID of the finished station scan.
	 */
	public void finishScan(UUID pkScanID) {
		Connection connection = null;
		CallableStatement callStatement = null;
		try {
			try {
				connection = dataSource.getConnection();
				callStatement = connection.prepareCall("SELECT * from fnfinishscan(?)");
				callStatement.setObject(1, pkScanID);
				callStatement.executeQuery();

			} finally {
				if (callStatement != null)
					callStatement.close();
				if (connection != null)
					connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}
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
	public Double getMetricValue(LocalDate date, String metricName, Station station, Channel channel) {
		Double value = null;
		Connection connection = null;
		CallableStatement callStatement = null;
		ResultSet resultSet = null;
		try {
			try {
				connection = dataSource.getConnection();
				callStatement = connection.prepareCall("SELECT spGetMetricValue(?, ?, ?, ?, ?, ?)");
				callStatement.setObject(1, date);
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
				if (resultSet != null)
					resultSet.close();
				if (callStatement != null)
					callStatement.close();
				if (connection != null)
					connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}
		if (value == null) {
			logger.warn("No value returned for sqldate:[{}] metric:[{}] station:[{}] channel:[{}]", date,
					metricName, station, channel);
		}
		return value;
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
	public ByteBuffer getMetricValueDigest(LocalDate date, String metricName, Station station, Channel channel) {
		ByteBuffer digest = null;
		Connection connection = null;
		CallableStatement callStatement = null;
		ResultSet resultSet = null;
		try {
			try {
				connection = dataSource.getConnection();
				callStatement = connection.prepareCall("SELECT spGetMetricValueDigest(?, ?, ?, ?, ?, ?)");

				callStatement.setObject(1, date);
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
				if (resultSet != null)
					resultSet.close();
				if (callStatement != null)
					callStatement.close();
				if (connection != null)
					connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}

		return digest;
	}

	public void insertChildScan(UUID parentID, String network, String station, String location, String channel,
			String metric, LocalDate startDate, LocalDate endDate, int priority, boolean deleteExisting) {
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			try {
				//@formatter:off
				connection = dataSource.getConnection();
				//We will let the db set the timestamp.
				statement = connection.prepareStatement(
					"INSERT INTO tblscan("
						+ "fkparentscan, "
						+ "networkfilter, "
						+ "stationfilter, "
						+ "locationfilter, "
						+ "channelfilter, "
						+ "metricfilter, "
						+ "startdate, "
						+ "enddate, "
						+ "priority, "
						+ "deleteexisting)"
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
				int i = 1;
				statement.setObject( i++, parentID);
				statement.setString( i++, network);
				statement.setString( i++, station);
				statement.setString( i++, location);
				statement.setString( i++, channel);
				statement.setString( i++, metric);
				statement.setObject(   i++, startDate);
				statement.setObject(   i++, endDate);
				statement.setInt(    i++, priority);
				statement.setBoolean(i++, deleteExisting);
				//@formatter:on

				if (statement.executeUpdate() != 1) {
					throw new SQLException("Failed to insert following scan into database:");
				}
			} finally {
				if (statement != null)
					statement.close();
				if (connection != null)
					connection.close();
			}
		} catch (SQLException e) {
			this.insertScanMessage(parentID, network, station, location, channel, metric, "Unable to add child scan");
		}
	}

	/**
	 * Insert a non scan specific error into the database logs.
	 * 
	 * @param message error message
	 */
	public void insertError(String message) {
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			try {
				connection = dataSource.getConnection();
				// We will let the db set the timestamp.
				statement = connection.prepareStatement("INSERT INTO tblerrorlog(errormessage) VALUES (?)");
				statement.setString(1, message);
				if (statement.executeUpdate() != 1) {
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
			logger.error("Error Message not inserted:\n" + message);
		}
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

				callStatement = connection.prepareCall("SELECT spInsertMetricData(?, ?, ?, ?, ?, ?, ?, ?)");

				for (String id : results.getIdSet()) {
					Channel channel = MetricResult.createChannel(id);

					callStatement.setObject(1, results.getDate());
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
				if (callStatement != null)
					callStatement.close();
				if (connection != null)
					connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}

		return result;
	}

	/**
	 * Insert a message (usually an error message) into the database regarding a
	 * scan.
	 * 
	 * Only the scanID is checked for validity.
	 * NSLC identifiers aren't required to be valid, but misuse is not advisable. There are hard size restrictions.
	 * 
	 * @param scanID
	 *            - Should be the highest level scan. As child scans will be
	 *            cleaned. Setting this to the child scan will result in it
	 *            being deleted during the cleanup of finished child scans.
	 * @param network 2 character limit
	 * @param station 10 character limit
	 * @param location 10 character limit
	 * @param channel 10 character limit
	 * @param metric 50 character limit
	 * @param message No limit in size. A more detailed message or exception should go here.
	 */
	public void insertScanMessage(UUID scanID, String network, String station, String location, String channel,
			String metric, String message) {
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			try {
				connection = dataSource.getConnection();
				// We will let the db set the timestamp.
				statement = connection.prepareStatement("INSERT INTO tblscanmessage"
						+ "(fkscanid, network, station, location, channel, metric, message)"
						+ "VALUES (?, ?, ?, ?, ?, ?, ?)");
				int i = 1;
				statement.setObject(i++, scanID);
				statement.setString(i++, network);
				statement.setString(i++, station);
				statement.setString(i++, location);
				statement.setString(i++, channel);
				statement.setString(i++, metric);
				statement.setString(i++, message);

				if (statement.executeUpdate() != 1) {
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
			logger.error("Scan Message not inserted:\n" + message);
		}
	}

	/**
	 * Checks if is connected.
	 *
	 * @return true, if is connected
	 */
	public boolean isConnected() {
		return dataSource != null;
	}

	/**
	 * Reset any existing station scans that are taken. This prevents orphaned
	 * scans, if seedscan dies while running a scan.
	 * 
	 * @throws SQLException for any exception from the JDBC driver
	 */
	private void resetStationScans() throws SQLException {
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			connection = dataSource.getConnection();
			//@formatter:off
			statement = connection.prepareStatement(
					"UPDATE tblscan "
					+ "SET taken=FALSE "
					+ "WHERE "
						+ "fkparentscan IS NOT NULL "
						+ "AND finished = FALSE "
						+ "AND taken = TRUE");
			//@formatter:on
			int orphanCount = statement.executeUpdate();
			logger.info("Reset {} orphaned scans", orphanCount);
		} finally {
			if (statement != null)
				statement.close();
			if (connection != null)
				connection.close();
		}
	}

	/**
	 * Gets the next priority scan from the database. The database handles its
	 * copy of the queue. Priority in the database queue may not exactly match
	 * priority in Seedscan.
	 * 
	 * @return A Scan object to be added to the Priority Queue.
	 */
	public DatabaseScan takeNextScan() {
		Connection connection = null;
		CallableStatement callStatement = null;
		ResultSet rs = null;
		try {
			try {
				connection = dataSource.getConnection();
				callStatement = connection.prepareCall("SELECT * from fntakenextscan()");

				rs = callStatement.executeQuery();
				//If we have a scan return it
				if(rs.next()){
				//@formatter:off
					return new DatabaseScan(
						(java.util.UUID) rs.getObject("pkscanid"),
						(java.util.UUID) rs.getObject("fkparentscan"),
						rs.getString("metricfilter"),
						rs.getString("networkfilter"),
						rs.getString("stationfilter"),
						rs.getString("locationfilter"),
						rs.getString("channelfilter"),
						rs.getObject("startdate", LocalDate.class),
						rs.getObject("enddate", LocalDate.class),
						rs.getInt("priority"),
						rs.getBoolean("deleteexisting"));
				//@formatter:on
				}
				else{
					return null;
				}
			} finally {
				if (rs != null)
					rs.close();
				if (callStatement != null)
					callStatement.close();
				if (connection != null)
					connection.close();
			}
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}
		return null;
	}

}
