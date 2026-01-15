package com.imstargg.core.enums

enum class BattleType(
    override val code: String,
) : EnumCodeType {
    NOT_FOUND("notFound"),

    RANKED("ranked"),
    SOLO_RANKED("soloRanked"),
    FRIENDLY("friendly"),
    CHALLENGE("challenge"),
    TOURNAMENT("tournament"),
    CHAMPIONSHIP_CHALLENGE("championshipChallenge"),
    ;

    fun isRegular(): Boolean = this in REGULAR_TYPES

    fun isOfficial(): Boolean = this in OFFICIAL_TYPES

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(BattleType::class.java)
        }

        private val REGULAR_TYPES = setOf(RANKED, SOLO_RANKED)

        private val OFFICIAL_TYPES = setOf(RANKED, SOLO_RANKED, CHALLENGE, TOURNAMENT, CHAMPIONSHIP_CHALLENGE)

        fun regularTypes(): Set<BattleType> = REGULAR_TYPES

        fun officialTypes(): Set<BattleType> = OFFICIAL_TYPES
    }
}
