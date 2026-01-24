package com.imstargg.core.enums

enum class BattleType(
    override val code: String,
) : EnumCodeType {
    RANKED("ranked"),
    SOLO_RANKED("soloRanked"),
    FRIENDLY("friendly"),
    CHALLENGE("challenge"),
    TOURNAMENT("tournament"),
    CHAMPIONSHIP_CHALLENGE("championshipChallenge"),
    ;

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(BattleType::class.java)
        }
    }
}
