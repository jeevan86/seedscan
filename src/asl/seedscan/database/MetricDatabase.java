
package asl.seedscan.database;

import java.nio.ByteBuffer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.seedscan.config.DatabaseT;
import asl.seedscan.metrics.MetricResult;

/**
 * The Class MetricDatabase.
 * This contains methods for inserting and retrieving data from the database.
 * 
 * @author James Holland - USGS
 * @author Joel Edwards - USGS
 */
public class MetricDatabase {
	
	/** The Constant logger. */
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.database.MetricDatabase.class);

	/** The connection. */
	private Connection connection;
	
	/** The uri. */
	private String URI;
	
	/** The username. */
	private String username;
	
	/** The password. */
	private String password;

	// private CallableStatement callStatement;

	/**
	 * Instantiates a new metric database based off the jaxb config.
	 *
	 * @param config the config
	 */
	public MetricDatabase(DatabaseT config) {
		this(config.getUri(), config.getUsername(), config.getPassword()
				.getPlain());
	}

	/**
	 * Instantiates a new metric database.
	 *
	 * @param URI the location of the database
	 * @param username the username
	 * @param password the password
	 */
	public MetricDatabase(String URI, String username, String password) {
		this.URI = URI;
		this.username = username;
		this.password = password;

		logger.info("MetricDatabase Constructor(): Attempting to connect to the database");

		try {
			logger.info(String.format(
					"Connection String = \"%s\", User = \"%s\", Pass = \"%s\"",
					this.URI, this.username, this.password));

			connection = DriverManager.getConnection(URI, username, password);
		} catch (SQLException e) {
			logger.error("Could not open station database.", e);
			// MTH: For now let's continue
			// throw new RuntimeException("Could not open station database.");
		}
	}

	/**
	 * Checks if is connected.
	 *
	 * @return true, if is connected
	 */
	public boolean isConnected() {
		Connection foo = getConnection();

		if (foo == null)
			return false;

		return true;
	}

	/**
	 * Gets the connection.
	 *
	 * @return the connection
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * Gets the metric value digest for a particular channel, metric, day.
	 *
	 * @param date the date
	 * @param metricName the metric name
	 * @param station the network and station information
	 * @param channel the channel and location information
	 * @return the metric value digest
	 */
	public ByteBuffer getMetricValueDigest(Calendar date, String metricName,
			Station station, Channel channel) {
		ByteBuffer digest = null;

		try {
			CallableStatement callStatement = connection
					.prepareCall("SELECT spGetMetricValueDigest(?, ?, ?, ?, ?, ?)");

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

				if (digestIn != null)
					digest = ByteBuffer.wrap(digestIn);
			}
		} catch (SQLException e) {
			// System.out.print(e);
			logger.error("SQLException:", e);
		}

		return digest;
	}

	/**
	 * Gets the metric value for a particular channel, metric, day.
	 *
	 * @param date the date
	 * @param metricName the metric name
	 * @param station the network and station information
	 * @param channel the channel and location information
	 * @return the metric value
	 */
	public Double getMetricValue(Calendar date, String metricName,
			Station station, Channel channel) {
		Double value = null;
		String sqlDateString = null;
		try {
			CallableStatement callStatement = connection
					.prepareCall("SELECT spGetMetricValue(?, ?, ?, ?, ?, ?)");
			java.sql.Date sqlDate = new java.sql.Date(date.getTime().getTime());
			sqlDateString = sqlDate.toString();
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
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}
		if (value == null) {
			logger.warn(
					"No value returned for sqldate:[{}] metric:[{}] station:[{}] channel:[{}]",
					sqlDateString, metricName, station, channel);
		}
		return value;
	}

	/**
	 * Insert metric result
	 *
	 * @param results the metric result to insert
	 * @return 0 if successful
	 */
	public int insertMetricData(MetricResult results) {
		int result = -1;

		synchronized (connection) {
			try {
				connection.setAutoCommit(false);

				CallableStatement callStatement = connection
						.prepareCall("SELECT spInsertMetricData(?, ?, ?, ?, ?, ?, ?, ?)");

				for (String id : results.getIdSet()) {
					java.sql.Date date = new java.sql.Date(results.getDate()
							.getTime().getTime());
					Channel channel = MetricResult.createChannel(id);

					callStatement.setDate(1, date, results.getDate());
					callStatement.setString(2, results.getMetricName());
					callStatement.setString(3, results.getStation()
							.getNetwork());
					callStatement.setString(4, results.getStation()
							.getStation());
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

				connection.commit();
				result = 0;
			} catch (SQLException e) {
				logger.error("SQLException:", e);
			}
		}

		return result;
	}
}
