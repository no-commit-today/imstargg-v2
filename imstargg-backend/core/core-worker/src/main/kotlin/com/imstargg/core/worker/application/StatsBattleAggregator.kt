package com.imstargg.core.worker.application

import com.imstargg.core.worker.domain.Battle
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class StatsBattleAggregator(
    private val statsBattleRetryableAggregator: StatsBattleRetryableAggregator,
) {
    @Async
    fun asyncAggregate(battles: List<Battle>) {
        statsBattleRetryableAggregator.aggregate(battles)
    }

    fun aggregate(battles: List<Battle>) {
        statsBattleRetryableAggregator.aggregate(battles)
    }
}
