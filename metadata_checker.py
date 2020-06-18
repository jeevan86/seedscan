import requests
from datetime import date, datetime, time, timedelta
from dateutil.parser import isoparse
import xml.etree.ElementTree as et
import psycopg2


def run_test():
    start = datetime.combine(date(year=2020, month=1, day=9), time(0))
    end = start + timedelta(days=1)
    run_scan(["IU"], start, end)


def get_connection_from_config_file():
    xml_data = et.parse('config.xml')
    namespace = {
        'cfg': 'config.seedscan.asl'
    }
    root = xml_data.getroot()
    database = root.find('cfg:database', namespace)
    url = database.find('cfg:uri', namespace)
    # get only the split components we need
    # the empty terms are protocol and empty string where '//' was
    (_, _, server, database) = url.text.split('/')
    # url is something like [domain].cr.usgs.gov:[port]
    # port, username, password should be gotten from .pgpass file
    # specified in a line like server:port:database:username:password
    # domain = server.partition('.')[0]
    return server, database


def run_scan(networks=None, start=None, end=None):
    # there are two child tags that describe the new metadata epoch (Channel)
    # and the description. While the description may be useful the channel
    # tag has the information that we need most -- which station, and what
    # the start and end times are -- which will be used to populate the scan
    # database
    station_map = {}  # map from stations to epochs w/ metadata changes
    if networks is None:
        networks = ['CU', 'GS', 'GT', 'IC', 'II', 'IU', 'IW', 'NE', 'US', 'N4']
    if start is not None and end is not None:
        # sanity check
        temp = max(start, end)
        start = min(start, end)
        end = temp
    elif end is None:
        # if only start is defined, set end as the end of the previous day
        end = datetime.combine(date.today() - timedelta(days=1), time(0))
        end = end.replace(microsecond=0)
    if start is None:
        # if start is not defined then define it as being a day before the end
        start = end - timedelta(hours=24)
    change_dict = {}
    for network in networks:
        url = "http://service.iris.edu/irisws/metadatachange/1/query?network=" \
              + network + "&startchange="
        url += datetime.isoformat(start) + "&endchange=" + datetime.isoformat(
            end)
        # print(url)
        r = requests.get(url)
        # r.text returns the XML-formatted text of the request
        # this gives us a list of all changes made in the given network
        if len(r.text) == 0:
            continue
        root = et.fromstring(r.text)
        r.close()
        # the format for the XML is that the root tag is <changes> which
        # lists every <change> the <change> tag has attribs "class", "detail"
        # and "changetime" which describe which metadata field changed,
        # which component of the metadata field (i.e., 'Value',
        # 'OutputUnits') changed, and when the change was made
        for child in root:
            for channel_node in child.findall('Channel'):
                station = channel_node.get('station')
                # create a new map from station epochs to channels with changes
                if station not in station_map.keys():
                    station_map[station] = {}
                epoch_change_map = station_map.get(station)
                # if any channel under a location changes then we'll submit
                # the whole location for re-processing
                location = channel_node.get('location')
                if len(location) == 0:
                    location = '--'
                start_time_string = channel_node.get('starttime')
                start_day = isoparse(start_time_string).date()
                end_time_string = channel_node.get('endtime')
                end_dt = isoparse(end_time_string)
                end_day = end_dt.date()
                if end_dt - datetime.combine(end_day, time(0)) > timedelta(0):
                    end_day + timedelta(days=1)
                # conversion to strings here makes for both easy hashing as
                # tuple and storing as DB components
                time_tuple = (date.isoformat(start_day),
                              date.isoformat(end_day))
                if time_tuple not in epoch_change_map.keys():
                    epoch_change_map[time_tuple] = set([])
                epoch_change_map[time_tuple].add(location)
                station_map[station] = epoch_change_map
        change_dict[network] = station_map

    """
    # this bit here is mostly for debugging/verification purposes and can be 
    # commented out as necessary
    for network in change_dict.keys():
        for station in change_dict[network]:
            station_map = change_dict[network]
            for epoch in station_map[station].keys():
                (start, end) = epoch
                for channel in station_map[station][epoch]:
                    print(network, station, start, end, channel)
    """
    # OK so now that we have the map of networks to stations the next thing
    # to do would be to populate the DB
    (host, dbname) = get_connection_from_config_file()
    dqa_db = psycopg2.connect(dbname=dbname, host=host)
    # now presumably we can iterate through our map structures and get each
    # table entry to add
    cursor = dqa_db.cursor()
    for network in change_dict.keys():
        for station in change_dict[network]:
            print("Inserting data for given station:", network, station)
            station_map = change_dict[network]
            for epoch in station_map[station].keys():
                (start, end) = epoch
                for location in station_map[station][epoch]:
                    cursor.execute("""INSERT INTO tblscan (networkfilter, 
                    stationfilter, locationfilter, startdate, enddate) VALUES 
                    (%(net)s, %(sta)s, %(loc)s, %(st)s, %(ed)s);""",
                                   {'net': network, 'sta': station,
                                    'loc': location, 'st': start, 'ed': end})
            dqa_db.commit()
    print("Database changes complete -- closing connection.")
    cursor.close()
    dqa_db.close()


if __name__ == '__main__':
    # TODO: parse in args from command line for custom scans?
    # if this is being run from the terminal it's almost certainly as a
    # cronjob where we run over the previous day's worth of metadata changes
    run_test()  # TODO: replace with run_scan call instead
