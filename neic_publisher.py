from datetime import datetime, date, timedelta
from itertools import product
import json

# pip-installed
from kafka import KafkaProducer
from kafka.errors import KafkaError

# requires a local repo
from dqatools.dqaclient import call_dqa


def run_test():
    start = [date(year=2020, month=1, day=1)]
    metrics = ['NLNMDeviationMetric:0.5-1']
    publish_messages(["IU"], start, metrics, True)


def topic_fix(topic_name):
    new_name = topic_name.replace('_', '')
    # turn, e.g., "0.5-1" to "0.5to1"
    new_name = new_name.replace('-', 'to')
    new_name = new_name.replace(':', '')
    new_name = new_name.replace(' ', '')
    return new_name


def publish_messages(networks=None, select_dates=None, metrics=None,
    is_test=False):
    if networks is None or len(networks) == 0:
        networks = ['CU', 'GS', 'GT', 'IC', 'II', 'IU', 'IW', 'NE', 'US', 'N4']
    if select_dates is None or len(select_dates) == 0:
        # scan over the past 5 days if no date was set
        select_dates = []
        for offset in range(1, 5):
            select_dates.append(datetime.now().date() - timedelta(days=offset))
    if metrics is None or len(metrics) == 0:
        metrics = ['NLNMDeviationMetric:0.5-1']
    # TODO: expect to iterate over a list of metric names to get
    #  data from -- WPhase, NLNMDeviation, etc.
    for select_date, network, metric in product(select_dates, networks,
                                                metrics):
        date_string = select_date.strftime("%Y-%m-%d")
        output = call_dqa(network=network, metric=metric, begin=date_string,
                          end=date_string, format='csv')
        # set up the kafka (producer) connection here
        # default blocktime is 60000 ms -- so let's try multiplying by 5
        blocktime = 10000
        producer = KafkaProducer(bootstrap_servers=
                                 'igskcicgvmkafka.cr.usgs.gov:9092',
                                 client_id='producer-from-dqa', acks=0,
                                 max_block_ms=blocktime,
                                 value_serializer=lambda v: json.dumps(v)
                                 .encode('utf-8'))
        # each line is effectively a row in the database
        for record in iter(output.splitlines()):
            # now we get the fields and jsonify them for publication
            # value order is how they come out of the call_dqa method
            (r_date, network, station, location, channel, metric, value) = \
                record.split(',')
            # (metric, value, channel, location, station, network) = record
            # json format description:
            # https://github.com/usgs/earthquake-detection-formats (cont.)
            # /blob/master/format-docs/StationInfo.md
            # we have some custom formats added here to disambiguate metric
            # and to give the date of data this metric was evaluated upon
            message = {"Type": "StationInfo", "Site": {"Network": network,
                                                       "Station": station,
                                                       "Location": location,
                                                       "Channel": channel},
                       "Quality": value, "Date": date_string, "Enable": "true"}
            # next step is to actually send this message
            # metric (topic) name might have disallowed character in it
            topic_name = metric
            if is_test:
                topic_name += ".test"
            topic_name = topic_fix(topic_name)
            print(topic_name, message)
            producer.send(topic_name, message)
        producer.flush()


if __name__ == '__main__':
    # TODO: parse in args from command line for custom scans?
    # if this is being run from the terminal it's almost certainly as a
    # cronjob where we run over the previous day's worth of metadata changes
    run_test()  # TODO: switch to non-test method
