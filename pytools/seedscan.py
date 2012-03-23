#!/usr/bin/env python
import calendar
import os
import pprint
import re
import resource
import shutil
import sys
import time

import MetricDatabase
import Memory

reg_station = re.compile("^[A-Za-z0-9]{2}_[A-Za-z0-9]{2,5}$")

class DB(object):
    def __init__(self, file=None, insert_cascade=False):
        self.data = {}
        self.db = None
        if file is not None:
            self.db = MetricDatabase.MetricDatabase(file)
        self.db.init()

    def add_metric(self, network, station, location, channel, year, jday, category, key, value, channel_derived):
        if network == None:
            network = ""
        if location == None:
            location = ""
        populate(self.data, (network, station, location, (channel,channel_derived), year, jday, category, key, value))

    def add_calibration(self, network, station, location, channel, year, jday, cal_year, cal_jday, key, value, channel_derived):
        if network == None:
            network = ""
        if location == None:
            location = ""
        populate(self.data, (network, station, location, (channel,channel_derived), year, jday, "calibration", cal_year, cal_jday, key, value))

    def commit(self):
        start = time.time()
        print "Starting commit..."
        d_stations = []
        d_sensors  = []
        d_channels = []
        d_metrics  = []
        d_calibrations = []

        print "Re-formatting data...",
        sys.stdout.flush()
        ps = time.time()
        if self.db:
            for network,stations in sorted(self.data.items()):
                for station,locations in sorted(stations.items()):
                    d_stations.append((network,station))
                    for location,channels in sorted(locations.items()):
                        d_sensors.append((network,station,location))
                        for channel,(years,derived) in sorted(channels.items()):
                            d_channels.append((network,station,location,channel,derived))
                            for year,jdays in sorted(years.items()):
                                for jday,categories in sorted(jdays.items()):
                                    for category,data in sorted(categories.items()):
                                        if category == "calibration":
                                            for cyear,cjdays in sorted(data.items()):
                                                for cjday,cals in sorted(cjdays.items()):
                                                    for key,value in sorted(cals.items()):
                                                        y,m,d = j_to_md(year,jday)
                                                        dt = "%04d-%02d-%02d" % (y,m,d)
                                                        cy,cm,cd = j_to_md(cyear,cjday)
                                                        cdt = "%04d-%02d-%02d" % (cy,cm,cd)
                                                        d_calibrations.append((network,station,location,channel,y,m,d,dt,cy,cm,cd,cdt,key,value))
                                        else:
                                            for key,value in sorted(data.items()):
                                                y,m,d = j_to_md(year,jday)
                                                dt = "%04d-%02d-%02d" % (y,m,d)
                                                d_metrics.append((network,station,location,channel,y,m,d,dt,category,key,value))

        print "Done. Took %f seconds (%f seconds so far)" % (time.time() - ps, time.time() - start)

        print "Adding stations...",
        sys.stdout.flush()
        ps = time.time()
        self.db.add_stations(d_stations)
        print "Done. Took %f seconds (%f seconds so far)" % (time.time() - ps, time.time() - start)

        print "Adding sensors...",
        sys.stdout.flush()
        ps = time.time()
        self.db.add_sensors(d_sensors)
        print "Done. Took %f seconds (%f seconds so far)" % (time.time() - ps, time.time() - start)

        print "Adding channels...",
        sys.stdout.flush()
        ps = time.time()
        self.db.add_channels(d_channels)
        print "Done. Took %f seconds (%f seconds so far)" % (time.time() - ps, time.time() - start)

        print "Adding metrics...",
        sys.stdout.flush()
        ps = time.time()
        self.db.add_metrics(d_metrics)
        print "Done. Took %f seconds (%f seconds so far)" % (time.time() - ps, time.time() - start)

        print "Adding calibrations...",
        sys.stdout.flush()
        ps = time.time()
        self.db.add_calibrations(d_calibrations)
        print "Done. Took %f seconds (%f seconds so far)" % (time.time() - ps, time.time() - start)

        print "Commit complete. (took %f seconds)" % (time.time() - start,)

def j_to_md(year, jday):
    y,m,d,_,_,_,_,_,_ = time.strptime("%04d-%03d" % (year,jday), "%Y-%j")
    return (y,m,d)

