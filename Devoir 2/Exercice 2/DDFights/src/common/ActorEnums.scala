package common


object ActorType extends Enumeration {
    val Solar:        ActorType.Value = Value("Solar")
    val Planetar:     ActorType.Value = Value("Planetar")
    val MovanicDeva:  ActorType.Value = Value("Movanic Deva")
    val AstralDeva:   ActorType.Value = Value("Astral Deva")
    val OrcBarbarian: ActorType.Value = Value("Orc Barbarian")
    val GreenDragon:  ActorType.Value = Value("Green Dragon")
    val AngelSlayer:  ActorType.Value = Value("Angel Slayer")
    val WorgRider:    ActorType.Value = Value("Worg Rider")
    val Warlord:      ActorType.Value = Value("Warlord")
}

object ActorSide extends Enumeration {
    val Angels: ActorSide.Value = Value("Angels")
    val Orcs:   ActorSide.Value = Value("Orcs")
}
