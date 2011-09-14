from Database import Database

import base64
import hashlib
import threading
try:
    import sqlite3 as sqlite
except ImportError, e:
    from pysqlite2 import dbapi2 as sqlite

class MetricDatabase(Database):
    def __init__(self, insert_cascade=True):
        Database.__init__(self)

  # === INSERT Queries ===
    def add_station(self, station):
        query = """INSERT OR IGNORE INTO Station(network,name) VALUES(?,?)"""
        self.insert(query, station.get_info())

    def add_sensor(self, sensor):
        query = """
            INSERT OR IGNORE INTO Sensor(station_id,location) 
            VALUES(
                (SELECT (Station.id) 
                 FROM Station 
                 WHERE Station.network = ? AND Station.name = ?),
                ?
            )
        """
        self.insert(query, station.get_info())

    def add_channel(self, channel):
        query = """
            INSERT OR IGNORE INTO Channel(sensor_id,name,derived)
            VALUES (
                (SELECT (Sensor.id)
                 FROM Station INNER JOIN Sensor
                 ON Station.id = Sensor.station_id
                 WHERE Station.network = ? AND
                       Station.name = ? AND
                       Sensor.location = ?),
                ?,
                ?
            )
        """
        args = list(channel.get_info())
        args.extend(channel.get_derived())
        args = tuple(args)
        self.insert(query, args)

    def add_metric(self, metric):
        query,data = metric.get_insert_query()
        self.insert(query, data)

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
                ON Station.id = Sensor.station.id
            %(where)s
            %(limit)s
        ) 
        """ % info
        return self.select(query, info['data'])


def StationInfo(object):
    def __init__(self, network, station):
        self._data = {}
        self.set_network(network)
        self.set_station(station)

    def _set_value(self, key, value):
        if value is not None:
            self._data[key] = value
        elif self._data.has_key(key):
            del self._data[key]

    def _get_value(self, key):
        if self._data.has_key(key):
            return self._data[key]
        return None

    def get_dict(self):
        return self._data

    def set_network(self, network):
        self._set_value('network', network)

    def set_station(self, station):
        self._set_value('station', station)

    def get_network(self):
        self._get_value('network')

    def get_station(self):
        self._get_value('station')

    def get_info(self):
        return (self.get_network(), self.get_station())

def SensorInfo(StationInfo):
    def __init__(self, network, station, location):
        StationInfo.__init__(network, station)
        self.set_location(location)
    
    def set_location(self, location):
        self._set_value('location', location)

    def get_location(self):
        self._get_value('location')

    def get_info(self):
        return (self.get_network(), self.get_station(), self.get_location())

def ChannelInfo(SensorInfo):
    def __init__(self, network, station, location, channel, derived=False):
        SensorInfo.__init__(network, station, location)
        self.set_channel(channel)
        self.derived = derived

    def set_channel(self, channel):
        self._set_value('channel', channel)

    def set_derived(self, derived):
        self._set_value('derived', derived)

    def get_channel(self):
        self._get_value('channel')

    def get_derived(self):
        self._get_value('derived')

    def get_info(self):
        return (self.get_network(), self.get_station(),
                self.get_location(), self.get_channel())


def Metric(object):
    def __init__(self, context):
        object.__init__(self)
        self.context = context

        year,month,day = map(int, time.strftime("%Y-%m-%d", time.gmtime()).split('-'))
        self.add_value("year", year)
        self.add_value("month", month)
        self.add_value("day", day)

        self.table = self.__class__.__name__
        self.required = []
        self.data = {}

    def set_year(self, year):
        self.add_value("year", year)

    def set_month(self, month):
        self.add_value("month", month)

    def set_day(self, day):
        self.add_value("day", day)

    def set_key(self, key):
        self.add_value("key", key)

    def set_value(self, value):
        self.add_value("value", value)

    def add_value(self, name, value):
        self.data[name] = value

    def get_insert_query(self):
        columns = sorted(self.data.keys())
        field_str = ",".join(columns)
        value_str = ",".join(islice(cycle("?"),len(columns)))
        #if self.context.__class__.__name__ == "StationInfo":
        query = """
            INSERT OR IGNORE INTO Metrics(channel_id,%s)
            VALUES (
                (SELECT (Channel.id) 
                 FROM Channel INNER JOIN Station 
                 ON Channel.station_id = Station.id
                 WHERE Station.network = ? AND Station.name = ? AND
                       Channel.location = ? AND Channel.name = ?),
                %s)
        """ % (field_str, value_str)
        return (query,self.get_args(columns))

    def get_args(self, columns=None):
        if columns is None:
            columns = sorted(self.data.keys())
        values = list(map(lambda f: self.data[f], columns))
        args = list(self.context.get_info())
        args.extend[values]
        return args

    def get_select_query(self, columns=None, year=None, month=None, day=None, limit=-1):
        args = list(self.context.get_info())

        field_str = "*"
        if columns is not None:
            field_str = ",".join(columns)

        year_str = ""
        if year is not None:
            year_str = "AND year = ?"
            args.append(year)

        month_str = ""
        if month is not None:
            month_str = "AND month = ?"
            args.append(month)

        day_str = ""
        if day is not None:
            day_str = "AND day = ?"
            args.append(day)

        limit_str = ""
        if limit > 1:
            limit_str = "LIMIT ?"
            args.append(limit)

        query = """
            SELECT %s 
            FROM Metrics
            WHERE channel_id IN (
                SELECT Channel.id 
                FROM Channel INNER JOIN Station
                ON Channel.station_id = Station.id
                WHERE Station.name = ? AND Station.network = ? AND
                      Channel.name = ? AND Channel.location = ?
            )
            %s
            %s
            %s
            %s
        """ % (field_str, year_str, month_str, day_str, limit_str)

        return query,args

