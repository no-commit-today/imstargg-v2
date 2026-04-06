package com.imstargg.core.worker.application

import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import org.springframework.stereotype.Component

@Component
class ClubRenewalFinder(
    private val renewalRepository: RenewalJpaRepository,
) {
    fun findByTag(brawlStarsTag: String): RenewalEntity =
        renewalRepository.findByBrawlStarsTagAndType(
            brawlStarsTag,
            RenewalType.CLUB,
        ) ?: throw IllegalArgumentException("Renewal not found for tag: $brawlStarsTag")
}
