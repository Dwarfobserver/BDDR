package engine

import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.broadcast.Broadcast
import org.apache.log4j.{Level, Logger}
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import common.{ActorSide, _}
import engine.actions.{Attack, Move}

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

        checkIfFinished()
        while (!mustStop.get && !finished.get) {
            updateActors()
            val actors = copyActors()
            channel.push(actors)
            checkIfFinished()
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

        val models = ActorType.values.map(t => (t, ActorModel.from(t))).toMap

        bModels = sc.broadcast(models)

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

    private def checkIfFinished(): Unit = {

        val (angels, orcs) = graph.vertices.collect()
            .filter(pair => {
                pair._2.life.alive
            })
            .map(pair => {
                val side = bModels.value(pair._2.t).side
                if (side == ActorSide.Angels)
                     (1, 0)
                else (0, 1)
            })
            .reduce((p1, p2) => (p1._1 + p2._1, p1._2 + p2._2))

        if (angels == 0 || orcs == 0)
            finished.set(true)
    }

    // Plays a turn
    private def updateActors(): Unit = {
        /*
        val targetId = graph.vertices
            .filter(_._2.life.alive)
            .map   (_._1)
            .reduce((a, b) => a max b)

        graph = graph.mapVertices((id, actor) => {
            if (id == targetId)
                actor.life.kill()
            actor
        })
        */

        val targets = graph.aggregateMessages[(VertexId, Double, Health)](
            //sendMessage
            triplet => {

                val dstPos = triplet.dstAttr.pos
                val srcPos = triplet.srcAttr.pos
                val distance = Math.sqrt(Math.pow(dstPos._1-srcPos._1, 2) + Math.pow(dstPos._2-srcPos._2, 2))
                val srcSide = ActorModel.from(triplet.srcAttr.t).side
                val dstSide = ActorModel.from(triplet.dstAttr.t).side

                if(srcSide != dstSide) triplet.sendToDst((triplet.srcId, distance, triplet.srcAttr.life))
            },

            //aggregate
            (a, b) => {

                val hostilityA = 10000/(Math.min(Math.max(1, a._2), 100) * Math.min(a._3.current,100))
                val hostilityB = 10000/(Math.min(Math.max(1, b._2), 100) * Math.min(b._3.current,100))

                if(hostilityA > hostilityB) a else b
            }
        )
    }

    // Get a list of actors from the graph actors (with those dead)
    private def copyActors(): List[Actor] = {
        graph.vertices.values.collect().toList
    }
}
