package com.imstargg.core.worker.application

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.statistics.StatsBattleHelper
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class StatsBattleAggregator(
    private val clock: Clock,
    private val statsBattleAggregateApplier: StatsBattleAggregateApplier,
) {
    @Async
    @Retryable(
        retryFor = [DataIntegrityViolationException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    fun asyncAggregate(battles: List<BattleEntity>) {
        doAggregate(battles)
    }

    @Retryable(
        retryFor = [DataIntegrityViolationException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    fun aggregate(battles: List<BattleEntity>) {
        doAggregate(battles)
    }

    private fun doAggregate(battles: List<BattleEntity>) {
        val aggregatableBattles = battles.filter { StatsBattleHelper.canAggregate(clock, it) }
        statsBattleAggregateApplier.apply(aggregatableBattles)
    }
}
