package com.imstargg.core.worker.application

import com.imstargg.core.worker.domain.BattleResult
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
    fun asyncAggregate(battleResults: List<BattleResult>) {
        doAggregate(battleResults)
    }

    @Retryable(
        retryFor = [DataIntegrityViolationException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    fun aggregate(battleResults: List<BattleResult>) {
        doAggregate(battleResults)
    }

    private fun doAggregate(battleResults: List<BattleResult>) {
        val aggregatableBattleResults = battleResults.filter { StatsBattleHelper.canAggregate(clock, it.battle) }
        statsBattleAggregateApplier.apply(aggregatableBattleResults)
    }
}
