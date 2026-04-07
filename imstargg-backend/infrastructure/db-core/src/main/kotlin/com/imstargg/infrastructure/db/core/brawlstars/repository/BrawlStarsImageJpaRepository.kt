package com.imstargg.infrastructure.db.core.brawlstars.repository

import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlStarsImageJpaRepository : JpaRepository<BrawlStarsImageEntity, Long> {
    fun findByTypeAndBrawlStarsId(
        type: BrawlStarsImageType,
        brawlStarsId: Long,
    ): BrawlStarsImageEntity?

    fun findAllByTypeAndBrawlStarsIdIn(
        type: BrawlStarsImageType,
        brawlStarsIds: Collection<Long>,
    ): List<BrawlStarsImageEntity>

    fun findAllByTypeIn(types: Collection<BrawlStarsImageType>): List<BrawlStarsImageEntity>

    fun findAllByTypeInAndBrawlStarsIdIn(
        types: Collection<BrawlStarsImageType>,
        brawlStarsIds: Collection<Long>,
    ): List<BrawlStarsImageEntity>

    fun deleteByTypeAndBrawlStarsId(
        type: BrawlStarsImageType,
        brawlStarsId: Long,
    )
}
