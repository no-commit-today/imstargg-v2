package com.imstargg.core.worker.application

import com.imstargg.core.worker.domain.Battle
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.TransientDataAccessException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

/**
 * 통계 배틀 집계 진입점 (동기). select-then-insert 레이스(유니크 충돌)와 일시적 DB 오류를
 * 짧은 재시도로 흡수한다. 재시도 소진 후의 예외는 호출측이 격리한다 — renewal 경로는
 * COMPLETED 유지 + error 로그(허용된 드문 유실, 대상 플레이어가 stats_player 에 있으면
 * 축적 스케줄러가 7일 내 재수집 가능), 축적 스케줄러 경로는 per-player catch.
 */
@Component
class StatsBattleAggregator(
    private val statsBattleAggregateApplier: StatsBattleAggregateApplier,
) {
    @Retryable(
        retryFor = [DataIntegrityViolationException::class, TransientDataAccessException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    fun aggregate(battles: List<Battle>) {
        statsBattleAggregateApplier.apply(battles)
    }
}
