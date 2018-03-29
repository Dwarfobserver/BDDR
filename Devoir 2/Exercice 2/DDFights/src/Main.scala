
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
    val scene = new SceneSetup(List(
        new ActorSetup(ActorType.Solar, (10, 20)),
        new ActorSetup(ActorType.OrcBarbarian, (-10, 12)),
        new ActorSetup(ActorType.OrcBarbarian, (-11, 14))
    ))

    // Create the channel where the scene status are passed
    val channel = new Channel[SceneStatus]

    // Create the engine
    val engine = new Engine(channel, scene)

    // Start the engine
    engine.start()

    // Get the first status (garanteed to be available after the start)
    var Some(status) = channel.pop()

    println("Engine started")

    // Wait asynchronously for the user input
    val fInput = Future {
        scala.io.StdIn.readLine()
    } (ExecutionContext.global)

    // Main loop, which keep polling for new status
    while (!fInput.isCompleted) {
        channel.pop() match {
            case None => // Nothing
            case Some(s) =>
                status = s
                println("Status updated")
        }
        Thread.sleep(10)
    }

    // Stop the engine
    engine.stop()

    println("Engine stopped")
}
