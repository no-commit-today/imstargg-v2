package com.imstargg.core.worker.application.renewal

import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import org.springframework.stereotype.Component

@Component
class RenewalFinder(
    private val renewalRepository: RenewalJpaRepository,
) {
    fun findByTag(
        brawlStarsTag: String,
        type: RenewalType,
    ): RenewalEntity =
        renewalRepository.findByBrawlStarsTagAndType(brawlStarsTag, type)
            ?: throw IllegalArgumentException("Renewal not found for tag: $brawlStarsTag, type: $type")
}
