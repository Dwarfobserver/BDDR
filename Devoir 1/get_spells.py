
'''
This script queries and organize wizard spells data from the site archivesofnethys.com.
By default, the results are stored in spells.json, in the same folder as the script.

This script will call get_spell_names.py and get_spell_info.py scripts in the same folder.

Arguments :
    arg1 (optional) - The output file path and name.

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

import os
import sys
from subprocess import call

if len(sys.argv) > 2:
    print('Usage : python get_spells.py [optional : output file]')
    sys.exit(1)

output_path = 'spells.json'
if len(sys.argv) == 2: output_path = sys.argv[1]

spell_names = '__tmp_names__.json'

try:
    call(['python', 'get_spell_names.py', spell_names])
    call(['python', 'get_spell_infos.py', spell_names, output_path])
except:
    raise
finally:
    os.remove(spell_names)
    print(spell_names + ' removed')
