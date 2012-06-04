from Database import Database

import base64
import threading
#try:
import _mysql as mysql
#except ImportError, e:
    

inserts = {
        "Station" : """
            INSERT IGNORE INTO metrics.Station(network,name) VALUES(%s,%s)
        """,
        "Sensor" : """
            INSERT IGNORE INTO metrics.Sensor(station_id,location) 
            VALUES(
                (SELECT (Station.id) 
                 FROM metrics.Station 
                 WHERE Station.network = %s AND 
                       Station.name = %s),
                %s
            )
        """,
        "Channel" : """
            INSERT IGNORE INTO metrics.Channel(sensor_id,name,derived)
            VALUES (
                (SELECT (Sensor.id)
                 FROM metrics.Sensor INNER JOIN Station
                    ON Station.id = Sensor.station_id
                 WHERE Station.network = %s AND
                       Station.name = %s AND
                       Sensor.location = %s),
                %s,
                %s
            )
        """,
        "Metrics" : """
            INSERT IGNORE INTO metrics.Metrics(channel_id,year,month,day,date,category,`key`,value)
            VALUES (
                (SELECT (Channel.id) 
                 FROM metrics.Channel 
                 INNER JOIN metrics.Sensor 
                    ON Sensor.id = Channel.sensor_id
                 INNER JOIN metrics.Station
                    ON Station.id = Sensor.station_id
                 WHERE Station.network = %s AND 
                       Station.name = %s AND
                       Sensor.location = %s AND 
                       Channel.name = %s),
                %s,
                %s,
                %s,
                metrics.fnJulianDay(%s),
                %s,
                %s,
                %s)
        """,
        "Calibrations" : """
            INSERT IGNORE INTO metrics.Calibrations(channel_id,year,month,day,date,cal_year,cal_month,cal_day,cal_date,`key`,value)
            VALUES (
                (SELECT (Channel.id) 
                 FROM metrics.Channel 
                 INNER JOIN metrics.Sensor 
                    ON Sensor.id = Channel.sensor_id
                 INNER JOIN metrics.Station
                    ON Station.id = Sensor.station_id
                 WHERE Station.network = %s AND 
                       Station.name = %s AND
                       Sensor.location = %s AND 
                       Channel.name = %s),
                %s,
                %s,
                %s,
                metrics.fnJulianDay(%s),
                %s,
                %s,
                %s,
                metrics.fnJulianDay(%s),
                %s,
                %s)
        """,
        "Metadata" : """
            REPLACE INTO metrics.Metadata(channel_id,epoch,sensor_info,raw_metadata)
            VALUES (
                (SELECT (Channel.id) 
                 FROM metrics.Channel 
                 INNER JOIN metrics.Sensor 
                    ON Sensor.id = Channel.sensor_id
                 INNER JOIN metrics.Station
                    ON Station.id = Sensor.station_id
                 WHERE Station.network = %s AND 
                       Station.name = %s AND
                       Sensor.location = %s AND 
                       Channel.name = %s),
                %s,
                %s,
                %s)
        """,
}

class MetricDatabase(Database):
    def __init__(self, conString=None):
        Database.__init__(self, conString)

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

    def add_metric(self, network, station, location, channel, year, month, day, date, category, key, value):
        self.insert(inserts["Metrics"], (network,station,location,channel,year,month,day,date,category,key,value))

    def add_metrics(self, iterator):
        self.insert_many(inserts["Metrics"], iterator)

    def add_calibration(self, network, station, location, channel, year, month, day, date, cal_year, cal_month, cal_day, cal_date, key, value):
        self.insert(inserts["Calibrations"], (network,station,location,channel,year,month,day,date,cal_year,cal_month,cal_day,cal_date,key_value))

    def add_calibrations(self, iterator):
        self.insert_many(inserts["Calibrations"], iterator)

    def add_metadata(self, network, station, location, channel, epoch, info, raw):
        self.insert(inserts["Metadata"], (network,station,location,channel,epoch,info,raw))

    def add_metadatas(self, iterator):
        self.insert_many(inserts["Metadata"], iterator)


  # === SELECT Queries ===
    def assemble_where(self, parts):
        results = []
        result = ""
        data = []
        for column,value in parts:
            if value is not None:
                results.append(" %s = %s" % column)
                data.append(value)
        if len(results):
            result = "WHERE" + " AND".join(results)
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
        INNER JOIN Station
            ON Station.id = Sensor.station.id
        %s
        %s
        """ % (column_str, where_str, limit_str)
        return self.select(query, where_data)

    def get_channels(self, columns=None, network=None, station=None, location=None, channel=None, limit=-1):
        info = {}
        info['data'] = None
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
        INNER JOIN Sensor
            ON Sensor.id = Channel.sensor_id
        INNER JOIN Station
            ON Station.id = Sensor.station_id
        %(where)s
        %(limit)s
        """ % info
        return self.select(query, info['data'])

    def get_metrics(self, columns=None, network=None, station=None, location=None, channel=None, year=None, month=None, day=None, category=None, limit=-1):
        info = {}
        info['data'] = None
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
                                                          ("Metrics.day",       day),
                                                          ("Metrics.category",  category)])

        query = """
        SELECT %(columns)s
        FROM Metrics
        INNER JOIN Channel
            ON Channel.id = Metrics.channel_id
        INNER JOIN Sensor
            ON Sensor.id = Channel.sensor_id
        INNER JOIN Station
            ON Station.id = Sensor.station_id
        %(where)s
        %(limit)s
        """ % info
        print query
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
            where += "Metadata.epoch >= %s"
            data.append(start)

        if end is not None:
            if where.startswith("WHERE"):
                where += " AND "
            else:
                where = "WHERE "
            where += "Metadata.epoch <= %s"
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
    month INTEGER NOT NULL,
    day INTEGER NOT NULL,
    date REAL NOT NULL,
    category TEXT,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    UNIQUE (channel_id, year, month, day, category, key)
);

CREATE TABLE IF NOT EXISTS Calibrations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id INTEGER NOT NULL REFERENCES Channel (id) ON DELETE CASCADE,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    day INTEGER NOT NULL,
    date REAL NOT NULL,
    cal_year INTEGER NOT NULL,
    cal_month INTEGER NOT NULL,
    cal_day INTEGER NOT NULL,
    cal_date REAL NOT NULL,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    UNIQUE (channel_id, year, month, day, cal_year, cal_month, cal_day, key)
);
        """
       # self.cur.executescript(script) #broken

