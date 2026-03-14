package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity

class StatsPlayerEntityFixture {
    private var brawlStarsTag: String = "#TEST12345"

    companion object {
        fun aStatsPlayerEntity() = StatsPlayerEntityFixture()
    }

    fun withBrawlStarsTag(value: String) = apply { brawlStarsTag = value }

    fun build() =
        StatsPlayerEntity(
            brawlStarsTag = brawlStarsTag,
        )
}
