package com.imstargg.core.domain.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import java.time.Duration
import java.time.Instant

data class Renewal(
    val tag: BrawlStarsTag,
    val type: RenewalType,
    val status: RenewalStatus,
    val requestedAt: Instant,
    val executedAt: Instant?,
    val completedAt: Instant?,
) {
    companion object {
        private val STALE_THRESHOLD = Duration.ofSeconds(10)
    }

    fun canRenew(now: Instant): Boolean {
        if (status.isCompleted()) return true
        val elapsed = Duration.between(requestedAt, now)
        return elapsed > STALE_THRESHOLD
    }

    /**
     * 재요청 상태로 전이한 새 [Renewal] 을 반환한다.
     *
     * "이미 진행 중" 여부([canRenew])는 application 계층이 먼저 검증해 비즈니스 예외를 발생시키는 것이 의도다.
     * 이 메서드의 [check] 는 그 검증을 건너뛰고 호출했을 때 터지는 전제조건 방어선일 뿐이다
     * (정상 흐름에선 항상 통과하며, 위반 시 프로그래밍 오류를 뜻하는 IllegalStateException).
     */
    fun request(now: Instant): Renewal {
        check(canRenew(now)) { "renewal not renewable: status=$status, requestedAt=$requestedAt, now=$now" }
        return copy(
            status = RenewalStatus.REQUESTED,
            requestedAt = now,
            executedAt = null,
            completedAt = null,
        )
    }
}
