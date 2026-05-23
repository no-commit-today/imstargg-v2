package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.brawlstars.event.RotationBattleEvent
import com.imstargg.core.enums.SoloRankSeason
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(readOnly = true)
class BattleEventService(
    private val battleEventRepository: BattleEventRepository,
    private val battleEventRepositoryWithCache: BattleEventRepositoryWithCache,
    private val clock: Clock,
) {
    fun get(id: BrawlStarsId): BattleEvent =
        battleEventRepositoryWithCache.find(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "id=$id")

    fun getCurrentRotationEvents(): List<RotationBattleEvent> = battleEventRepositoryWithCache.findAllCurrentRotationEvents()

    fun getSoloRankEvents(): List<BattleEvent> {
        val season = SoloRankSeason.current(clock)
        return battleEventRepositoryWithCache
            .findAllSoloRankEventBrawlStarsIds(season.key)
            .mapNotNull { battleEventRepositoryWithCache.find(it) }
    }

    fun search(name: String): List<BattleEvent> = battleEventRepository.searchByMapName(name)
}
