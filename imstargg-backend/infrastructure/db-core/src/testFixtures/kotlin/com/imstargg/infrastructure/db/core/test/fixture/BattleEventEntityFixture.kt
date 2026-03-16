package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.BrawlStarsEntityStatus
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity

class BattleEventEntityFixture {
    private var brawlStarsId: Long = 15000001L
    private var mode: BattleEventModeEntity? = null
    private var mapName: String? = "Hard Rock Mine"
    private var status: BrawlStarsEntityStatus = BrawlStarsEntityStatus.ACTIVE

    companion object {
        fun aBattleEventEntity() = BattleEventEntityFixture()
    }

    fun withBrawlStarsId(value: Long) = apply { brawlStarsId = value }

    fun withMode(value: BattleEventModeEntity?) = apply { mode = value }

    fun withMapBrawlStarsName(value: String?) = apply { mapName = value }

    fun withStatus(value: BrawlStarsEntityStatus) = apply { status = value }

    fun build() =
        BattleEventEntity(
            brawlStarsId = brawlStarsId,
            mode = mode,
            mapName = mapName,
            status = status,
        )
}
