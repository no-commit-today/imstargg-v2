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

        fun resolveAll(
            battleType: BattleType,
            tierValue: Int,
        ): List<StatsTierRange> =
            if (battleType == BattleType.SOLO_RANKED) {
                buildList {
                    if (tierValue >= SoloRankTier.MYTHIC_1.value) add(SOLO_RANK_MYTHIC_PLUS)
                    if (tierValue >= SoloRankTier.LEGENDARY_1.value) add(SOLO_RANK_LEGENDARY_PLUS)
                    if (tierValue >= SoloRankTier.MASTER_1.value) add(SOLO_RANK_MASTER_PLUS)
                }
            } else {
                buildList {
                    if (tierValue >= 1000) add(TROPHY_1000_PLUS)
                    if (tierValue >= 2000) add(TROPHY_2000_PLUS)
                    if (tierValue >= 3000) add(TROPHY_3000_PLUS)
                }
            }
    }
}
