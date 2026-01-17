package com.imstargg.infrastructure.db.core.renewal

import com.imstargg.core.enums.RenewalType
import org.springframework.data.jpa.repository.JpaRepository

interface RenewalJpaRepository : JpaRepository<RenewalEntity, Long> {
    fun findTopByBrawlStarsTagAndTypeOrderByRequestedAtDesc(
        brawlStarsTag: String,
        type: RenewalType,
    ): RenewalEntity?
}
