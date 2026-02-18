package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity

class BattleEventModeEntityFixture {
    private var brawlStarsId: Long = 1L

    companion object {
        fun aBattleEventModeEntity() = BattleEventModeEntityFixture()
    }

    fun withBrawlStarsId(value: Long) = apply { brawlStarsId = value }

    fun build() =
        BattleEventModeEntity(
            brawlStarsId = brawlStarsId,
        )
}
