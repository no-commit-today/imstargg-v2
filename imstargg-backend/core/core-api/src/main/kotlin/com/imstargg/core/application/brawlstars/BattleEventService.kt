package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.brawlstars.event.RotationBattleEvent
import com.imstargg.core.enums.SoloRankSeason
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime

@Service
@Transactional(readOnly = true)
class BattleEventService(
    private val battleEventRepository: BattleEventRepository,
    private val eventRepositoryWithCache: EventRepositoryWithCache,
    private val clock: Clock,
) {
    fun getCurrentRotationEvents(): List<RotationBattleEvent> {
        val now = OffsetDateTime.now(clock)
        return battleEventRepository.findAllCurrentRotationEvents(now)
    }

    fun getSoloRankEvents(): List<BattleEvent> {
        val season = SoloRankSeason.current(clock)
        return battleEventRepository
            .findAllSoloRankEventBrawlStarsIds(season.key)
            .mapNotNull { eventRepositoryWithCache.find(it) }
    }
}
