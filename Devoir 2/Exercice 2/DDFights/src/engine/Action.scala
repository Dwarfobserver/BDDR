package engine

abstract class Action(val id: ActionId.Value) {
    def priority(actor: Actor) : Float
    def execute (actor: Actor) : Unit
    def update() : Unit
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
    val Move:   ActionId.Value = Value("Move")
    val Attack: ActionId.Value = Value("Attack")
}
