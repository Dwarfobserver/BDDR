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

    private var bModels: Broadcast[Map[ActorType.Value, ActorModel]] = _

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

    // Create the actors, the broadcast variables and the graph
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

        // Count the angels & arcs :
        // If one side is empty, stop the engine
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

        // Give to each actor his best target
        val targets = graph.aggregateMessages[(Actor, Actor, VertexId, Double, Double)](

            // Send messages
            triplet => {
                val target = triplet.srcAttr
                val actor  = triplet.dstAttr
                if (actor.life.alive && target.life.alive) {

                    val srcSide = ActorModel.from(target.t).side
                    val dstSide = ActorModel.from(actor.t).side

                    if (srcSide != dstSide) {

                        val dstPos = actor.pos
                        val srcPos = target.pos
                        val distance = Math.sqrt(Math.pow(dstPos._1 - srcPos._1, 2) + Math.pow(dstPos._2 - srcPos._2, 2))

                        triplet.sendToDst((
                            actor,
                            target,
                            triplet.srcId,
                            distance,
                            target.life.current))
                    }
                }
            },

            // Aggregate messages
            (a, b) => {
                val hostilityA = 10000/(Math.min(Math.max(1, a._4), 100) * Math.min(a._5,100))
                val hostilityB = 10000/(Math.min(Math.max(1, b._4), 100) * Math.min(b._5,100))

                if(hostilityA > hostilityB) a else b
            })

        class Message(val actor: Actor,
                      val target: Actor,
                      val targetId: VertexId,
                      val distance: Double,
                      val pv: Double)
            extends Serializable

        class MsgData extends Serializable
        case class MoveData(id: VertexId, actor: Actor, pos: (Float, Float)) extends MsgData
        case class HurtData(id: VertexId,target: Actor, dmg: Float)          extends MsgData

        // Make each actor attack or move towards his target
        val newActors = targets.collect()
            .map(tuple => (tuple._1, new Message(tuple._2._1, tuple._2._2, tuple._2._3, tuple._2._4, tuple._2._5)))
            .map(info => {
                val actorId = info._1
                val msg = info._2
                val actorModel  = bModels.value(msg.actor.t)
                val targetModel = bModels.value(msg.target.t)
                val attack = actorModel.actions(ActionId.Attack).asInstanceOf[actions.Attack]

                // Take distance and approach actor
                if (msg.distance - (actorModel.size + targetModel.size) > 2) {

                    val destinationDistance = msg.distance - (actorModel.size + targetModel.size) - 2
                    var direction = (msg.actor.pos._1 - msg.target.pos._1, msg.actor.pos._2 - msg.target.pos._2)
                    val directionNorme = Math.sqrt(direction._1 * direction._1 + direction._2 * direction._2).toFloat
                    direction = (direction._1 / directionNorme, direction._2 / directionNorme)
                    val travelDistance = Math.min(destinationDistance, 50)
                    val finalDestination = (msg.target.pos._1 + direction._1 * travelDistance, msg.target.pos._2 + direction._2 * travelDistance)

                    val floatPos = (finalDestination._1.toFloat, finalDestination._2.toFloat)
                    MoveData(actorId, msg.actor, floatPos)
                }
                else
                    HurtData(msg.targetId, msg.target, attack.damages)
            })
            .map {
                case MoveData(id, actor, pos) =>
                    actor.pos = pos
                    (id, actor)

                case HurtData(id, target, dmg) =>
                    target.life.damage(dmg, Elements.Physic)
                    (id, target)
            }

            graph = graph.joinVertices(sc.parallelize(newActors))((_, _, actor) => actor)
    }

    // Get a list of actors from the graph actors (with those dead)
    private def copyActors(): List[Actor] = {
        graph.vertices.values.collect().toList
    }
}
