package com.imstargg.core.worker.application.renewal

import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/**
 * Renewal 상태 전이 트랜잭션 경계. Player/Club 공통 — 유일한 차이였던 NOT_FOUND 시
 * 비활성화는 deactivate 액션으로 호출부가 주입한다 (상태 전이와 같은 트랜잭션에서 실행).
 */
@Component
class RenewalProcessor(
    private val renewalRepository: RenewalJpaRepository,
    private val clock: Clock,
) {
    @Transactional
    fun executing(renewal: RenewalEntity): RenewalEntity {
        renewal.toExecuting(clock)
        return renewalRepository.save(renewal)
    }

    @Transactional
    fun complete(renewal: RenewalEntity): RenewalEntity {
        renewal.toCompleted(clock)
        return renewalRepository.save(renewal)
    }

    @Transactional
    fun notFound(
        renewal: RenewalEntity,
        deactivate: () -> Unit,
    ): RenewalEntity {
        deactivate()
        renewal.toNotFound(clock)
        return renewalRepository.save(renewal)
    }

    @Transactional
    fun fail(renewal: RenewalEntity): RenewalEntity {
        renewal.toFailed(clock)
        return renewalRepository.save(renewal)
    }
}
