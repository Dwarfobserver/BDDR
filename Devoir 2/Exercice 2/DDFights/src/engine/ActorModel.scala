package engine


import common.{ActorSide, ActorType}

import scala.collection.immutable

// Lists all characteristics for a type of actor
class ActorModel(val actorType: ActorType.Value) {
    val name: String = actorType.toString
    var side: ActorSide.Value = _
    var life: Float = _
    var size: Float = _
    var energy: Float = _
    var regeneration: Float = _
    var makeActions: () => List[Action] = _
}
object ActorModel {
    private var models: immutable.HashMap[ActorType.Value, ActorModel] = new immutable.HashMap

    // Access the model of the given actor type
    def of(actorType: ActorType.Value) = models(actorType)

    // Create all models

    // Solar
    val solar = new ActorModel(ActorType.Solar)
    solar.side = ActorSide.Angels
    solar.life = 100
    solar.size = 5
    solar.energy = 20
    solar.regeneration = 10
    solar.makeActions = () => {
        List(new actions.Move(20))
    }
    models += (kv = (ActorType.Solar, solar))

    // Barbarian orc
    val orc = new ActorModel(ActorType.OrcBarbarian)
    solar.side = ActorSide.Orcs
    orc.life = 5
    orc.size = 1
    solar.energy = 3
    solar.regeneration = 0
    orc.makeActions = () => {
        List(new actions.Move(10))
    }
    models += (kv = (ActorType.OrcBarbarian, orc))

    // ...
}
