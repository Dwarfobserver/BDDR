package engine

abstract class Action(val id: ActionId.Value) {
    def priority(actor: Actor) : Float
    def execute (actor: Actor) : Unit
    def update() : Unit
}
object Action {
    var factories: Map[ActionId.Value, Map[String, Any] => Action] = _

    def from(id: ActionId.Value, jSpell: Map[String, Any]) : Action = factories(id)(jSpell)
}

class ActionMap {
    private var map: Map[ActionId.Value, Action] = Map()

    def add(action: Action): Unit = {
        if (map.contains(action.id)) throw new Exception("Tried to add two actions with the same id")
        map += (kv = (action.id, action))
    }
    def get(id: ActionId.Value): Option[Action] = {
        if (!map.contains(id))
             None
        else Some(map(id))
    }
}

object ActionId extends Enumeration {
    val Move:   Value = Value("Move")
    val Attack: Value = Value("Attack")
}
