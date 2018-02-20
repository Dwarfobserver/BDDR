
'use strict';

const MongoClient = require('mongodb').MongoClient;
const assert = require('assert');
const fs = require("fs");

MongoClient.connect('mongodb://localhost:27017', async (err, client) => {
  assert.equal(null, err);

  const db = client.db('TP1Database');
  const name = 'Websites';

  const collection = await new Promise((resolve, reject) => {
    db.collection(name, { strict: true }, async (error, collection) => {
      if (collection) {
        resolve(collection);
        return;
      }
      console.log('Creating collection ...');
      await db.createCollection(name).then((collection) => {
        const content = fs.readFileSync('graph.json');
        collection.insertMany(JSON.parse(content));
        console.log('Collection filled with graph.json');
        resolve(collection);
      });
    });
  });

  const result = await db.collection('').mapReduce(
    () => { // Map

    },
    () => { // Reduce

    }
  );

  client.close();
});
