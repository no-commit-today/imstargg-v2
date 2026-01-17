package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity

class BattleEventEntityFixture {
    private var brawlStarsId: Long = 15000001L
    private var mode: String = "gemGrab"
    private var mapBrawlStarsName: String? = "Hard Rock Mine"
    private var soloRanked: Boolean = false

    companion object {
        fun aBattleEventEntity() = BattleEventEntityFixture()
    }

    fun withBrawlStarsId(value: Long) = apply { brawlStarsId = value }

    fun withMode(value: String) = apply { mode = value }

    fun withMapBrawlStarsName(value: String?) = apply { mapBrawlStarsName = value }

    fun withSoloRanked(value: Boolean) = apply { soloRanked = value }

    fun build() =
        BattleEventEntity(
            brawlStarsId = brawlStarsId,
            mode = mode,
            mapBrawlStarsName = mapBrawlStarsName,
            soloRanked = soloRanked,
        )
}
