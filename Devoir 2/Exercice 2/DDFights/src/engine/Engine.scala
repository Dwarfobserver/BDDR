package engine

import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.broadcast.Broadcast
import org.apache.log4j.{Level, Logger}
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import common._

// The class which runs the fight
class Engine(val channel: Channel[List[Actor]], val setup: List[ActorSetup])
{
    private val mustStop = new AtomicBoolean(false)
    private var task: Future[Unit] = _
    private var sc: SparkContext = _
    private var graph: Graph[Actor, Unit] = _
    private val finished = new AtomicBoolean(false)
    private var turnCount = 1

    private var bModels: Broadcast[Map[ActorType.Value, ActorModel]] = _
    private var aAngels: Accumulator[Int] = _
    private var aOrcs:   Accumulator[Int] = _

    // Start the engine asynchronously
    def start(): Unit = {
        task = Future { run() } (ExecutionContext.global)
    }

    def isFinished: Boolean = finished.get

    // Ask the engine to stop, then wait him to finish
    def stop(): Unit = {
        mustStop.set(true)
        Await.result(task, 10.second)
    }

    private def run(): Unit = {
        createContext()

        println("Spark context created")

        createDistributedData()

        println("Distributed data created")

        val actors = copyActors()
        channel.push(actors)

        var time = System.currentTimeMillis()
        while (!mustStop.get && !finished.get) {
            val t2 = System.currentTimeMillis()
            if (t2 - time >= 1000) {
                time = t2
                updateActors()
                val actors = copyActors()
                channel.push(actors)
            }
            else Thread.sleep(10)
        }
        cleanup()
    }

    // Create the Spark context
    private def createContext(): Unit = {
        System.setProperty("hadoop.home.dir", "C:/hadoop")
        val conf = new SparkConf().setAppName("DDFights").setMaster("local[*]")
        Logger.getRootLogger.setLevel(Level.WARN)
        sc = new SparkContext(conf)
    }

    // Free broadcast variables and stop Spark
    private def cleanup(): Unit = {
        bModels.destroy()
        sc.stop()
    }

    // Create the accumulators, the actors, the broadcast variables and the graph
    private def createDistributedData(): Unit = {
        val models = {
            for (t <- ActorType.values)
                yield (t, ActorModel.from(t))
        }.toMap

        val ids    = Iterator.from(1)
        val actors = for {
            info <- setup
            t     = info.actorType
            actor = new Actor(ids.next(), t, info.pos)
            model = models(t)
            _ = actor.initialize(model)
        } yield actor

        val angels = Iterator.from(0)
        val orcs   = Iterator.from(0)
        for (a <- actors) if (models(a.t).side == ActorSide.Angels)
            angels.next()
        else
            orcs.next()

        bModels = sc.broadcast(models)
        aAngels = sc.accumulator(angels.next())
        aOrcs   = sc.accumulator(orcs.next())

        val vertices = sc.parallelize(
            for (a <- actors) yield (a.id, a))

        val edges    = sc.parallelize(for {
            a1 <- actors
            a2 <- actors
            if a1 != a2
        } yield Edge(a1.id, a2.id, ()))

        graph = Graph(vertices, edges)
    }

    // Plays a turn
    private def updateActors(): Unit = {
        if (graph.vertices.count <= 1) {
            finished.set(true)
            return
        }

        val actor = graph.vertices.first()._2
        actor.life.kill()
    }

    // Get a list of actors from the graph actors (with those dead)
    private def copyActors(): List[Actor] = {
        graph.vertices.values.collect().toList
    }

}
