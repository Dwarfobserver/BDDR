let mongojs = require('mongojs');
let fs = require('fs');
let dummyjson = require('dummy-json');

let db = mongojs('localhost/TP1Database', ['WebsitePagerank', 'MapReduceResult']);
const dampingFactor = 0.85;
let result = fs.readFileSync("WebsitePagerank.json", "UTF-8");

let mapper = function(){
    let url = this._id; 
    let outlink_list = this.value.outlink_list;
    let pagerank = this.value.pagerank;
    emit(url, outlink_list);
    emit(url, outlink_list);
    outlink_list.forEach(outlink => {
        emit(outlink, pagerank/outlink_list.length);
    });
}

let reducer = function(url, list_pr_or_urls){
    let outlink_list = [];
    let pagerank = 0;

    list_pr_or_urls.forEach(pr_or_urls => {
        if(Array.isArray(pr_or_urls)){
            outlink_list = pr_or_urls;
        }
        else{
            pagerank = pagerank + pr_or_urls;
        }
    });

    pagerank = 1 - 0.85 + ( 0.85 * pagerank);
    return({"outlink_list":outlink_list,"pagerank":pagerank});
}

db.WebsitePagerank.remove(function (argument) {
	console.log("DB Cleanup Completed");
});

db.MapReduceResult.remove(function (argument) {
	console.log("DB Cleanup Completed");
});
 
db.WebsitePagerank.insert(JSON.parse(result), function (err, docs) {
	console.log("DB Insert Completed");
});

let mapreducer = function(){
    db.WebsitePagerank.mapReduce(
        mapper,
        reducer,
        {
            out: 'WebsitePagerank'
        },
        function(err, docs, lastErrorObject){
            if(i<20){
                i = i + 1;
                console.log(i);
                mapreducer();
            }
        }
    );
}

let i=0
mapreducer();

