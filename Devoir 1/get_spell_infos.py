
'''
This script queries and organize wizard spells data from their names on archivesofnethys.com.
By default, the results are stored in spells.json, in the same folder as the script.
Arguments :
    ar1            - The input file containing spell names.
    ar2 (optional) - The output file path and name.

JSON Struture :
[
    {
        "name": "Acid Arrow",
        "level": 2,
        "components" : ["V", "S", "M"],
        "spell_resistance" : false
    },
    {
        "name": "Detect Magic",
        "level": 0,
        "components" : ["V", "S"],
        "spell_resistance" : false
    },
    ...
]
'''

import sys
import json
import urllib.request
import urllib.parse
import html.parser

SPELL_URL_ROOT   = 'http://archivesofnethys.com/SpellDisplay.aspx?ItemName='

class SpellParser(html.parser.HTMLParser):

    def __init__(self, name):
        html.parser.HTMLParser.__init__(self)
        self.name = name
        self.level = -1
        self.components = []
        self.spell_resistance = False
        self.next_data = ''
        self.info_set = 0
        self.found_section = False

    def handle_starttag(self, tag, attrs):
        if self.info_set == 3: return

        if not self.found_section:
            if tag == 'h1':
                self.next_data = 'name'
        elif tag == 'b':
            self.next_data = 'title'

    def handle_data(self, data):
        if self.info_set == 3 or not self.next_data: return

        if not self.found_section:
            if   self.next_data == 'name':
                self.next_data = ''
                if data == self.name or data[1:] == self.name:
                    self.found_section = True
        elif     self.next_data == 'title':
            self.next_data = data
        else:
            if   self.next_data == 'Level':
                pos = data.find('wizard') + len('wizard') + 1
                self.level = int(data[pos:pos + 1])
                self.info_set += 1

            elif self.next_data == 'Components':
                split = data.split(',')
                for letters in split:
                    letter = letters[1]
                    if letter.isalpha() and letter.isupper():
                        self.components.append(letter)
                self.info_set += 1

            elif self.next_data == 'Spell Resistance':
                self.spell_resistance = len(data) > 3
                self.info_set += 1

            self.next_data = ''

def get_spell_info(name):
    url = SPELL_URL_ROOT + urllib.parse.quote(name)
    with urllib.request.urlopen(url) as response:
        html = response.read().decode('utf-8', 'ignore')
        parser = SpellParser(name)
        parser.feed(html)
        return {
            'name':             name,
            'level':            parser.level,
            'components':       parser.components,
            'spell_resistance': parser.spell_resistance
        }

# Main script

if len(sys.argv) == 1 or len(sys.argv) > 3:
    print('Usage : python get_spell_infos.py [input file] [optional : output file]')
    sys.exit(1)

input_path = sys.argv[1]
output_path = 'spells.json'
if len(sys.argv) == 3: output_path = sys.argv[2]

names = None
with open(input_path, 'r') as input_file:
    names = json.load(input_file)

total_spells = len(names)
parsed_spells = 0

spells = []
for name in names:
    indice = parsed_spells + 1
    line = 'Fetch spell {}/{} ...'.format(indice, total_spells)
    print (line, end = '\r')
    try:
        spells.append(get_spell_info(name))
        parsed_spells += 1
    except Exception as e:
        print('ignored error :')
        print(str(e))
        pass

with open(output_path, 'w') as output_file:
    result = json.dumps(spells, indent = 4)
    output_file.write(result)

percent = int((parsed_spells * 100) / total_spells)
print('{} of {} spells ({}%) written in {}'.format(parsed_spells, total_spells, percent, output_path))
