package com.imstargg.infrastructure.db.core.brawlstars.event.repository

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface BattleEventJpaRepository : JpaRepository<BattleEventEntity, Long> {
    fun findByBrawlStarsId(brawlStarsId: Long): BattleEventEntity?

    @EntityGraph(attributePaths = ["mode"])
    fun findAllByBrawlStarsIdIn(brawlStarsIds: Collection<Long>): List<BattleEventEntity>
}
