package UI

import common.ActorSetup

@SerialVersionUID(100L)
class SceneSetup(
                        _actorList: List[ActorSetup],
                        _top: Double,
                        _left: Double,
                        _bottom: Double,
                        _right: Double
                ) extends Serializable{
    var actorList: List[ActorSetup] = _actorList
    var top: Double = _top
    var left: Double = _left
    var bottom: Double = _bottom
    var right: Double = _right

}
