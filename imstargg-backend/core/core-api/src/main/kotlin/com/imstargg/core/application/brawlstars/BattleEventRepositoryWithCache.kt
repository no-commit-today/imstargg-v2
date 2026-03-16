package com.imstargg.core.application.brawlstars

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.brawlstars.event.RotationBattleEvent
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventSoloRankEventJpaRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class BattleEventRepositoryWithCache(
    private val battleEventRepository: BattleEventRepository,
    private val battleEventRotationItemJpaRepository: BattleEventRotationItemJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
) {
    @Cacheable(cacheNames = [CacheNames.BATTLE_EVENT], key = "#id.value")
    fun find(id: BrawlStarsId): BattleEvent? = battleEventRepository.find(id)

    fun findAllCurrentRotationEvents(now: OffsetDateTime): List<RotationBattleEvent> =
        battleEventRotationItemJpaRepository
            .findAllByEndTimeAfter(now)
            .distinctBy { it.eventBrawlStarsId }
            .mapNotNull { item ->
                find(BrawlStarsId(item.eventBrawlStarsId))?.let { event ->
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
