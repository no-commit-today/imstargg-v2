package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import java.time.Clock
import java.time.OffsetDateTime

object StatsBattleHelper {
    const val BATTLE_MAX_AGE_DAYS = 30L

    fun canAggregate(
        clock: Clock,
        battle: BattleEntity,
    ): Boolean {
        val now = OffsetDateTime.now(clock)
        return battle.battleAt.isAfter(now.minusDays(BATTLE_MAX_AGE_DAYS))
    }
}
