package engine

import engine.Elements._

import scala.collection.mutable

@SerialVersionUID(103L)
class Health(val max: Float) extends Serializable {
    var current: Float = max
    val resistances: mutable.Map[Elements.Value, Float] = mutable.Map(
        (Physic, 0),
        (Fire, 0),
        (Acid, 0),
        (Holy, 0))

    var dead:  Boolean = false
    def alive: Boolean = !dead

    def damage(value: Float, element: Elements.Value): Unit = {
        if (dead) return
        current -= value * (1 - resistances(element))
        if (current <= 0) {
            current = 0
            dead = true
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
    }

    def copy() : Health = {
        val h     = new Health(max)
        h.current = current
        h.dead    = dead
        resistances.foreach(pair => {
            h.resistances(pair._1) = pair._2
        })
        h
    }
}
