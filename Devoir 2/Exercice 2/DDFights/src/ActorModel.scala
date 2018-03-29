
import scala.collection.immutable

// Lists all characteristics for a type of actor
class ActorModel(val actorType: ActorType.Value) {
    val name: String = actorType.toString
    var side: ActorSide.Value = ActorSide.Angels
    var life: Float = 1
    var size: Float = 1
}
object ActorModel {
    private var models: immutable.HashMap[ActorType.Value, ActorModel] = new immutable.HashMap

    // Access the model of the given actor type
    def of(actorType: ActorType.Value) = models(actorType)

    // Create all models

    val solar = new ActorModel(ActorType.Solar)
    solar.side = ActorSide.Angels
    solar.life = 100
    solar.size = 5
    models += (kv = (ActorType.Solar, solar))

    val orc = new ActorModel(ActorType.OrcBarbarian)
    solar.side = ActorSide.Orcs
    orc.life = 5
    orc.size = 1
    models += (kv = (ActorType.OrcBarbarian, orc))

    // ...
}
