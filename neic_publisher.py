from datetime import date, timedelta
from itertools import product
import json
import csv

# pip-installed
from kafka import KafkaProducer

# requires a local installation of the dqatools python module
from dqatools.dqaclient import call_dqa

# this should contain all networks that DQA analyzes
all_networks = ['CU', 'GS', 'GT', 'IC', 'II', 'IU', 'IW', 'NE', 'US', 'N4']


def run_test():
    start = [date(year=2020, month=1, day=1)]
    metrics = ['NLNMDeviationMetric:0.5-1']
    publish_messages(all_networks, start, metrics, True)


def run_dev():
    start = None
    metrics = ['NLNMDeviationMetric:0.5-1']
    # This should not be run as a test, it's dev!
    publish_messages(all_networks, start, metrics, False)


def topic_fix(metric_name, is_test=False):
    new_name = "ASL-"
    # TODO: manually change to 'Prod' when ready production stuff
    if is_test:
        new_name += "Test"
    else:
        new_name += "Dev"
    new_name += "-SEEDScan-"
    # remove the ':' and anything after it for metrics like NLNM deviation
    # where the last half is what band the deviation was taken over
    # (e.g., "NLNMDeviationMetric:0.5-1" becomes "NLNMDeviationMetric")
    # the full metric name including range should be part of the JSON fields
    truncated_metric_name = metric_name.split(':')[0]
    new_name += truncated_metric_name
    return new_name


def publish_messages(networks=None, select_dates=None, metrics=None,
                     is_test=False):
    if networks is None or len(networks) == 0:
        networks = all_networks
    if select_dates is None or len(select_dates) == 0:
        # scan over the past 5 days if no date was set
        select_dates = []
        for offset in range(1, 5):
            select_dates.append(date.today() - timedelta(days=offset))
    if metrics is None or len(metrics) == 0:
        metrics = ['NLNMDeviationMetric:0.5-1']
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
        data = csv.reader(output.splitlines(), skipinitialspace=True)
        # note that CSV class works in such a way that the iterator is the only reasonable way to
        # access the data once converted into it -- fortunately the first entry can still easily be
        # checked to determine if there's really any content to iterator over
        for record in data:
            # this will happen if DQA doesn't have data for a given day/metric, not an empty list:
            if str(record[0]).startswith("Error"):
                if is_test:
                    print("Nothing available for", select_date, network, metric)
                break  # go back to outer loop, no data in this record exists
            # now we get the fields and jsonify them for publication
            # value order is how they come out of the call_dqa method
            (r_date, network, station, location, channel, metric, value) = record
            if is_test:
                print(r_date, network, station, location, channel, metric, value)
            # get the topic name derived from the metric and run type
            topic_name = topic_fix(metric, is_test)
            # json format description:
            # https://github.com/usgs/earthquake-detection-formats (cont.)
            # /blob/master/format-docs/StationInfo.md
            # we have some custom formats added here to disambiguate metric
            # and to give the date of data this metric was evaluated upon
            message = {"Type": "StationInfo", "Site": {"Network": network,
                                                       "Station": station,
                                                       "Location": location,
                                                       "Channel": channel},
                       "Quality": value, "Date": date_string, "Enable": "true",
                       "Metric": metric}
            # next step is to actually send this message
            # metric (topic) name might have disallowed character in it
            # print(topic_name, message)
            producer.send(topic_name, message)
        producer.flush()


if __name__ == '__main__':
    # TODO: parse in args from command line for custom scans?
    # if this is being run from the terminal it's almost certainly as a
    # cronjob where we run over the previous day's worth of metadata changes
    run_dev()  # TODO: switch to non-test method
