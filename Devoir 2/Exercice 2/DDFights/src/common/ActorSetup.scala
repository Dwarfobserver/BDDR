package common

// Describes an actor in a scene configuration
class ActorSetup(val actorType: ActorType.Value, val pos: (Float, Float)) {
    override def toString: String = {
        actorType + " : ("
            + pos._1.toInt + ", "
            + pos._2.toInt + ")"
    }
}
