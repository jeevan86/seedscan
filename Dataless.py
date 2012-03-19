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

class Blockette:
    def __init__(self, number):
        self.number = number
        self.fields = {}

        self.last_start_id = 0

    def add_field_data(self, id, data):
        ids = map(int,id.split('-'))
        if ids[0] < self.last_start_id:
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
            parts = data.split(':', 1)
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

class Dataless: 
    def __init__(self, dataless_file):
        self.dataless_file = dataless_file
        self.raw_dataless = None
        self.blockettes = []
        self.map = {
            'stations' : {},
            'volume-info' : {},
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
        #self._assemble_data()
        print "Parsed out %d blockettes" % len(self.blockettes)
        self.counts = {}
        for blockette in self.blockettes:
            num = blockette.number
            if not self.counts.has_key(num):
                self.counts[num] = 0
            self.counts[num] += 1

        for num in sorted(self.counts.keys()):
            print "% 3d: %d" % (num, self.counts[num])

    def _read_dataless(self):
        fh = open(self.dataless_file, 'r')
        self.raw_dataless = fh.readlines()
        fh.close()

    def _parse_dataless(self):
        if self.raw_dataless is None:
            return

        self.total = len(self.raw_dataless)
        self.count = 0
        self.skipped = 0
        self.percent = 0.0
        self.last_percent = 0

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


    def _assemble_data(self):
        if self.raw_dataless is None:
            return

        self.total = len(self.raw_dataless)
        self.count = 0
        self.skipped = 0
        self.percent = 0.0
        self.last_percent = 0

        new_station = False
        station_name = "INIT"
        station_network = "XX"
        station_data = {}
        volume_stations = {}
        current_comment = {}

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

            if line[0] == '#':
                continue
            if line[0] != 'B':
                continue

            # If we didn't skip, revert the increment
            self.skipped -= 1

            key,data = line.split(None,1)
            blk_id,rest = key[1:].split('F', 1)
            field_ids = map(int, rest.split('-', 1))
            blockette = int(blk_id)

          # Volume Information
            if blockette == 10:
                description,value = map(string.strip, data.split(':', 1))
                self.map['volume-info'][field_ids[0]] = {
                    'desciption' : description,
                    'value' : value,
                }
                continue
          # List of Stations in Volume
            elif blockette == 11:
                station,_ = map(string.strip, data.split())
                if not volume_stations.has_key(station):
                    volume_stations[station] = 0
                volume_stations[station] += 1
                continue
          # Station Epochs
            elif blockette == 50:
                if field_ids[0] == 3:
                    station_data = {
                        'comments' : {},
                        'epochs' : {},
                        'channels' : {},
                    }
                    station_name = data.strip()
                    new_station = True
                    if not volume_stations.has_key(station):
                        raise Exception("Encountered unexpected station [%s]" % station_name)
                    if volume_stations[station] < 1:
                        raise Exception("Encountered too many instances of station [%s]" % station_name)
                    volume_stations[station] -= 1
                elif field_ids[0] == 16:
                    station_network = data.strip()
                    new_station = False
                    key = station_network + '_' + station_name
                    if self.map['stations'].has_key(key):
                        raise Exception("Found a duplicate station [%s]" % key)
                    self.map['stations'][key] = station_data
          # Station Comments
            elif blockette == 51:
                if field_ids[0] == 3:
                    key = parse_epoch_time(data.split(':').strip())
                    current_comment = {}
                    station_data['comments'][key] = current_comment
                    current_comment[blockette] = {}
                current_comment


            if not station_data.has_key(blockette):
                station_data[blockette] = {}

          # populate multi-field (child) items
            if len(field_ids) > 1:
                parts = map(string.strip, data.split())
                index = parts[0]
                fields = parts[1:]
                field_low,field_high = map(int, field_ids)
                parent_id = field_low - 1 
                pocket = station_data[blockette][parent_id]['children']
                idx = 0
                for field_id in range(field_low, field_high+1):
                    if not pocket.has_key(field_id):
                        pocket[field_id] = {'value':[]}
                    pocket[field_id]['value'].append(fields[idx])
                    idx += 1

          # populate normal and count (parent) items
            else:
                field_id = int(field_ids[0])
                description,value = map(string.strip, data.split(':', 1))
                station_data[blockette][field_id] = {
                    'children'    : {},
                    'description' : description,
                    'value'       : value,
                }

