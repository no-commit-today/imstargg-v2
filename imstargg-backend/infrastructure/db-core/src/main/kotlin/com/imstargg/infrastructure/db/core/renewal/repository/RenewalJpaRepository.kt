package com.imstargg.infrastructure.db.core.renewal.repository

import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RenewalJpaRepository : JpaRepository<RenewalEntity, Long> {
    fun findTopByBrawlStarsTagAndTypeOrderByRequestedAtDesc(
        brawlStarsTag: String,
        type: RenewalType,
    ): RenewalEntity?
}
