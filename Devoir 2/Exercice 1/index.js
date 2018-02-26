
'use strict';

/*
    This program queries all D&D monsters and their spells from paizo.com.
    It output them as a JSON at 'monsters.json' in this format :
    [
        {
            'name': 'Bob',
            'spells': [
                'fireball',
                 'whirlwind'
            ]
        }, ...
    ]
*/

// Modify the variable 'simultaneousConnexions' as you wish, regarding your connexion.
//    - Too low value  : slow processing
//    - Too high value : connexions timeout
const simultaneousConnexions = 100;

const { httpRequest } = require('xhttp/node'); // Load http content
const cheerio         = require('cheerio');    // Parse HTML like jQuery
const fs              = require('fs');         // Write results to monsters.json

// The stringified class.
class Monster {
    constructor(name) {
        this.name = name;
        this.spells = [];
    }
}

// Limits the number of tasks which run asynchronously.
// Used to avoid timeout issues with http requests.
// This class is not generic, it only contains the minimum for the script.
class TaskQueue {
    constructor() {
        this._tasks = [];
        this._results = [];
        this.onTaskCompleted = (i, max) => {};
        this._tasksCompleted = 0;
    }
    // Adds a task (before running the queue).
    push(task) {
        this._tasks.push(task);
    }
    // Runs the tasks with <concurrency> asynchronous workers.
    run(concurrency) {
        return new Promise((resolve) => {
            this._tasksCount = this._tasks.length;
            const promises = [];
            for (let i = 0; i < concurrency; i += 1) {
                promises.push(new Promise((resolve) => {
                    this._worker(resolve);
                }));
            }
            Promise.all(promises).then(() => {
                resolve();
            })
            .catch((err) => {
                console.log(' --- Error in TaskQueue : ' + err);
                resolve();
            });
        });
    }
    // Executes sequentially tasks until there is none left.
    _worker(resolve) {
        if (this._tasks.length === 0)
            return resolve();

        const task = this._tasks.shift();
        task(() => {
            this._tasksCompleted += 1;
            this.onTaskCompleted(this._tasksCompleted, this._tasksCount);
            // Use timeout to avoid stack overflow when there is a lot of tasks.
            setTimeout(() => {
                this._worker(resolve);
            }, 0);
        });
    }
}

// Returns a promise with the html body of the link required.
function queryPage(link) {
    return httpRequest({url: link})
    .then((response) => {
        return cheerio.load(response.body);
    })
    .catch((reason) => {
        if (('' + reason).includes('Not Found')) return;
        if (('' + reason).includes('Moved Permanently')) return;
        console.error(" --- Error in queryPage : " + reason + ', on link ' + link);
    });
}

// Returns the html page of a link, if it corresponds to a monster.
// Some non-monster pages are still filtered later.
function tryExtractMonsterPage(link) {
    let hashtagIndex = link.lastIndexOf('#');
    if (hashtagIndex === -1) hashtagIndex = link.length;

    if (link[0] === '/') {
        if (link.substring(19, 27) !== 'bestiary') return;
        link = link.substring(link.lastIndexOf('/') + 1, hashtagIndex);
    }
    else {
        if (!link.includes('.html')) return;
        link = link.substring(0, hashtagIndex);
    }
    return link;
}

// Returns all the monster links referenced by the given link.
function queryMonsterURLs(bestiaryLink) {
    return new Promise((resolve) => {
        const monstersURL = [];
        const directory = bestiaryLink.substring(0 , bestiaryLink.lastIndexOf('/') + 1);
        queryPage(bestiaryLink)
        .then(($) => {
            if (!$) return resolve(monstersURL);
            $('a')
            .each(function() {
                const link = $(this).attr('href');
                const page = tryExtractMonsterPage(link);
                if (!page) return;

                const url = directory + page;
                // Avoids to crawls the same page multiple times.
                if (monstersURL.includes(url)) return;
                monstersURL.push(url);
            });
            resolve(monstersURL);
        })
        .catch((reason) => {
            console.error(" --- Error in inner queryMonsterURLs : " + reason);
        });
    })
    .catch((reason) => {
        console.error(" --- Error in outer queryMonsterURLs : " + reason);
    });
}

