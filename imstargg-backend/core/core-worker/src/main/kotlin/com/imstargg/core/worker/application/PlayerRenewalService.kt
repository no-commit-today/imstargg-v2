package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.worker.domain.BattleBuilder
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
class PlayerRenewalService(
    private val brawlStarsClient: BrawlStarsClient,
    private val playerRenewalFinder: PlayerRenewalFinder,
    private val playerRenewalProcessor: PlayerRenewalProcessor,
    private val playerUpdater: PlayerUpdater,
    private val statsBattleAggregator: StatsBattleAggregator,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun renew(brawlStarsTag: String) {
        var renewal = playerRenewalFinder.findByTag(brawlStarsTag)

        if (renewal.status != RenewalStatus.PENDING) {
            log.info("[플레이어 갱신] 이미 처리됨 tag={}, status={}", brawlStarsTag, renewal.status)
            return
        }

        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            updateRenewalStatus(renewal) { playerRenewalProcessor.fail(it) }
            log.info("[플레이어 갱신] API 점검 중 tag={}", brawlStarsTag)
            return
        }

        try {
            renewal = playerRenewalProcessor.executing(renewal)
        } catch (ex: ObjectOptimisticLockingFailureException) {
            log.warn("[플레이어 갱신] 중복 처리 감지 tag={}", brawlStarsTag, ex)
            return
        }

        try {
            val playerResponse = brawlStarsClient.getPlayerInformation(brawlStarsTag)
            val battleResponses = brawlStarsClient.getPlayerRecentBattles(brawlStarsTag).items
            val battles = battleResponses.map { BattleBuilder(playerResponse.tag, it).build() }

            val savedBattles = playerUpdater.update(playerResponse, battles)

            if (updateRenewalStatus(renewal) { playerRenewalProcessor.complete(it) }) {
                statsBattleAggregator.asyncAggregate(savedBattles)
            }
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            updateRenewalStatus(renewal) { playerRenewalProcessor.notFound(it) }
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[플레이어 갱신] ServiceUnavailable tag={}", brawlStarsTag, ex)
            brawlStarsApiMaintenanceAutoRegistrar.register()
            updateRenewalStatus(renewal) { playerRenewalProcessor.fail(it) }
        } catch (ex: Exception) {
            log.error("[플레이어 갱신] 실패 tag={}", brawlStarsTag, ex)
            updateRenewalStatus(renewal) { playerRenewalProcessor.fail(it) }
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
            log.warn("[플레이어 갱신] 갱신 상태 업데이트 중 버전 충돌 tag={}", renewal.brawlStarsTag, ex)
            false
        }
}
