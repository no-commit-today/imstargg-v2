package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.event.RotationBattleEvent
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventSoloRankEventJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Repository
@Transactional(readOnly = true)
class BattleEventRepository(
    private val battleEventRotationItemJpaRepository: BattleEventRotationItemJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
    private val eventRepositoryWithCache: EventRepositoryWithCache,
) {
    fun findAllCurrentRotationEvents(now: OffsetDateTime): List<RotationBattleEvent> =
        battleEventRotationItemJpaRepository
            .findAllByEndTimeAfter(now)
            .distinctBy { it.eventBrawlStarsId }
            .mapNotNull { item ->
                eventRepositoryWithCache.find(BrawlStarsId(item.eventBrawlStarsId))?.let { event ->
                    RotationBattleEvent(
                        event = event,
                        startTime = item.startTime,
                        endTime = item.endTime,
                    )
                }
            }

    fun findAllSoloRankEventBrawlStarsIds(soloRankSeasonKey: String): List<BrawlStarsId> =
        battleEventSoloRankEventJpaRepository
            .findBySoloRankSeasonKey(soloRankSeasonKey)
            .map { BrawlStarsId(it.eventBrawlStarsId) }
}
