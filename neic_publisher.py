from datetime import datetime, date, timedelta
from itertools import product

import psycopg2
import json
from kafka import KafkaProducer


def run_test():
    start = date(year=2020, month=1, day=1)
    publish_messages(["IU"], [start], True)


def publish_messages(networks=None, select_dates=None, is_test=False):
    if networks is None:
        networks = ['CU', 'GS', 'GT', 'IC', 'II', 'IU', 'IW', 'NE', 'US', 'N4']
    if select_dates is None or len(select_dates) == 0:
        # scan over the past 5 days if no date was set
        select_dates = []
        for offset in range(1, 5):
            select_dates.append(datetime.now().date() - timedelta(days=offset))
    # TODO: change this to the DQA main scan db for running as a cron'd script
    dqa_db = psycopg2.connect(dbname='dqa_prod_prod_clone', host='vmdbx01')
    # now presumably we can iterate through our map structures and get each
    # table entry to add
    cursor = dqa_db.cursor()
    # TODO: will probably need to iterate over a list of metric names to get
    #  data from -- WPhase, NLNMDeviation, etc.
    for select_date, network in product(select_dates, networks):
        date_string = select_date.strftime("%Y-%m-%d")
        # TODO: need to make sure that WHERE clause filters by date
        cursor.execute("""SELECT tblmetric.displayname, tblmetricdata.value, 
                       tblchannel.name, tblsensor.location, 
                       tblstation.name, "tblGroup".name 
                       FROM tblmetric INNER JOIN tblmetricdata 
                       ON tblmetric.pkmetricid = tblmetricdata.fkmetricid 
                       INNER JOIN tbldate 
                       ON tbldate.pkdateid = tblmetricdata.date 
                       INNER JOIN tblchannel 
                       ON tblchannel.pkchannelid = tblmetricdata.fkchannelid 
                       INNER JOIN tblsensor 
                       ON tblsensor.pksensorid = tblchannel.fksensorid 
                       INNER JOIN tblstation 
                       ON tblstation.pkstationid = tblsensor.fkstationid 
                       INNER JOIN "tblGroup" 
                       ON "tblGroup".pkgroupid = tblstation.fknetworkid 
                       WHERE tblmetric.name = \'NLNMDeviationMetric:0.5-1\' 
                       AND "tblGroup".name = (%s)
                       AND tbldate.date = (%s)""", (network, select_date))
        # set up the kafka (producer) connection here
        producer = KafkaProducer(bootstrap_servers='igskcicgvmkafka:9092',
                                 client_id='producer-from-dqa')
        for record in cursor:
            # now we get the fields and jsonify them for publication
            (metric, value, channel, location, station, network) = record
            # json format description:
            # https://github.com/usgs/earthquake-detection-formats (cont.)
            # /blob/master/format-docs/StationInfo.md
            # we have some custom formats added here to disambiguate metric
            # and to give the date of data this metric was evaluated upon
            message = json.JSONEncoder().encode(
                {"Type": "StationInfo", "Site": {"Network": network,
                                                 "Station": station,
                                                 "Location": location,
                                                 "Channel": channel},
                 "Quality": value, "Date": date_string, "Enable": True})
            # next step is to actually send this message
            topic_name = metric
            if is_test:
                topic_name += "_test"
            producer.send(topic_name, message)


if __name__ == '__main__':
    # TODO: parse in args from command line for custom scans?
    # if this is being run from the terminal it's almost certainly as a
    # cronjob where we run over the previous day's worth of metadata changes
    run_test()  # TODO: switch to non-test method
