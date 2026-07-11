package com.imstargg.core.worker.application.player

import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PlayerDeactivator(
    private val playerRepository: PlayerJpaRepository,
    private val statsPlayerRepository: StatsPlayerJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun deactivate(brawlStarsTag: String) {
        val player = playerRepository.findByBrawlStarsTag(brawlStarsTag)
        player?.toDeleted()
        statsPlayerRepository.deleteByBrawlStarsTag(brawlStarsTag)
        log.info("[플레이어 비활성화] tag={}", brawlStarsTag)
    }
}
