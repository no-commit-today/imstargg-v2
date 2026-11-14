package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface StatsRankBattleJpaRepository :
    JpaRepository<StatsRankBattleEntity, Long>,
    StatsRankBattleJpaRepositoryCustom {
    fun existsByBattleDateAndEventBrawlStarsId(
        battleDate: LocalDate,
        eventBrawlStarsId: Long,
    ): Boolean
}