def md_to_j(year, month, day):
    y,_,_,_,_,_,_,j,_ = time.strptime("%04d-%02d-%02d" % (year,month,day), "%Y-%m-%d")
    return (y,j)


def populate(db, chain):
    if len(chain) < 2:
        raise Exception("Invalid depth")
    extra = None
    key = chain[0]
    if type(key) == tuple:
        key,extra = key
    rest = chain[1:]
    if len(chain) > 2:
        if not db.has_key(key):
            if extra is not None:
                db[key] = ({},extra)
            else:
                db[key] = {}
        sub_db = db[key]
        if type(sub_db) == tuple:
            sub_db = sub_db[0]
        populate(sub_db, rest)
    else:
        db[key] = rest[0]



def gen_soh(path, database, parser, start=None, end=None, net=None, st=None):
    start_time = -1
    end_time = -1
    if start:
        if len(start) == 2:
            start_time = calendar.timegm(time.strptime("%04d-%03d 01:00:00" % start, "%Y-%j %H:%M:%S"))
        elif len(start) == 3:
            start_time = calendar.timegm(time.strptime("%04d-%02d-%2d 01:00:00" % start, "%Y-%m-%d %H:%M:%S"))
    if end:
        if len(end) == 2:
            end_time = calendar.timegm(time.strptime("%04d-%03d 22:00:00" % end, "%Y-%j %H:%M:%S"))
        elif len(end) == 3:
            end_time = calendar.timegm(time.strptime("%04d-%02d-%2d 22:00:00" % end, "%Y-%m-%d %H:%M:%S"))

    process_path(path, database, parser, start_time, end_time, net, st)

def process_path(path, database, parser, start_time=-1, end_time=-1, net=None, st=None):
    for station_dir in sorted(os.listdir(path)):
        if not reg_station.match(station_dir):
            continue
        network,station = station_dir.split("_")
        if net and (net != network):
            continue
        if st and (st != station):
            continue
        station_path = os.path.abspath("%s/%s" % (path, station_dir))

        print "checking directory '%s'" % station_path
        start = start_time
        end = end_time
        if end < 1:
            year,_,_,_,_,_,_,jday,_ = time.gmtime()
            start = calendar.timegm(time.strptime("%04d-%03d 22:00:00" % (year,jday), "%Y-%j %H:%M:%S"))
        if start < 1:
            year = min(map(int, os.listdir(station_path)))
            year_path = os.path.abspath("%s/%04d" % (station_path, year))
            jday = int(sorted(os.listdir(year_path))[0].split('_')[1])
            start = calendar.timegm(time.strptime("%04d-%03d 01:00:00" % (year,jday), "%Y-%j %H:%M:%S"))

        check_time = start
        while check_time < end:
            check_base = time.strftime("%s/%%Y/%%Y_%%j_%s_%s" % (station_path, network, station), time.gmtime(check_time))
            year,_,_,_,_,_,_,jday,_ = time.gmtime(check_time)
            check_time += 86400
            parser(database, network, station, year, jday, check_base)

def quality_matrix(database, network, station, year, jday, check_base):
    qm_file = check_base + "_qm.txt"
    if os.path.exists(qm_file):
        fh = open(qm_file, 'r')
        ready = False
        for line in fh:
            if line.strip().startswith("Channel"):
                ready = True
                continue
            if not ready:
                continue
            if line.strip() == "":
                continue
            if line.strip().startswith("Sensor"):
                continue
            if line.strip().startswith("---"):
                continue
            parts = map(lambda s: s.strip(), line.strip().split())
            if len(parts) != 7:
                continue

            _,avail,tq_ave,gaps,amp_sat,charge,time_err = parts
            location,channel = parts[0].split('/')

            if avail.lower() != 'n/a':
                database.add_metric(network, station, location, channel, year, jday, "SOH", "availability", avail, channel_derived=0)
            if tq_ave.lower() != 'n/a':
                database.add_metric(network, station, location, channel, year, jday, "SOH", "timing-quality", tq_ave, channel_derived=0)
            if gaps.lower() != 'n/a':
                database.add_metric(network, station, location, channel, year, jday, "SOH", "gap-count", gaps, channel_derived=0)
            if amp_sat.lower() != 'n/a':
                database.add_metric(network, station, location, channel, year, jday, "SOH", "amp-sat", amp_sat, channel_derived=0)
            if charge.lower() != 'n/a':
                database.add_metric(network, station, location, channel, year, jday, "SOH", "charge", charge, channel_derived=0)
            if time_err.lower() != 'n/a':
                database.add_metric(network, station, location, channel, year, jday, "SOH", "timing-error", time_err, channel_derived=0)
    else:
        print "Could not find file '%s'" % qm_file


