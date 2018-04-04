package engine.actions

import engine.{Action, ActionId, Actor}

class Move(speed: Float) extends Action(ActionId.Move) {
    override def priority(actor: Actor): Float = 0.5f

    override def execute(actor: Actor): Unit = ()

    override def update(): Unit = ()
}
object Move {
    Action.factories += (kv = (ActionId.Move, (jSpell) => {
        val speed = jSpell("speed").asInstanceOf[Double].toFloat
        new Move(speed)
    }))
}