// Returns monsters created from the given link.
function makeMonsters($) {
    const monsters = [];
    $('.stat-block-title')
    .each(function() {
        let name = $(this).text();
        const indexCR = name.indexOf(' CR');
        if (indexCR === -1) return;
        name = name.substring(0, indexCR);

        const monster = new Monster(name);
        $(this)
        .nextUntil('.stat-block-title')
        .each(function() {
            $(this).find('a')
            .each(function() {
                if (!$(this).attr('href')) return;
                if ($(this).attr('href').includes('/spells/')) {
                    monster.spells.push($(this).text());
                }
            });
        });
        monsters.push(monster);
    });
    return monsters;
}

// Return a promise of all monsters contained in the html of the given link.
function queryMonsters(monsterLink) {
    return new Promise((resolve) => {
        queryPage(monsterLink)
        .then(($) => {
            if (!$) return resolve([]);
            resolve(makeMonsters($));
        })
        .catch((reason) => {
            console.error(" --- Error in queryMonsters : " + reason);
        });
    });
}

// ---- Main ----
(async function() {
    // Pages referencing monsters pages.
    const bestiaryURLs = [
        'http://paizo.com/pathfinderRPG/prd/bestiary/monsterIndex.html',
        'http://paizo.com/pathfinderRPG/prd/bestiary2/additionalMonsterIndex.html',
        'http://paizo.com/pathfinderRPG/prd/bestiary3/monsterIndex.html',
        'http://paizo.com/pathfinderRPG/prd/bestiary4/monsterIndex.html',
        'http://paizo.com/pathfinderRPG/prd/bestiary5/index.html'
    ];

    const monsters = [];

    const httpQueue = new TaskQueue();
    httpQueue.onTaskCompleted = (i, count) => {
        // Show progress on the same console line.
        process.stdout.cursorTo(0);
        process.stdout.clearLine();
        process.stdout.write('[ ' + i + ' / ' + count + ' ]');
    };

    console.log('Query monster pages ...');
    const bestiaryPromises = [];
    for (const bestiaryURL of bestiaryURLs) {
        bestiaryPromises.push(queryMonsterURLs(bestiaryURL)
        .then((monsterURLs) => {
            for (const monsterURL of monsterURLs) {
                httpQueue.push((unlock) => {
                    queryMonsters(monsterURL)
                    .then((newMonsters) => {
                        for (const monster of newMonsters) {
                            monsters.push(monster);
                        }
                        unlock();
                    })
                    .catch((reason) => {
                        console.error(" --- Error in inner promise : " + reason);
                        unlock();
                    });
                });
            }
        })
        .catch((reason) => {
            console.error(" --- Error in outer promise : " + reason);
        }));
    }
    await Promise.all(bestiaryPromises).then(() => {
        console.log('Found ' + httpQueue.tasksCount + ' monster pages');
        console.log('Start crawling ' + simultaneousConnexions + ' pages at a time');
        httpQueue.run(simultaneousConnexions)
        .then(() => {
            process.stdout.cursorTo(0);
            process.stdout.clearLine(); // Clear progress output
            console.log('Writting ' + monsters.length + ' monsters data to monsters.json ...');
            const jsonMonsters = JSON.stringify(monsters, undefined, 4);
            fs.writeFile('monsters.json', jsonMonsters, 'utf8', (err) => {
                if (err) return console.log('Error while writting to monsters.json : ' + err);
                console.log('Success !');
            });
        })
        .catch((reason) => {
            console.error(" --- Error in TaskQueue finalizer : " + reason);
        });
    });
}());
