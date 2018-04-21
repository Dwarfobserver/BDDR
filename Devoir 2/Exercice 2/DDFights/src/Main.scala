
import common._
import engine.{Actor, Engine}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {
/*
    println("Initialize Spark ...")

    System.setProperty("hadoop.home.dir", "C:/hadoop")
    val conf = new SparkConf().setAppName("ScalaRDD").setMaster("local[*]")
    Logger.getRootLogger.setLevel(Level.WARN)
    val sc = new SparkContext(conf)

    println("Spark initialized")
*/
    // Sample code

    // Create a scene (maybe from a json or the GUI)
    val scene = List(
        new ActorSetup(ActorType.Solar, (10, 20)),
        new ActorSetup(ActorType.OrcBarbarian, (-10, 12)),
        new ActorSetup(ActorType.OrcBarbarian, (-11, 14)),
        new ActorSetup(ActorType.OrcBarbarian, (-11, 14)),
        new ActorSetup(ActorType.OrcBarbarian, (-11, 14))
    )

    // Create the channel where the scene status are passed
    val channel = new Channel[List[Actor]]

    // Create the engine
    val engine = new Engine(channel, scene)

    // Start the engine
    engine.start()

    println("Engine started")

    // Wait asynchronously for the user input
    val fInput = Future {
        scala.io.StdIn.readLine()
    } (ExecutionContext.global)

    def consume(opt: Option[List[Actor]]): Unit = {
        opt match {
            case None => // Nothing
            case Some(actors) =>
                println("actors : " + actors.size)
        }
    }

    // Main loop, which keep polling for new status
    while (!fInput.isCompleted && !engine.isFinished) {
        consume(channel.pop())
        Thread.sleep(10)
    }
    var opt = channel.pop()
    while (opt.nonEmpty) {
        consume(opt)
    }

    // Stop the engine
    engine.stop()

    println("Engine stopped")
}
