package com.imstargg.core.enums

enum class BattleResult(
    override val code: String,
) : EnumCodeType {
    DEFEAT("defeat"),
    VICTORY("victory"),
    DRAW("draw"),
    ;

    override val title: String
        get() = code

    fun opposite(): BattleResult =
        when (this) {
            DEFEAT -> VICTORY
            VICTORY -> DEFEAT
            DRAW -> DRAW
        }
}
