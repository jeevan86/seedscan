from Database import Database

import base64
import hashlib
import threading
try:
    import sqlite3 as sqlite
except ImportError, e:
    from pysqlite2 import dbapi2 as sqlite

inserts = {
        "Station" : """
            INSERT OR IGNORE INTO Station(network,name) VALUES(?,?)
        """,
        "Sensor" : """
            INSERT OR IGNORE INTO Sensor(station_id,location) 
            VALUES(
                (SELECT (Station.id) 
                 FROM Station 
                 WHERE Station.network = ? AND 
                       Station.name = ?),
                ?
            )
        """,
        "Channel" : """
            INSERT OR IGNORE INTO Channel(sensor_id,name,derived)
            VALUES (
                (SELECT (Sensor.id)
                 FROM Sensor INNER JOIN Station
                    ON Station.id = Sensor.station_id
                 WHERE Station.network = ? AND
                       Station.name = ? AND
                       Sensor.location = ?),
                ?,
                ?
            )
        """,
        "Metrics" : """
            INSERT OR REPLACE INTO Metrics(channel_id,year,month,day,key,value)
            VALUES (
                (SELECT (Channel.id) 
                 FROM Channel 
                 INNER JOIN Sensor 
                    ON Sensor.id = Channel.sensor_id
                 INNER JOIN Station
                    ON Station.id = Sensor.station_id
                 WHERE Station.network = ? AND 
                       Station.name = ? AND
                       Sensor.location = ? AND 
                       Channel.name = ?),
                ?,
                ?,
                ?,
                ?,
                ?)
        """,
        "Metadata" : """
            INSERT OR REPLACE INTO Metadata(channel_id,epoch,sensor_info,raw_metadata)
            VALUES (
                (SELECT (Channel.id) 
                 FROM Channel 
                 INNER JOIN Sensor 
                    ON Sensor.id = Channel.sensor_id
                 INNER JOIN Station
                    ON Station.id = Sensor.station_id
                 WHERE Station.network = ? AND 
                       Station.name = ? AND
                       Sensor.location = ? AND 
                       Channel.name = ?),
                ?,
                ?,
                ?)
        """,
}

