import pprint
import string

# TODO: Revamp how we parse the Dataless text file
#
# The previous assumption I made about lines with a range of blocks is false
#
# The parser is probably going to need to know more about the structure of
# the data. If we know which are counts, and to which fields they refere,
# we can easily pair them in a cleaner fashion...
#
# We can also more easily handle the process of storing epochs if we know
# which fields contain epoch dates.
#
# We need to assemle a Map of rules for how to parse various types, and 
# aslo contain descriptions of relationships.
#

stages = {
    53 : 4,
    54 : 4,
    55 : 3,
    56 : 3,
    57 : 3,
    58 : 3,
    61 : 3,
}

def parse_epoch(epoch):
    hour,minute,second = 0,0,0
    parts = epoch.split(',')
    year,day = map(int, parts[0:2])
    if len(parts) > 2:
        parts = map(int, parts[2].split(':'))
        hour = parts[0]
        if len(parts) > 1:
            minute = parts[1]
        if len(parts) > 2:
            second = parts[2]
    return year,day,hour,minute,second

def epoch_string(epoch):
    return "%04d,%03d,%02d:%02d:%02d" % epoch


class Blockette:
    def __init__(self, number):
        self.number = number
        self.fields = {}

        self.last_start_id = 0

    def add_field_data(self, id, data):
        ids = map(int,id.split('-'))
        if (self.last_start_id > ids[0]) and ((((self.number != 52) or (self.last_start_id > 4)) and (ids[0] == 3)) or ((self.number == 52) and (ids[0] == 4))):
            return False
            #raise Exception("Blockette field out of order [last=%d -> current=%d]" % (self.last_start_id, ids[0]))
        self.last_start_id = ids[0]
        if len(ids) > 1:
            ids = range(ids[0], ids[1]+1)
            data_items = data.split()
            if len(data_items) > (len(ids) + 1):
                raise Exception("Too many parts for multi-field entry")
            if len(data_items) > (len(ids)):
                data_items = data_items[1:]
            for i in range(0, len(ids)):
                if not self.fields.has_key(id):
                    self.fields[ids[i]] = {
                        'description' : '',
                        'values' : [],
                    }
                self.fields[ids[i]]['values'].append(data_items[i])
        else:
            id = ids[0]
            parts = map(string.strip, data.split(':', 1))
            description = ''
            if len(parts) == 2:
                description,data = parts
            else:
                data = parts[0]
            if self.fields.has_key(id):
                self.fields[id]['values'].append(data)
            else:
                self.fields[id] = {
                    'description' : description,
                    'values' : [data],
                }
        return True

    def get_values(self, *args):
        results = []
        for field in args:
            if not self.fields.has_key(field):
                values = (None,)
            else:
                values = self.fields[field]['values']
            if len(values) == 1:
                results.append(values[0])
            else:
                results.append(values)
        return tuple(results)

    def get_field(self, arg):
        results = None
        if self.fields.has_key(arg):
            results = self.fields[arg]['values']
        return results

    def get_descriptions(self, *args):
        results = []
        for field in args:
            results.append[self.fields[field]['description']]
        return tuple(results)

