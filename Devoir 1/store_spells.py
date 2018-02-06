import json
from pymongo import MongoClient

names = None
with open("spells.json", 'r') as input_file:
    names = json.load(input_file)

client = MongoClient('localhost', 27017)
db = client.TP1Database
collection = db.WizardSpells
collection.insert_many(names)