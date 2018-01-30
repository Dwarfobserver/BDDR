
'''
This script list spell names for wizard from the site archivesofnethys.com.
By default, the results are stored in spell_names.json, in the same folder as the script.
Arguments :
    arg1 (optional) - The output file path and name.

JSON Struture :
[
    "Acid Arrow",
    "Detect Magic",
    ...
]
'''

import sys
import json
import urllib.request
import html.parser

SPELLS_URL       = 'http://archivesofnethys.com/Spells.aspx?Class=Wizard'

class SpellsFinder(html.parser.HTMLParser):

    def __init__(self):
        html.parser.HTMLParser.__init__(self)
        self.names = []

    def handle_starttag(self, tag, attrs):
        for attr in attrs:
            if attr[0] == 'href' and attr[1][:12] == 'SpellDisplay':
                name = attr[1].split('=')[1]
                self.names.append(name)

def get_spell_names():
    with urllib.request.urlopen(SPELLS_URL) as response:
        html = response.read().decode('utf-8', 'ignore')
        parser = SpellsFinder()
        parser.feed(html)
        return parser.names

# Main script

if len(sys.argv) > 2:
    print('Usage : python get_spell_names.py [optional : output file]')
    sys.exit(1)

output_path = 'spell_names.json'
if len(sys.argv) == 2: output_path = sys.argv[1]

print('Loading the website page ...', end = '\r')
names = get_spell_names()

with open(output_path, 'w') as output_file:
    result = json.dumps(names, indent = 4)
    output_file.write(result)

print('Spell names written in ' + output_path)
