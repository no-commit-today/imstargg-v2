package com.imstargg.core.application.renewal

import com.imstargg.core.application.brawlstars.BrawlStarsApiMaintenanceRepository
import com.imstargg.core.application.brawlstars.ClubRepository
import com.imstargg.core.application.brawlstars.PlayerRepository
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.renewal.Renewal
import com.imstargg.core.enums.RenewalType
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime

@Service
class RenewalService(
    private val renewalRepository: RenewalRepository,
    private val renewalProducer: RenewalMessageProducer,
    private val brawlStarsApiMaintenanceRepository: BrawlStarsApiMaintenanceRepository,
    private val playerRepository: PlayerRepository,
    private val clubRepository: ClubRepository,
    private val clock: Clock,
) {
    fun getPlayerRenewal(tag: BrawlStarsTag): Renewal =
        renewalRepository.find(tag, RenewalType.PLAYER)
            ?: throw CoreException(ErrorType.NOT_FOUND, "tag=$tag")

    fun getClubRenewal(tag: BrawlStarsTag): Renewal =
        renewalRepository.find(tag, RenewalType.CLUB)
            ?: throw CoreException(ErrorType.NOT_FOUND, "tag=$tag")

    fun renewPlayer(tag: BrawlStarsTag): Renewal {
        if (playerRepository.isDeleted(tag)) {
            throw CoreException(ErrorType.PLAYER_DELETED, "tag=$tag")
        }
        return doRenew(tag, RenewalType.PLAYER) {
            renewalProducer.producePlayerRenewal(tag)
        }
    }

    fun renewClub(tag: BrawlStarsTag): Renewal {
        if (clubRepository.isDeleted(tag)) {
            throw CoreException(ErrorType.CLUB_DELETED, "tag=$tag")
        }
        return doRenew(tag, RenewalType.CLUB) {
            renewalProducer.produceClubRenewal(tag)
        }
    }

    private fun doRenew(
        tag: BrawlStarsTag,
        type: RenewalType,
        publishAction: () -> Unit,
    ): Renewal {
        if (brawlStarsApiMaintenanceRepository.isUnderMaintenance()) {
            throw CoreException(ErrorType.BRAWLSTARS_API_UNDER_MAINTENANCE)
        }

        val now = OffsetDateTime.now(clock)
        val existing = renewalRepository.find(tag, type)

        if (existing != null && !existing.canRenew(now)) {
            throw CoreException(ErrorType.RENEWAL_ALREADY_IN_PROGRESS, "tag=$tag, type=$type")
        }

        val renewal = renewalRepository.request(tag, type, now)

        publishAction()

        return renewalRepository.toPending(renewal)
    }
}
