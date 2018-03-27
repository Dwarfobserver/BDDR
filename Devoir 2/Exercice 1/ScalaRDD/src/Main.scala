
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}

import java.io.File

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.io.Source
import scala.concurrent.duration._
import scala.util.parsing.json.JSON

object Main extends App {

    implicit val executor: ExecutionContextExecutor = ExecutionContext.global

    val fSparkContext = Future {
        println("Initialize Spark ...")

        System.setProperty("hadoop.home.dir", "C:/hadoop")
        val conf = new SparkConf().setAppName("ScalaRDD").setMaster("local[*]")
        Logger.getRootLogger.setLevel(Level.WARN)
        new SparkContext(conf)
    }

    val fMonsters = Future {
        println("Parse JSON file ...")

        val jFile = Source.fromFile("../monsters.json")
        val jString = try jFile.mkString finally jFile.close()
        val Some(json) = JSON.parseFull(jString)

        class CastExtractor[T] {
            def unapply(a:Any): Option[T] = Some(a.asInstanceOf[T])
        }
        val exList    = new CastExtractor[List[Any]]
        val exMap     = new CastExtractor[Map[String, Any]]
        val exStr     = new CastExtractor[String]
        val exStrList = new CastExtractor[List[String]]

        for {
            // 'json' is wrapped into a list to allow the 'for comprehension' expression
            exList(monsters) <- List(json)
            exMap(monster)   <- monsters
            exStr(name)       = monster("name")
            exStrList(spells) = monster("spells")
        } yield {
            (name, spells)
        }
    }

    val (sc, monsters) = Await.result(for {
        _1 <- fSparkContext
        _2 <- fMonsters
    } yield (_1, _2), 5.second)

    println("Manipulate data in RDDs ...")

    val rddMonsters = sc.parallelize(monsters)
    val rddSpells = rddMonsters
        .flatMap(monster => {
            for (spell <- monster._2)
                yield (spell, monster._1)
        })
        .groupByKey()

    val folder = new File("spells")
    if (folder.exists) {
        folder.listFiles.foreach(_.delete())
        folder.delete()
    }
    rddSpells.saveAsTextFile(folder.getPath)

    println("Spells have been written to 'ScalaRDD/spells'")
}