class MetricDatabase(Database):
    def __init__(self, file=None):
        Database.__init__(self, file)

  # === INSERT Queries ===
    def add_station(self, network, station):
        self.insert(inserts["Station"], (network,station))

    def add_stations(self, iterator):
        self.insert_many(inserts["Station"], iterator)

    def add_sensor(self, network, station, location):
        self.insert(inserts["Sensor"], (network,station,location))

    def add_sensors(self, iterator):
        self.insert_many(inserts["Sensor"], iterator)

    def add_channel(self, network, station, location, channel, derived=0):
        self.insert(inserts["Channel"], (network, station, location, channel, derived))

    def add_channels(self, iterator):
        self.insert_many(inserts["Channel"], iterator)

    def add_metric(self, network, station, location, channel, year, month, day, key, value):
        self.insert(inserts["Metrics"], (network,station,location,channel,year,month,day,key,value))

    def add_metrics(self, iterator):
        self.insert_many(inserts["Metrics"], iterator)

    def add_metadata(self, network, station, location, channel, epoch, info, raw):
        self.insert(inserts["Metadata"], (network,station,location,channel,epoch,info,raw))

    def add_metadatas(self, iterator):
        self.insert_many(inserts["Metadata"], iterator)


  # === SELECT Queries ===
    def assemble_where(self, parts):
        result = ""
        data = []
        for column,value in parts:
            if value is not None:
                result += " %s = ?" % column
                data.append(value)
        if len(result):
            result = "WHERE" + result
        return result,data
            
    def assemble_limit(self, limit):
        result = ""
        if limit >= 0:
            result = "LIMIT %d" % limit
        return result

    def get_stations(self, columns=None, network=None, station=None, limit=-1):
        column_str = "*"
        if columns is not None:
            column_str = ",".join(columns)

        limit_str = self.assemble_limit(limit)
        where_str,where_data = self.assemble_where([("Station.network", network),
                                                    ("Station.name",    station)])

        query = """
        SELECT %s
        FROM Station
        %s
        %s
        """ % (column_str, where_str, limit_str)
        return self.select(query, where_data)


    def get_sensors(self, columns=None, network=None, station=None, location=None, limit=-1):
        column_str = "*"
        if columns is not None:
            column_str = ",".join(columns)

        limit_str = self.assemble_limit(limit)
        where_str,where_data = self.assemble_where([("Station.network", network),
                                                    ("Station.name",    station),
                                                    ("Sensor.location", location)])

        query = """
        SELECT %s
        FROM Sensor
        WHERE Sensor.id IN (
            SELECT (Sensor.id) 
            FROM Sensor 
            INNER JOIN Station
                ON Station.id = Sensor.station.id
            %s
            %s
        ) 
        """ % (column_str, where_str, limit_str)
        return self.select(query, where_data)

    def get_channels(self, columns=None, network=None, station=None, location=None, channel=None, limit=-1):
        info = {}
        info['columns'] = "*"
        if columns is not None:
            info['columns'] = ",".join(columns)

        info['limit'] = self.assemble_limit(limit)
        info['where'],info['data'] = self.assemble_where([("Station.network", network),
                                                          ("Station.name",    station),
                                                          ("Sensor.location", location),
                                                          ("Channel.name" % info, channel)])

        query = """
        SELECT %(columns)s
        FROM Channel
        WHERE Channel.id IN (
            SELECT (Channel.id) 
            FROM Channel 
            INNER JOIN Sensor
                ON Sensor.id = Channel.sensor_id
            INNER JOIN Station
                ON Station.id = Sensor.station_id
            %(where)s
            %(limit)s
        ) 
        """ % info
        return self.select(query, info['data'])

    def get_metrics(self, columns=None, network=None, station=None, location=None, channel=None, year=None, month=None, day=None, limit=-1):
        info = {}
        info['columns'] = "*"
        if columns is not None:
            info['columns'] = ",".join(columns)

        info['limit'] = self.assemble_limit(limit)
        info['where'],info['data'] = self.assemble_where([("Station.network",   network),
                                                          ("Station.name",      station),
                                                          ("Sensor.location",   location),
                                                          ("Channel.name",      channel),
                                                          ("Metrics.year",      year),
                                                          ("Metrics.month",     month),
                                                          ("Metrics.day",       day)])

        query = """
        SELECT %(columns)s
        FROM Metrics
        WHERE Metrics.id IN (
            SELECT (Metrics.id
            FROM Metrics
            INNER JOIN Channel
                ON Channel.id = Metrics.channel_id
            INNER JOIN Sensor
                ON Sensor.id = Channel.sensor_id
            INNER JOIN Station
                ON Station.id = Sensor.station_id
            %(where)s
            %(limit)s
        ) 
        """ % info
        return self.select(query, info['data'])

    def get_metadata(self, columns=None, network=None, station=None, location=None, channel=None, start=None, end=None, limit=-1):
        info = {}
        info['columns'] = "*"
        if columns is not None:
            info['columns'] = ",".join(columns)

        info['limit'] = self.assemble_limit(limit)
        where,data = self.assemble_where([("Station.network",   network),
                                          ("Station.name",      station),
                                          ("Sensor.location",   location),
                                          ("Channel.name",      channel),
                                          ("Metadata.year",     year),
                                          ("Metadata.month",    month),
                                          ("Metadata.day",      day)])

        if start is not None:
            if where.startswith("WHERE"):
                where += " AND "
            else:
                where = "WHERE "
            where += "Metadata.epoch >= ?"
            data.append(start)

        if end is not None:
            if where.startswith("WHERE"):
                where += " AND "
            else:
                where = "WHERE "
            where += "Metadata.epoch <= ?"
            data.append(end)

        info['where'] = where
        info['data']  = data
        query = """
        SELECT %(columns)s
        FROM Metrics
        WHERE Metrics.id IN (
            SELECT (Metrics.id
            FROM Metrics
            INNER JOIN Channel
                ON Channel.id = Metrics.channel_id
            INNER JOIN Sensor
                ON Sensor.id = Channel.sensor_id
            INNER JOIN Station
                ON Station.id = Sensor.station.id
            %(where)s
            %(limit)s
        ) 
        """ % info
        return self.select(query, info['data'])

    def init(self):
        script = """
CREATE TABLE IF NOT EXISTS Station (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    network TEXT,
    name TEXT NOT NULL,
    UNIQUE (network, name)
);

CREATE TABLE IF NOT EXISTS Sensor (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    station_id INTEGER NOT NULL REFERENCES Station (id) ON DELETE CASCADE,
    location TEXT NOT NULL,
    UNIQUE (station_id, location)
);

CREATE TABLE IF NOT EXISTS Channel (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sensor_id INTEGER NOT NULL REFERENCES Sensor (id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    derived INTEGER NOT NULL,
    UNIQUE (sensor_id, name)
);

CREATE TABLE IF NOT EXISTS Metadata (
    channel_id INTEGER NOT NULL REFERENCES Channel (id) ON DELETE CASCADE,
    epoch DATETIME NOT NULL,
    sensor_info TEXT,
    raw_metadata BLOB,
    UNIQUE (channel_id, epoch)
);

CREATE TABLE IF NOT EXISTS Metrics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id INTEGER NOT NULL REFERENCES Channel (id) ON DELETE CASCADE,
    year INTEGER NOT NULL,
    month INTEGER,
    day INTEGER,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    UNIQUE (channel_id, year, month, day, key)
);
        """
        self.cur.executescript(script)

