
'''
mongod.exe must be started on localhost:27017.
This script will then launch a map-reduce algorithm on the spells table to get
those which are below level 4 and only requires a verbal component to be casted.
'''

from pymongo import MongoClient
from bson.code import Code

client = MongoClient('mongodb://localhost:27017/')
bd = client.TP1Database
spells_table = bd.WizardSpells

fmap = Code(
"""
function() {
    if (this.level > 4) return;
    if (this.components.length !== 1) return;
    if (this.components[0] !== 'V') return;
    emit(this.name, null);
}
""")

freduce = Code(
"""
function(key, vals) {
    return null;
}
""")

usable_spells = spells_table.map_reduce(fmap, freduce, 'usable_spells')

for spell_json in usable_spells.find():
    print(spell_json['_id'])
