package common

// Describes an actor in a scene configuration
@SerialVersionUID(100L)
class ActorSetup(val actorType: ActorType.Value, val pos: (Float, Float)) extends Serializable {
    override def toString: String = {
        actorType + " : ("
            + pos._1.toInt + ", "
            + pos._2.toInt + ")"
    }
}
