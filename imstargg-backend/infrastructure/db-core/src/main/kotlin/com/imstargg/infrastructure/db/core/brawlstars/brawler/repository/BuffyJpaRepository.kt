package com.imstargg.infrastructure.db.core.brawlstars.brawler.repository

import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BuffyEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BuffyJpaRepository : JpaRepository<BuffyEntity, Long> {
    fun findAllByBrawlerBrawlStarsId(brawlerBrawlStarsId: Long): List<BuffyEntity>

    fun findAllByBrawlerBrawlStarsIdIn(brawlerBrawlStarsIds: Collection<Long>): List<BuffyEntity>

    fun findByBrawlerBrawlStarsIdAndType(
        brawlerBrawlStarsId: Long,
        type: BuffyType,
    ): BuffyEntity?

    fun deleteByBrawlerBrawlStarsIdAndType(
        brawlerBrawlStarsId: Long,
        type: BuffyType,
    )
}
