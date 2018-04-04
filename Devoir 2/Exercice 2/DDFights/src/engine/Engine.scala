package engine

import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.log4j.{Level, Logger}

import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

import common.{ActorStatus, Channel, SceneSetup, SceneStatus}

// The class which runs the fight
class Engine(val channel: Channel[SceneStatus], val setup: SceneSetup) {

    private val mustStop = new AtomicBoolean(false)
    private var task: Future[Unit] = _
    private var status: SceneStatus = _

    private [engine] var sc: SparkContext = _
    private [engine] var graph: Graph[Actor, Unit] = _

    // Start the engine asynchronously
    def start(): Unit = {
        status = new SceneStatus
        status.turnCount = 1
        status.actors =
            for { actor <- setup.actors }
            yield ActorStatus.from(actor)

        channel.push(status)

        task = Future { run() } (ExecutionContext.global)
    }

    // Ask the engine to stop, then wait him to finish
    def stop(): Unit = {
        mustStop.set(true)
        Await.result(task, 10.second)
    }

    private def run(): Unit = {
        sc    = Engine.createContext()
        graph = Engine.createGraph(sc, setup)

        var time = System.currentTimeMillis()
        while (!mustStop.get()) {
            val t2 = System.currentTimeMillis()
            if (t2 - time >= 1000) {
                time = t2
                channel.push(status)
            }
            Thread.sleep(10)
        }
    }

}
object Engine {

    private def createContext(): SparkContext = {
        System.setProperty("hadoop.home.dir", "C:/hadoop")
        val conf = new SparkConf().setAppName("DDFights").setMaster("local[*]")
        Logger.getRootLogger.setLevel(Level.WARN)
        new SparkContext(conf)
    }

    private def createGraph(sc: SparkContext, setup: SceneSetup): Graph[Actor, Unit] = {
        val ids = Iterator.from(1)
        val actors = for {
            info <- setup.actors
            model = ActorModel.of(info.actorType)
            actor = new Actor(model, ids.next(), info.pos)
        } yield (actor.id, actor)

        val links = List(Edge(1L, 2L, ()))

        val vertices = sc.parallelize(actors)
        val edges    = sc.parallelize(links)
        Graph(vertices, edges)
    }

}