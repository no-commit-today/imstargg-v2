package com.imstargg.core.worker.application.stats

import com.imstargg.core.worker.domain.Battle
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * 통계 배틀 집계 진입점. renewal 경로는 비동기(asyncAggregate)로 호출해 SQS 리스너 스레드를 빠르게
 * 반환시키고, 축적 스케줄러는 이미 백그라운드 루프이므로 동기(aggregate)로 호출한다.
 *
 * 재시도는 별도 빈([StatsBattleRetryableAggregator])에 위임한다 — @Async 와 @Retryable 은 모두
 * 프록시 기반이라 한 클래스 안에 함께 두면 self-invocation 으로 재시도 어드바이스가 적용되지 않는다.
 */
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
