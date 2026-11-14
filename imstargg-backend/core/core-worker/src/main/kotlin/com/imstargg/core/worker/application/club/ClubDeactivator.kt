package com.imstargg.core.worker.application.club

import com.imstargg.infrastructure.db.core.brawlstars.player.repository.ClubJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class ClubDeactivator(
    private val clubRepository: ClubJpaRepository,
) {
    @Transactional
    fun deactivate(brawlStarsTag: String) {
        val club = clubRepository.findByBrawlStarsTag(brawlStarsTag)
        club?.toDeleted()
        log.info { "[클럽 비활성화] tag=$brawlStarsTag" }
    }
}
