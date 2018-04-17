package engine

import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.log4j.{Level, Logger}
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import common.{ActorSetup, Channel}

// The class which runs the fight
class Engine(val channel: Channel[List[Actor]], val setup: List[ActorSetup])
{
    private val mustStop = new AtomicBoolean(false)
    private var task: Future[Unit] = _

    // Start the engine asynchronously
    def start(): Unit = {
        task = Future { run() } (ExecutionContext.global)
    }

    def isFinished: Boolean = Engine.finished.get

    // Ask the engine to stop, then wait him to finish
    def stop(): Unit = {
        mustStop.set(true)
        Await.result(task, 10.second)
    }

    private def run(): Unit = {
        Engine.createContext()
        Engine.createGraph(setup)

        var time = System.currentTimeMillis()
        while (!mustStop.get && !Engine.finished.get) {
            val t2 = System.currentTimeMillis()
            if (t2 - time >= 1000) {
                println("update")
                time = t2
                Engine.updateActors()
                val actors = Engine.copyActors()
                Engine.updateGraph()
                channel.push(actors)
            }
            else Thread.sleep(10)
        }
    }

}
object Engine {
    private [engine] var sc: SparkContext = _
    private [engine] var graph: Graph[Unit, Unit] = _
    private val finished = new AtomicBoolean(false)
    private var turnCount = 1
    private var graphCount = 0

    // Create the Apache context
    private def createContext(): Unit = {
        System.setProperty("hadoop.home.dir", "C:/hadoop")
        val conf = new SparkConf().setAppName("DDFights").setMaster("local[*]")
        Logger.getRootLogger.setLevel(Level.WARN)
        sc = new SparkContext(conf)
    }

    // Create the actors and then the graph
    private def createGraph(setup: List[ActorSetup]): Unit = {
        val ids = Iterator.from(1)
        val actors = for {
            info <- setup
            model = ActorModel.from(info.actorType)
            actor = new Actor(ids.next(), model, info.pos)
            _ = Actor.add(actor, actor.id)
        } yield (actor.id, ())

        println("init actors count = " + actors.size)

        val links = for { // Making a clique
            a1 <- actors
            a2 <- actors
            if a1 != a2
        } yield Edge(a1._1, a2._1, ())

        val vertices = sc.parallelize(actors)
        val edges    = sc.parallelize(links)
        graph = Graph(vertices, edges)
    }

    // Plays a turn
    private def updateActors(): Unit = {
        if (graph.vertices.count == 1) {
            finished.set(true)
            return
        }
        val id = graph.vertices.first()._1
        Actor.get(id).life.kill()
    }

    // Get a list of actors from the graph actors (counting those dead in the last turn)
    private def copyActors(): List[Actor] = {
        val actors = List()
        graph.vertices.foreach(vertex =>
            actors :+ Actor.get(vertex._1).copy())
        println("actors count = " + actors.size)
        actors
    }

    // Remove the deads from the graph
    private def updateGraph(): Unit = {
        graph = graph.subgraph(
            edge    => {
                val a1 = Actor.get(edge.srcId)
                val a2 = Actor.get(edge.dstId)
                !a1.life.dead && !a2.life.dead
            },
            (id, _) => {
                val actor = Actor.get(id)
                !actor.life.dead
            })

        graphCount += 1
    }

}
