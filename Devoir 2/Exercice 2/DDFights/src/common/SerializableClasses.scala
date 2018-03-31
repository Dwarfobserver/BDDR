package common

import engine.ActorModel

// Describes an actor in a scene configuration
class ActorSetup(val actorType: ActorType.Value, val pos: (Float, Float)) {
    override def toString: String = {
        actorType + " : ("
            + pos._1.asInstanceOf[Int] + ", "
            + pos._2.asInstanceOf[Int] + ")"
    }
}

// Describes a scene initial configuration
class SceneSetup(val actors: List[ActorSetup]) {

}

// Describes an actor in a scene
class ActorStatus(val actorType: ActorType.Value) {
    var life: Float = 1
    var pos: (Float, Float) = (0, 0)
}
object ActorStatus {
    // Makes an actor status from the initial actor configuration
    def from(setup: ActorSetup): ActorStatus = {
        val status = new ActorStatus(setup.actorType)
        val model = ActorModel.of(setup.actorType)

        status.pos  = setup.pos
        status.life = model.life
        // ...
        status
    }
}

// Describes a scene
class SceneStatus {
    var turnCount: Int = 1
    var actors: List[ActorStatus] = List[ActorStatus]()
}
