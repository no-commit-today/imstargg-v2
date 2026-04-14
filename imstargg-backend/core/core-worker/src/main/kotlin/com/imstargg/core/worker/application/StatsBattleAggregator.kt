package com.imstargg.core.worker.application

import com.imstargg.core.worker.domain.Battle
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class StatsBattleAggregator(
    private val statsBattleAggregateApplier: StatsBattleAggregateApplier,
) {
    @Async
    @Retryable(
        retryFor = [DataIntegrityViolationException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    fun asyncAggregate(battles: List<Battle>) {
        doAggregate(battles)
    }

    @Retryable(
        retryFor = [DataIntegrityViolationException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    fun aggregate(battles: List<Battle>) {
        doAggregate(battles)
    }

    private fun doAggregate(battles: List<Battle>) {
        statsBattleAggregateApplier.apply(battles)
    }
}
