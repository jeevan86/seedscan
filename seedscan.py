#!/usr/bin/env python
import calendar
import os
import re
import resource
import shutil
import sys
import time

import MetricDatabase

## {{{ http://code.activestate.com/recipes/286222/ (r1)

_proc_status = '/proc/%d/status' % os.getpid()
_scale = {
    'kB': 1024.0,
    'KB': 1024.0,
    'mB': 1024.0*1024.0,
    'MB': 1024.0*1024.0,
}

def _VmB(VmKey):
    global _proc_status, _scale
    try:
        t = open(_proc_status)
        v = t.read()
        t.close()
    except:
        return 0.0
    i = v.index(VmKey)
    v = v[i:].split(None, 3)  # whitespace
    if len(v) < 3:
        return 0.0
    return float(v[1]) * _scale[v[2]]

def memory(since=0.0):
    return _VmB('VmSize:') - since

def resident(since=0.0):
    return _VmB('VmRSS:') - since

def stacksize(since=0.0):
    return _VmB('VmStk:') - since
## end of http://code.activestate.com/recipes/286222/ }}}


reg_station = re.compile("^[A-Za-z0-9]{2}_[A-Za-z0-9]{2,5}$")

class DB(object):
    def __init__(self, file=None, insert_cascade=False):
        self.data = {}
        self.db = None
        if file is not None:
            self.db = MetricDatabase.MetricDatabase(file)
            self.db.init()

    def add(self, network, station, location, channel, year, jday, category, key, value, channel_derived):
        if network == None:
            network = ""
        if location == None:
            network = ""
        populate(self.data, (network, station, location, (channel,channel_derived), year, jday, category, key, value))

    def commit(self):
        start = time.time()
        print "Starting commit..."
        stations = []
        sensors  = []
        channels = []
        metrics  = []

        print "Re-formatting data...",
        sys.stdout.flush()
        ps = time.time()
        if self.db:
            for n,ss in sorted(self.data.items()):
                for s,ls in sorted(ss.items()):
                    stations.append((n,s))
                    for l,cs in sorted(ls.items()):
                        sensors.append((n,s,l))
                        for c,(ys,dr) in sorted(cs.items()):
                            channels.append((n,s,l,c,dr))
                            for y,js in sorted(ys.items()):
                                for j,cts in sorted(js.items()):
                                    for ct,d in sorted(cts.items()):
                                        for k,v in sorted(d.items()):
                                            y,m,d = j_to_md(y,j)
                                            metrics.append((n,s,l,c,y,m,d,ct+'-'+k,v))

        print "Done. Took %f seconds (%f seconds so far)" % (time.time() - ps, time.time() - start)

        print "Adding stations...",
        sys.stdout.flush()
        ps = time.time()
        self.db.add_stations(stations)
        print "Done. Took %f seconds (%f seconds so far)" % (time.time() - ps, time.time() - start)

        print "Adding sensors...",
        sys.stdout.flush()
        ps = time.time()
        self.db.add_sensors(sensors)
        print "Done. Took %f seconds (%f seconds so far)" % (time.time() - ps, time.time() - start)

        print "Adding channels...",
        sys.stdout.flush()
        ps = time.time()
        self.db.add_channels(channels)
        print "Done. Took %f seconds (%f seconds so far)" % (time.time() - ps, time.time() - start)

        print "Adding metrics...",
        sys.stdout.flush()
        ps = time.time()
        self.db.add_metrics(metrics)
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


def availability(path, database, start=None, end=None, net=None, st=None):
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

    process_path(path, database, start_time, end_time, net, st)

