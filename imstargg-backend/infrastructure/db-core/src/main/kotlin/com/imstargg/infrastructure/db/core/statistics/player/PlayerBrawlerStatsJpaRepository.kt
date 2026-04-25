package com.imstargg.infrastructure.db.core.statistics.player

import com.imstargg.core.enums.BattleType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface PlayerBrawlerStatsJpaRepository : JpaRepository<PlayerBrawlerStatsEntity, Long> {
    fun findByPlayerBrawlStarsTagAndBattleTypeAndStartBattleDate(
        playerBrawlStarsTag: String,
        battleType: BattleType,
        startBattleDate: LocalDate,
    ): PlayerBrawlerStatsEntity?
}
