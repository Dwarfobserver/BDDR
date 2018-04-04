package engine

class Actor(val model: ActorModel,
            val id: Long,
            var pos: (Float, Float))
{
    val life: Health = new Health(model.life)
    val actions: ActionMap = new ActionMap
    var energy: Float = model.energy
    var range: Float = _

    def update(): Unit = {
        life.heal(model.regeneration)
        energy = model.energy
    }
}
