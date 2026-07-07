package com.imstargg.core.worker.application.club

import com.imstargg.infrastructure.db.core.brawlstars.player.repository.ClubJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ClubDeactivator(
    private val clubRepository: ClubJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun deactivate(brawlStarsTag: String) {
        val club = clubRepository.findByBrawlStarsTag(brawlStarsTag)
        club?.toDeleted()
        log.info("[클럽 비활성화] tag={}", brawlStarsTag)
    }
}
