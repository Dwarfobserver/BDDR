
'''
mongod.exe must be started on localhost:27017.
This script will fill the database with spells.json then launch a map-reduce
algorithm on the spells table to get those which are below level 4 and only
requires a verbal component to be casted.
'''

from pymongo import MongoClient
from bson.code import Code
import json



client = MongoClient('localhost', 27017)
db = client.TP1Database
spells_table = db.WizardSpells
if spells_table.find().count() == 0 :
    names = None
    with open("../spells.json", 'r') as input_file:
        names = json.load(input_file)
        
    spells_table.insert_many(names)

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

usable_spells = spells_table.map_reduce(fmap, freduce, 'UsableSpells').find()
print(str(usable_spells.count()) + " spells usable :")
for spell_json in usable_spells:
    print(spell_json['_id'])
