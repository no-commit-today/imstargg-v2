package com.imstargg.core.application.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.renewal.Renewal
import com.imstargg.core.domain.renewal.RenewalCreate
import com.imstargg.core.enums.RenewalType
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime

@Service
class RenewalService(
    private val renewalRepository: RenewalRepository,
    private val renewalPublisher: RenewalPublisher,
    private val clock: Clock,
) {
    fun getPlayerRenewal(tag: BrawlStarsTag): Renewal =
        renewalRepository.find(tag, RenewalType.PLAYER)
            ?: throw CoreException(ErrorType.NOT_FOUND, "tag=$tag")

    fun getClubRenewal(tag: BrawlStarsTag): Renewal =
        renewalRepository.find(tag, RenewalType.CLUB)
            ?: throw CoreException(ErrorType.NOT_FOUND, "tag=$tag")

    fun renewPlayer(tag: BrawlStarsTag): Renewal =
        doRenew(tag, RenewalType.PLAYER) {
            renewalPublisher.publishPlayerRenewal(tag)
        }

    fun renewClub(tag: BrawlStarsTag): Renewal =
        doRenew(tag, RenewalType.CLUB) {
            renewalPublisher.publishClubRenewal(tag)
        }

    private fun doRenew(
        tag: BrawlStarsTag,
        type: RenewalType,
        publishAction: () -> Unit,
    ): Renewal {
        val now = OffsetDateTime.now(clock)
        val existing = renewalRepository.find(tag, type)

        if (existing != null && !existing.canRenew(now)) {
            throw CoreException(ErrorType.RENEWAL_ALREADY_IN_PROGRESS, "tag=$tag, type=$type")
        }

        val renewalCreate = RenewalCreate(tag, type, now)
        val renewal = renewalRepository.create(renewalCreate)

        publishAction()

        val pendingRenewal = renewal.toPending()
        renewalRepository.update(pendingRenewal)

        return pendingRenewal
    }
}
