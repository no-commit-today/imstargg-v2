package com.imstargg.core.worker.application

import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Component
class PlayerRenewalProcessor(
    private val renewalRepository: RenewalJpaRepository,
    private val playerDeactivator: PlayerDeactivator,
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
    fun notFound(renewal: RenewalEntity): RenewalEntity {
        playerDeactivator.deactivate(renewal.brawlStarsTag)
        renewal.toNotFound(clock)
        return renewalRepository.save(renewal)
    }

    @Transactional
    fun fail(renewal: RenewalEntity): RenewalEntity {
        renewal.toFailed(clock)
        return renewalRepository.save(renewal)
    }
}
