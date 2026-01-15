package com.imstargg.core.enums

enum class BattleResult(
    override val code: String,
) : EnumCodeType {
    DEFEAT("defeat"),
    VICTORY("victory"),
    DRAW("draw"),
    ;

    fun opposite(): BattleResult =
        when (this) {
            DEFEAT -> VICTORY
            VICTORY -> DEFEAT
            DRAW -> DRAW
        }

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(BattleResult::class.java)
        }
    }
}
