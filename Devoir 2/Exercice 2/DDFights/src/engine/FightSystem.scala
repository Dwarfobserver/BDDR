package engine

import common.ActorSide

import scala.collection.mutable


object FightSystem {

    def isRoundFinished(actors: List[Actor]): Boolean = {
        val angels = mutable.ArrayBuffer[Actor]()
        val orcs = mutable.ArrayBuffer[Actor]()
        actors.foreach(a =>
            if (a.life.alive && !a.turnFinished)
                a.model.side match {
                    case ActorSide.Angels => angels += a
                    case ActorSide.Orcs   => orcs   += a
                }
        )
        angels.isEmpty || orcs.isEmpty
    }

    def newRound(actors: List[Actor]): Unit = {
        actors.foreach(_.update())
    }

    def playTurn(actors: List[Actor]): Unit = {
        val actor = actors
            .filter(a => !a.turnFinished && a.life.alive)
            .maxBy(_.model.initiative)


    }



}
