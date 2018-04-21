package engine

class Actor(val id: Long,
            val model: ActorModel,
            var pos: (Float, Float))
{
    val life: Health = new Health(model.life)
    val actions: ActionMap = new ActionMap
    var energy: Float = model.energy
    var range: Float = _
    var turnFinished: Boolean = false

    model.makeActions().foreach(action => actions.add(action))

    def copy(): Actor = {
        val a = new Actor(id, model, (pos._1, pos._2))
        life.copyTo(a.life)
        a.turnFinished = false
        a.energy = energy
        a.range  = range
        a
    }

    def update(): Unit = {
        life.heal(model.regeneration)
        energy = model.energy
    }

    def isWith(actor: Actor) : Boolean = {
        model.side == actor.model.side
    }
}
object Actor {
    var actors: Map[Long, Actor] = Map()

    def add(actor: Actor, id: Long): Unit = {
        if (actors.contains(id))
            throw new Exception("Tried to add a new actor with existing (id, graphNum) pair")

        actors += (kv = (id, actor))
    }

    def get(id: Long): Actor = actors(id)
}
