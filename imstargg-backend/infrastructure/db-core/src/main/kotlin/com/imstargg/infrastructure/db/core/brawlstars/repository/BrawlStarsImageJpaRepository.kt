package com.imstargg.infrastructure.db.core.brawlstars.repository

import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageCode
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlStarsImageJpaRepository : JpaRepository<BrawlStarsImageEntity, Long> {
    fun findByCode(code: BrawlStarsImageCode): BrawlStarsImageEntity?

    fun findAllByCodeIn(codes: Collection<BrawlStarsImageCode>): List<BrawlStarsImageEntity>

    fun deleteByCode(code: BrawlStarsImageCode)
}