def availability(database, network, station, year, jday, check_base):
    avail_file = check_base + "/data_avail.txt"
    if os.path.exists(avail_file):
        fh = open(avail_file, 'r')
        for line in fh:
            if line.strip().startswith("Data"):
                continue
            if line.strip().startswith("Loc"):
                continue

            location,channel,rate,gaps,revs,_,_,_,avail = line.split()
            #database.add_metric(network, station, location, channel, year, jday, "SOH", "availability", avail, channel_derived=0)
            #database.add_metric(network, station, location, channel, year, jday, "SOH", "gap-count", gaps, channel_derived=0)
            database.add_metric(network, station, location, channel, year, jday, "SOH", "sample-rate", rate, channel_derived=0)
            database.add_metric(network, station, location, channel, year, jday, "SOH", "reversals", revs, channel_derived=0)

def cals(path, database, start=None, end=None, net=None, st=None):
    if start and (len(start) == 3):
        year,_,_,_,_,_,_,jday,_ = time.strptime("%04d-%02d-%2d 12:00:00" % start, "%Y-%m-%d %H:%M:%S")
        start = year,jday
    if end and (len(end) == 3):
        year,_,_,_,_,_,_,jday,_ = time.strptime("%04d-%02d-%2d 12:00:00" % end, "%Y-%m-%d %H:%M:%S")
        end = year,jday

    regex = re.compile("\d{4}_\d{3}_\w{2}[.]csv")
    for name in sorted(os.listdir(path)):
        if not regex.match(name):
            print "bad file name '%s'" % name
            continue
        year,jday,_ = name[:-4].split('_')
        year,jday = map(int, (year,jday))

        check_date = tuple(map(int, (year,jday)))
        if check_date < start:
            print "bad start date"
            continue
        if check_date > end:
            print "bad end date"
            continue

        file = path + "/" + name
        print "Processing file '%s'" % file
        fh = open(file, 'r')
        first = True
        line_no = 0
        for line in fh:
            line_no += 1
            parts = map(lambda l: l.strip(), line.strip().strip(",").split(","))
            if first:
                keys = parts[7:]
                first = False
            else:
                network,station,location,channel,cal_year,cal_jday,sensor = parts[:7]
                if (cal_year == "NO") or (cal_jday == "CAL"):
                    print "No cal for", network, station, location, channel
                    continue
                if net and (net != network):
                    continue
                if st and (st != station):
                    continue

                cal_year,cal_jday = tuple(map(int,(cal_year,cal_jday)))
                values = parts[7:]
                for (key,value) in zip(keys,values):
                    if value.lower() == "nan":
                        continue
                    value = float(value)

                    #print network, station, location, channel, year, jday, "calibration", key, value
                    key = ('-').join(key.split()).lower()
                    database.add_calibration(network, station, location, channel, year, jday, cal_year, cal_jday, key, value, channel_derived=0)

def noise(path, database, start=None, end=None, net=None, st=None):
    if start and (len(start) == 3):
        year,_,_,_,_,_,_,jday,_ = time.strptime("%04d-%02d-%2d 12:00:00" % start, "%Y-%m-%d %H:%M:%S")
        start = year,jday
    if end and (len(end) == 3):
        year,_,_,_,_,_,_,jday,_ = time.strptime("%04d-%02d-%2d 12:00:00" % end, "%Y-%m-%d %H:%M:%S")
        end = year,jday

    regex = re.compile("\d{4}_\d{3}_\w{2}[.]csv")
    for name in sorted(os.listdir(path)):
        if not regex.match(name):
            continue
        year,jday,_ = name[:-4].split('_')
        year,jday = map(int,(year,jday))

        check_date = tuple(map(int, (year,jday)))
        if check_date < start:
            continue
        if check_date > end:
            continue

        file = path + "/" + name
        print "Processing file '%s'" % file
        fh = open(file, 'r')
        for line in fh:
            parts = map(lambda l: l.strip(), line.strip().strip(",").split(","))
            network,station,location,channel,category,key,value = parts
            if value.lower() == "nan":
                continue
            if net and (net != network):
                continue
            if st and (st != station):
                continue

            if category == "Pm":
                category = "noise"
            else:
                continue
                
            key = ('-').join(key.split()).lower()
            value = float(value)
            database.add_metric(network, station, location, channel, year, jday, category, key, value, channel_derived=1)

