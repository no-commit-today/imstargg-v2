package com.imstargg.core.application.renewal

import com.imstargg.core.application.brawlstars.BrawlStarsApiMaintenanceRepository
import com.imstargg.core.application.brawlstars.ClubRepository
import com.imstargg.core.application.brawlstars.PlayerRepository
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.renewal.Renewal
import com.imstargg.core.enums.RenewalType
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class RenewalService(
    private val renewalRepository: RenewalRepository,
    private val renewalProducer: RenewalMessageProducer,
    private val brawlStarsApiMaintenanceRepository: BrawlStarsApiMaintenanceRepository,
    private val playerRepository: PlayerRepository,
    private val clubRepository: ClubRepository,
    private val clock: Clock,
) {
    companion object {
        private val log = LoggerFactory.getLogger(RenewalService::class.java)
    }

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

        val now = clock.instant()
        val renewal = renewalRepository.request(tag, type, now)

        // worker 는 PENDING 이 아닌 renewal 의 메시지를 버리므로, PENDING 커밋이 발행보다 반드시 앞서야 한다.
        // (발행이 먼저면 worker 가 REQUESTED 를 읽고 메시지를 버려 영구 PENDING 으로 남는 레이스 발생)
        val pending = renewalRepository.toPending(renewal)

        publishAction()

        log.info("[갱신 요청] type={} tag={}", type, tag.value)
        return pending
    }
}
