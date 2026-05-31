package com.imstargg.infrastructure.db.core.brawlstars.battle.repository

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface BattleJpaRepository :
    JpaRepository<BattleEntity, Long>,
    BattleJpaRepositoryCustom {
    fun findTopByPlayerIdOrderByBattleAtDesc(playerId: Long): BattleEntity?

    fun findSliceByPlayerIdOrderByBattleAtDesc(
        playerId: Long,
        pageable: Pageable,
    ): Slice<BattleEntity>
}
