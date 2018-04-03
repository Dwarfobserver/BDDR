package engine

class Actor(model: ActorModel) {
    val life: Health = new Health(model.life)
    val actions: ActionMap = new ActionMap
    var energy: Float = model.energy
    var range: Float = _

    def update(): Unit = {
        life.heal(model.regeneration)
        energy = model.energy
    }
}
