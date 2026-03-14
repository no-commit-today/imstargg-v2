package com.imstargg.core.enums

enum class StatsTierRange {
    TROPHY_1000_PLUS,
    TROPHY_2000_PLUS,
    TROPHY_3000_PLUS,
    SOLO_RANK_MYTHIC_PLUS,
    SOLO_RANK_LEGENDARY_PLUS,
    SOLO_RANK_MASTER_PLUS,
    ;

    companion object {
        const val MIN_TROPHY = 1000
        val MIN_SOLO_RANK_TIER = SoloRankTier.MYTHIC_1
    }
}
