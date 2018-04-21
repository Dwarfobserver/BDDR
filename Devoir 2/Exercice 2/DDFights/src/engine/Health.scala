package engine

import engine.Elements._

import scala.collection.mutable

class Health(val max: Float) {
    var current: Float = max
    val resistances: mutable.Map[Elements.Value, Float] = mutable.Map(
        (Physic, 0),
        (Fire, 0),
        (Acid, 0),
        (Holy, 0))

    var dead: Boolean = false
    def alive: Boolean = !dead

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
    def kill(): Unit = {
        if (dead) return
        current = 0
        dead = true
        onDeath()
    }

    def copyTo(health: Health) : Unit = {
        resistances.foreach(pair => {
            health.resistances(pair._1) = pair._2
        })
        health.current = current
        health.dead = dead
    }
}
