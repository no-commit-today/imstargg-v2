package com.imstargg.core.worker.application.player

import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class PlayerDeactivator(
    private val playerRepository: PlayerJpaRepository,
    private val statsPlayerRepository: StatsPlayerJpaRepository,
) {
    @Transactional
    fun deactivate(brawlStarsTag: String) {
        val player = playerRepository.findByBrawlStarsTag(brawlStarsTag)
        player?.toDeleted()
        statsPlayerRepository.deleteByBrawlStarsTag(brawlStarsTag)
        log.info { "[플레이어 비활성화] tag=$brawlStarsTag" }
    }
}
