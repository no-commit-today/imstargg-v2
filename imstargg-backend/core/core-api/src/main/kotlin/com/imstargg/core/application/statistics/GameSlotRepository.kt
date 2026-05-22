package com.imstargg.core.application.statistics

import com.imstargg.core.application.brawlstars.BattleEventRepositoryWithCache
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.GameSlot
import com.imstargg.core.domain.statistics.GameSlots
import org.springframework.stereotype.Repository

@Repository
class GameSlotRepository(
    private val battleEventRepositoryWithCache: BattleEventRepositoryWithCache,
) {
    fun find(eventId: BrawlStarsId): GameSlot =
        battleEventRepositoryWithCache
            .find(eventId)
            ?.mode
            ?.playerCounts
            ?.let(::GameSlot)
            ?: GameSlot.default()

    fun findAllByEventIds(eventIds: List<BrawlStarsId>): GameSlots = GameSlots(eventIds.distinct().associateWith { find(it) })
}
