package com.imstargg.infrastructure.db.core.brawlstars.event

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface BattleEventJpaRepository : JpaRepository<BattleEventEntity, Long> {
    fun findByBrawlStarsId(brawlStarsId: Long): BattleEventEntity?

    @EntityGraph(attributePaths = ["mode"])
    fun findAllByBrawlStarsIdIn(brawlStarsIds: Collection<Long>): List<BattleEventEntity>
}
