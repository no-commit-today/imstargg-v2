package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity

class BattleEventModeEntityFixture {
    private var brawlStarsId: Long = 1L
    private var brawlStarsName: String = "gemGrab"
    private var resultType: BattleEventModeResultType = BattleEventModeResultType.RESULT
    private var playerCounts: List<Int> = listOf(3, 3)
    private var color: String = "#2A2E38"

    companion object {
        fun aBattleEventModeEntity() = BattleEventModeEntityFixture()
    }

    fun withBrawlStarsId(value: Long) = apply { brawlStarsId = value }

    fun withBrawlStarsName(value: String) = apply { brawlStarsName = value }

    fun withResultType(value: BattleEventModeResultType) = apply { resultType = value }

    fun withPlayerCounts(value: List<Int>) = apply { playerCounts = value }

    fun withColor(value: String) = apply { color = value }

    fun build() =
        BattleEventModeEntity(
            brawlStarsId = brawlStarsId,
            brawlStarsName = brawlStarsName,
            resultType = resultType,
            playerCounts = playerCounts,
            color = color,
        )
}