class Dataless: 
    def __init__(self, dataless_file):
        self.dataless_file = dataless_file
        self.raw_dataless = None
        self.blockettes = None
        self.map = {
            'volume-info' : None,
            'stations' : {},
        }

        self.total = 0
        self.count = 0
        self.skipped = 0
        self.percent = 0.0
        self.last_percent = 0.0
        self.line = ""

    def process(self):
        self._read_dataless()
        self._parse_dataless()
        self._assemble_data()

        #print "Parsed out %d blockettes" % len(self.blockettes)
        #self.counts = {}
        #for blockette in self.blockettes:
        #    num = blockette.number
        #    if not self.counts.has_key(num):
        #        self.counts[num] = 0
        #    self.counts[num] += 1
        #    summary = ""
        #    if num == 10:
        #        summary = " ----- %s: %s - %s" % blockette.get_values(9,5,6)
        #    elif num == 11:
        #        summary = " ----- %s" % blockette.get_values(4)
        #    elif num == 50:
        #        summary = " ----- %s_%s: %s - %s" % blockette.get_values(16,3,13,14)
        #    elif num == 52:
        #        summary = " ----- %s-%s: %s - %s" % blockette.get_values(3,4,22,23)
        #    print "B%03d [%d fields]%s" % (blockette.number, len(blockette.fields), summary)
        #
        #for num in sorted(self.counts.keys()):
        #   print "% 3d: %d" % (num, self.counts[num])

        pprint.pprint(self.map)

    def _read_dataless(self):
        self.stage = "Reading Dataless"
        fh = open(self.dataless_file, 'r')
        self.raw_dataless = fh.readlines()
        fh.close()

    def _parse_dataless(self):
        if self.raw_dataless is None:
            return

        self.blockettes = []

        self.total = len(self.raw_dataless)
        self.count = 0
        self.skipped = 0
        self.percent = 0.0
        self.last_percent = 0
        self.stage = "Parsing Dataless"

        blockettes = {}
        for line in self.raw_dataless:

            # Track our progress
            self.count += 1
            self.percent = float(int(float(self.count) / float(self.total) * 100.0))
            if self.percent > self.last_percent:
                self.last_percent = self.percent
                print "%0.2f%% (%d/%d - %d lines skipped)" % (self.percent, self.count, self.total, self.skipped)

            line = line.strip()
            self.line = line

            # Assume we will skip this line
            self.skipped += 1

            if line == '':
                continue
            if line[0] == '#':
                continue
            if line[0] != 'B':
                continue

            # If we didn't skip, revert the increment
            self.skipped -= 1

            key,data = line.split(None,1)
            blockette_num,field_ids = key[1:].split('F', 1)
            blockette_num = int(blockette_num)

            # If this blockette does not exist, create it
            if not blockettes.has_key(blockette_num):
                blockette = Blockette(blockette_num)
                blockettes[blockette_num] = blockette
                self.blockettes.append(blockette)
            # If this blockette does exist, retrieve it
            else:
                blockette = blockettes[blockette_num]
            
            # If we stepped backward in the field_id value we need to start a new blockette
            if not blockette.add_field_data(field_ids, data):
                blockette = Blockette(blockette_num)
                blockettes[blockette_num] = blockette
                self.blockettes.append(blockette)
                blockette.add_field_data(field_ids, data)


    def _assemble_data(self):
        if self.blockettes is None:
            return

        self.total = len(self.blockettes)
        self.count = 0
        self.skipped = 0
        self.percent = 0.0
        self.last_percent = 0
        self.stage = "Assembling Data"

        stations = self.map['stations']
        station = None
        channel = None
        epoch = None

        for blockette in self.blockettes:
            # Track our progress
            self.count += 1
            self.percent = float(int(float(self.count) / float(self.total) * 100.0))
            if self.percent > self.last_percent:
                self.last_percent = self.percent
                print "%0.2f%% (%d/%d)" % (self.percent, self.count, self.total)

            number = blockette.number

          # Volume Information
            if number == 10:
                if self.map['volume-info'] is not None:
                    raise Exception("Found multiple volume-info blockettes.")
                self.map['volume-info'] = blockette

          # List of Stations in Volume
            elif number == 11:
                # This information is only necessay for parsing the SEED volume
                pass

          # Station Epochs
            elif number == 50:
                key = "%s_%s" % blockette.get_values(16,3)
                if not stations.has_key(key):
                    stations[key] = {
                        'comments' : {},
                        'epochs' : {},
                        'channels' : {},
                    }
                station = stations[key]
                try:
                    epoch_key = epoch_string(parse_epoch(blockette.get_values(13)[0]))
                    station['epochs'][epoch_key] = blockette
                except AttributeError:
                    pass

          # Station Comments
            elif number == 51:
                epoch_key = epoch_string(parse_epoch(blockette.get_values(3)[0]))
                station['comments'][epoch_key] = blockette

          # Channel Epochs
            elif number == 52:
                key = "%s-%s" % blockette.get_values(3,4)
                if not station['channels'].has_key(key):
                    station['channels'][key] = {
                        'comments' : {},
                        'epochs' : {},
                    }
                channel = station['channels'][key]
                epoch_key = epoch_string(parse_epoch(blockette.get_values(22)[0]))
                channel['epochs'][epoch_key] = {
                    'info' : blockette,
                    'format' : None,
                    'stages' : {},
                    'misc' : [],
                }
                epoch = channel['epochs'][epoch_key]

          # Epoch Format
            elif number == 30:
                epoch['format'] = blockette

          # Channel Comments
            elif number == 59:
                epoch_key = epoch_string(parse_epoch(blockette.get_values(3)[0]))
                channel['comments'][epoch_key] = blockette

          # Channel Stages
            elif stages.has_key(number):
                stage_key = blockette.get_values(stages[number])[0]
                if not epoch['stages'].has_key(stage_key):
                    epoch['stages'][stage_key] = {}
                epoch['stages'][stage_key][blockette.number] = blockette
          
          # All other data
            else:
                epoch['misc'].append(blockette)


