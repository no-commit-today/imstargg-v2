package com.imstargg.core.enums

enum class BattleEventMode(
    override val code: String,
) : EnumCodeType {
    SOLO_SHOWDOWN("soloShowdown"),
    DUO_SHOWDOWN("duoShowdown"),
    TRIO_SHOWDOWN("trioShowdown"),
    BASKET_BRAWL("basketBrawl"),
    BIG_GAME("bigGame"),
    BOSS_FIGHT("bossFight"),
    BOT_DROP("botDrop"),
    BOUNTY("bounty"),
    BRAWL_BALL("brawlBall"),
    BRAWL_BALL_5V5("brawlBall5V5"),
    DUELS("duels"),
    GEM_GRAB("gemGrab"),
    GEM_GRAB_5V5("gemGrab5V5"),
    HOT_ZONE("hotZone"),
    HUNTERS("hunters"),
    HEIST("heist"),
    JELLYFISHING("jellyfishing"),
    SIEGE("siege"),
    KNOCKOUT("knockout"),
    KNOCKOUT_5V5("knockout5V5"),
    PAINT_BRAWL("paintBrawl"),
    PAYLOAD("payload"),
    PRESENT_PLUNDER("presentPlunder"),
    ROBO_RUMBLE("roboRumble"),
    TAKEDOWN("takedown"),
    TROPHY_ESCAPE("trophyEscape"),
    VOLLEY_BRAWL("volleyBrawl"),
    WIPEOUT("wipeout"),
    WIPEOUT_5V5("wipeout5V5"),
    LONE_STAR("loneStar"),
    SUPER_CITY_RAMPAGE("superCityRampage"),
    HOLD_THE_TROPHY("holdTheTrophy"),
    TROPHY_THIEVES("trophyThieves"),
    LAST_STAND("lastStand"),
    SNOWTEL_THIEVES("snowtelThieves"),
    PUMPKIN_PLUNDER("pumpkinPlunder"),
    GODZILLA_CITY_SMASH("godzillaCitySmash"),
    ZOMBIE_PLUNDER("zombiePlunder"),
    UNKNOWN("unknown"),
    ;

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(BattleEventMode::class.java)
        }

        fun showdowns(): Set<BattleEventMode> = setOf(SOLO_SHOWDOWN, DUO_SHOWDOWN, TRIO_SHOWDOWN)
    }
}
