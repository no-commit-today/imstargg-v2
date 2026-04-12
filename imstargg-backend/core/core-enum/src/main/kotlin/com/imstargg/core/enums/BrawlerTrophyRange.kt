package com.imstargg.core.enums

enum class BrawlerTrophyRange {
    TROPHY_500_PLUS,
    TROPHY_1000_PLUS,
    TROPHY_2000_PLUS,
    TROPHY_3000_PLUS,
    ;

    companion object {
        fun resolveAll(trophies: Int): List<BrawlerTrophyRange> =
            buildList {
                if (trophies >= 500) add(TROPHY_500_PLUS)
                if (trophies >= 1000) add(TROPHY_1000_PLUS)
                if (trophies >= 2000) add(TROPHY_2000_PLUS)
                if (trophies >= 3000) add(TROPHY_3000_PLUS)
            }
    }
}