def sensor_compare(path, database, start=None, end=None, net=None, st=None):
    if start and (len(start) == 3):
        year,_,_,_,_,_,_,jday,_ = time.strptime("%04d-%02d-%2d 12:00:00" % start, "%Y-%m-%d %H:%M:%S")
        start = year,jday
    if end and (len(end) == 3):
        year,_,_,_,_,_,_,jday,_ = time.strptime("%04d-%02d-%2d 12:00:00" % end, "%Y-%m-%d %H:%M:%S")
        end = year,jday

    regex = re.compile("\d{4}_\d{3}_\w{2}[.]csv")
    for name in sorted(os.listdir(path)):
        if not regex.match(name):
            continue
        year,jday,_ = name[:-4].split('_')
        year,jday = map(int, (year,jday))
        check_date = tuple(map(int, (year,jday)))
        if check_date < start:
            continue
        if check_date > end:
            continue

        file = path + "/" + name
        print "Processing file '%s'" % file
        fh = open(file, 'r')
        for line in fh:
            parts = map(lambda l: l.strip(), line.strip().strip(",").split(","))
            network,station,location,channel,category,key,value = parts
            if value.lower() == "nan":
                continue
            if net and (net != network):
                continue
            if st and (st != station):
                continue
            if category == "Co":
                category = "coherence"
            elif category == "Pd":
                category = "power-difference"
            else:
                continue

            key = ('-').join(key.split()).lower()
            value = float(value)
            database.add_metric(network, station, location, channel, year, jday, category, key, value, channel_derived=1)

#   <network> : {
#       <station> : {
#           <location> : {
#               <channel> : {
#                   <year> : {
#                       <jday> : {
#                           <category> : {
#                               <key> : value
#                           }
#                       }
#                   }
#               }
#           }
#       }
#   }

def main():
    network = None
    station = None
    start = (2012,2,1)
    end   = (2012,2,29)
    #network = "IU"
    #station = "COR"
    #start = (2012,2,1)
    #end   = (2012,2,29)

    database = DB("/dataq/metrics/metrics.db")

    start_time = time.time()
    #gen_soh("/xs0/seed", database, availability, start=start, end=end, net=network, st=station)
    #gen_soh("/xs1/seed", database, availability, start=start, end=end, net=network, st=station)
    #gen_soh("/r02/projects/AVAIL/", database, quality_matrix, start=start, end=end, net=network, st=station)
    #sensor_compare("/qcwork/dqresults/sencomp", database, start=start, end=end, net=network, st=station)
    #noise("/qcwork/dqresults/noise", database, start=start, end=end, net=network, st=station)
    cals("/qcwork/dqresults/caldev", database, start=start, end=end, net=network, st=station)
    database.commit()
    end_time = time.time()

    #for network,stations in sorted(database.data.items()):
    #    for station,locations in sorted(stations.items()):
    #        for location,channels in sorted(locations.items()):
    #            for channel,(years,derived) in sorted(channels.items()):
    #                for year,jdays in sorted(years.items()):
    #                    for jday,categories in sorted(jdays.items()):
    #                        for category,data in sorted(categories.items()):
    #                            for key,value in sorted(data.items()):
    #                                derived_str = ""
    #                                if derived:
    #                                    derived_str = "*"
    #                                print "%s_%s %s-%s%s %s,%s %s: %s = %s" % (network, station, location, channel, derived_str, str(year), str(jday), category, key, str(value))
    
    time.sleep(1.0)
    mem = Memory.Memory()
    print "Memory:    ",; pprint.pprint(mem.memory())
    print "Resident:  ",; pprint.pprint(mem.resident())
    print
    print "Total Time:", end_time - start_time, "seconds"

if __name__ == '__main__':
    try:
        import psyco
        #psyco.full()
        psyco.profile()
        print "Psyco JIT enabled."
    except ImportError:
        pass
    try:
        main()
    except KeyboardInterrupt, e:
        print

