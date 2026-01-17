package com.imstargg.core.worker.application

import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.RenewalJpaRepository
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class PlayerRenewalProcessor(
    private val renewalRepository: RenewalJpaRepository,
    private val clock: Clock,
) {
    fun process(brawlStarsTag: String) {
        val renewal = getEntity(brawlStarsTag)
        renewal.toExecuting(clock)
    }

    fun complete(brawlStarsTag: String) {
        val renewal = getEntity(brawlStarsTag)
        renewal.toCompleted(clock)
    }

    fun notFound(brawlStarsTag: String) {
        val renewal = getEntity(brawlStarsTag)
        renewal.toNotFound(clock)
    }

    fun fail(brawlStarsTag: String) {
        val renewal = getEntity(brawlStarsTag)
        renewal.toFailed(clock)
    }

    private fun getEntity(brawlStarsTag: String) =
        renewalRepository.findTopByBrawlStarsTagAndTypeOrderByRequestedAtDesc(
            brawlStarsTag,
            RenewalType.PLAYER,
        ) ?: throw NoSuchElementException("Renewal not found for tag: $brawlStarsTag")
}
