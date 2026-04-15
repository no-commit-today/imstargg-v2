package com.imstargg.core.worker.application

import com.imstargg.core.worker.domain.Battle
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

@Component
class StatsBattleRetryableAggregator(
    private val statsBattleAggregateApplier: StatsBattleAggregateApplier,
) {
    @Retryable(
        retryFor = [DataIntegrityViolationException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    fun aggregate(battles: List<Battle>) {
        statsBattleAggregateApplier.apply(battles)
    }
}
