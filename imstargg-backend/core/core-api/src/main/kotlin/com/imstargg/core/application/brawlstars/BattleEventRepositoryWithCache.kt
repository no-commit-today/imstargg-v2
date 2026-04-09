package com.imstargg.core.application.brawlstars

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.brawlstars.event.RotationBattleEvent
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventRotationItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventRotationJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventSoloRankEventJpaRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class BattleEventRepositoryWithCache(
    private val battleEventRepository: BattleEventRepository,
    private val battleEventRotationJpaRepository: BattleEventRotationJpaRepository,
    private val battleEventRotationItemJpaRepository: BattleEventRotationItemJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
) {
    @Cacheable(cacheNames = [CacheNames.BATTLE_EVENT], key = "'v1:' + #id")
    fun find(id: BrawlStarsId): BattleEvent? = battleEventRepository.find(id)

    fun findAllCurrentRotationEvents(): List<RotationBattleEvent> {
        val latestRotation =
            battleEventRotationJpaRepository.findTopByOrderByCollectedAtDesc()
                ?: return emptyList()
        return battleEventRotationItemJpaRepository
            .findAllByBattleEventRotationId(latestRotation.id!!)
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
    }

    fun findAllSoloRankEventBrawlStarsIds(soloRankSeasonKey: String): List<BrawlStarsId> =
        battleEventSoloRankEventJpaRepository
            .findAllBySoloRankSeasonKey(soloRankSeasonKey)
            .map { BrawlStarsId(it.eventBrawlStarsId) }
}
