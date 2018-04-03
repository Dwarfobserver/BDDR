package engine.actions

import engine.{Action, ActionId, Actor}

class Attack(val damages: Float) extends Action(ActionId.Attack) {
    override def priority(actor: Actor): Float = 0.5f

    override def execute(actor: Actor): Unit = ()

    override def update(): Unit = ()
}
