package com.imstargg.core.worker.application

import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.RenewalJpaRepository
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class ClubRenewalProcessor(
    private val renewalRepository: RenewalJpaRepository,
    private val clock: Clock,
) {
    fun executing(brawlStarsTag: String): Long {
        val renewal = getEntityByTag(brawlStarsTag)
        renewal.toExecuting(clock)
        return renewal.id!!
    }

    fun complete(renewalId: Long) {
        val renewal = getEntityById(renewalId)
        renewal.toCompleted(clock)
    }

    fun notFound(renewalId: Long) {
        val renewal = getEntityById(renewalId)
        renewal.toNotFound(clock)
    }

    fun fail(renewalId: Long) {
        val renewal = getEntityById(renewalId)
        renewal.toFailed(clock)
    }

    private fun getEntityByTag(brawlStarsTag: String) =
        renewalRepository.findTopByBrawlStarsTagAndTypeOrderByRequestedAtDesc(
            brawlStarsTag,
            RenewalType.CLUB,
        ) ?: throw IllegalArgumentException("Renewal not found for tag: $brawlStarsTag")

    private fun getEntityById(renewalId: Long) =
        renewalRepository
            .findById(renewalId)
            .orElseThrow { IllegalArgumentException("Renewal not found for id: $renewalId") }
}
