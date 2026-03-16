package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.BrawlStarsEntityStatus
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity

class BattleEventModeEntityFixture {
    private var brawlStarsId: Long = 1L
    private var brawlStarsName: String = "gemGrab"
    private var status: BrawlStarsEntityStatus = BrawlStarsEntityStatus.ACTIVE

    companion object {
        fun aBattleEventModeEntity() = BattleEventModeEntityFixture()
    }

    fun withBrawlStarsId(value: Long) = apply { brawlStarsId = value }

    fun withBrawlStarsName(value: String) = apply { brawlStarsName = value }

    fun withStatus(value: BrawlStarsEntityStatus) = apply { status = value }

    fun build() =
        BattleEventModeEntity(
            brawlStarsId = brawlStarsId,
            brawlStarsName = brawlStarsName,
            status = status,
        )
}
