package engine


import common.{ActorSide, ActorType}

import scala.collection.immutable
import scala.io.Source
import scala.util.parsing.json.JSON

// Lists all characteristics for a type of actor
// @SerialVersionUID(753951L)
class ActorModel(val actorType: ActorType.Value) extends Serializable {
    val name: String = actorType.toString
    var side: ActorSide.Value = _
    var life: Float = _
    var size: Float = _
    var energy: Float = _
    var initiative: Float = _
    var regeneration: Float = _
    var actions: Map[ActionId.Value, Action] = _
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
            (name, data)    <- jActors
            ExModel(model) = name
            ExJMap(jActor) = data
        } yield (model, jActor)

        // Fill each model with it's data

        for ((model, jActor) <- models) {
            model.side = ActorSide.withName(jActor("side").asInstanceOf[String])

            def jFloatOf(any: Any) = any.asInstanceOf[Double].toFloat

            model.life         = jFloatOf(jActor("life"))
            model.size         = jFloatOf(jActor("size"))
            model.energy       = jFloatOf(jActor("energy"))
            model.initiative   = jFloatOf(jActor("initiative"))
            model.regeneration = jFloatOf(jActor("regeneration"))

            val ExJMap(jSpells) = jActor("spells")
            model.actions = {
                for {
                    (key, value) <- jSpells
                    id = ActionId.withName(key)
                    ExJMap(jSpell) = value
                } yield (id, Action.from(id, jSpell))
            }.toMap

            map += (kv = (model.actorType, model))
        }
        map
    }

}
