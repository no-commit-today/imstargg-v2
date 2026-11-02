package com.imstargg.core.worker.application.player

import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class PlayerDeactivator(
    private val playerRepository: PlayerJpaRepository,
) {
    @Transactional
    fun deactivate(brawlStarsTag: String) {
        val player = playerRepository.findByBrawlStarsTag(brawlStarsTag)
        player?.toDeleted()
        log.info { "[플레이어 비활성화] tag=$brawlStarsTag" }
    }
}
