package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity

class BattleEventModeEntityFixture {
    private var brawlStarsId: Long = 1L
    private var brawlStarsName: String = "gemGrab"

    companion object {
        fun aBattleEventModeEntity() = BattleEventModeEntityFixture()
    }

    fun withBrawlStarsId(value: Long) = apply { brawlStarsId = value }

    fun withBrawlStarsName(value: String) = apply { brawlStarsName = value }

    fun build() =
        BattleEventModeEntity(
            brawlStarsId = brawlStarsId,
            brawlStarsName = brawlStarsName,
        )
}
