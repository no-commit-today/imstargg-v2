package com.imstargg.core.enums

enum class BattleMode(
    override val code: String,
) : EnumCodeType {
    NOT_FOUND("notFound"),

    KNOCKOUT("knockout"),
    GEM_GRAB("gemGrab"),
    HEIST("heist"),
    HOT_ZONE("hotZone"),
    SIEGE("siege"),
    BOUNTY("bounty"),
    BRAWL_BALL("brawlBall"),
    DUELS("duels"),
    SOLO_SHOWDOWN("soloShowdown"),
    DUO_SHOWDOWN("duoShowdown"),
    WIPEOUT("wipeout"),
    VOLLEY_BRAWL("volleyBrawl"),
    TROPHY_THIEVES("trophyThieves"),
    BOSS_FIGHT("bossFight"),
    BIG_GAME("bigGame"),
    BASKET_BRAWL("basketBrawl"),
    ROBO_RUMBLE("roboRumble"),
    PAYLOAD("payload"),
    TAKEDOWN("takedown"),
    HUNTERS("hunters"),
    BOT_DROP("botDrop"),
    HOLD_THE_TROPHY("holdTheTrophy"),
    ;

    override val title: String
        get() = code
}
