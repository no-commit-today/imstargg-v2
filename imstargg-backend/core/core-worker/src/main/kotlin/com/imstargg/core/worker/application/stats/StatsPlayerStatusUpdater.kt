package com.imstargg.core.worker.application.stats

import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class StatsPlayerStatusUpdater(
    private val statsPlayerRepository: StatsPlayerJpaRepository,
) {
    @Transactional
    fun markBattleLogUnavailable(statsPlayerId: Long) {
        statsPlayerRepository.findById(statsPlayerId).orElse(null)?.markBattleLogUnavailable()
    }
}
