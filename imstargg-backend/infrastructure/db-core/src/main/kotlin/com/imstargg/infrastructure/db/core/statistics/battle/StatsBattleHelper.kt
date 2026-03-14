package com.imstargg.infrastructure.db.core.statistics.battle

import java.time.Clock
import java.time.OffsetDateTime

object StatsBattleHelper {
    const val BATTLE_MAX_AGE_DAYS = 30L

    fun canAggregate(
        clock: Clock,
        battleAt: OffsetDateTime,
    ): Boolean {
        val now = OffsetDateTime.now(clock)
        return battleAt.isAfter(now.minusDays(BATTLE_MAX_AGE_DAYS))
    }
}
