package engine


import common.{ActorSide, ActorType}

import scala.collection.immutable
import scala.io.Source
import scala.util.parsing.json.JSON

// Lists all characteristics for a type of actor
class ActorModel(val actorType: ActorType.Value) {
    val name: String = actorType.toString
    var side: ActorSide.Value = _
    var life: Float = _
    var size: Float = _
    var energy: Float = _
    var initiative: Float = _
    var regeneration: Float = _
    var makeActions: () => List[Action] = _
}
object ActorModel {

    // Access the model of the given actor type
    def from(actorType: ActorType.Value) = models(actorType)

    private val models: immutable.HashMap[ActorType.Value, ActorModel] = {

        // Open JSON with the common stats

        val jFile = Source.fromFile("actors.json")
        val jString = try jFile.mkString finally jFile.close()
        val Some(json) = JSON.parseFull(jString)

        // Extractors

        object ExJMap {
            def unapply(arg: Any): Option[Map[String, Any]] =
                Some(arg.asInstanceOf[Map[String, Any]])
        }
        object ExModel {
            def unapply(arg: String): Option[ActorModel] =
                Some(new ActorModel(ActorType.withName(arg)))
        }

        // Extract the models from the JSON

        var map = immutable.HashMap[ActorType.Value, ActorModel]()

        val models = for {
            ExJMap(jActors) <- List(json)
            (name, data)  <- jActors
            ExModel(model) = name
            ExJMap(jActor) = data
        } yield (model, jActor)

        for ((model, jActor) <- models) {
            model.side = ActorSide.withName(jActor("side").asInstanceOf[String])
            model.life         = jActor("life")        .asInstanceOf[Double].toFloat
            model.size         = jActor("size")        .asInstanceOf[Double].toFloat
            model.energy       = jActor("energy")      .asInstanceOf[Double].toFloat
            model.initiative   = jActor("initiative")  .asInstanceOf[Double].toFloat
            model.regeneration = jActor("regeneration").asInstanceOf[Double].toFloat

            val factories: List[() => Action] = List()
            val ExJMap(jSpells) = jActor("spells")
            for ((key, value) <- jSpells) {
                val id = ActionId.withName(key)
                val ExJMap(jSpell) = value
                factories :+ (() => Action.from(id, jSpell))
            }
            model.makeActions = () => for (f <- factories) yield f()

            map += (kv = (model.actorType, model))
        }
        map
    }

}
