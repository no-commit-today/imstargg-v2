package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.StatsPlayerStatus
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity

class StatsPlayerEntityFixture {
    private var brawlStarsTag: String = "#TEST12345"
    private var status = StatsPlayerStatus.ACTIVE

    companion object {
        fun aStatsPlayerEntity() = StatsPlayerEntityFixture()
    }

    fun withBrawlStarsTag(value: String) = apply { brawlStarsTag = value }

    fun withStatus(value: StatsPlayerStatus) = apply { status = value }

    fun build(): StatsPlayerEntity {
        val entity =
            StatsPlayerEntity(
                brawlStarsTag = brawlStarsTag,
            )
        if (status == StatsPlayerStatus.BATTLE_LOG_UNAVAILABLE) {
            entity.markBattleLogUnavailable()
        }
        return entity
    }
}
