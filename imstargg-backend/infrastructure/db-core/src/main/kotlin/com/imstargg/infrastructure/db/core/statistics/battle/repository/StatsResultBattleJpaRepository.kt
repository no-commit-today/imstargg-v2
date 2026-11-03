package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface StatsResultBattleJpaRepository :
    JpaRepository<StatsResultBattleEntity, Long>,
    StatsResultBattleJpaRepositoryCustom {
    fun existsByBattleDateAndEventBrawlStarsId(
        battleDate: LocalDate,
        eventBrawlStarsId: Long,
    ): Boolean
}
