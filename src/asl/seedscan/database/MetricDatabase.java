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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.seedscan.config.DatabaseT;
import asl.seedscan.metrics.MetricResult;

public class MetricDatabase {
	public static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.database.MetricDatabase.class);

	private Connection connection;
	private String URI;
	private String username;
	private String password;

	// private CallableStatement callStatement;

	public MetricDatabase(DatabaseT config) {
		this(config.getUri(), config.getUsername(), config.getPassword()
				.getPlain());
	}

	public MetricDatabase(String URI, String username, String password) {
		this.URI = URI;
		this.username = username;
		this.password = password;

		logger.info("MetricDatabase Constructor(): Attempt to connect to the dbase");

		try {
			logger.info(String.format(
					"Connection String = \"%s\", User = \"%s\", Pass = \"%s\"",
					this.URI, this.username, this.password));

			connection = DriverManager.getConnection(URI, username, password);
		} catch (SQLException e) {
			// System.err.print(e);
			logger.error("Could not open station database.", e);
			// MTH: For now let's continue
			// throw new RuntimeException("Could not open station database.");
		}
	}

	public boolean isConnected() {
		Connection foo = getConnection();

		if (foo == null)
			return false;

		return true;
	}

	public Connection getConnection() {
		return connection;
	}

	public ByteBuffer getMetricDigest(Calendar date, String metricName,
			Station station) {
		ByteBuffer digest = null;

		try {
			CallableStatement callStatement = connection
					.prepareCall("SELECT spGetMetricDigest(?, ?, ?, ?)");
			// callStatement =
			// connection.prepareCall("SELECT spGetMetricDigest(?, ?, ?, ?)");

			java.sql.Date sqlDate = new java.sql.Date(date.getTime().getTime());
			callStatement.setDate(1, sqlDate, date);
			callStatement.setString(2, metricName);
			callStatement.setString(3, station.getNetwork());
			callStatement.setString(4, station.getStation());
			ResultSet resultSet = callStatement.executeQuery();

			if (resultSet.next())
				digest = ByteBuffer.wrap(resultSet.getBytes(1));
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}

		return digest;
	}

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

	public String selectAll(String startDate, String endDate) {
		String result = "";
		try {
			CallableStatement callStatement = connection
					.prepareCall("CALL spGetAll(?, ?, ?)");
			callStatement.setString(1, startDate);
			callStatement.setString(2, endDate);
			callStatement.registerOutParameter(3, java.sql.Types.VARCHAR);
			callStatement.executeQuery();
			result = callStatement.getString(3);
		} catch (SQLException e) {
			logger.error("SQLException:", e);
		}
		return result;
	}

}
