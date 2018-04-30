package engine

import common.ActorType


class Actor(val id:  Long,
            val t:   ActorType.Value,
            var pos: (Float, Float)) extends Serializable
{
    var life: Health = _

    def initialize(model: ActorModel): Unit = {
        life = new Health(model.life)
    }

    def copy() : Actor = {
        val a = new Actor(id, t, pos)
        a.life = life.copy()
        a
    }
}
