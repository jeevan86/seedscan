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

class Dataless: 
    def __init__(self, dataless_file):
        self.dataless_file = dataless_file
        self.raw_dataless = None
        self.map = {}

        self.total = 0
        self.count = 0
        self.skipped = 0
        self.percent = 0.0
        self.last_percent = 0.0
        self.line = ""

    def process(self):
        self._read_dataless()
        self._parse_dataless()

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
            if not self.map.has_key(blockette):
                self.map[blockette] = {}

          # populate multi-field (child) items
            if len(field_ids) > 1:
                parts = map(string.strip, data.split())
                index = parts[0]
                fields = parts[1:]
                field_low,field_high = map(int, field_ids)
                parent_id = field_low - 1 
                pocket = self.map[blockette][parent_id]['children']
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
                self.map[blockette][field_id] = {
                    'children'    : {},
                    'description' : description,
                    'value'       : value,
                }

