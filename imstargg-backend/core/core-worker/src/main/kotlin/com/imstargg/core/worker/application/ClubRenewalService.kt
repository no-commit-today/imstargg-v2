package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
class ClubRenewalService(
    private val brawlStarsClient: BrawlStarsClient,
    private val clubRenewalFinder: ClubRenewalFinder,
    private val clubRenewalProcessor: ClubRenewalProcessor,
    private val clubUpdater: ClubUpdater,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun renew(brawlStarsTag: String) {
        var renewal = clubRenewalFinder.findByTag(brawlStarsTag)

        if (renewal.status != RenewalStatus.PENDING) {
            log.info("[클럽 갱신] 이미 처리됨 tag={}, status={}", brawlStarsTag, renewal.status)
            return
        }

        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            updateRenewalStatus(renewal) { clubRenewalProcessor.fail(it) }
            log.info("[클럽 갱신] API 점검 중 tag={}", brawlStarsTag)
            return
        }

        try {
            renewal = clubRenewalProcessor.executing(renewal)
        } catch (ex: ObjectOptimisticLockingFailureException) {
            log.warn("[클럽 갱신] 중복 처리 감지 tag={}", brawlStarsTag, ex)
            return
        }

        try {
            val clubResponse = brawlStarsClient.getClub(brawlStarsTag)
            clubUpdater.update(clubResponse)
            updateRenewalStatus(renewal) { clubRenewalProcessor.complete(it) }
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            updateRenewalStatus(renewal) { clubRenewalProcessor.notFound(it) }
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[클럽 갱신] ServiceUnavailable tag={}", brawlStarsTag, ex)
            brawlStarsApiMaintenanceAutoRegistrar.register()
            updateRenewalStatus(renewal) { clubRenewalProcessor.fail(it) }
        } catch (ex: Exception) {
            log.error("[클럽 갱신] 실패 tag={}", brawlStarsTag, ex)
            updateRenewalStatus(renewal) { clubRenewalProcessor.fail(it) }
        }
    }

    private fun updateRenewalStatus(
        renewal: RenewalEntity,
        action: (RenewalEntity) -> Unit,
    ): Boolean =
        try {
            action(renewal)
            true
        } catch (ex: ObjectOptimisticLockingFailureException) {
            log.warn("[클럽 갱신] 갱신 상태 업데이트 중 버전 충돌 tag={}", renewal.brawlStarsTag, ex)
            false
        }
}
