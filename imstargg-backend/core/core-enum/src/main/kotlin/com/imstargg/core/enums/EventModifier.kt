package com.imstargg.core.enums

enum class EventModifier(
    override val code: String,
) : EnumCodeType {
    UNKNOWN("unknown"),
    NONE("none"),
    ENERGY_DRINK("energyDrink"),
    ANGRY_ROBO("angryRobo"),
    METEOR_SHOWER("meteorShower"),
    GRAVEYARD_SHIFT("graveyardShift"),
    HEALING_MUSHROOMS("healingMushrooms"),
    BOSS_FIGHT_ROCKETS("bossFightRockets"),
    TAKEDOWN_LASERS("takedownLasers"),
    TAKEDOWN_CHAIN_LIGHTNING("takedownChainLightning"),
    TAKEDOWN_ROCKETS("takedownRockets"),
    WAVES("waves"),
    HAUNTED_BALL("hauntedBall"),
    SUPER_CHARGE("superCharge"),
    FAST_BRAWLERS("fastBrawlers"),
    SHOWDOWN_PLUS("showdown+"),
    PEEK_A_BOO("peekABoo"),
    BURNING_BALL("burningBall"),
    ;

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(EventModifier::class.java)
        }
    }
}
