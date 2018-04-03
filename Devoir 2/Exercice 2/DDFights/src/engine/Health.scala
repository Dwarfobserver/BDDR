package engine

import engine.Elements._

class Health(val max: Float) {
    var current: Float = max
    val resistances: Map[Elements.Value, Float] = Map(
        (Physic, 0),
        (Fire, 0),
        (Acid, 0),
        (Holy, 0))

    var dead: Boolean = false
    var onDeath: () => Unit = () => {}

    def damage(value: Float, element: Elements.Value): Unit = {
        if (dead) return
        current -= value * (1 - resistances(element))
        if (current <= 0) {
            current = 0
            dead = true
            onDeath()
        }
    }
    def heal(value: Float): Unit = {
        if (dead) return
        current += value
        if (current > max) {
            current = max
        }
    }
}
