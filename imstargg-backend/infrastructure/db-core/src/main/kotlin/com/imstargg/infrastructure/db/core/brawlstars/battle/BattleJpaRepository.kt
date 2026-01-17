package com.imstargg.infrastructure.db.core.brawlstars.battle

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface BattleJpaRepository : JpaRepository<BattleEntity, Long> {
    fun findTopByPlayerPlayerIdOrderByBattleAtDesc(playerId: Long): BattleEntity?

    fun findSliceByPlayerPlayerIdAndDeletedFalseOrderByBattleAtDesc(
        playerId: Long,
        pageable: Pageable,
    ): Slice<BattleEntity>
}
