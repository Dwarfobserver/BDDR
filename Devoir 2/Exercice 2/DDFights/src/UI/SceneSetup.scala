package UI

import common.ActorSetup

@SerialVersionUID(100L)
class SceneSetup(
        val actorList: List[ActorSetup],
        val top: Double,
        val left: Double,
        val bottom: Double,
        val right: Double) extends Serializable
{}