def process_path(path, database, start_time=-1, end_time=-1, net=None, st=None):
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
            check_dir = time.strftime("%s/%%Y/%%Y_%%j_%s_%s" % (station_path, network, station), time.gmtime(check_time))
            year,_,_,_,_,_,_,jday,_ = time.gmtime(check_time)
            check_time += 86400
            #print "checking directory '%s'" % check_dir
            avail_file = check_dir + "/data_avail.txt"
            if os.path.exists(avail_file):
                fh = open(avail_file, 'r')
                start = 2
                for line in fh:
                    if start > 0:
                        start -= 1
                        continue

                    location,channel,rate,gaps,revs,_,_,_,avail = line.split()
                    database.add(network, station, location, channel, year, jday, "SOH", "sample-rate", rate, channel_derived=0)
                    database.add(network, station, location, channel, year, jday, "SOH", "gap-count", gaps, channel_derived=0)
                    database.add(network, station, location, channel, year, jday, "SOH", "reversals", revs, channel_derived=0)
                    database.add(network, station, location, channel, year, jday, "SOH", "availability", avail, channel_derived=0)

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
        year,jday,network = name[:-4].split('_')
        year,jday = map(int,(year,jday))
        if net and (net != network):
            continue

        check_date = tuple(map(int, (year,jday)))
        if check_date < start:
            continue
        if check_date > end:
            continue

        file = path + "/" + name
        print "Processing file '%s'" % file
        fh = open(file, 'r')
        first = True
        for line in fh:
            parts = map(lambda l: l.strip(), line.strip().strip(",").split(","))
            if first:
                keys = parts[1:]
                first = False
            else:
                station = parts[0]
                if st and (st != station):
                    continue

                values = parts[1:]

                for (key,value) in zip(keys,values):
                    if value.lower() == "nan":
                        continue
                    value = float(value)
                    chan_info,range = map(lambda p: p.strip(), key.split(':'))
                    range = "-".join(range.split())
                    cat,channel,location = chan_info.split()
                    if cat == "Pm":
                        category = "noise"
                    else:
                        continue
                    
                    database.add(network, station, location, channel, year, jday, category, range, value, channel_derived=1)

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
        year,jday,network = name[:-4].split('_')
        year,jday = map(int, (year,jday))
        if net and (net != network):
            continue

        check_date = tuple(map(int, (year,jday)))
        if check_date < start:
            continue
        if check_date > end:
            continue

        file = path + "/" + name
        print "Processing file '%s'" % file
        fh = open(file, 'r')
        first = True
        for line in fh:
            parts = map(lambda l: l.strip(), line.strip().strip(",").split(","))
            if first:
                keys = parts[1:]
                first = False
            else:
                station = parts[0]
                if st and (st != station):
                    continue

                values = parts[1:]

                for (key,value) in zip(keys,values):
                    if value.lower() == "nan":
                        continue
                    value = float(value)
                    chan_info,range = map(lambda p: p.strip(), key.split(':'))
                    range = "-".join(range.split())
                    cat,channel = chan_info.split()
                    #cat,channel,sens = chan_info.split()
                    #loc_a,loc_b = sens.split('-')
                    #chan_a = "%s-%s" % (loc_a,chan)
                    #chan_b = "%s-%s" % (loc_b,chan)
                    #a_value = value
                    #b_value = value
                    if cat == "Co":
                        category = "coherence"
                    elif cat == "Pd":
                        category = "power-difference"
                        #b_value = -value
                    else:
                        continue

                    database.add(network, station, "", channel, year, jday, category, range, value, channel_derived=1)
                    #database.add(network, station, loc_a, channel, year, jday, category, range, (chan_b, a_value))
                    #database.add(network, station, loc_b, channel, year, jday, category, range, (chan_a, b_value))


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
    start = (2011,8,1)
    end   = (2011,8,31)
    #network = "IU"
    #station = "ANMO"
    #start = (2011,8,1)
    #end   = (2011,8,1)

    database = DB("/qcwork/dqresults/metrics.db")
    availability("/xs0/seed", database, start=start, end=end, net=network, st=station)
    #availability("/xs1/seed", database, start=start, end=end, net=network, st=station)
    sensor_compare("/qcwork/dqresults/sencomp", database, start=start, end=end, net=network, st=station)
    noise("/qcwork/dqresults/noise", database, start=start, end=end, net=network, st=station)
    database.commit()

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
    print "Stack Size:", stacksize()
    print "Memory:    ", memory()
    print "Resident:  ", resident()

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

