package common


object ActorType extends Enumeration {
    val Solar:        Value = Value("Solar")
    val Planetar:     Value = Value("Planetar")
    val MovanicDeva:  Value = Value("Movanic Deva")
    val AstralDeva:   Value = Value("Astral Deva")
    val OrcBarbarian: Value = Value("Orc Barbarian")
    val GreenDragon:  Value = Value("Green Dragon")
    val AngelSlayer:  Value = Value("Angel Slayer")
    val WorgRider:    Value = Value("Worg Rider")
    val Warlord:      Value = Value("Warlord")
}

object ActorSide extends Enumeration {
    val Angels: Value = Value("Angels")
    val Orcs:   Value = Value("Orcs")
}
